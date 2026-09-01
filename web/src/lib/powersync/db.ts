import { PowerSyncDatabase } from '@powersync/web';
import { supabase } from '../supabase';
import { SupabaseConnector } from './connector';
import { AppSchema } from './schema';

export const db = new PowerSyncDatabase({
  schema: AppSchema,
  database: { dbFilename: 'unidos-pelo-volei.db' },
});

const connector = new SupabaseConnector();

let conectado = false;
let emAndamento: Promise<void> = Promise.resolve();

/** Serializa conexão e desconexão, como o Mutex do SyncService do Android. */
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

/**
 * Liga o ciclo de vida do sync ao da sessão: conecta quando há usuário e limpa o
 * banco local quando ele sai, para não deixar dados de uma conta visíveis a outra.
 */
export function iniciarSync(): void {
  void supabase.auth.getSession().then(({ data: { session } }) => {
    if (session) void conectar();
  });

  supabase.auth.onAuthStateChange((evento, session) => {
    if (evento === 'SIGNED_OUT') {
      void desconectar(true);
      return;
    }
    if (session) {
      void conectar();
    } else {
      void desconectar(false);
    }
  });
}
