import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  base: './',
  build: {
    // O PowerSync usa top-level await e workers ES; alvos antigos quebram o bundle.
    target: 'esnext',
  },
  plugins: [
    react(),
    VitePWA({
      strategies: 'injectManifest',
      srcDir: 'src',
      filename: 'sw.ts',
      registerType: 'autoUpdate',
      includeAssets: ['logo-upv.png'],
      injectManifest: {
        globPatterns: ['**/*.{js,css,html,png,svg,wasm}'],
        maximumFileSizeToCacheInBytes: 8 * 1024 * 1024,
      },
      manifest: {
        name: 'Unidos Pelo Vôlei',
        short_name: 'UP Vôlei',
        description: 'Presença, sorteio de times, chaveamento e classificação dos sábados de vôlei.',
        lang: 'pt-BR',
        start_url: './',
        scope: './',
        display: 'standalone',
        orientation: 'portrait',
        background_color: '#0B0F17',
        theme_color: '#131B2B',
        icons: [
          { src: 'logo-upv.png', sizes: '512x512', type: 'image/png' },
          { src: 'logo-upv.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
    }),
  ],
  optimizeDeps: {
    exclude: ['@journeyapps/wa-sqlite', '@powersync/web'],
    include: ['@powersync/web > js-logger'],
  },
  worker: {
    format: 'es',
  },
});
