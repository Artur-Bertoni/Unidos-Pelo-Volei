import { novoId } from '../data/mappers';
import { chamarApi } from './api';

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

  const { url } = await chamarApi<{ url: string }>('PUT', `/arquivos/grupos/${grupoId}/logo-${novoId()}.${extensao}`, arquivo);
  return url;
}
