import {
  limparCodigo,
  papelDe,
  type ChaveDeAcesso,
  type MembroDoGrupo,
  type MeuGrupo,
  type Papel,
} from '../domain/models';
import { db } from '../lib/powersync/db';
import { supabase } from '../lib/supabase';

/**
 * O Postgres já responde em português nas funções de grupo, e esconder a causa atrás
 * de "precisa de internet" transforma erro de permissão em mistério.
 */
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

  const { data, error } = await supabase.rpc('entrar_no_grupo', { p_codigo: limpo });
  if (error) throw new Error(error.message);

  const resposta = (data as GrupoRespondido[] | null)?.[0];
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

  const { data, error } = await supabase.rpc('criar_grupo', {
    p_nome: nome.trim(),
    p_cidade: cidade?.trim() || null,
  });
  if (error) throw new Error(error.message);

  const resposta = (data as GrupoRespondido[] | null)?.[0];
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
  const { error } = await supabase.rpc('sair_do_grupo', { p_grupo: grupoId });
  if (error) throw new Error(error.message);
}

/** Nome, cidade e logo sobem pelo sync como qualquer outra edição do grupo. */
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
  const { data, error } = await supabase.from('grupo_chaves').select().eq('grupo_id', grupoId);
  if (error) throw new Error(falar(error.message, 'Não foi possível carregar as chaves.'));

  return ((data ?? []) as ChaveLida[])
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
  const { data, error } = await supabase
    .from('grupo_chaves')
    .insert({
      grupo_id: grupoId,
      rotulo: rotulo?.trim() || null,
      papel,
      usos_max: usosMax,
      expira_em: expiraEm,
    })
    .select()
    .single();

  if (error) throw new Error(falar(error.message, 'Não foi possível criar a chave.'));
  return paraChave(data as ChaveLida);
}

export async function definirChaveAtiva(chaveId: string, ativa: boolean): Promise<void> {
  const { error } = await supabase.from('grupo_chaves').update({ ativa }).eq('id', chaveId);
  if (error) throw new Error(falar(error.message, 'Não foi possível mudar a chave.'));
}

export async function excluirChave(chaveId: string): Promise<void> {
  const { error } = await supabase.from('grupo_chaves').delete().eq('id', chaveId);
  if (error) throw new Error(falar(error.message, 'Não foi possível apagar a chave.'));
}

export async function listarMembros(grupoId: string): Promise<MembroDoGrupo[]> {
  const { data, error } = await supabase
    .from('grupo_membros')
    .select('profile_id, papel, profiles(id, nome, email)')
    .eq('grupo_id', grupoId);

  if (error) throw new Error(falar(error.message, 'Não foi possível carregar os membros.'));

  return (data ?? [])
    .map((linha): MembroDoGrupo => {
      const perfil = linha.profiles as { nome?: string | null; email?: string | null } | null;
      return {
        profileId: String(linha.profile_id),
        nome: perfil?.nome ?? null,
        email: perfil?.email ?? null,
        papel: papelDe(String(linha.papel)),
      };
    })
    .sort((a, b) => (a.nome ?? a.email ?? '').localeCompare(b.nome ?? b.email ?? ''));
}

export async function definirPapel(
  grupoId: string,
  profileId: string,
  papel: Papel,
): Promise<void> {
  const { error } = await supabase
    .from('grupo_membros')
    .update({ papel })
    .eq('grupo_id', grupoId)
    .eq('profile_id', profileId);

  if (error) throw new Error(error.message);
}

export async function removerMembro(grupoId: string, profileId: string): Promise<void> {
  const { error } = await supabase
    .from('grupo_membros')
    .delete()
    .eq('grupo_id', grupoId)
    .eq('profile_id', profileId);

  if (error) throw new Error(error.message);
}
