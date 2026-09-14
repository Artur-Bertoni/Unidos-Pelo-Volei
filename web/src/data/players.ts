import { mediaDasNotas, type Genero, type NotasPorFundamento, type Player } from '../domain/models';
import { db } from '../lib/powersync/db';
import { exigirGrupo } from './grupoAtivo';
import { agoraIso, novoId } from './mappers';

const COLUNAS_DAS_NOTAS = `nota_saque = ?, nota_passe = ?, nota_ataque = ?,
         nota_bloqueio = ?, nota_defesa = ?, nota_atitude = ?, nota_media = ?, skill_level = ?`;

const valoresDasNotas = (notas: NotasPorFundamento): number[] => {
  const media = mediaDasNotas(notas);
  return [
    notas.saque,
    notas.passe,
    notas.ataque,
    notas.bloqueio,
    notas.defesa,
    notas.atitude,
    media,
    Math.min(5, Math.max(1, Math.round(media))),
  ];
};

export async function criarJogador(
  nome: string,
  notas: NotasPorFundamento,
  genero: Genero,
  ativo: boolean,
): Promise<void> {
  const agora = agoraIso();
  await db.execute(
    `INSERT INTO players (id, grupo_id, nome, nota_saque, nota_passe, nota_ataque,
         nota_bloqueio, nota_defesa, nota_atitude, nota_media, skill_level,
         genero, ativo, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      novoId(),
      exigirGrupo(),
      nome.trim(),
      ...valoresDasNotas(notas),
      genero,
      ativo ? 1 : 0,
      agora,
      agora,
    ],
  );
}

export async function atualizarJogador(player: Player): Promise<void> {
  await db.execute(
    `UPDATE players
     SET nome = ?, ${COLUNAS_DAS_NOTAS}, genero = ?, ativo = ?, regime = ?, updated_at = ?
     WHERE id = ?`,
    [
      player.nome.trim(),
      ...valoresDasNotas(player.notas),
      player.genero,
      player.ativo ? 1 : 0,
      player.regime,
      agoraIso(),
      player.id,
    ],
  );
}

export async function definirPresenca(playerId: string, ativo: boolean): Promise<void> {
  await db.execute('UPDATE players SET ativo = ?, updated_at = ? WHERE id = ?', [
    ativo ? 1 : 0,
    agoraIso(),
    playerId,
  ]);
}

export async function definirPresencaDeTodos(presente: boolean): Promise<void> {
  await db.execute(
    'UPDATE players SET ativo = ?, updated_at = ? WHERE grupo_id = ? AND ativo <> ?',
    [presente ? 1 : 0, agoraIso(), exigirGrupo(), presente ? 1 : 0],
  );
}

export async function excluirJogador(playerId: string): Promise<void> {
  await db.writeTransaction(async (tx) => {
    await tx.execute('DELETE FROM team_players WHERE player_id = ?', [playerId]);
    await tx.execute('DELETE FROM player_day_stats WHERE player_id = ?', [playerId]);
    await tx.execute('DELETE FROM player_contatos WHERE player_id = ?', [playerId]);
    await tx.execute('DELETE FROM vinculo_pedidos WHERE player_id = ?', [playerId]);
    await tx.execute('DELETE FROM players WHERE id = ?', [playerId]);
  });
}
