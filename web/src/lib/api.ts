import { tokenDeAcesso } from './auth';
import { env } from './env';

export class ErroDaApi extends Error {
  constructor(
    mensagem: string,
    readonly status: number,
    readonly codigo: string | null,
  ) {
    super(mensagem);
  }
}

type Metodo = 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE';

async function enviar(metodo: Metodo, caminho: string, token: string | null, corpo?: BodyInit, tipo?: string) {
  try {
    return await fetch(env.apiUrl + caminho, {
      method: metodo,
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(tipo ? { 'Content-Type': tipo } : {}),
      },
      body: corpo,
    });
  } catch {
    throw new ErroDaApi('Sem conexão com o servidor. Precisa de internet.', 0, null);
  }
}

export async function chamarApi<T = unknown>(
  metodo: Metodo,
  caminho: string,
  corpo?: unknown,
  opcoes: { tipo?: string } = {},
): Promise<T> {
  const ehArquivo = corpo instanceof Blob;
  const conteudo = corpo === undefined ? undefined : ehArquivo ? corpo : JSON.stringify(corpo);
  const tipo = corpo === undefined ? undefined : (opcoes.tipo ?? (ehArquivo ? corpo.type : 'application/json'));

  let resposta = await enviar(metodo, caminho, await tokenDeAcesso(), conteudo, tipo);
  if (resposta.status === 401) {
    resposta = await enviar(metodo, caminho, await tokenDeAcesso(true), conteudo, tipo);
  }

  const texto = await resposta.text();
  let json: unknown = null;
  try {
    json = texto ? JSON.parse(texto) : null;
  } catch {
    json = null;
  }

  if (!resposta.ok) {
    const erro = json as { erro?: string; codigo?: string } | null;
    throw new ErroDaApi(erro?.erro ?? `O servidor respondeu ${resposta.status}.`, resposta.status, erro?.codigo ?? null);
  }
  return json as T;
}
