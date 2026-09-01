import type { ResumoDoDia } from '../domain/models';
import type { ElencoPassado } from '../domain/teamDraft';
import { db } from '../lib/powersync/db';
import { agoraIso, inteiro, novoId, texto, textoOuNulo, type Row } from './mappers';
import { ELENCOS_SQL } from './queries';

interface Desempenho {
  jogos: number;
  vitorias: number;
  derrotas: number;
  pontosPro: number;
  pontosContra: number;
}

const zerado = (): Desempenho => ({
  jogos: 0,
  vitorias: 0,
  derrotas: 0,
  pontosPro: 0,
  pontosContra: 0,
});

/**
 * Agrupa as linhas por dia e por time, dando peso decrescente aos dias mais
 * antigos: o sábado passado pesa 1, o retrasado 1/2, e assim por diante.
 */
export function agruparEmElencos(
  linhas: readonly { dayId: string; teamId: string; playerId: string }[],
): ElencoPassado[] {
  const porDia = new Map<string, Map<string, string[]>>();
  linhas.forEach(({ dayId, teamId, playerId }) => {
    const times = porDia.get(dayId) ?? new Map<string, string[]>();
    const jogadores = times.get(teamId) ?? [];
    jogadores.push(playerId);
    times.set(teamId, jogadores);
    porDia.set(dayId, times);
  });

  const elencos: ElencoPassado[] = [];
  [...porDia.values()].forEach((times, indice) => {
    const peso = 1.0 / (1.0 + indice);
    times.forEach((jogadores) => elencos.push({ jogadores: [...jogadores], peso }));
  });
  return elencos;
}

export async function lerElencosPassados(): Promise<ElencoPassado[]> {
  const linhas = await db.getAll<Row>(ELENCOS_SQL);
  return agruparEmElencos(
    linhas.map((linha) => ({
      dayId: texto(linha, 'day_id'),
      teamId: texto(linha, 'team_id'),
      playerId: texto(linha, 'player_id'),
    })),
  );
}

export async function encerrarDia(): Promise<ResumoDoDia> {
  return db.writeTransaction(async (tx) => {
    const vinculos = (
      await tx.getAll<Row>(
        `SELECT tp.team_id, tp.player_id, t.nome AS team_nome, t.cor_hex AS team_cor_hex
         FROM team_players tp
         JOIN teams t ON t.id = tp.team_id`,
      )
    ).map((linha) => ({
      teamId: texto(linha, 'team_id'),
      playerId: texto(linha, 'player_id'),
      teamNome: textoOuNulo(linha, 'team_nome'),
      teamCorHex: textoOuNulo(linha, 'team_cor_hex'),
    }));

    const presentes = (await tx.getAll<Row>('SELECT id FROM players WHERE ativo = 1')).map((linha) =>
      texto(linha, 'id'),
    );

    const partidas = (
      await tx.getAll<Row>(
        `SELECT team_a_id, team_b_id, score_a, score_b, winner_id
         FROM matches
         WHERE status = ?`,
        ['finalizado'],
      )
    ).map((linha) => ({
      teamAId: texto(linha, 'team_a_id'),
      teamBId: texto(linha, 'team_b_id'),
      scoreA: inteiro(linha, 'score_a'),
      scoreB: inteiro(linha, 'score_b'),
      winnerId: textoOuNulo(linha, 'winner_id'),
    }));

    const porTime = new Map<string, Desempenho>();
    const acumular = (
      teamId: string,
      pontosPro: number,
      pontosContra: number,
      venceu: boolean,
    ): void => {
      const atual = porTime.get(teamId) ?? zerado();
      porTime.set(teamId, {
        jogos: atual.jogos + 1,
        vitorias: atual.vitorias + (venceu ? 1 : 0),
        derrotas: atual.derrotas + (venceu ? 0 : 1),
        pontosPro: atual.pontosPro + pontosPro,
        pontosContra: atual.pontosContra + pontosContra,
      });
    };

    partidas.forEach((partida) => {
      acumular(partida.teamAId, partida.scoreA, partida.scoreB, partida.winnerId === partida.teamAId);
      acumular(partida.teamBId, partida.scoreB, partida.scoreA, partida.winnerId === partida.teamBId);
    });

    const comTime = new Set(vinculos.map((vinculo) => vinculo.playerId));
    const presentesSemTime = presentes.filter((playerId) => !comTime.has(playerId));

    if (vinculos.length > 0 || presentesSemTime.length > 0) {
      const diaId = novoId();
      const agora = agoraIso();
      await tx.execute(
        'INSERT INTO game_days (id, encerrado_em, partidas, created_at) VALUES (?, ?, ?, ?)',
        [diaId, agora, partidas.length, agora],
      );

      for (const vinculo of vinculos) {
        const desempenho = porTime.get(vinculo.teamId) ?? zerado();
        await tx.execute(
          `INSERT INTO player_day_stats (
              id, day_id, player_id, team_id, team_nome, team_cor_hex,
              jogos, vitorias, derrotas, pontos_pro, pontos_contra, created_at
           ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
          [
            novoId(),
            diaId,
            vinculo.playerId,
            vinculo.teamId,
            vinculo.teamNome,
            vinculo.teamCorHex,
            desempenho.jogos,
            desempenho.vitorias,
            desempenho.derrotas,
            desempenho.pontosPro,
            desempenho.pontosContra,
            agora,
          ],
        );
      }

      for (const playerId of presentesSemTime) {
        await tx.execute(
          `INSERT INTO player_day_stats (
              id, day_id, player_id, team_id, team_nome, team_cor_hex,
              jogos, vitorias, derrotas, pontos_pro, pontos_contra, created_at
           ) VALUES (?, ?, ?, NULL, NULL, NULL, 0, 0, 0, 0, 0, ?)`,
          [novoId(), diaId, playerId, agora],
        );
      }
    }

    await tx.execute('UPDATE players SET ativo = 0, updated_at = ? WHERE ativo = 1', [agoraIso()]);
    await tx.execute('DELETE FROM team_players');
    await tx.execute('DELETE FROM matches');
    await tx.execute('DELETE FROM rounds');

    return {
      partidas: partidas.length,
      atletas: vinculos.length,
      presencas: vinculos.length + presentesSemTime.length,
    };
  });
}
