import { Hono, type Context } from 'hono';
import { bodyLimit } from 'hono/body-limit';
import { cors } from 'hono/cors';
import { HTTPException } from 'hono/http-exception';
import { ArquivoRecusado, guardarArquivo, lerArquivo } from './arquivos.js';
import { ErroDeAutenticacao, encerrarSessao, entrarComGoogle, renovarSessao, verificarAcesso } from './auth.js';
import { pool, type Usuario } from './db.js';
import {
  FuncaoDesconhecida,
  chamarFuncao,
  criarChave,
  definirChaveAtiva,
  definirPapel,
  excluirChave,
  listarChaves,
  listarMembros,
  listarPagamentos,
  removerMembro,
  type NovaChave,
} from './grupos.js';
import { OperacaoInvalida, aplicarOperacoes, ehFatal, type Operacao } from './sincronizacao.js';

type Ambiente = { Variables: { usuario: Usuario } };

const MEGA = 1024 * 1024;

const lerJson = async <T>(c: Context): Promise<T> => {
  try {
    return (await c.req.json()) as T;
  } catch {
    throw new HTTPException(400, { message: 'Corpo da requisição inválido.' });
  }
};

export function criarApp(): Hono<Ambiente> {
  const app = new Hono<Ambiente>();

  app.use('*', cors({ origin: '*', allowHeaders: ['Authorization', 'Content-Type'], maxAge: 86400 }));

  app.onError((erro, c) => {
    if (erro instanceof HTTPException) return c.json({ erro: erro.message }, erro.status);
    if (erro instanceof ErroDeAutenticacao) return c.json({ erro: erro.message }, 401);
    if (erro instanceof ArquivoRecusado) return c.json({ erro: erro.message }, erro.status);
    if (erro instanceof FuncaoDesconhecida || erro instanceof OperacaoInvalida) {
      return c.json({ erro: erro.message }, 400);
    }
    const { code, message } = erro as { code?: string; message: string };
    if (ehFatal(code)) return c.json({ erro: message, codigo: code }, 400);
    console.error(erro);
    return c.json({ erro: 'Erro inesperado no servidor.' }, 500);
  });

  app.get('/saude', async (c) => {
    await pool().query('select 1');
    return c.json({ ok: true });
  });

  app.post('/auth/google', async (c) => {
    const corpo = await lerJson<{ idToken?: string; nonce?: string }>(c);
    if (!corpo.idToken) throw new HTTPException(400, { message: 'Faltou o token do Google.' });
    return c.json(await entrarComGoogle(corpo.idToken, corpo.nonce ?? '', c.req.header('user-agent') ?? null));
  });

  app.post('/auth/renovar', async (c) => {
    const corpo = await lerJson<{ refresh_token?: string }>(c);
    return c.json(await renovarSessao(corpo.refresh_token ?? ''));
  });

  app.post('/auth/sair', async (c) => {
    const corpo = await lerJson<{ refresh_token?: string }>(c);
    await encerrarSessao(corpo.refresh_token ?? '');
    return c.body(null, 204);
  });

  app.get('/arquivos/:bucket/:pasta/:nome', async (c) => {
    const { bucket, pasta, nome } = c.req.param();
    const arquivo = await lerArquivo(bucket, `${pasta}/${nome}`);
    if (!arquivo) return c.json({ erro: 'Arquivo não encontrado.' }, 404);
    return c.body(new Uint8Array(arquivo.conteudo), 200, {
      'Content-Type': arquivo.tipo,
      'Cache-Control': 'public, max-age=31536000, immutable',
      'Last-Modified': arquivo.criadoEm.toUTCString(),
    });
  });

  const protegido = new Hono<Ambiente>();

  protegido.use('*', async (c, proximo) => {
    const cabecalho = c.req.header('authorization') ?? '';
    const token = cabecalho.startsWith('Bearer ') ? cabecalho.slice(7) : '';
    if (!token) throw new ErroDeAutenticacao('Entre com a sua conta para continuar.');
    c.set('usuario', await verificarAcesso(token));
    await proximo();
  });

  protegido.post('/sync/upload', bodyLimit({ maxSize: 10 * MEGA }), async (c) => {
    const corpo = await lerJson<{ operacoes?: Operacao[] }>(c);
    if (!Array.isArray(corpo.operacoes)) throw new HTTPException(400, { message: 'Faltou a lista de operações.' });
    const resultado = await aplicarOperacoes(c.get('usuario'), corpo.operacoes);
    if (resultado.descartadas.length > 0) {
      console.warn('Operações recusadas pelo banco:', JSON.stringify(resultado.descartadas));
    }
    return c.json(resultado);
  });

  protegido.post('/rpc/:funcao', async (c) => {
    const argumentos = await lerJson<Record<string, unknown>>(c);
    return c.json(await chamarFuncao(c.get('usuario'), c.req.param('funcao'), argumentos ?? {}));
  });

  protegido.get('/grupos/:grupo/chaves', async (c) =>
    c.json(await listarChaves(c.get('usuario'), c.req.param('grupo'))),
  );

  protegido.post('/grupos/:grupo/chaves', async (c) => {
    const chave = await lerJson<NovaChave>(c);
    return c.json(await criarChave(c.get('usuario'), c.req.param('grupo'), chave), 201);
  });

  protegido.patch('/chaves/:chave', async (c) => {
    const { ativa } = await lerJson<{ ativa?: boolean }>(c);
    if (typeof ativa !== 'boolean') throw new HTTPException(400, { message: 'Informe se a chave fica ativa.' });
    await definirChaveAtiva(c.get('usuario'), c.req.param('chave'), ativa);
    return c.body(null, 204);
  });

  protegido.delete('/chaves/:chave', async (c) => {
    await excluirChave(c.get('usuario'), c.req.param('chave'));
    return c.body(null, 204);
  });

  protegido.get('/grupos/:grupo/membros', async (c) =>
    c.json(await listarMembros(c.get('usuario'), c.req.param('grupo'))),
  );

  protegido.patch('/grupos/:grupo/membros/:perfil', async (c) => {
    const { papel } = await lerJson<{ papel?: string }>(c);
    if (!papel) throw new HTTPException(400, { message: 'Informe o papel.' });
    await definirPapel(c.get('usuario'), c.req.param('grupo'), c.req.param('perfil'), papel);
    return c.body(null, 204);
  });

  protegido.delete('/grupos/:grupo/membros/:perfil', async (c) => {
    await removerMembro(c.get('usuario'), c.req.param('grupo'), c.req.param('perfil'));
    return c.body(null, 204);
  });

  protegido.get('/grupos/:grupo/pagamentos', async (c) =>
    c.json(await listarPagamentos(c.get('usuario'), c.req.param('grupo'))),
  );

  protegido.put('/arquivos/:bucket/:pasta/:nome', bodyLimit({ maxSize: 6 * MEGA }), async (c) => {
    const { bucket, pasta, nome } = c.req.param();
    const tipo = (c.req.header('content-type') ?? '').split(';')[0]!.trim().toLowerCase();
    const conteudo = Buffer.from(await c.req.arrayBuffer());
    const url = await guardarArquivo(c.get('usuario'), bucket, pasta, nome, tipo, conteudo);
    return c.json({ url }, 201);
  });

  app.route('/', protegido);

  return app;
}
