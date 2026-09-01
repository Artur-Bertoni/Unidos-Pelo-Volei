import { PowerSyncContext } from '@powersync/react';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { configurado } from './lib/env';
import { db, iniciarSync } from './lib/powersync/db';
import './ui/theme.css';

if (configurado()) {
  iniciarSync();
}

const raiz = document.getElementById('root');
if (!raiz) throw new Error('Elemento #root não encontrado no index.html.');

createRoot(raiz).render(
  <StrictMode>
    <PowerSyncContext.Provider value={db}>
      <App />
    </PowerSyncContext.Provider>
  </StrictMode>,
);
