/// <reference lib="webworker" />
import { cleanupOutdatedCaches, precacheAndRoute } from 'workbox-precaching';

declare const self: ServiceWorkerGlobalScope;

precacheAndRoute(self.__WB_MANIFEST);
cleanupOutdatedCaches();

self.addEventListener('install', () => {
  void self.skipWaiting();
});

self.addEventListener('activate', (evento) => {
  evento.waitUntil(self.clients.claim());
});

interface AvisoDoPush {
  titulo?: string;
  corpo?: string;
}

self.addEventListener('push', (evento) => {
  let aviso: AvisoDoPush = {};
  try {
    aviso = (evento.data?.json() ?? {}) as AvisoDoPush;
  } catch {
    aviso = { titulo: evento.data?.text() };
  }

  const titulo = aviso.titulo ?? 'Unidos Pelo Vôlei';
  evento.waitUntil(
    self.registration.showNotification(titulo, {
      body: aviso.corpo ?? '',
      icon: 'logo-upv.png',
      badge: 'logo-upv.png',
      tag: 'unidos-pelo-volei',
    }),
  );
});

self.addEventListener('notificationclick', (evento) => {
  evento.notification.close();
  evento.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((janelas) => {
      const aberta = janelas.find((janela) => 'focus' in janela);
      if (aberta) return aberta.focus();
      return self.clients.openWindow('./');
    }),
  );
});
