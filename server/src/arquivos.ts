import { config } from './config.js';
import { comoUsuario, pool, type Usuario } from './db.js';

export class ArquivoRecusado extends Error {
  constructor(
    mensagem: string,
    readonly status: 400 | 403 | 404 | 413 | 415 = 400,
  ) {
    super(mensagem);
  }
}

const NOME_VALIDO = /^[A-Za-z0-9][A-Za-z0-9._-]{0,120}$/;

export const urlDoArquivo = (bucket: string, caminho: string): string =>
  `${config.urlPublica}/arquivos/${bucket}/${caminho}`;

interface Bucket {
  file_size_limit: string | null;
  allowed_mime_types: string[] | null;
}

export async function guardarArquivo(
  usuario: Usuario,
  bucket: string,
  pasta: string,
  nome: string,
  tipo: string,
  conteudo: Buffer,
): Promise<string> {
  if (!NOME_VALIDO.test(pasta) || !NOME_VALIDO.test(nome)) throw new ArquivoRecusado('Nome de arquivo inválido.');

  const { rows } = await pool().query<Bucket>(
    'select file_size_limit, allowed_mime_types from storage.buckets where id = $1',
    [bucket],
  );
  const destino = rows[0];
  if (!destino) throw new ArquivoRecusado('Destino de arquivo desconhecido.', 404);
  if (conteudo.length === 0) throw new ArquivoRecusado('Não foi possível ler a imagem escolhida.');
  if (destino.file_size_limit && conteudo.length > Number(destino.file_size_limit)) {
    throw new ArquivoRecusado('A imagem passa do tamanho permitido. Escolha uma menor.', 413);
  }
  if (destino.allowed_mime_types && !destino.allowed_mime_types.includes(tipo)) {
    throw new ArquivoRecusado('Formato de imagem não aceito.', 415);
  }

  const caminho = `${pasta}/${nome}`;
  try {
    await comoUsuario(usuario, (cliente) =>
      cliente.query(
        `insert into storage.objects (bucket_id, name, owner, content_type, tamanho, conteudo)
         values ($1, $2, $3, $4, $5, $6)`,
        [bucket, caminho, usuario.id, tipo, conteudo.length, conteudo],
      ),
    );
  } catch (erro) {
    const codigo = (erro as { code?: string }).code;
    if (codigo === '42501') throw new ArquivoRecusado('Só a diretoria do grupo pode enviar esta imagem.', 403);
    if (codigo === '23505') throw new ArquivoRecusado('Já existe um arquivo com este nome.');
    throw erro;
  }
  return urlDoArquivo(bucket, caminho);
}

export async function lerArquivo(
  bucket: string,
  caminho: string,
): Promise<{ tipo: string; conteudo: Buffer; criadoEm: Date } | null> {
  const { rows } = await pool().query<{ content_type: string | null; conteudo: Buffer; created_at: Date }>(
    `select o.content_type, o.conteudo, o.created_at
     from storage.objects o
     join storage.buckets b on b.id = o.bucket_id
     where b.public and o.bucket_id = $1 and o.name = $2`,
    [bucket, caminho],
  );
  const linha = rows[0];
  if (!linha?.conteudo) return null;
  return {
    tipo: linha.content_type ?? 'application/octet-stream',
    conteudo: linha.conteudo,
    criadoEm: linha.created_at,
  };
}
