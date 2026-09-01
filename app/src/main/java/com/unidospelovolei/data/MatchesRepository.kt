package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.unidospelovolei.domain.model.MatchCard
import com.unidospelovolei.domain.model.MatchStatus
import com.unidospelovolei.domain.model.Round
import com.unidospelovolei.domain.scheduling.ScheduledRound
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Formato(
    val times: Int,
    val quadras: Int,
)

class MatchesRepository(
    private val db: PowerSyncDatabase,
) {
    fun observeFormato(): Flow<Formato> =
        db.watch(
            """
            SELECT
                (SELECT COUNT(*) FROM teams WHERE ativo = 1)  AS times,
                (SELECT COALESCE(MAX(quadra), 0) FROM matches) AS quadras
            """.trimIndent(),
        ) { Formato(times = it.int("times"), quadras = it.int("quadras")) }
            .map { it.firstOrNull() ?: Formato(times = 0, quadras = 0) }

    fun observeRounds(): Flow<List<Round>> =
        db.watch("SELECT id, numero, fase FROM rounds ORDER BY numero") { cursor ->
            Round(
                id = cursor.getString("id"),
                numero = cursor.int("numero"),
                fase = cursor.int("fase", 1),
            )
        }

    fun observeMatches(): Flow<List<MatchCard>> =
        db.watch("$MATCH_CARD_SQL ORDER BY r.numero, m.quadra") { it.toMatchCard() }

    fun observeTeamHistory(teamId: String): Flow<List<MatchCard>> =
        db.watch(
            "$MATCH_CARD_SQL WHERE m.team_a_id = ? OR m.team_b_id = ? ORDER BY r.numero, m.quadra",
            listOf(teamId, teamId),
        ) { it.toMatchCard() }

    fun observeMatch(matchId: String): Flow<List<MatchCard>> =
        db.watch("$MATCH_CARD_SQL WHERE m.id = ?", listOf(matchId)) { it.toMatchCard() }

    suspend fun updateScore(
        matchId: String,
        scoreA: Int,
        scoreB: Int,
    ) {
        db.execute(
            "UPDATE matches SET score_a = ?, score_b = ?, updated_at = ? WHERE id = ?",
            listOf(scoreA.coerceAtLeast(0), scoreB.coerceAtLeast(0), agoraIso(), matchId),
        )
    }

    suspend fun finish(
        matchId: String,
        scoreA: Int,
        scoreB: Int,
        teamAId: String,
        teamBId: String,
    ) {
        val vencedor = if (scoreA > scoreB) teamAId else if (scoreB > scoreA) teamBId else null
        db.execute(
            """
            UPDATE matches
            SET score_a = ?, score_b = ?, status = ?, winner_id = ?, updated_at = ?
            WHERE id = ?
            """.trimIndent(),
            listOf(scoreA, scoreB, MatchStatus.FINALIZADO.value, vencedor, agoraIso(), matchId),
        )
    }

    suspend fun reopen(matchId: String) {
        db.execute(
            """
            UPDATE matches
            SET status = ?, winner_id = NULL, updated_at = ?
            WHERE id = ?
            """.trimIndent(),
            listOf(MatchStatus.AGENDADO.value, agoraIso(), matchId),
        )
    }

    suspend fun replaceSchedule(rodadas: List<ScheduledRound>) {
        db.writeTransactionAsync { tx ->
            tx.execute("DELETE FROM matches", listOf())
            tx.execute("DELETE FROM rounds", listOf())

            rodadas.forEach { rodada ->
                val roundId = novoId()
                val agora = agoraIso()
                tx.execute(
                    "INSERT INTO rounds (id, numero, fase, created_at) VALUES (?, ?, ?, ?)",
                    listOf(roundId, rodada.numero, rodada.fase, agora),
                )
                rodada.matches.forEach { partida ->
                    tx.execute(
                        """
                        INSERT INTO matches (
                            id, round_id, quadra, team_a_id, team_b_id,
                            score_a, score_b, status, winner_id, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, 0, 0, ?, NULL, ?, ?)
                        """.trimIndent(),
                        listOf(
                            novoId(),
                            roundId,
                            partida.quadra,
                            partida.teamA.id,
                            partida.teamB.id,
                            MatchStatus.AGENDADO.value,
                            agora,
                            agora,
                        ),
                    )
                }
            }
        }
    }

    suspend fun clearResults() {
        db.execute(
            """
            UPDATE matches
            SET score_a = 0, score_b = 0, status = ?, winner_id = NULL, updated_at = ?
            """.trimIndent(),
            listOf(MatchStatus.AGENDADO.value, agoraIso()),
        )
    }

    private companion object {
        val MATCH_CARD_SQL =
            """
            SELECT
                m.id, r.numero, r.fase, m.quadra,
                m.score_a, m.score_b, m.status, m.winner_id,
                ta.id AS a_id, ta.nome AS a_nome, ta.cor_hex AS a_cor_hex,
                ta.sigla AS a_sigla, ta.ativo AS a_ativo, ta.ordem AS a_ordem,
                tb.id AS b_id, tb.nome AS b_nome, tb.cor_hex AS b_cor_hex,
                tb.sigla AS b_sigla, tb.ativo AS b_ativo, tb.ordem AS b_ordem
            FROM matches m
            JOIN rounds r ON r.id = m.round_id
            JOIN teams ta ON ta.id = m.team_a_id
            JOIN teams tb ON tb.id = m.team_b_id
            """.trimIndent()
    }
}
