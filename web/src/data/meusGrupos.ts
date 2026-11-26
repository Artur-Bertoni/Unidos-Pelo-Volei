import {
  limparCodigo,
  papelDe,
  type ChaveDeAcesso,
  type MembroDoGrupo,
  type MeuGrupo,
  type Papel,
} from '../domain/models';
import { db } from '../lib/powersync/db';
import { chamarApi } from '../lib/api';

const falar = (mensagem: string, padrao: string): string =>
  mensagem.trim().length > 0 ? mensagem : padrao;

interface GrupoRespondido {
  grupo_id: string;
  nome: string;
  papel: string;
}

interface ChaveLida {
  id: string;
  codigo: string;
  rotulo: string | null;
  papel: string;
  usos: number;
  usos_max: number | null;
  expira_em: string | null;
  ativa: boolean;
}

interface MembroLido {
  profile_id: string;
  papel: string;
  nome: string | null;
  email: string | null;
}

const paraChave = (linha: ChaveLida): ChaveDeAcesso => ({
  id: linha.id,
  codigo: linha.codigo,
  rotulo: linha.rotulo,
  papel: papelDe(linha.papel),
  usos: linha.usos,
  usosMax: linha.usos_max,
  expiraEm: linha.expira_em,
  ativa: linha.ativa,
});

export async function entrarComChave(codigo: string): Promise<MeuGrupo> {
  const limpo = limparCodigo(codigo);
  if (limpo.length < 4) throw new Error('Digite a chave que a diretoria do grupo passou.');

  const resposta = (await chamarApi<GrupoRespondido[]>('POST', '/rpc/entrar_no_grupo', { p_codigo: limpo }))[0];
  if (!resposta) throw new Error('Chave não encontrada.');

  return {
    id: resposta.grupo_id,
    nome: resposta.nome,
    cidade: null,
    papel: papelDe(resposta.papel),
    logoUrl: null,
    ativo: true,
  };
}

export async function criarGrupo(nome: string, cidade: string | null): Promise<MeuGrupo> {
  if (nome.trim().length === 0) throw new Error('O grupo precisa de um nome.');

  const resposta = (
    await chamarApi<GrupoRespondido[]>('POST', '/rpc/criar_grupo', {
      p_nome: nome.trim(),
      p_cidade: cidade?.trim() || null,
    })
  )[0];
  if (!resposta) throw new Error('Não foi possível criar o grupo.');

  return {
    id: resposta.grupo_id,
    nome: resposta.nome,
    cidade: cidade?.trim() || null,
    papel: 'diretoria',
    logoUrl: null,
    ativo: true,
  };
}

export async function sairDoGrupo(grupoId: string): Promise<void> {
  await chamarApi('POST', '/rpc/sair_do_grupo', { p_grupo: grupoId });
}

export async function salvarIdentidade(
  grupoId: string,
  nome: string,
  cidade: string | null,
  logoUrl: string | null,
): Promise<void> {
  if (nome.trim().length === 0) throw new Error('O grupo precisa de um nome.');
  await db.execute('UPDATE grupos SET nome = ?, cidade = ?, logo_url = ? WHERE id = ?', [
    nome.trim(),
    cidade?.trim() || null,
    logoUrl,
    grupoId,
  ]);
}

export async function listarChaves(grupoId: string): Promise<ChaveDeAcesso[]> {
  const linhas = await chamarApi<ChaveLida[]>('GET', `/grupos/${grupoId}/chaves`).catch((erro: Error) => {
    throw new Error(falar(erro.message, 'Não foi possível carregar as chaves.'));
  });

  return linhas
    .map(paraChave)
    .sort(
      (a, b) =>
        Number(b.ativa) - Number(a.ativa) || (a.rotulo ?? '').localeCompare(b.rotulo ?? ''),
    );
}

export async function criarChave(
  grupoId: string,
  rotulo: string | null,
  papel: Papel,
  usosMax: number | null,
  expiraEm: string | null,
): Promise<ChaveDeAcesso> {
  const criada = await chamarApi<ChaveLida>('POST', `/grupos/${grupoId}/chaves`, {
    rotulo: rotulo?.trim() || null,
    papel,
    usos_max: usosMax,
    expira_em: expiraEm,
  }).catch((erro: Error) => {
    throw new Error(falar(erro.message, 'Não foi possível criar a chave.'));
  });
  return paraChave(criada);
}

export async function definirChaveAtiva(chaveId: string, ativa: boolean): Promise<void> {
  await chamarApi('PATCH', `/chaves/${chaveId}`, { ativa }).catch((erro: Error) => {
    throw new Error(falar(erro.message, 'Não foi possível mudar a chave.'));
  });
}

export async function excluirChave(chaveId: string): Promise<void> {
  await chamarApi('DELETE', `/chaves/${chaveId}`).catch((erro: Error) => {
    throw new Error(falar(erro.message, 'Não foi possível apagar a chave.'));
  });
}

export async function listarMembros(grupoId: string): Promise<MembroDoGrupo[]> {
  const linhas = await chamarApi<MembroLido[]>('GET', `/grupos/${grupoId}/membros`).catch((erro: Error) => {
    throw new Error(falar(erro.message, 'Não foi possível carregar os membros.'));
  });

  return linhas
    .map(
      (linha): MembroDoGrupo => ({
        profileId: linha.profile_id,
        nome: linha.nome,
        email: linha.email,
        papel: papelDe(linha.papel),
      }),
    )
    .sort((a, b) => (a.nome ?? a.email ?? '').localeCompare(b.nome ?? b.email ?? ''));
}

export async function definirPapel(
  grupoId: string,
  profileId: string,
  papel: Papel,
): Promise<void> {
  await chamarApi('PATCH', `/grupos/${grupoId}/membros/${profileId}`, { papel });
}

export async function removerMembro(grupoId: string, profileId: string): Promise<void> {
  await chamarApi('DELETE', `/grupos/${grupoId}/membros/${profileId}`);
}
