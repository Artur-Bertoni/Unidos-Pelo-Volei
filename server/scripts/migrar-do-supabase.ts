import { pipeline } from 'node:stream/promises';
import { parseArgs } from 'node:util';
import type pg from 'pg';
import { from as copiarPara, to as copiarDe } from 'pg-copy-streams';
import { abrirPool, identificador } from '../src/db.js';

const { values: opcoes } = parseArgs({
  options: {
    origem: { type: 'string' },
    destino: { type: 'string' },
    'supabase-url': { type: 'string' },
    'api-url': { type: 'string' },
    confirmar: { type: 'boolean', default: false },
  },
});

const exigir = (nome: keyof typeof opcoes): string => {
  const valor = opcoes[nome];
  if (typeof valor !== 'string' || !valor) {
    console.error(
      'Uso: npm run migrar-do-supabase -- --origem <url do Postgres do Supabase> --destino <url pública do Postgres do Railway> --supabase-url https://XXXX.supabase.co --api-url https://sua-api.up.railway.app [--confirmar]',
    );
    process.exit(1);
  }
  return valor.replace(/\/+$/, '');
};

const origemUrl = exigir('origem');
const destinoUrl = exigir('destino');
const supabaseUrl = exigir('supabase-url');
const apiUrl = exigir('api-url');

const origem = abrirPool(origemUrl, 2);
const destino = abrirPool(destinoUrl, 2);

async function tabelasPublicas(pool: pg.Pool): Promise<string[]> {
  const { rows } = await pool.query<{ tabela: string }>(
    `select c.relname as tabela
     from pg_class c join pg_namespace n on n.oid = c.relnamespace
     where n.nspname = 'public' and c.relkind = 'r' and c.relname <> 'migracoes'
     order by c.relname`,
  );
  return rows.map((linha) => linha.tabela);
}

async function colunasCopiaveis(pool: pg.Pool, esquema: string, tabela: string): Promise<string[]> {
  const { rows } = await pool.query<{ coluna: string }>(
    `select a.attname as coluna
     from pg_attribute a
     join pg_class c on c.oid = a.attrelid
     join pg_namespace n on n.oid = c.relnamespace
     where n.nspname = $1 and c.relname = $2 and a.attnum > 0 and not a.attisdropped and a.attgenerated = ''
     order by a.attnum`,
    [esquema, tabela],
  );
  return rows.map((linha) => linha.coluna);
}

async function contar(pool: pg.Pool, tabela: string): Promise<number> {
  const { rows } = await pool.query<{ n: number }>(`select count(*)::int as n from public.${identificador(tabela)}`);
  return rows[0]!.n;
}

async function copiarTabela(alvo: pg.PoolClient, tabela: string): Promise<void> {
  const daOrigem = new Set(await colunasCopiaveis(origem, 'public', tabela));
  const colunas = (await colunasCopiaveis(destino, 'public', tabela)).filter((coluna) => daOrigem.has(coluna));
  if (colunas.length === 0) return;
  const lista = colunas.map(identificador).join(', ');
  const fonte = await origem.connect();
  try {
    const leitura = fonte.query(copiarDe(`copy (select ${lista} from public.${identificador(tabela)}) to stdout`));
    const escrita = alvo.query(copiarPara(`copy public.${identificador(tabela)} (${lista}) from stdin`));
    await pipeline(leitura, escrita);
  } finally {
    fonte.release();
  }
}

async function copiarUsuarios(alvo: pg.PoolClient): Promise<number> {
  const { rows } = await origem.query<{
    id: string;
    email: string | null;
    raw_user_meta_data: Record<string, unknown> | null;
    created_at: Date;
    google_sub: string | null;
  }>(
    `select u.id, u.email, u.raw_user_meta_data, u.created_at,
            (select i.provider_id from auth.identities i where i.user_id = u.id and i.provider = 'google' limit 1) as google_sub
     from auth.users u`,
  );
  for (const usuario of rows) {
    await alvo.query(
      `insert into auth.users (id, email, raw_user_meta_data, created_at, google_sub)
       values ($1, $2, $3, $4, $5)`,
      [usuario.id, usuario.email, usuario.raw_user_meta_data ?? {}, usuario.created_at, usuario.google_sub],
    );
  }
  return rows.length;
}

async function copiarArquivos(alvo: pg.PoolClient): Promise<number> {
  const { rows } = await origem.query<{ bucket_id: string; name: string; tipo: string | null; owner: string | null }>(
    `select bucket_id, name, metadata ->> 'mimetype' as tipo, owner
     from storage.objects
     where bucket_id in (select id from storage.buckets where public)`,
  );
  let copiados = 0;
  for (const objeto of rows) {
    const caminho = objeto.name.split('/').map(encodeURIComponent).join('/');
    const resposta = await fetch(`${supabaseUrl}/storage/v1/object/public/${objeto.bucket_id}/${caminho}`);
    if (!resposta.ok) {
      console.warn('  não baixou %s/%s (%d)', objeto.bucket_id, objeto.name, resposta.status);
      continue;
    }
    const conteudo = Buffer.from(await resposta.arrayBuffer());
    await alvo.query(
      `insert into storage.objects (bucket_id, name, owner, content_type, tamanho, conteudo)
       values ($1, $2, $3, $4, $5, $6)
       on conflict (bucket_id, name) do nothing`,
      [objeto.bucket_id, objeto.name, objeto.owner, objeto.tipo ?? resposta.headers.get('content-type'), conteudo.length, conteudo],
    );
    copiados += 1;
  }
  return copiados;
}

async function principal(): Promise<void> {
  const { rowCount } = await destino.query(
    "select 1 from information_schema.tables where table_schema = 'public' and table_name = 'migracoes'",
  );
  if (!rowCount) throw new Error('O destino ainda não tem as migrations. Faça o primeiro deploy da API antes.');

  const tabelasDaOrigem = new Set(await tabelasPublicas(origem));
  const tabelas = (await tabelasPublicas(destino)).filter((tabela) => tabelasDaOrigem.has(tabela));

  console.log('Linhas na origem (Supabase):');
  for (const tabela of tabelas) console.log('  %s: %d', tabela, await contar(origem, tabela));

  if (!opcoes.confirmar) {
    console.log('\nNada foi gravado. Rode de novo com --confirmar para APAGAR os dados do destino e copiar estes.');
    return;
  }

  const alvo = await destino.connect();
  try {
    await alvo.query('begin');
    await alvo.query('set local session_replication_role = replica');
    await alvo.query(`truncate ${tabelas.map((tabela) => `public.${identificador(tabela)}`).join(', ')} cascade`);
    await alvo.query('truncate auth.sessoes, auth.users cascade');
    await alvo.query("delete from storage.objects where bucket_id in ('mural', 'grupos')");

    console.log('\nUsuários: %d', await copiarUsuarios(alvo));
    for (const tabela of tabelas) {
      await copiarTabela(alvo, tabela);
      console.log('Copiada: %s', tabela);
    }

    console.log('Imagens copiadas: %d', await copiarArquivos(alvo));

    const antigo = `${supabaseUrl}/storage/v1/object/public/`;
    const novo = `${apiUrl}/arquivos/`;
    const posts = await alvo.query('update public.posts set imagem_url = replace(imagem_url, $1, $2) where imagem_url like $1 || \'%\'', [antigo, novo]);
    const logos = await alvo.query('update public.grupos set logo_url = replace(logo_url, $1, $2) where logo_url like $1 || \'%\'', [antigo, novo]);
    console.log('URLs reescritas: %d posts, %d logos', posts.rowCount, logos.rowCount);

    await alvo.query('commit');
  } catch (erro) {
    await alvo.query('rollback').catch(() => undefined);
    throw erro;
  } finally {
    alvo.release();
  }

  console.log('\nLinhas no destino (Railway):');
  for (const tabela of tabelas) console.log('  %s: %d', tabela, await contar(destino, tabela));
}

principal()
  .catch((erro) => {
    console.error('\nFalhou:', (erro as Error).message);
    process.exitCode = 1;
  })
  .finally(async () => {
    await origem.end();
    await destino.end();
  });
