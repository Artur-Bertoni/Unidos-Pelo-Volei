import { novoId } from '../data/mappers';
import { supabase } from './supabase';

const BUCKET = 'mural';
const LIMITE_DE_BYTES = 5 * 1024 * 1024;

const EXTENSOES: Record<string, string> = {
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
  'image/gif': 'gif',
};

export async function enviarImagemDoMural(arquivo: File): Promise<string> {
  if (arquivo.size > LIMITE_DE_BYTES) {
    throw new Error('A imagem passa de 5 MB. Escolha uma menor.');
  }

  const extensao = EXTENSOES[arquivo.type];
  if (!extensao) throw new Error('Formato não aceito. Use JPG, PNG, WEBP ou GIF.');

  const caminho = `${novoId()}.${extensao}`;
  const { error } = await supabase.storage
    .from(BUCKET)
    .upload(caminho, arquivo, { contentType: arquivo.type, upsert: false });

  if (error) throw new Error('Não foi possível enviar a imagem. Precisa de internet.');

  return supabase.storage.from(BUCKET).getPublicUrl(caminho).data.publicUrl;
}
