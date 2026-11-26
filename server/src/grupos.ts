import { comoUsuario, type Usuario } from './db.js';

const FUNCOES: Record<string, string[]> = {
  entrar_no_grupo: ['p_codigo'],
  criar_grupo: ['p_nome', 'p_cidade'],
  sair_do_grupo: ['p_grupo'],
};

export class FuncaoDesconhecida extends Error {}

export async function chamarFuncao(
  usuario: Usuario,
  nome: string,
  argumentos: Record<string, unknown>,
): Promise<Record<string, unknown>[]> {
  const parametros = FUNCOES[nome];
  if (!parametros) throw new FuncaoDesconhecida(`Função desconhecida: ${nome}`);
  const usados = parametros.filter((parametro) => parametro in argumentos);
  const chamada = usados.map((parametro, indice) => `${parametro} => $${indice + 1}`).join(', ');
  const valores = usados.map((parametro) => argumentos[parametro] ?? null);
  const { rows } = await comoUsuario(usuario, (cliente) =>
    cliente.query(`select * from public.${nome}(${chamada})`, valores),
  );
  return rows;
}

const COLUNAS_DA_CHAVE = 'id, grupo_id, codigo, rotulo, papel, usos, usos_max, expira_em, ativa';

export const listarChaves = (usuario: Usuario, grupoId: string) =>
  comoUsuario(usuario, async (cliente) => {
    const { rows } = await cliente.query(
      `select ${COLUNAS_DA_CHAVE} from public.grupo_chaves where grupo_id = $1`,
      [grupoId],
    );
    return rows;
  });

export interface NovaChave {
  rotulo?: string | null;
  papel?: string;
  usos_max?: number | null;
  expira_em?: string | null;
}

export const criarChave = (usuario: Usuario, grupoId: string, chave: NovaChave) =>
  comoUsuario(usuario, async (cliente) => {
    const { rows } = await cliente.query(
      `insert into public.grupo_chaves (grupo_id, rotulo, papel, usos_max, expira_em)
       values ($1, $2, coalesce($3, 'atleta'), $4, $5)
       returning ${COLUNAS_DA_CHAVE}`,
      [grupoId, chave.rotulo ?? null, chave.papel ?? null, chave.usos_max ?? null, chave.expira_em ?? null],
    );
    return rows[0];
  });

export const definirChaveAtiva = (usuario: Usuario, chaveId: string, ativa: boolean) =>
  comoUsuario(usuario, (cliente) =>
    cliente.query('update public.grupo_chaves set ativa = $2 where id = $1', [chaveId, ativa]),
  );

export const excluirChave = (usuario: Usuario, chaveId: string) =>
  comoUsuario(usuario, (cliente) => cliente.query('delete from public.grupo_chaves where id = $1', [chaveId]));

export const listarMembros = (usuario: Usuario, grupoId: string) =>
  comoUsuario(usuario, async (cliente) => {
    const { rows } = await cliente.query(
      `select m.profile_id, m.papel, p.nome, p.email
       from public.grupo_membros m
       left join public.profiles p on p.id = m.profile_id
       where m.grupo_id = $1`,
      [grupoId],
    );
    return rows;
  });

export const definirPapel = (usuario: Usuario, grupoId: string, profileId: string, papel: string) =>
  comoUsuario(usuario, (cliente) =>
    cliente.query('update public.grupo_membros set papel = $3 where grupo_id = $1 and profile_id = $2', [
      grupoId,
      profileId,
      papel,
    ]),
  );

export const removerMembro = (usuario: Usuario, grupoId: string, profileId: string) =>
  comoUsuario(usuario, (cliente) =>
    cliente.query('delete from public.grupo_membros where grupo_id = $1 and profile_id = $2', [grupoId, profileId]),
  );

export const listarPagamentos = (usuario: Usuario, grupoId: string) =>
  comoUsuario(usuario, async (cliente) => {
    const { rows } = await cliente.query(
      `select id, cobranca_id, player_id, profile_id, valor_centavos, status, pago_em, observacao
       from public.pagamentos
       where grupo_id = $1`,
      [grupoId],
    );
    return rows;
  });
