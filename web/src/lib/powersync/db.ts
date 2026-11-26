import { PowerSyncDatabase } from '@powersync/web';
import { definirUsuario } from '../../data/grupoAtivo';
import { observarSessao, sessaoAtual } from '../auth';
import { BackendConnector } from './connector';
import { AppSchema } from './schema';

export const db = new PowerSyncDatabase({
  schema: AppSchema,
  database: { dbFilename: 'unidos-pelo-volei.db' },
});

const connector = new BackendConnector();

let conectado = false;
let emAndamento: Promise<void> = Promise.resolve();

const enfileirar = (acao: () => Promise<void>): Promise<void> => {
  emAndamento = emAndamento.then(acao, acao);
  return emAndamento;
};

const conectar = (): Promise<void> =>
  enfileirar(async () => {
    if (conectado) return;
    try {
      await db.connect(connector);
      conectado = true;
    } catch (erro) {
      console.error('Falha ao conectar o PowerSync:', erro);
    }
  });

const desconectar = (limpar: boolean): Promise<void> =>
  enfileirar(async () => {
    if (!conectado && !limpar) return;
    try {
      if (limpar) {
        await db.disconnectAndClear();
      } else {
        await db.disconnect();
      }
    } catch (erro) {
      console.error('Falha ao desconectar o PowerSync:', erro);
    }
    conectado = false;
  });

export function iniciarSync(): void {
  const inicial = sessaoAtual();
  definirUsuario(inicial?.usuarioId ?? null);
  if (inicial) void conectar();

  observarSessao((sessao) => {
    definirUsuario(sessao?.usuarioId ?? null);
    if (sessao) {
      void desconectar(true).then(conectar);
    } else {
      void desconectar(true);
    }
  });
}
