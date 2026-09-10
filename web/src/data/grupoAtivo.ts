const PREFIXO = 'grupo_ativo';

let usuario: string | null = null;
let escolhido: string | null = null;

const ouvintes = new Set<() => void>();

const chaveDe = (usuarioId: string): string => `${PREFIXO}_${usuarioId}`;

const lerGuardado = (usuarioId: string): string | null => {
  try {
    return window.localStorage.getItem(chaveDe(usuarioId));
  } catch {
    return null;
  }
};

const guardar = (usuarioId: string, grupoId: string | null): void => {
  try {
    if (grupoId === null) window.localStorage.removeItem(chaveDe(usuarioId));
    else window.localStorage.setItem(chaveDe(usuarioId), grupoId);
  } catch {
    /* navegador sem armazenamento: a escolha vale só nesta sessão */
  }
};

const avisar = (): void => ouvintes.forEach((ouvinte) => ouvinte());

/** Liga o grupo guardado à conta que acabou de entrar, ou limpa quando ela sai. */
export function definirUsuario(usuarioId: string | null): void {
  if (usuario === usuarioId) return;
  usuario = usuarioId;
  escolhido = usuarioId === null ? null : lerGuardado(usuarioId);
  avisar();
}

export function grupoAtivo(): string | null {
  return escolhido;
}

export function exigirGrupo(): string {
  if (escolhido === null) throw new Error('Escolha um grupo antes de continuar.');
  return escolhido;
}

export function selecionarGrupo(grupoId: string | null): void {
  escolhido = grupoId;
  if (usuario !== null) guardar(usuario, grupoId);
  avisar();
}

export function observarGrupoAtivo(ouvinte: () => void): () => void {
  ouvintes.add(ouvinte);
  return () => {
    ouvintes.delete(ouvinte);
  };
}
