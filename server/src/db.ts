import pg from 'pg';
import { config } from './config.js';

const precisaDeSsl = (url: string): boolean => {
  const host = new URL(url).hostname;
  return !(host === 'localhost' || host === '127.0.0.1' || host.endsWith('.railway.internal') || !host.includes('.'));
};

export const abrirPool = (url: string, max = 10): pg.Pool =>
  new pg.Pool({
    connectionString: url,
    max,
    ssl: precisaDeSsl(url) ? { rejectUnauthorized: false } : undefined,
  });

let poolGlobal: pg.Pool | null = null;

export const pool = (): pg.Pool => {
  poolGlobal ??= abrirPool(config.databaseUrl());
  return poolGlobal;
};

export interface Usuario {
  id: string;
  email: string | null;
}

export async function emTransacao<T>(acao: (cliente: pg.PoolClient) => Promise<T>): Promise<T> {
  const cliente = await pool().connect();
  try {
    await cliente.query('begin');
    const resultado = await acao(cliente);
    await cliente.query('commit');
    return resultado;
  } catch (erro) {
    await cliente.query('rollback').catch(() => undefined);
    throw erro;
  } finally {
    cliente.release();
  }
}

export function comoUsuario<T>(
  usuario: Usuario,
  acao: (cliente: pg.PoolClient) => Promise<T>,
): Promise<T> {
  return emTransacao(async (cliente) => {
    const claims = JSON.stringify({ sub: usuario.id, email: usuario.email, role: 'authenticated' });
    await cliente.query(
      `select set_config('request.jwt.claims', $1, true),
              set_config('request.jwt.claim.sub', $2, true),
              set_config('role', 'authenticated', true)`,
      [claims, usuario.id],
    );
    return acao(cliente);
  });
}

export const identificador = (nome: string): string => `"${nome.replace(/"/g, '""')}"`;
