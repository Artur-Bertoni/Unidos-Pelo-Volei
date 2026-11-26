import { exigirGrupo } from '../data/grupoAtivo';
import { novoId } from '../data/mappers';
import { chamarApi } from './api';

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

  try {
    const { url } = await chamarApi<{ url: string }>('PUT', `/arquivos/mural/${exigirGrupo()}/${novoId()}.${extensao}`, arquivo);
    return url;
  } catch (erro) {
    throw new Error(erro instanceof Error ? erro.message : 'Não foi possível enviar a imagem. Precisa de internet.');
  }
}
