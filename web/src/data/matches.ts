import type { ScheduledRound } from '../domain/roundRobin';
import { db } from '../lib/powersync/db';
import { agoraIso, novoId } from './mappers';

export const PLACAR_MAXIMO = 199;

export async function atualizarPlacar(
  matchId: string,
  scoreA: number,
  scoreB: number,
): Promise<void> {
  await db.execute('UPDATE matches SET score_a = ?, score_b = ?, updated_at = ? WHERE id = ?', [
    Math.max(scoreA, 0),
    Math.max(scoreB, 0),
    agoraIso(),
    matchId,
  ]);
}

export async function finalizarPartida(
  matchId: string,
  scoreA: number,
  scoreB: number,
  teamAId: string,
  teamBId: string,
): Promise<void> {
  const vencedor = scoreA > scoreB ? teamAId : scoreB > scoreA ? teamBId : null;
  await db.execute(
    `UPDATE matches
     SET score_a = ?, score_b = ?, status = ?, winner_id = ?, updated_at = ?
     WHERE id = ?`,
    [scoreA, scoreB, 'finalizado', vencedor, agoraIso(), matchId],
  );
}

export async function reabrirPartida(matchId: string): Promise<void> {
  await db.execute(
    `UPDATE matches
     SET status = ?, winner_id = NULL, updated_at = ?
     WHERE id = ?`,
    ['agendado', agoraIso(), matchId],
  );
}

export async function substituirChaveamento(rodadas: readonly ScheduledRound[]): Promise<void> {
  await db.writeTransaction(async (tx) => {
    await tx.execute('DELETE FROM matches');
    await tx.execute('DELETE FROM rounds');

    for (const rodada of rodadas) {
      const roundId = novoId();
      const agora = agoraIso();
      await tx.execute('INSERT INTO rounds (id, numero, fase, created_at) VALUES (?, ?, ?, ?)', [
        roundId,
        rodada.numero,
        rodada.fase,
        agora,
      ]);
      for (const partida of rodada.matches) {
        await tx.execute(
          `INSERT INTO matches (
              id, round_id, quadra, team_a_id, team_b_id,
              score_a, score_b, status, winner_id, created_at, updated_at
           ) VALUES (?, ?, ?, ?, ?, 0, 0, ?, NULL, ?, ?)`,
          [
            novoId(),
            roundId,
            partida.quadra,
            partida.teamA.id,
            partida.teamB.id,
            'agendado',
            agora,
            agora,
          ],
        );
      }
    }
  });
}

export async function apagarResultados(): Promise<void> {
  await db.execute(
    `UPDATE matches
     SET score_a = 0, score_b = 0, status = ?, winner_id = NULL, updated_at = ?`,
    ['agendado', agoraIso()],
  );
}
