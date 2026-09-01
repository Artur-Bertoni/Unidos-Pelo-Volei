import type { Team, TeamRoster } from '../domain/models';
import { db } from '../lib/powersync/db';
import { agoraIso, novoId, toTeam, type Row } from './mappers';
import { ALL_TEAMS_SQL, TEAMS_SQL } from './queries';

const siglaLimpa = (sigla: string): string => sigla.trim().toUpperCase().slice(0, 2);

export async function lerTimesAtivos(): Promise<Team[]> {
  const linhas = await db.getAll<Row>(TEAMS_SQL);
  return linhas.map((linha) => toTeam(linha));
}

export async function lerTodosOsTimes(): Promise<Team[]> {
  const linhas = await db.getAll<Row>(ALL_TEAMS_SQL);
  return linhas.map((linha) => toTeam(linha));
}

export async function criarTime(
  nome: string,
  corHex: string,
  sigla: string,
  ordem: number,
): Promise<void> {
  const agora = agoraIso();
  await db.execute(
    `INSERT INTO teams (id, nome, cor_hex, sigla, ativo, ordem, created_at, updated_at)
     VALUES (?, ?, ?, ?, 1, ?, ?, ?)`,
    [novoId(), nome.trim(), corHex, siglaLimpa(sigla), ordem, agora, agora],
  );
}

export async function atualizarTime(team: Team): Promise<void> {
  await db.execute(
    `UPDATE teams
     SET nome = ?, cor_hex = ?, sigla = ?, ativo = ?, ordem = ?, updated_at = ?
     WHERE id = ?`,
    [
      team.nome.trim(),
      team.corHex,
      siglaLimpa(team.sigla),
      team.ativo ? 1 : 0,
      team.ordem,
      agoraIso(),
      team.id,
    ],
  );
}

export async function definirTimeAtivo(teamId: string, ativo: boolean): Promise<void> {
  await db.execute('UPDATE teams SET ativo = ?, updated_at = ? WHERE id = ?', [
    ativo ? 1 : 0,
    agoraIso(),
    teamId,
  ]);
}

export async function definirAtivos(ativos: Map<string, boolean>): Promise<void> {
  const agora = agoraIso();
  await db.writeTransaction(async (tx) => {
    for (const [teamId, ativo] of ativos) {
      await tx.execute('UPDATE teams SET ativo = ?, updated_at = ? WHERE id = ? AND ativo <> ?', [
        ativo ? 1 : 0,
        agora,
        teamId,
        ativo ? 1 : 0,
      ]);
    }
  });
}

export async function excluirTime(teamId: string): Promise<void> {
  await db.writeTransaction(async (tx) => {
    await tx.execute('DELETE FROM matches WHERE team_a_id = ? OR team_b_id = ?', [teamId, teamId]);
    await tx.execute('DELETE FROM team_players WHERE team_id = ?', [teamId]);
    await tx.execute('DELETE FROM teams WHERE id = ?', [teamId]);
  });
}

export async function substituirElencos(rosters: readonly TeamRoster[]): Promise<void> {
  await db.writeTransaction(async (tx) => {
    await tx.execute('DELETE FROM team_players');
    for (const roster of rosters) {
      for (const jogador of roster.players) {
        await tx.execute('INSERT INTO team_players (id, team_id, player_id) VALUES (?, ?, ?)', [
          novoId(),
          roster.team.id,
          jogador.id,
        ]);
      }
    }
  });
}
