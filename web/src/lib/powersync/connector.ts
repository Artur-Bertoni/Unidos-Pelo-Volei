import {
  UpdateType,
  type AbstractPowerSyncDatabase,
  type PowerSyncBackendConnector,
  type PowerSyncCredentials,
} from '@powersync/web';
import { env } from '../env';
import { supabase } from '../supabase';

/**
 * Códigos do Postgres que nunca vão passar numa nova tentativa: repetir a fila
 * travaria a sincronização para sempre, então a transação é descartada.
 */
const CODIGOS_FATAIS = [/^22...$/, /^23...$/, /^42501$/];

const ehFatal = (codigo: string | undefined): boolean =>
  codigo !== undefined && CODIGOS_FATAIS.some((padrao) => padrao.test(codigo));

export class SupabaseConnector implements PowerSyncBackendConnector {
  async fetchCredentials(): Promise<PowerSyncCredentials | null> {
    const {
      data: { session },
      error,
    } = await supabase.auth.getSession();

    if (error) throw new Error(error.message);
    if (!session) return null;

    return {
      endpoint: env.powersyncUrl,
      token: session.access_token,
    };
  }

  async uploadData(database: AbstractPowerSyncDatabase): Promise<void> {
    const transacao = await database.getNextCrudTransaction();
    if (!transacao) return;

    let ultimaOperacao: string | null = null;
    try {
      for (const operacao of transacao.crud) {
        ultimaOperacao = `${operacao.op} em ${operacao.table}/${operacao.id}`;
        const tabela = supabase.from(operacao.table);

        let resultado;
        switch (operacao.op) {
          case UpdateType.PUT:
            resultado = await tabela.upsert({ ...(operacao.opData ?? {}), id: operacao.id });
            break;
          case UpdateType.PATCH:
            resultado = await tabela.update(operacao.opData ?? {}).eq('id', operacao.id);
            break;
          case UpdateType.DELETE:
            resultado = await tabela.delete().eq('id', operacao.id);
            break;
        }

        if (resultado?.error) {
          throw Object.assign(new Error(resultado.error.message), { code: resultado.error.code });
        }
      }
      await transacao.complete();
    } catch (erro) {
      const codigo = (erro as { code?: string }).code;
      if (ehFatal(codigo)) {
        console.error(`Descartando alteração que o servidor recusou (${ultimaOperacao}):`, erro);
        await transacao.complete();
        return;
      }
      throw erro;
    }
  }
}
