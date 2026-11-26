import { env } from './env';

export interface Sessao {
  usuarioId: string;
  email: string | null;
}

export type EventoDeSessao = 'entrou' | 'saiu' | 'expirou';

interface SessaoGuardada {
  access_token: string;
  expires_at: number;
  refresh_token: string;
  user: { id: string; email: string | null };
}

const CHAVE = 'upv.sessao';
const MARGEM_EM_SEGUNDOS = 120;

const ler = (): SessaoGuardada | null => {
  try {
    const bruto = localStorage.getItem(CHAVE);
    return bruto ? (JSON.parse(bruto) as SessaoGuardada) : null;
  } catch {
    return null;
  }
};

let guardada: SessaoGuardada | null = ler();
let resumo: Sessao | null = guardada ? { usuarioId: guardada.user.id, email: guardada.user.email } : null;
const ouvintes = new Set<(sessao: Sessao | null, evento: EventoDeSessao) => void>();

const avisar = (evento: EventoDeSessao): void => {
  ouvintes.forEach((ouvinte) => ouvinte(resumo, evento));
};

const trocar = (nova: SessaoGuardada | null, evento: EventoDeSessao | null): void => {
  const usuarioAntes = guardada?.user.id ?? null;
  guardada = nova;
  if (nova) localStorage.setItem(CHAVE, JSON.stringify(nova));
  else localStorage.removeItem(CHAVE);
  const usuarioDepois = nova?.user.id ?? null;
  if (usuarioAntes !== usuarioDepois) {
    resumo = nova ? { usuarioId: nova.user.id, email: nova.user.email } : null;
    if (evento) avisar(evento);
  }
};

window.addEventListener('storage', (evento) => {
  if (evento.key !== CHAVE) return;
  const nova = ler();
  const usuarioAntes = guardada?.user.id ?? null;
  guardada = nova;
  if ((nova?.user.id ?? null) !== usuarioAntes) {
    resumo = nova ? { usuarioId: nova.user.id, email: nova.user.email } : null;
    avisar(nova ? 'entrou' : 'saiu');
  }
});

export const sessaoAtual = (): Sessao | null => resumo;

export function observarSessao(ouvinte: (sessao: Sessao | null, evento: EventoDeSessao) => void): () => void {
  ouvintes.add(ouvinte);
  return () => ouvintes.delete(ouvinte);
}

async function postar<T>(caminho: string, corpo: unknown): Promise<{ status: number; dados: T | null; erro: string | null }> {
  let resposta: Response;
  try {
    resposta = await fetch(env.apiUrl + caminho, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(corpo),
    });
  } catch {
    throw new Error('Sem conexão com o servidor. Precisa de internet.');
  }
  const texto = await resposta.text();
  const json = texto ? (JSON.parse(texto) as unknown) : null;
  if (!resposta.ok) {
    return { status: resposta.status, dados: null, erro: (json as { erro?: string } | null)?.erro ?? null };
  }
  return { status: resposta.status, dados: json as T, erro: null };
}

export async function entrarComTokenDoGoogle(idToken: string, nonce: string): Promise<void> {
  const { dados, erro } = await postar<SessaoGuardada>('/auth/google', { idToken, nonce });
  if (!dados) throw new Error(erro ?? 'Não foi possível entrar com o Google.');
  trocar(dados, 'entrou');
}

let renovacao: Promise<string | null> | null = null;

async function renovar(): Promise<string | null> {
  const atual = guardada;
  if (!atual) return null;
  const { status, dados, erro } = await postar<SessaoGuardada>('/auth/renovar', {
    refresh_token: atual.refresh_token,
  });
  if (status === 401) {
    trocar(null, 'expirou');
    return null;
  }
  if (!dados) throw new Error(erro ?? 'Não foi possível renovar a sessão.');
  trocar(dados, null);
  return dados.access_token;
}

export function tokenDeAcesso(forcarRenovacao = false): Promise<string | null> {
  const atual = guardada;
  if (!atual) return Promise.resolve(null);
  const agora = Date.now() / 1000;
  if (!forcarRenovacao && atual.expires_at - agora > MARGEM_EM_SEGUNDOS) {
    return Promise.resolve(atual.access_token);
  }
  renovacao ??= renovar().finally(() => {
    renovacao = null;
  });
  return renovacao;
}

export async function sair(): Promise<void> {
  const refresh = guardada?.refresh_token;
  trocar(null, 'saiu');
  window.google?.accounts.id.disableAutoSelect();
  if (refresh) await postar('/auth/sair', { refresh_token: refresh }).catch(() => undefined);
}
