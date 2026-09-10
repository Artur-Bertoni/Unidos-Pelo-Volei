import { novoId } from '../data/mappers';
import { supabase } from './supabase';

const BUCKET = 'grupos';
const LIMITE_DE_BYTES = 2 * 1024 * 1024;

const EXTENSOES: Record<string, string> = {
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
};

export async function enviarLogoDoGrupo(grupoId: string, arquivo: File): Promise<string> {
  if (arquivo.size > LIMITE_DE_BYTES) {
    throw new Error('A logo passa de 2 MB. Escolha uma menor.');
  }

  const extensao = EXTENSOES[arquivo.type];
  if (!extensao) throw new Error('Formato não aceito na logo. Use JPG, PNG ou WEBP.');

  const caminho = `${grupoId}/logo-${novoId()}.${extensao}`;
  const { error } = await supabase.storage
    .from(BUCKET)
    .upload(caminho, arquivo, { contentType: arquivo.type, upsert: false });

  if (error) throw new Error(error.message);

  return supabase.storage.from(BUCKET).getPublicUrl(caminho).data.publicUrl;
}
