import type { Genero, Player } from '../domain/models';
import { db } from '../lib/powersync/db';
import { agoraIso, novoId } from './mappers';

export async function criarJogador(
  nome: string,
  skillLevel: number,
  genero: Genero,
  ativo: boolean,
): Promise<void> {
  const agora = agoraIso();
  await db.execute(
    `INSERT INTO players (id, nome, skill_level, genero, ativo, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?)`,
    [novoId(), nome.trim(), skillLevel, genero, ativo ? 1 : 0, agora, agora],
  );
}

export async function atualizarJogador(player: Player): Promise<void> {
  await db.execute(
    `UPDATE players
     SET nome = ?, skill_level = ?, genero = ?, ativo = ?, updated_at = ?
     WHERE id = ?`,
    [
      player.nome.trim(),
      player.skillLevel,
      player.genero,
      player.ativo ? 1 : 0,
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
  await db.execute('UPDATE players SET ativo = ?, updated_at = ? WHERE ativo <> ?', [
    presente ? 1 : 0,
    agoraIso(),
    presente ? 1 : 0,
  ]);
}

export async function excluirJogador(playerId: string): Promise<void> {
  await db.writeTransaction(async (tx) => {
    await tx.execute('DELETE FROM team_players WHERE player_id = ?', [playerId]);
    await tx.execute('DELETE FROM player_day_stats WHERE player_id = ?', [playerId]);
    await tx.execute('DELETE FROM players WHERE id = ?', [playerId]);
  });
}
