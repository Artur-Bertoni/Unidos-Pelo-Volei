import { readdir, readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import type pg from 'pg';
import { config } from './config.js';
import { identificador, pool } from './db.js';

const PASTA = fileURLToPath(new URL('../migrations/', import.meta.url));
const TRAVA = 4815162342;

async function garantirReplicacaoLogica(cliente: pg.PoolClient): Promise<void> {
  const { rows } = await cliente.query<{ wal_level: string }>('show wal_level');
  if (rows[0]?.wal_level === 'logical') return;
  try {
    await cliente.query('alter system set wal_level = logical');
    console.warn(
      'wal_level estava em "%s" e foi trocado para "logical". Reinicie o serviço do Postgres uma vez para o PowerSync conseguir replicar.',
      rows[0]?.wal_level,
    );
  } catch (erro) {
    console.warn('Não foi possível ligar wal_level = logical:', (erro as Error).message);
  }
}

async function garantirBancoDoPowerSync(cliente: pg.PoolClient): Promise<void> {
  const nome = config.bancoDoPowerSync;
  const { rowCount } = await cliente.query('select 1 from pg_database where datname = $1', [nome]);
  if (rowCount) return;
  await cliente.query(`create database ${identificador(nome)}`);
  console.log('Banco "%s" criado para o armazenamento do PowerSync.', nome);
}

export async function migrar(): Promise<void> {
  const cliente = await pool().connect();
  try {
    await garantirReplicacaoLogica(cliente);
    await garantirBancoDoPowerSync(cliente);

    await cliente.query('select pg_advisory_lock($1)', [TRAVA]);
    await cliente.query(
      `create table if not exists public.migracoes (
         nome text primary key,
         aplicada_em timestamptz not null default now()
       )`,
    );

    const { rows } = await cliente.query<{ nome: string }>('select nome from public.migracoes');
    const aplicadas = new Set(rows.map((linha) => linha.nome));
    const arquivos = (await readdir(PASTA)).filter((nome) => nome.endsWith('.sql')).sort();

    for (const arquivo of arquivos) {
      if (aplicadas.has(arquivo)) continue;
      const sql = await readFile(PASTA + arquivo, 'utf8');
      try {
        await cliente.query('begin');
        await cliente.query(sql);
        await cliente.query('insert into public.migracoes (nome) values ($1)', [arquivo]);
        await cliente.query('commit');
        console.log('Migration aplicada: %s', arquivo);
      } catch (erro) {
        await cliente.query('rollback').catch(() => undefined);
        throw new Error(`Falha na migration ${arquivo}: ${(erro as Error).message}`);
      }
    }
  } finally {
    await cliente.query('select pg_advisory_unlock($1)', [TRAVA]).catch(() => undefined);
    cliente.release();
  }
}
