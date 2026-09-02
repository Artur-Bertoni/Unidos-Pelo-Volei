interface Ambiente {
  supabaseUrl: string;
  supabaseAnonKey: string;
  powersyncUrl: string;
  vapidPublicKey: string;
  nome: string;
}

const ler = (valor: string | undefined): string => (valor ?? '').trim();

export const env: Ambiente = {
  supabaseUrl: ler(import.meta.env.VITE_SUPABASE_URL),
  supabaseAnonKey: ler(import.meta.env.VITE_SUPABASE_ANON_KEY),
  powersyncUrl: ler(import.meta.env.VITE_POWERSYNC_URL).replace(/\/+$/, ''),
  vapidPublicKey: ler(import.meta.env.VITE_VAPID_PUBLIC_KEY),
  nome: ler(import.meta.env.VITE_ENVIRONMENT) || (import.meta.env.DEV ? 'dev' : 'prod'),
};

export const chavesFaltando = (): string[] => {
  const faltando: string[] = [];
  if (!env.supabaseUrl) faltando.push('VITE_SUPABASE_URL');
  if (!env.supabaseAnonKey) faltando.push('VITE_SUPABASE_ANON_KEY');
  if (!env.powersyncUrl) faltando.push('VITE_POWERSYNC_URL');
  return faltando;
};

export const configurado = (): boolean => chavesFaltando().length === 0;
