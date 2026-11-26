import type pg from 'pg';
import { comoUsuario, identificador, pool, type Usuario } from './db.js';

export type TipoDeOperacao = 'PUT' | 'PATCH' | 'DELETE';

export interface Operacao {
  op: TipoDeOperacao;
  tabela: string;
  id: string;
  dados?: Record<string, unknown> | null;
}

export interface Descartada {
  indice: number;
  tabela: string;
  id: string;
  codigo: string;
  mensagem: string;
}

export class OperacaoInvalida extends Error {}

const CODIGOS_FATAIS = [/^22...$/, /^23...$/, /^42501$/, /^42703$/, /^P0001$/];

export const ehFatal = (codigo: string | undefined): boolean =>
  codigo !== undefined && CODIGOS_FATAIS.some((padrao) => padrao.test(codigo));

let colunasPorTabela: Promise<Map<string, Set<string>>> | null = null;

const carregarColunas = async (): Promise<Map<string, Set<string>>> => {
  const { rows } = await pool().query<{ tabela: string; coluna: string }>(
    `select c.table_name as tabela, c.column_name as coluna
     from pg_publication_tables p
     join information_schema.columns c
       on c.table_schema = p.schemaname and c.table_name = p.tablename
     where p.pubname = 'powersync' and p.schemaname = 'public'`,
  );
  const mapa = new Map<string, Set<string>>();
  for (const { tabela, coluna } of rows) {
    if (!mapa.has(tabela)) mapa.set(tabela, new Set());
    mapa.get(tabela)!.add(coluna);
  }
  return mapa;
};

const colunas = (): Promise<Map<string, Set<string>>> => {
  colunasPorTabela ??= carregarColunas().catch((erro) => {
    colunasPorTabela = null;
    throw erro;
  });
  return colunasPorTabela;
};

const montarComando = (
  operacao: Operacao,
  conhecidas: Set<string>,
): { sql: string; registro: Record<string, unknown> } | null => {
  const tabela = `public.${identificador(operacao.tabela)}`;
  const dados = operacao.dados ?? {};
  const nomes = Object.keys(dados).filter((nome) => nome !== 'id' && conhecidas.has(nome));
  const registro: Record<string, unknown> = { id: operacao.id };
  for (const nome of nomes) registro[nome] = dados[nome];
  const lista = nomes.map(identificador);

  switch (operacao.op) {
    case 'PUT': {
      const todas = ['"id"', ...lista];
      const conflito =
        lista.length === 0
          ? 'do nothing'
          : `do update set ${lista.map((nome) => `${nome} = excluded.${nome}`).join(', ')}`;
      return {
        sql: `insert into ${tabela} (${todas.join(', ')})
              select ${todas.join(', ')} from json_populate_record(null::${tabela}, $1::json)
              on conflict ("id") ${conflito}`,
        registro,
      };
    }
    case 'PATCH': {
      if (lista.length === 0) return null;
      return {
        sql: `update ${tabela} as alvo
              set ${lista.map((nome) => `${nome} = novo.${nome}`).join(', ')}
              from json_populate_record(null::${tabela}, $1::json) as novo
              where alvo."id" = novo."id"`,
        registro,
      };
    }
    case 'DELETE':
      return {
        sql: `delete from ${tabela} as alvo
              using json_populate_record(null::${tabela}, $1::json) as novo
              where alvo."id" = novo."id"`,
        registro: { id: operacao.id },
      };
    default:
      throw new OperacaoInvalida(`Operação desconhecida: ${String(operacao.op)}`);
  }
};

export async function aplicarOperacoes(
  usuario: Usuario,
  operacoes: Operacao[],
): Promise<{ aplicadas: number; descartadas: Descartada[] }> {
  const mapa = await colunas();
  for (const operacao of operacoes) {
    if (!mapa.has(operacao.tabela)) throw new OperacaoInvalida(`Tabela não sincronizada: ${operacao.tabela}`);
    if (typeof operacao.id !== 'string' || operacao.id.length === 0) throw new OperacaoInvalida('Operação sem id.');
  }

  return comoUsuario(usuario, async (cliente: pg.PoolClient) => {
    const descartadas: Descartada[] = [];
    let aplicadas = 0;

    for (const [indice, operacao] of operacoes.entries()) {
      const comando = montarComando(operacao, mapa.get(operacao.tabela)!);
      if (!comando) continue;

      await cliente.query('savepoint operacao');
      try {
        await cliente.query(comando.sql, [JSON.stringify(comando.registro)]);
        await cliente.query('release savepoint operacao');
        aplicadas += 1;
      } catch (erro) {
        const { code, message } = erro as { code?: string; message: string };
        if (!ehFatal(code)) throw erro;
        await cliente.query('rollback to savepoint operacao');
        descartadas.push({
          indice,
          tabela: operacao.tabela,
          id: operacao.id,
          codigo: code ?? '',
          mensagem: message,
        });
      }
    }

    return { aplicadas, descartadas };
  });
}
