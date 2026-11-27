import { readFile } from 'node:fs/promises';
import { parseArgs } from 'node:util';
import { JWT } from 'google-auth-library';

const PACOTE = 'com.unidospelovolei';
const API = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACOTE}`;
const UPLOAD = `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${PACOTE}`;
const COMPARTILHAMENTO = `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/internalappsharing/${PACOTE}/artifacts/bundle`;

const { positionals, values } = parseArgs({
  allowPositionals: true,
  options: {
    credencial: { type: 'string' },
    faixa: { type: 'string' },
    aab: { type: 'string' },
    notas: { type: 'string' },
    nome: { type: 'string' },
  },
});

class ErroDaPlay extends Error {
  constructor(
    readonly status: number,
    mensagem: string,
  ) {
    super(mensagem);
  }
}

interface Versao {
  name?: string;
  versionCodes?: string[];
  status?: string;
}

interface Faixa {
  track: string;
  releases?: Versao[];
}

const exigir = (nome: keyof typeof values): string => {
  const valor = values[nome];
  if (typeof valor !== 'string' || valor.trim() === '') throw new Error(`Faltou o parâmetro --${nome}.`);
  return valor;
};

let auth: JWT | null = null;

async function autenticacao(): Promise<JWT> {
  if (auth) return auth;
  const caminho = exigir('credencial');
  let credenciais;
  try {
    credenciais = JSON.parse(await readFile(caminho, 'utf8'));
  } catch {
    throw new Error(`Não consegui ler a credencial da Play Store em ${caminho}.`);
  }
  if (!credenciais?.client_email || !credenciais?.private_key) {
    throw new Error(`O arquivo ${caminho} não é a chave JSON de uma conta de serviço.`);
  }
  auth = new JWT({
    email: credenciais.client_email,
    key: credenciais.private_key,
    scopes: ['https://www.googleapis.com/auth/androidpublisher'],
  });
  return auth;
}

async function chamar<T>(metodo: string, url: string, corpo?: unknown, arquivo?: Buffer): Promise<T> {
  const { token } = await (await autenticacao()).getAccessToken();
  const cabecalhos: Record<string, string> = { Authorization: `Bearer ${token}` };
  if (arquivo) cabecalhos['Content-Type'] = 'application/octet-stream';
  else if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json';

  const resposta = await fetch(url, {
    method: metodo,
    headers: cabecalhos,
    body: arquivo ? new Uint8Array(arquivo) : corpo === undefined ? undefined : JSON.stringify(corpo),
  });
  const texto = await resposta.text();
  let json: unknown = {};
  try {
    json = texto ? JSON.parse(texto) : {};
  } catch {
    json = {};
  }
  if (!resposta.ok) {
    const mensagem = (json as { error?: { message?: string } }).error?.message ?? texto;
    throw new ErroDaPlay(resposta.status, mensagem);
  }
  return json as T;
}

const abrirEdicao = async (): Promise<string> => (await chamar<{ id: string }>('POST', `${API}/edits`, {})).id;

const descartarEdicao = (edicao: string): Promise<unknown> =>
  chamar('DELETE', `${API}/edits/${edicao}`).catch(() => undefined);

async function listarFaixas(): Promise<void> {
  const edicao = await abrirEdicao();
  try {
    const { tracks = [] } = await chamar<{ tracks?: Faixa[] }>('GET', `${API}/edits/${edicao}/tracks`);
    const { bundles = [] } = await chamar<{ bundles?: { versionCode: number }[] }>(
      'GET',
      `${API}/edits/${edicao}/bundles`,
    );
    const codigosNasFaixas = tracks.flatMap((faixa) =>
      (faixa.releases ?? []).flatMap((versao) => (versao.versionCodes ?? []).map(Number)),
    );
    const maiorVersionCode = Math.max(0, ...bundles.map((bundle) => bundle.versionCode), ...codigosNasFaixas);

    const faixas = tracks.map((faixa) => {
      const atual = (faixa.releases ?? []).find((versao) => (versao.versionCodes ?? []).length > 0);
      return {
        faixa: faixa.track,
        versaoAtual: atual ? `${atual.name ?? ''} (${(atual.versionCodes ?? []).join(', ')}) ${atual.status ?? ''}`.trim() : '',
      };
    });

    process.stdout.write(JSON.stringify({ faixas, maiorVersionCode }));
  } finally {
    await descartarEdicao(edicao);
  }
}

async function enviar(): Promise<void> {
  const faixa = (values.faixa ?? process.env.PLAY_FAIXA ?? '').trim();
  if (!faixa) throw new Error('Faltou a faixa de destino.');
  const notas = (values.notas ?? process.env.PLAY_NOTAS ?? '').trim();
  if (!notas) throw new Error('Faltaram as notas da versão.');
  if (notas.length > 500) throw new Error('As notas da versão passam de 500 caracteres, o limite da Play Store.');
  const arquivo = await readFile(exigir('aab'));

  const edicao = await abrirEdicao();
  let concluida = false;
  try {
    const { versionCode } = await chamar<{ versionCode: number }>(
      'POST',
      `${UPLOAD}/edits/${edicao}/bundles?uploadType=media`,
      undefined,
      arquivo,
    );

    const publicar = (status: string) =>
      chamar('PUT', `${API}/edits/${edicao}/tracks/${encodeURIComponent(faixa)}`, {
        track: faixa,
        releases: [
          {
            name: values.nome ?? String(versionCode),
            versionCodes: [String(versionCode)],
            status,
            releaseNotes: [{ language: 'pt-BR', text: notas }],
          },
        ],
      });

    let rascunho = false;
    try {
      await publicar('completed');
    } catch (erro) {
      if (!(erro instanceof ErroDaPlay) || !/draft/i.test(erro.message)) throw erro;
      await publicar('draft');
      rascunho = true;
    }

    let enviadoParaAnalise = true;
    try {
      await chamar('POST', `${API}/edits/${edicao}:commit`);
    } catch (erro) {
      if (!(erro instanceof ErroDaPlay) || !/changesNotSentForReview/i.test(erro.message)) throw erro;
      await chamar('POST', `${API}/edits/${edicao}:commit?changesNotSentForReview=true`);
      enviadoParaAnalise = false;
    }
    concluida = true;

    process.stdout.write(JSON.stringify({ versionCode, faixa, rascunho, enviadoParaAnalise }));
  } finally {
    if (!concluida) await descartarEdicao(edicao);
  }
}

async function compartilhar(): Promise<void> {
  const arquivo = await readFile(exigir('aab'));
  const { downloadUrl } = await chamar<{ downloadUrl: string }>(
    'POST',
    `${COMPARTILHAMENTO}?uploadType=media`,
    undefined,
    arquivo,
  );
  process.stdout.write(JSON.stringify({ downloadUrl }));
}

const comandos: Record<string, () => Promise<void>> = { faixas: listarFaixas, enviar, compartilhar };

const comando = comandos[positionals[0] ?? ''];
if (!comando) {
  console.error('Uso: play-store.ts <faixas|enviar|compartilhar> --credencial <arquivo.json> [...]');
  process.exit(1);
}

comando().catch((erro: unknown) => {
  const mensagem = erro instanceof Error ? erro.message : String(erro);
  const status = erro instanceof ErroDaPlay ? ` (HTTP ${erro.status})` : '';
  console.error(`A Play Store recusou${status}: ${mensagem}`);
  process.exit(1);
});
