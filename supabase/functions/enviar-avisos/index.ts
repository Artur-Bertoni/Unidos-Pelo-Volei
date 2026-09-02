import { createClient } from 'jsr:@supabase/supabase-js@2';
import { GoogleAuth } from 'npm:google-auth-library@9';
import webpush from 'npm:web-push@3';

interface Dispositivo {
  id: string;
  profile_id: string;
  token: string;
  plataforma: 'android' | 'web';
}

interface Corpo {
  tipo?: 'convite' | 'cobranca-resposta' | 'avulso';
  titulo?: string;
  corpo?: string;
  data?: string;
}

const supabase = createClient(
  Deno.env.get('SUPABASE_URL') ?? '',
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '',
);

const vapidPublica = Deno.env.get('VAPID_PUBLIC_KEY') ?? '';
const vapidPrivada = Deno.env.get('VAPID_PRIVATE_KEY') ?? '';
const vapidAssunto = Deno.env.get('VAPID_SUBJECT') ?? 'mailto:contato@unidospelovolei.app';
const contaDeServico = Deno.env.get('FIREBASE_SERVICE_ACCOUNT') ?? '';

if (vapidPublica && vapidPrivada) {
  webpush.setVapidDetails(vapidAssunto, vapidPublica, vapidPrivada);
}

function proximoSabado(hoje = new Date()): string {
  const alvo = new Date(Date.UTC(hoje.getUTCFullYear(), hoje.getUTCMonth(), hoje.getUTCDate()));
  alvo.setUTCDate(alvo.getUTCDate() + ((6 - alvo.getUTCDay() + 7) % 7));
  return alvo.toISOString().slice(0, 10);
}

async function tokenDoFirebase(): Promise<{ token: string; projectId: string } | null> {
  if (!contaDeServico) return null;
  const credenciais = JSON.parse(contaDeServico);
  const auth = new GoogleAuth({
    credentials: credenciais,
    scopes: ['https://www.googleapis.com/auth/firebase.messaging'],
  });
  const cliente = await auth.getClient();
  const acesso = await cliente.getAccessToken();
  if (!acesso.token) return null;
  return { token: acesso.token, projectId: credenciais.project_id };
}

async function enviarFcm(
  credencial: { token: string; projectId: string },
  registro: string,
  titulo: string,
  corpo: string,
): Promise<boolean> {
  const resposta = await fetch(
    `https://fcm.googleapis.com/v1/projects/${credencial.projectId}/messages:send`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${credencial.token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        message: {
          token: registro,
          notification: { title: titulo, body: corpo },
          android: { priority: 'HIGH' },
        },
      }),
    },
  );
  return resposta.ok;
}

async function enviarWebPush(
  inscricao: string,
  titulo: string,
  corpo: string,
): Promise<boolean> {
  if (!vapidPublica || !vapidPrivada) return false;
  try {
    await webpush.sendNotification(JSON.parse(inscricao), JSON.stringify({ titulo, corpo }));
    return true;
  } catch {
    return false;
  }
}

async function alvosDoConvite(data: string, somenteSemResposta: boolean): Promise<Set<string>> {
  const { data: jogadores } = await supabase
    .from('players')
    .select('id, profile_id')
    .not('profile_id', 'is', null);

  const perfis = new Set((jogadores ?? []).map((j) => j.profile_id as string));
  if (!somenteSemResposta) return perfis;

  const { data: respostas } = await supabase
    .from('presencas')
    .select('profile_id')
    .eq('data', data);

  (respostas ?? []).forEach((r) => {
    if (r.profile_id) perfis.delete(r.profile_id as string);
  });
  return perfis;
}

Deno.serve(async (requisicao) => {
  if (requisicao.method !== 'POST') {
    return new Response('Use POST.', { status: 405 });
  }

  const corpo: Corpo = await requisicao.json().catch(() => ({}));
  const tipo = corpo.tipo ?? 'convite';
  const data = corpo.data ?? proximoSabado();
  const partes = data.split('-');
  const diaEMes = partes.length === 3 ? `${partes[2]}/${partes[1]}` : data;

  const titulo =
    corpo.titulo ??
    (tipo === 'cobranca-resposta' ? 'Você ainda não respondeu' : 'Tem jogo no sábado');
  const texto =
    corpo.corpo ??
    (tipo === 'cobranca-resposta'
      ? `O sábado ${diaEMes} está chegando e a gente ainda não sabe se você vem.`
      : `Sábado ${diaEMes} tem vôlei. Você vai? Responda no app.`);

  const perfis =
    tipo === 'avulso'
      ? null
      : await alvosDoConvite(data, tipo === 'cobranca-resposta');

  let consulta = supabase
    .from('dispositivos')
    .select('id, profile_id, token, plataforma')
    .eq('ativo', true);

  if (perfis !== null) {
    if (perfis.size === 0) {
      return Response.json({ enviados: 0, falhas: 0, motivo: 'ninguem-para-avisar' });
    }
    consulta = consulta.in('profile_id', [...perfis]);
  }

  const { data: dispositivos, error } = await consulta;
  if (error) {
    return Response.json({ erro: error.message }, { status: 500 });
  }

  const credencialFcm = await tokenDoFirebase();
  let enviados = 0;
  const mortos: string[] = [];

  for (const dispositivo of (dispositivos ?? []) as Dispositivo[]) {
    const ok =
      dispositivo.plataforma === 'android'
        ? credencialFcm !== null &&
          (await enviarFcm(credencialFcm, dispositivo.token, titulo, texto))
        : await enviarWebPush(dispositivo.token, titulo, texto);

    if (ok) enviados += 1;
    else mortos.push(dispositivo.id);
  }

  if (mortos.length > 0) {
    await supabase.from('dispositivos').update({ ativo: false }).in('id', mortos);
  }

  await supabase.from('avisos').insert({
    tipo: tipo === 'avulso' ? 'mural' : 'lembrete',
    titulo,
    corpo: texto,
    referencia: data,
  });

  return Response.json({ enviados, falhas: mortos.length });
});
