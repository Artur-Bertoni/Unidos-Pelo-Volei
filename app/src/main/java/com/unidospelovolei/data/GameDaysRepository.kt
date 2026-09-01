package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.powersync.db.getStringOptional
import com.unidospelovolei.domain.model.MatchStatus
import com.unidospelovolei.domain.model.PlayerPerformance
import com.unidospelovolei.domain.model.ResumoDoDia
import com.unidospelovolei.domain.scheduling.ElencoPassado
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameDaysRepository(
    private val db: PowerSyncDatabase,
) {
    fun observeElencosPassados(): Flow<List<ElencoPassado>> =
        db
            .watch(ELENCOS_SQL) { cursor ->
                Triple(
                    cursor.getString("day_id"),
                    cursor.getString("team_id"),
                    cursor.getString("player_id"),
                )
            }.map(::agruparEmElencos)

    fun observePerformances(): Flow<Map<String, PlayerPerformance>> =
        db
            .watch(PERFORMANCE_SQL) { cursor ->
                PlayerPerformance(
                    playerId = cursor.getString("player_id"),
                    dias = cursor.int("dias"),
                    jogos = cursor.int("jogos"),
                    vitorias = cursor.int("vitorias"),
                    derrotas = cursor.int("derrotas"),
                    pontosPro = cursor.int("pontos_pro"),
                    pontosContra = cursor.int("pontos_contra"),
                )
            }.map { linhas -> linhas.associateBy { it.playerId } }

    suspend fun encerrarDia(): ResumoDoDia =
        db.writeTransactionAsync { tx ->
            val vinculos =
                tx.getAll(
                    """
                    SELECT tp.team_id, tp.player_id, t.nome AS team_nome, t.cor_hex AS team_cor_hex
                    FROM team_players tp
                    JOIN teams t ON t.id = tp.team_id
                    """.trimIndent(),
                ) { cursor ->
                    Vinculo(
                        teamId = cursor.getString("team_id"),
                        playerId = cursor.getString("player_id"),
                        teamNome = cursor.getStringOptional("team_nome"),
                        teamCorHex = cursor.getStringOptional("team_cor_hex"),
                    )
                }

            val presentes =
                tx.getAll("SELECT id FROM players WHERE ativo = 1") { it.getString("id") }

            val partidas =
                tx.getAll(
                    """
                    SELECT team_a_id, team_b_id, score_a, score_b, winner_id
                    FROM matches
                    WHERE status = ?
                    """.trimIndent(),
                    listOf(MatchStatus.FINALIZADO.value),
                ) { cursor ->
                    PartidaFinalizada(
                        teamAId = cursor.getString("team_a_id"),
                        teamBId = cursor.getString("team_b_id"),
                        scoreA = cursor.int("score_a"),
                        scoreB = cursor.int("score_b"),
                        winnerId = cursor.getStringOptional("winner_id"),
                    )
                }

            val porTime = mutableMapOf<String, Desempenho>()
            partidas.forEach { partida ->
                porTime.acumular(partida.teamAId, partida.scoreA, partida.scoreB, partida.winnerId == partida.teamAId)
                porTime.acumular(partida.teamBId, partida.scoreB, partida.scoreA, partida.winnerId == partida.teamBId)
            }

            val comTime = vinculos.map { it.playerId }.toSet()
            val presentesSemTime = presentes.filterNot { it in comTime }

            if (vinculos.isNotEmpty() || presentesSemTime.isNotEmpty()) {
                val diaId = novoId()
                val agora = agoraIso()
                tx.execute(
                    "INSERT INTO game_days (id, encerrado_em, partidas, created_at) VALUES (?, ?, ?, ?)",
                    listOf(diaId, agora, partidas.size, agora),
                )
                vinculos.forEach { vinculo ->
                    val desempenho = porTime[vinculo.teamId] ?: Desempenho()
                    tx.execute(
                        """
                        INSERT INTO player_day_stats (
                            id, day_id, player_id, team_id, team_nome, team_cor_hex,
                            jogos, vitorias, derrotas, pontos_pro, pontos_contra, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                        listOf(
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
                        ),
                    )
                }
                presentesSemTime.forEach { playerId ->
                    tx.execute(
                        """
                        INSERT INTO player_day_stats (
                            id, day_id, player_id, team_id, team_nome, team_cor_hex,
                            jogos, vitorias, derrotas, pontos_pro, pontos_contra, created_at
                        ) VALUES (?, ?, ?, NULL, NULL, NULL, 0, 0, 0, 0, 0, ?)
                        """.trimIndent(),
                        listOf(novoId(), diaId, playerId, agora),
                    )
                }
            }

            tx.execute(
                "UPDATE players SET ativo = 0, updated_at = ? WHERE ativo = 1",
                listOf(agoraIso()),
            )
            tx.execute("DELETE FROM team_players", listOf())
            tx.execute("DELETE FROM matches", listOf())
            tx.execute("DELETE FROM rounds", listOf())

            ResumoDoDia(
                partidas = partidas.size,
                atletas = vinculos.size,
                presencas = vinculos.size + presentesSemTime.size,
            )
        }

    private fun agruparEmElencos(linhas: List<Triple<String, String, String>>): List<ElencoPassado> {
        val porDia = linkedMapOf<String, MutableMap<String, MutableList<String>>>()
        linhas.forEach { (diaId, timeId, jogadorId) ->
            porDia.getOrPut(diaId) { linkedMapOf() }.getOrPut(timeId) { mutableListOf() } += jogadorId
        }
        return porDia.values.flatMapIndexed { indice, times ->
            val peso = 1.0 / (1.0 + indice)
            times.values.map { jogadores -> ElencoPassado(jogadores = jogadores.toList(), peso = peso) }
        }
    }

    private fun MutableMap<String, Desempenho>.acumular(
        teamId: String,
        pontosPro: Int,
        pontosContra: Int,
        venceu: Boolean,
    ) {
        val atual = this[teamId] ?: Desempenho()
        this[teamId] =
            Desempenho(
                jogos = atual.jogos + 1,
                vitorias = atual.vitorias + if (venceu) 1 else 0,
                derrotas = atual.derrotas + if (venceu) 0 else 1,
                pontosPro = atual.pontosPro + pontosPro,
                pontosContra = atual.pontosContra + pontosContra,
            )
    }

    private data class Vinculo(
        val teamId: String,
        val playerId: String,
        val teamNome: String?,
        val teamCorHex: String?,
    )

    private data class PartidaFinalizada(
        val teamAId: String,
        val teamBId: String,
        val scoreA: Int,
        val scoreB: Int,
        val winnerId: String?,
    )

    private data class Desempenho(
        val jogos: Int = 0,
        val vitorias: Int = 0,
        val derrotas: Int = 0,
        val pontosPro: Int = 0,
        val pontosContra: Int = 0,
    )

    companion object {
        const val DIAS_NO_HISTORICO: Int = 12

        private val ELENCOS_SQL =
            """
            SELECT s.day_id, s.team_id, s.player_id
            FROM player_day_stats s
            JOIN (
                SELECT id, encerrado_em
                FROM game_days
                ORDER BY encerrado_em DESC
                LIMIT $DIAS_NO_HISTORICO
            ) d ON d.id = s.day_id
            WHERE s.team_id IS NOT NULL
            ORDER BY d.encerrado_em DESC, s.day_id, s.team_id
            """.trimIndent()

        val PERFORMANCE_SQL =
            """
            SELECT
                p.id                                             AS player_id,
                COUNT(s.id)                                      AS dias,
                COALESCE(SUM(s.jogos), 0)                        AS jogos,
                COALESCE(SUM(s.vitorias), 0)                     AS vitorias,
                COALESCE(SUM(s.derrotas), 0)                     AS derrotas,
                COALESCE(SUM(s.pontos_pro), 0)                   AS pontos_pro,
                COALESCE(SUM(s.pontos_contra), 0)                AS pontos_contra
            FROM players p
            LEFT JOIN player_day_stats s ON s.player_id = p.id
            GROUP BY p.id
            """.trimIndent()
    }
}
