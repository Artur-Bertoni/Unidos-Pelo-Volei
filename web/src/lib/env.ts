interface Ambiente {
  apiUrl: string;
  powersyncUrl: string;
  googleClientId: string;
  vapidPublicKey: string;
  nome: string;
}

const ler = (valor: string | undefined): string => (valor ?? '').trim();

export const env: Ambiente = {
  apiUrl: ler(import.meta.env.VITE_API_URL).replace(/\/+$/, ''),
  powersyncUrl: ler(import.meta.env.VITE_POWERSYNC_URL).replace(/\/+$/, ''),
  googleClientId: ler(import.meta.env.VITE_GOOGLE_CLIENT_ID),
  vapidPublicKey: ler(import.meta.env.VITE_VAPID_PUBLIC_KEY),
  nome: ler(import.meta.env.VITE_ENVIRONMENT) || (import.meta.env.DEV ? 'dev' : 'prod'),
};

export const chavesFaltando = (): string[] => {
  const faltando: string[] = [];
  if (!env.apiUrl) faltando.push('VITE_API_URL');
  if (!env.powersyncUrl) faltando.push('VITE_POWERSYNC_URL');
  if (!env.googleClientId) faltando.push('VITE_GOOGLE_CLIENT_ID');
  return faltando;
};

export const configurado = (): boolean => chavesFaltando().length === 0;
