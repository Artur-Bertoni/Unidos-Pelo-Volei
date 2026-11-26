const ler = (nome: string): string => (process.env[nome] ?? '').trim();

const obrigatorio = (nome: string): string => {
  const valor = ler(nome);
  if (!valor) throw new Error(`Variável de ambiente ${nome} não definida.`);
  return valor;
};

const porta = Number(ler('PORT') || '8080');

const urlPublica = (): string => {
  const explicita = ler('URL_PUBLICA').replace(/\/+$/, '');
  if (explicita) return explicita;
  const dominio = ler('RAILWAY_PUBLIC_DOMAIN');
  if (dominio) return `https://${dominio}`;
  return `http://localhost:${porta}`;
};

export const config = {
  porta,
  urlPublica: urlPublica(),
  databaseUrl: () => obrigatorio('DATABASE_URL'),
  jwtSecret: () => obrigatorio('JWT_SECRET'),
  jwtKid: ler('JWT_KID') || 'upv',
  googleClientIds: () =>
    obrigatorio('GOOGLE_CLIENT_IDS')
      .split(',')
      .map((id) => id.trim())
      .filter(Boolean),
  bancoDoPowerSync: ler('POWERSYNC_STORAGE_DB') || 'powersync_storage',
  vapidPublica: ler('VAPID_PUBLIC_KEY'),
  vapidPrivada: ler('VAPID_PRIVATE_KEY'),
  vapidAssunto: ler('VAPID_SUBJECT') || 'mailto:contato@unidospelovolei.app',
  contaDeServicoFirebase: ler('FIREBASE_SERVICE_ACCOUNT'),
};
