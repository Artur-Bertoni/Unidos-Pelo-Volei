import { pathToFileURL } from 'node:url';
import { GoogleAuth } from 'google-auth-library';
import * as webpush from 'web-push';
import { config } from './config.js';
import { pool } from './db.js';

export type TipoDeAviso = 'convite' | 'cobranca-resposta';

interface CredencialFcm {
  token: string;
  projectId: string;
}

interface Dispositivo {
  id: string;
  token: string;
  plataforma: 'android' | 'web';
}

export function proximoSabado(hoje = new Date()): string {
  const alvo = new Date(Date.UTC(hoje.getUTCFullYear(), hoje.getUTCMonth(), hoje.getUTCDate()));
  alvo.setUTCDate(alvo.getUTCDate() + ((6 - alvo.getUTCDay() + 7) % 7));
  return alvo.toISOString().slice(0, 10);
}

export function tipoDoHorario(agora = new Date()): TipoDeAviso | null {
  const dia = agora.getUTCDay();
  const hora = agora.getUTCHours();
  if (dia === 5 && hora === 22) return 'convite';
  if (dia === 6 && hora === 11) return 'cobranca-resposta';
  return null;
}

async function credencialDoFirebase(): Promise<CredencialFcm | null> {
  if (!config.contaDeServicoFirebase) return null;
  const credenciais = JSON.parse(config.contaDeServicoFirebase);
  const auth = new GoogleAuth({
    credentials: credenciais,
    scopes: ['https://www.googleapis.com/auth/firebase.messaging'],
  });
  const acesso = await (await auth.getClient()).getAccessToken();
  if (!acesso.token) return null;
  return { token: acesso.token, projectId: credenciais.project_id };
}

async function enviarFcm(credencial: CredencialFcm, registro: string, titulo: string, corpo: string): Promise<boolean> {
  const resposta = await fetch(`https://fcm.googleapis.com/v1/projects/${credencial.projectId}/messages:send`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${credencial.token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      message: {
        token: registro,
        notification: { title: titulo, body: corpo },
        android: { priority: 'HIGH' },
      },
    }),
  });
  return resposta.ok;
}

const webPushLigado = (): boolean => config.vapidPublica !== '' && config.vapidPrivada !== '';

async function enviarWebPush(inscricao: string, titulo: string, corpo: string): Promise<boolean> {
  if (!webPushLigado()) return false;
  try {
    await webpush.sendNotification(JSON.parse(inscricao), JSON.stringify({ titulo, corpo }));
    return true;
  } catch {
    return false;
  }
}

async function perfisAlvo(grupoId: string, data: string, somenteSemResposta: boolean): Promise<string[]> {
  const { rows } = await pool().query<{ profile_id: string }>(
    `select distinct p.profile_id
     from public.players p
     where p.grupo_id = $1
       and p.profile_id is not null
       and (not $3 or not exists (
         select 1 from public.presencas r
         where r.grupo_id = $1 and r.data = $2::date and r.profile_id = p.profile_id
       ))`,
    [grupoId, data, somenteSemResposta],
  );
  return rows.map((linha) => linha.profile_id);
}

async function avisarGrupo(
  grupo: { id: string; nome: string },
  tipo: TipoDeAviso,
  data: string,
  titulo: string,
  texto: string,
  fcm: CredencialFcm | null,
): Promise<{ grupo: string; enviados: number; falhas: number }> {
  const perfis = await perfisAlvo(grupo.id, data, tipo === 'cobranca-resposta');
  if (perfis.length === 0) return { grupo: grupo.nome, enviados: 0, falhas: 0 };

  const { rows: dispositivos } = await pool().query<Dispositivo>(
    `select id, token, plataforma from public.dispositivos
     where ativo and profile_id = any($1::uuid[])`,
    [perfis],
  );

  let enviados = 0;
  const mortos: string[] = [];
  for (const dispositivo of dispositivos) {
    const ok =
      dispositivo.plataforma === 'android'
        ? fcm !== null && (await enviarFcm(fcm, dispositivo.token, titulo, texto))
        : await enviarWebPush(dispositivo.token, titulo, texto);
    if (ok) enviados += 1;
    else mortos.push(dispositivo.id);
  }

  if (mortos.length > 0) {
    await pool().query('update public.dispositivos set ativo = false where id = any($1::uuid[])', [mortos]);
  }

  await pool().query(
    `insert into public.avisos (grupo_id, tipo, titulo, corpo, referencia)
     values ($1, 'lembrete', $2, $3, $4)`,
    [grupo.id, titulo, texto, data],
  );

  return { grupo: grupo.nome, enviados, falhas: mortos.length };
}

export async function enviarAvisos(tipo: TipoDeAviso, data = proximoSabado()) {
  if (webPushLigado()) webpush.setVapidDetails(config.vapidAssunto, config.vapidPublica, config.vapidPrivada);

  const [, mes, dia] = data.split('-');
  const diaEMes = `${dia}/${mes}`;
  const titulo = tipo === 'cobranca-resposta' ? 'Você ainda não respondeu' : 'Tem jogo no sábado';
  const texto =
    tipo === 'cobranca-resposta'
      ? `O sábado ${diaEMes} está chegando e a gente ainda não sabe se você vem.`
      : `Sábado ${diaEMes} tem vôlei. Você vai? Responda no app.`;

  const { rows: grupos } = await pool().query<{ id: string; nome: string }>(
    'select id, nome from public.grupos where ativo',
  );
  const fcm = await credencialDoFirebase();

  const resultados = [];
  for (const grupo of grupos) resultados.push(await avisarGrupo(grupo, tipo, data, titulo, texto, fcm));
  return resultados;
}

async function principal(): Promise<void> {
  const pedido = process.argv[2];
  const tipo: TipoDeAviso | null =
    pedido === 'convite' || pedido === 'cobranca-resposta' ? pedido : tipoDoHorario();

  if (!tipo) {
    console.log('Nenhum aviso agendado para este horário.');
    return;
  }

  const resultados = await enviarAvisos(tipo);
  console.log(JSON.stringify({ tipo, resultados }));
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) {
  principal()
    .catch((erro) => {
      console.error(erro);
      process.exitCode = 1;
    })
    .finally(() => pool().end());
}
