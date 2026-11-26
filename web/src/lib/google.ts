import { env } from './env';

interface RespostaDoGoogle {
  credential: string;
}

interface OpcoesDoBotao {
  theme?: 'outline' | 'filled_blue' | 'filled_black';
  size?: 'large' | 'medium' | 'small';
  text?: 'signin_with' | 'signup_with' | 'continue_with' | 'signin';
  shape?: 'rectangular' | 'pill' | 'circle' | 'square';
  locale?: string;
  width?: number;
}

interface GoogleIdentity {
  accounts: {
    id: {
      initialize(opcoes: {
        client_id: string;
        callback: (resposta: RespostaDoGoogle) => void;
        nonce?: string;
        auto_select?: boolean;
        ux_mode?: 'popup' | 'redirect';
        use_fedcm_for_button?: boolean;
      }): void;
      renderButton(elemento: HTMLElement, opcoes: OpcoesDoBotao): void;
      disableAutoSelect(): void;
    };
  };
}

declare global {
  interface Window {
    google?: GoogleIdentity;
  }
}

let carregamento: Promise<GoogleIdentity> | null = null;

function carregarGoogle(): Promise<GoogleIdentity> {
  if (window.google) return Promise.resolve(window.google);
  carregamento ??= new Promise<GoogleIdentity>((resolver, rejeitar) => {
    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.onload = () => (window.google ? resolver(window.google) : rejeitar(new Error('Google indisponível.')));
    script.onerror = () => {
      carregamento = null;
      rejeitar(new Error('Não foi possível carregar o login do Google. Confira a internet.'));
    };
    document.head.appendChild(script);
  });
  return carregamento;
}

async function sha256(valor: string): Promise<string> {
  const bytes = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(valor));
  return Array.from(new Uint8Array(bytes), (byte) => byte.toString(16).padStart(2, '0')).join('');
}

export async function mostrarBotaoDoGoogle(
  elemento: HTMLElement,
  aoReceberToken: (idToken: string, nonce: string) => void,
): Promise<void> {
  const google = await carregarGoogle();
  const nonce = crypto.randomUUID();
  google.accounts.id.initialize({
    client_id: env.googleClientId,
    nonce: await sha256(nonce),
    auto_select: false,
    ux_mode: 'popup',
    use_fedcm_for_button: true,
    callback: (resposta) => aoReceberToken(resposta.credential, nonce),
  });
  elemento.replaceChildren();
  google.accounts.id.renderButton(elemento, {
    theme: 'filled_black',
    size: 'large',
    text: 'signin_with',
    shape: 'pill',
    locale: 'pt-BR',
    width: 260,
  });
}
