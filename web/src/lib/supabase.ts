import { createClient } from '@supabase/supabase-js';
import { env } from './env';

// Valores de reserva mantêm o módulo carregável quando falta configuração:
// nesse caso o app mostra a tela de configuração pendente em vez de quebrar na importação.
export const supabase = createClient(
  env.supabaseUrl || 'https://configuracao-pendente.supabase.co',
  env.supabaseAnonKey || 'configuracao-pendente',
  {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
    flowType: 'pkce',
    },
  },
);

export async function entrarComGoogle(): Promise<void> {
  const { error } = await supabase.auth.signInWithOAuth({
    provider: 'google',
    options: {
      redirectTo: window.location.origin + window.location.pathname,
      queryParams: { prompt: 'select_account' },
    },
  });
  if (error) throw new Error(error.message);
}

export async function sair(): Promise<void> {
  const { error } = await supabase.auth.signOut();
  if (error) throw new Error(error.message);
}
