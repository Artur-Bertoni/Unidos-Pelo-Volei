import { createHash, randomBytes } from 'node:crypto';
import { OAuth2Client } from 'google-auth-library';
import { SignJWT, base64url, jwtVerify } from 'jose';
import { config } from './config.js';
import { emTransacao, pool, type Usuario } from './db.js';

const DURACAO_DO_ACESSO_EM_SEGUNDOS = 55 * 60;
const DIAS_DE_SESSAO_PARADA = 180;
const AUDIENCIA_DA_API = 'upv-api';
const AUDIENCIA_DO_POWERSYNC = 'powersync';

export class ErroDeAutenticacao extends Error {}

let chave: Uint8Array | null = null;
const chaveDeAssinatura = (): Uint8Array => {
  if (chave) return chave;
  const bytes = base64url.decode(config.jwtSecret());
  if (bytes.length < 32) throw new Error('JWT_SECRET precisa ter pelo menos 32 bytes em base64url.');
  chave = bytes;
  return chave;
};

const google = new OAuth2Client();

const sha256 = (valor: string): string => createHash('sha256').update(valor).digest('hex');

export interface Sessao {
  access_token: string;
  expires_at: number;
  refresh_token: string;
  user: Usuario;
}

async function emitirAcesso(usuario: Usuario): Promise<{ token: string; expiraEm: number }> {
  const agora = Math.floor(Date.now() / 1000);
  const expiraEm = agora + DURACAO_DO_ACESSO_EM_SEGUNDOS;
  const token = await new SignJWT({ email: usuario.email, role: 'authenticated' })
    .setProtectedHeader({ alg: 'HS256', kid: config.jwtKid, typ: 'JWT' })
    .setSubject(usuario.id)
    .setAudience([AUDIENCIA_DO_POWERSYNC, AUDIENCIA_DA_API])
    .setIssuedAt(agora)
    .setExpirationTime(expiraEm)
    .sign(chaveDeAssinatura());
  return { token, expiraEm };
}

export async function verificarAcesso(token: string): Promise<Usuario> {
  try {
    const { payload } = await jwtVerify(token, chaveDeAssinatura(), {
      audience: AUDIENCIA_DA_API,
      algorithms: ['HS256'],
    });
    if (!payload.sub) throw new ErroDeAutenticacao('Token sem usuário.');
    return { id: payload.sub, email: typeof payload.email === 'string' ? payload.email : null };
  } catch {
    throw new ErroDeAutenticacao('Sessão expirada. Entre de novo.');
  }
}

interface IdentidadeGoogle {
  sub: string;
  email: string;
  nome: string | null;
  foto: string | null;
}

async function validarTokenDoGoogle(idToken: string, nonce: string): Promise<IdentidadeGoogle> {
  let payload;
  try {
    const ticket = await google.verifyIdToken({ idToken, audience: config.googleClientIds() });
    payload = ticket.getPayload();
  } catch {
    throw new ErroDeAutenticacao('O Google não confirmou este login. Tente de novo.');
  }
  if (!payload?.sub || !payload.email) throw new ErroDeAutenticacao('A conta Google não informou o e-mail.');
  if (payload.email_verified === false) throw new ErroDeAutenticacao('O e-mail desta conta Google não está verificado.');
  if (!nonce || payload.nonce !== sha256(nonce)) throw new ErroDeAutenticacao('Login expirado. Tente de novo.');
  return {
    sub: payload.sub,
    email: payload.email,
    nome: payload.name ?? null,
    foto: payload.picture ?? null,
  };
}

async function encontrarOuCriarUsuario(identidade: IdentidadeGoogle): Promise<Usuario> {
  const metadados = JSON.stringify({
    full_name: identidade.nome,
    name: identidade.nome,
    email: identidade.email,
    picture: identidade.foto,
  });

  return emTransacao(async (cliente) => {
    const existente = await cliente.query<{ id: string }>(
      `select id from auth.users
       where google_sub = $1
          or (google_sub is null and lower(email) = lower($2))
       order by (google_sub = $1) desc nulls last, created_at
       limit 1
       for update`,
      [identidade.sub, identidade.email],
    );

    if (existente.rows[0]) {
      const id = existente.rows[0].id;
      await cliente.query(
        `update auth.users
         set google_sub = $2, email = $3, raw_user_meta_data = raw_user_meta_data || $4::jsonb, updated_at = now()
         where id = $1`,
        [id, identidade.sub, identidade.email, metadados],
      );
      return { id, email: identidade.email };
    }

    const criado = await cliente.query<{ id: string }>(
      `insert into auth.users (email, raw_user_meta_data, google_sub)
       values ($1, $2::jsonb, $3)
       returning id`,
      [identidade.email, metadados, identidade.sub],
    );
    return { id: criado.rows[0]!.id, email: identidade.email };
  });
}

export async function entrarComGoogle(idToken: string, nonce: string, agente: string | null): Promise<Sessao> {
  const identidade = await validarTokenDoGoogle(idToken, nonce);
  const usuario = await encontrarOuCriarUsuario(identidade);

  const refresh = randomBytes(32).toString('base64url');
  await pool().query(
    `insert into auth.sessoes (user_id, token_hash, agente, expira_em)
     values ($1, $2, $3, now() + make_interval(days => $4))`,
    [usuario.id, sha256(refresh), agente?.slice(0, 200) ?? null, DIAS_DE_SESSAO_PARADA],
  );

  const acesso = await emitirAcesso(usuario);
  return { access_token: acesso.token, expires_at: acesso.expiraEm, refresh_token: refresh, user: usuario };
}

export async function renovarSessao(refresh: string): Promise<Sessao> {
  if (!refresh) throw new ErroDeAutenticacao('Sessão encerrada. Entre de novo.');
  const { rows } = await pool().query<{ id: string; email: string | null }>(
    `update auth.sessoes s
     set usada_em = now(), expira_em = now() + make_interval(days => $2)
     from auth.users u
     where s.token_hash = $1
       and s.revogada_em is null
       and s.expira_em > now()
       and u.id = s.user_id
     returning u.id, u.email`,
    [sha256(refresh), DIAS_DE_SESSAO_PARADA],
  );
  const usuario = rows[0];
  if (!usuario) throw new ErroDeAutenticacao('Sessão encerrada. Entre de novo.');

  const acesso = await emitirAcesso(usuario);
  return { access_token: acesso.token, expires_at: acesso.expiraEm, refresh_token: refresh, user: usuario };
}

export async function encerrarSessao(refresh: string): Promise<void> {
  if (!refresh) return;
  await pool().query(
    'update auth.sessoes set revogada_em = now() where token_hash = $1 and revogada_em is null',
    [sha256(refresh)],
  );
}
