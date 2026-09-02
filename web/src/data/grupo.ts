import type {
  CategoriaPagina,
  NotasDaAvaliacao,
  StatusPagamento,
  StatusPresenca,
  TipoEvento,
} from '../domain/models';
import { db } from '../lib/powersync/db';
import { agoraIso, novoId } from './mappers';

export function proximoSabado(hoje = new Date()): string {
  const alvo = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate());
  const faltam = (6 - alvo.getDay() + 7) % 7;
  alvo.setDate(alvo.getDate() + faltam);
  const mes = String(alvo.getMonth() + 1).padStart(2, '0');
  const dia = String(alvo.getDate()).padStart(2, '0');
  return `${alvo.getFullYear()}-${mes}-${dia}`;
}

export async function responderChamada(
  playerId: string,
  data: string,
  status: StatusPresenca,
  peloProprio: boolean,
  registradoPor: string,
): Promise<void> {
  const agora = agoraIso();
  const origem = peloProprio ? 'atleta' : 'diretoria';

  await db.writeTransaction(async (tx) => {
    const existente = await tx.getOptional<{ id: string }>(
      'SELECT id FROM presencas WHERE player_id = ? AND data = ?',
      [playerId, data],
    );

    if (existente) {
      await tx.execute(
        `UPDATE presencas
         SET status = ?, origem = ?, registrado_por = ?, atualizado_em = ?
         WHERE id = ?`,
        [status, origem, registradoPor, agora, existente.id],
      );
    } else {
      await tx.execute(
        `INSERT INTO presencas (id, player_id, data, status, origem, registrado_por, atualizado_em)
         VALUES (?, ?, ?, ?, ?, ?, ?)`,
        [novoId(), playerId, data, status, origem, registradoPor, agora],
      );
    }
  });
}

export async function trazerConfirmados(data: string): Promise<number> {
  return db.writeTransaction(async (tx) => {
    const linhas = await tx.getAll<{ player_id: string }>(
      'SELECT player_id FROM presencas WHERE data = ? AND status = ?',
      [data, 'vou'],
    );
    const ids = linhas.map((linha) => linha.player_id);
    if (ids.length === 0) return 0;

    const agora = agoraIso();
    const marcadores = ids.map(() => '?').join(',');
    await tx.execute('UPDATE players SET ativo = 0, updated_at = ? WHERE ativo = 1', [agora]);
    await tx.execute(
      `UPDATE players SET ativo = 1, updated_at = ? WHERE id IN (${marcadores})`,
      [agora, ...ids],
    );
    return ids.length;
  });
}

export async function registrarDispositivo(
  profileId: string,
  token: string,
  plataforma: 'android' | 'web',
): Promise<void> {
  const agora = agoraIso();
  await db.writeTransaction(async (tx) => {
    const existente = await tx.getOptional<{ id: string }>(
      'SELECT id FROM dispositivos WHERE token = ?',
      [token],
    );
    if (existente) {
      await tx.execute(
        'UPDATE dispositivos SET profile_id = ?, ativo = 1, visto_em = ? WHERE id = ?',
        [profileId, agora, existente.id],
      );
    } else {
      await tx.execute(
        `INSERT INTO dispositivos (id, profile_id, token, plataforma, ativo, visto_em, criado_em)
         VALUES (?, ?, ?, ?, 1, ?, ?)`,
        [novoId(), profileId, token, plataforma, agora, agora],
      );
    }
  });
}

export async function publicarPost(
  autorProfileId: string,
  autorNome: string | null,
  titulo: string,
  corpo: string,
  fixado: boolean,
): Promise<void> {
  const agora = agoraIso();
  await db.execute(
    `INSERT INTO posts (id, autor_profile_id, autor_nome, titulo, corpo, fixado, publicado_em, atualizado_em)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
    [novoId(), autorProfileId, autorNome, titulo.trim(), corpo.trim(), fixado ? 1 : 0, agora, agora],
  );
}

export async function excluirPost(postId: string): Promise<void> {
  await db.writeTransaction(async (tx) => {
    await tx.execute('DELETE FROM post_reacoes WHERE post_id = ?', [postId]);
    await tx.execute('DELETE FROM posts WHERE id = ?', [postId]);
  });
}

export async function alternarReacao(postId: string, profileId: string): Promise<void> {
  await db.writeTransaction(async (tx) => {
    const existente = await tx.getOptional<{ id: string }>(
      'SELECT id FROM post_reacoes WHERE post_id = ? AND profile_id = ?',
      [postId, profileId],
    );
    if (existente) {
      await tx.execute('DELETE FROM post_reacoes WHERE id = ?', [existente.id]);
    } else {
      await tx.execute(
        'INSERT INTO post_reacoes (id, post_id, profile_id, emoji, criado_em) VALUES (?, ?, ?, ?, ?)',
        [novoId(), postId, profileId, '👏', agoraIso()],
      );
    }
  });
}

const ouNulo = (valor: string | null): string | null => {
  const limpo = valor?.trim() ?? '';
  return limpo.length > 0 ? limpo : null;
};

export async function salvarEvento(
  eventoId: string | null,
  titulo: string,
  descricao: string | null,
  tipo: TipoEvento,
  inicio: string,
  local: string | null,
  criadoPor: string,
): Promise<void> {
  if (eventoId === null) {
    await db.execute(
      `INSERT INTO eventos (id, titulo, descricao, tipo, inicio, local, criado_por, criado_em)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [novoId(), titulo.trim(), ouNulo(descricao), tipo, inicio, ouNulo(local), criadoPor, agoraIso()],
    );
  } else {
    await db.execute(
      `UPDATE eventos SET titulo = ?, descricao = ?, tipo = ?, inicio = ?, local = ? WHERE id = ?`,
      [titulo.trim(), ouNulo(descricao), tipo, inicio, ouNulo(local), eventoId],
    );
  }
}

export async function excluirEvento(eventoId: string): Promise<void> {
  await db.execute('DELETE FROM eventos WHERE id = ?', [eventoId]);
}

export async function salvarPagina(
  paginaId: string,
  titulo: string,
  corpo: string,
  atualizadoPor: string,
): Promise<void> {
  await db.execute(
    'UPDATE paginas SET titulo = ?, corpo = ?, atualizado_por = ?, atualizado_em = ? WHERE id = ?',
    [titulo.trim(), corpo, atualizadoPor, agoraIso(), paginaId],
  );
}

export async function criarPagina(
  categoria: CategoriaPagina,
  titulo: string,
  atualizadoPor: string,
): Promise<void> {
  const slug =
    titulo
      .trim()
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '') || `pagina-${novoId().slice(0, 8)}`;

  await db.execute(
    `INSERT INTO paginas (id, slug, categoria, titulo, corpo, ordem, atualizado_por, atualizado_em)
     VALUES (?, ?, ?, ?, '', 99, ?, ?)`,
    [novoId(), slug, categoria, titulo.trim(), atualizadoPor, agoraIso()],
  );
}

export async function excluirPagina(paginaId: string): Promise<void> {
  await db.execute('DELETE FROM paginas WHERE id = ?', [paginaId]);
}

export async function salvarConfigFinanceiro(
  id: string,
  pixChave: string | null,
  pixNome: string | null,
  pixCidade: string | null,
  mensalidadeCentavos: number,
  diariaCentavos: number,
): Promise<void> {
  await db.execute(
    `UPDATE config_financeiro
     SET pix_chave = ?, pix_nome = ?, pix_cidade = ?,
         mensalidade_centavos = ?, diaria_centavos = ?, atualizado_em = ?
     WHERE id = ?`,
    [
      ouNulo(pixChave),
      ouNulo(pixNome),
      ouNulo(pixCidade),
      mensalidadeCentavos,
      diariaCentavos,
      agoraIso(),
      id,
    ],
  );
}

const MESES = [
  'janeiro',
  'fevereiro',
  'março',
  'abril',
  'maio',
  'junho',
  'julho',
  'agosto',
  'setembro',
  'outubro',
  'novembro',
  'dezembro',
];

async function gerarCobranca(
  titulo: string,
  tipo: 'mensalidade' | 'diaria',
  valorCentavos: number,
  competencia: string,
  venceEm: string,
  criadoPor: string,
  regime: string,
  somentePresentes: boolean,
): Promise<number> {
  if (valorCentavos <= 0) return 0;

  return db.writeTransaction(async (tx) => {
    const jaExiste = await tx.getOptional<{ id: string }>(
      'SELECT id FROM cobrancas WHERE tipo = ? AND competencia = ?',
      [tipo, competencia],
    );
    if (jaExiste) return 0;

    const filtro = somentePresentes ? ' AND ativo = 1' : '';
    const alvos = await tx.getAll<{ id: string }>(
      `SELECT id FROM players WHERE regime = ?${filtro}`,
      [regime],
    );
    if (alvos.length === 0) return 0;

    const cobrancaId = novoId();
    const agora = agoraIso();
    await tx.execute(
      `INSERT INTO cobrancas (id, titulo, tipo, valor_centavos, competencia, vence_em, criado_por, criado_em)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [cobrancaId, titulo, tipo, valorCentavos, competencia, venceEm, criadoPor, agora],
    );
    for (const alvo of alvos) {
      await tx.execute(
        `INSERT INTO pagamentos (id, cobranca_id, player_id, valor_centavos, status, criado_em)
         VALUES (?, ?, ?, ?, 'pendente', ?)`,
        [novoId(), cobrancaId, alvo.id, valorCentavos, agora],
      );
    }
    return alvos.length;
  });
}

const iso = (data: Date): string =>
  `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(
    data.getDate(),
  ).padStart(2, '0')}`;

export async function gerarMensalidade(
  valorCentavos: number,
  criadoPor: string,
  hoje = new Date(),
): Promise<number> {
  const primeiro = new Date(hoje.getFullYear(), hoje.getMonth(), 1);
  const vence = new Date(hoje.getFullYear(), hoje.getMonth(), 10);
  return gerarCobranca(
    `Mensalidade ${MESES[hoje.getMonth()]} de ${hoje.getFullYear()}`,
    'mensalidade',
    valorCentavos,
    iso(primeiro),
    iso(vence),
    criadoPor,
    'mensalista',
    false,
  );
}

export async function gerarDiaria(
  valorCentavos: number,
  criadoPor: string,
  hoje = new Date(),
): Promise<number> {
  return gerarCobranca(
    `Diária de ${hoje.getDate()}/${hoje.getMonth() + 1}`,
    'diaria',
    valorCentavos,
    iso(hoje),
    iso(hoje),
    criadoPor,
    'diarista',
    true,
  );
}

export async function definirStatusDoPagamento(
  pagamentoId: string,
  status: StatusPagamento,
  registradoPor: string,
): Promise<void> {
  await db.execute(
    'UPDATE pagamentos SET status = ?, pago_em = ?, registrado_por = ? WHERE id = ?',
    [status, status === 'pago' ? agoraIso() : null, registradoPor, pagamentoId],
  );
}

export async function enviarAvaliacao(
  dayId: string,
  avaliadorPlayerId: string,
  avaliadoPlayerId: string,
  notas: NotasDaAvaliacao,
): Promise<void> {
  await db.execute(
    `INSERT INTO avaliacoes (
       id, day_id, avaliador_player_id, avaliado_player_id,
       saque, passe, ataque, bloqueio, defesa, atitude, criado_em
     ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      novoId(),
      dayId,
      avaliadorPlayerId,
      avaliadoPlayerId,
      notas.saque,
      notas.passe,
      notas.ataque,
      notas.bloqueio,
      notas.defesa,
      notas.atitude,
      agoraIso(),
    ],
  );
}
