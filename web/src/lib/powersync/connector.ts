import {
  UpdateType,
  type AbstractPowerSyncDatabase,
  type PowerSyncBackendConnector,
  type PowerSyncCredentials,
} from '@powersync/web';
import { ErroDaApi, chamarApi } from '../api';
import { tokenDeAcesso } from '../auth';
import { env } from '../env';

interface RespostaDoUpload {
  aplicadas: number;
  descartadas: { tabela: string; id: string; codigo: string; mensagem: string }[];
}

const NOMES: Record<UpdateType, 'PUT' | 'PATCH' | 'DELETE'> = {
  [UpdateType.PUT]: 'PUT',
  [UpdateType.PATCH]: 'PATCH',
  [UpdateType.DELETE]: 'DELETE',
};

export class BackendConnector implements PowerSyncBackendConnector {
  async fetchCredentials(): Promise<PowerSyncCredentials | null> {
    const token = await tokenDeAcesso();
    if (!token) return null;
    return { endpoint: env.powersyncUrl, token };
  }

  async uploadData(database: AbstractPowerSyncDatabase): Promise<void> {
    const transacao = await database.getNextCrudTransaction();
    if (!transacao) return;

    const operacoes = transacao.crud.map((operacao) => ({
      op: NOMES[operacao.op],
      tabela: operacao.table,
      id: operacao.id,
      dados: operacao.opData ?? null,
    }));

    try {
      const resposta = await chamarApi<RespostaDoUpload>('POST', '/sync/upload', { operacoes });
      if (resposta.descartadas.length > 0) {
        console.error('O servidor recusou alterações:', resposta.descartadas);
      }
    } catch (erro) {
      if (!(erro instanceof ErroDaApi) || erro.status !== 400) throw erro;
      console.error('Descartando transação que o servidor recusou:', erro.message);
    }
    await transacao.complete();
  }
}
