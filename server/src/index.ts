import { serve } from '@hono/node-server';
import { criarApp } from './app.js';
import { config } from './config.js';
import { migrar } from './migrar.js';

await migrar();

serve({ fetch: criarApp().fetch, port: config.porta, hostname: '::' }, (endereco) => {
  console.log('API ouvindo na porta %d (%s)', endereco.port, config.urlPublica);
});
