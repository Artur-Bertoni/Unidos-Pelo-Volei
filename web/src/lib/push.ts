import { registrarDispositivo } from '../data/grupo';
import { env } from './env';

const base64ParaBytes = (base64: string): ArrayBuffer => {
  const preenchido = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
  const normalizado = preenchido.replace(/-/g, '+').replace(/_/g, '/');
  const bruto = atob(normalizado);
  const buffer = new ArrayBuffer(bruto.length);
  const bytes = new Uint8Array(buffer);
  for (let i = 0; i < bruto.length; i++) bytes[i] = bruto.charCodeAt(i);
  return buffer;
};

export const pushSuportado = (): boolean =>
  typeof window !== 'undefined' && 'serviceWorker' in navigator && 'PushManager' in window;

export const pushConfigurado = (): boolean => env.vapidPublicKey !== '';

export const instaladoNaTelaDeInicio = (): boolean =>
  window.matchMedia('(display-mode: standalone)').matches ||
  (window.navigator as { standalone?: boolean }).standalone === true;

export const ehIphone = (): boolean => /iPad|iPhone|iPod/.test(navigator.userAgent);

export type EstadoDoPush = 'indisponivel' | 'sem-chave' | 'precisa-instalar' | 'negado' | 'pronto';

export function estadoDoPush(): EstadoDoPush {
  if (!pushSuportado()) return ehIphone() ? 'precisa-instalar' : 'indisponivel';
  if (!pushConfigurado()) return 'sem-chave';
  if (ehIphone() && !instaladoNaTelaDeInicio()) return 'precisa-instalar';
  if (Notification.permission === 'denied') return 'negado';
  return 'pronto';
}

export async function ativarPush(profileId: string): Promise<boolean> {
  if (estadoDoPush() !== 'pronto') return false;

  const permissao = await Notification.requestPermission();
  if (permissao !== 'granted') return false;

  const registro = await navigator.serviceWorker.ready;
  const existente = await registro.pushManager.getSubscription();
  const inscricao =
    existente ??
    (await registro.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: base64ParaBytes(env.vapidPublicKey),
    }));

  await registrarDispositivo(profileId, JSON.stringify(inscricao), 'web');
  return true;
}
