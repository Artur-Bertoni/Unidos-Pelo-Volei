package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.powersync.db.getStringOptional
import com.unidospelovolei.domain.model.Genero
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.domain.model.TeamRoster
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TeamsRepository(
    private val db: PowerSyncDatabase,
) {
    fun observeTeams(): Flow<List<Team>> =
        db.watch(
            """
            SELECT id, nome, cor_hex, sigla, ativo, ordem
            FROM teams
            WHERE ativo = 1
            ORDER BY ordem, nome COLLATE NOCASE
            """.trimIndent(),
        ) { it.toTeam() }

    fun observeAllTeams(): Flow<List<Team>> =
        db.watch(
            """
            SELECT id, nome, cor_hex, sigla, ativo, ordem
            FROM teams
            ORDER BY ativo DESC, ordem, nome COLLATE NOCASE
            """.trimIndent(),
        ) { it.toTeam() }

    fun observeRoster(teamId: String): Flow<List<Player>> =
        db.watch(
            """
            SELECT p.id, p.nome, p.skill_level, p.genero, p.ativo
            FROM team_players tp
            JOIN players p ON p.id = tp.player_id
            WHERE tp.team_id = ?
            ORDER BY p.skill_level DESC, p.nome COLLATE NOCASE
            """.trimIndent(),
            listOf(teamId),
        ) { it.toPlayer() }

    fun observeRosters(): Flow<List<TeamRoster>> =
        db
            .watch(
                """
                SELECT
                    t.id AS team_id, t.nome AS team_nome, t.cor_hex AS team_cor_hex,
                    t.sigla AS team_sigla, t.ativo AS team_ativo, t.ordem AS team_ordem,
                    p.id AS player_id, p.nome AS player_nome,
                    p.skill_level AS player_skill_level, p.genero AS player_genero,
                    p.ativo AS player_ativo
                FROM teams t
                LEFT JOIN team_players tp ON tp.team_id = t.id
                LEFT JOIN players p ON p.id = tp.player_id
                WHERE t.ativo = 1
                ORDER BY t.ordem, t.nome COLLATE NOCASE, p.skill_level DESC, p.nome COLLATE NOCASE
                """.trimIndent(),
            ) { cursor ->
                val time =
                    Team(
                        id = cursor.getString("team_id"),
                        nome = cursor.getStringOptional("team_nome").orEmpty(),
                        corHex = cursor.getStringOptional("team_cor_hex") ?: "#6B7280",
                        sigla = cursor.getStringOptional("team_sigla").orEmpty(),
                        ativo = cursor.bool("team_ativo", true),
                        ordem = cursor.int("team_ordem"),
                    )
                val jogador =
                    cursor.getStringOptional("player_id")?.let { id ->
                        Player(
                            id = id,
                            nome = cursor.getStringOptional("player_nome").orEmpty(),
                            skillLevel = cursor.int("player_skill_level", 3),
                            genero = Genero.from(cursor.getStringOptional("player_genero")),
                            ativo = cursor.bool("player_ativo", true),
                        )
                    }
                time to jogador
            }.map { linhas ->
                linhas
                    .groupBy({ it.first }, { it.second })
                    .map { (time, jogadores) -> TeamRoster(time, jogadores.filterNotNull()) }
            }

    suspend fun create(
        nome: String,
        corHex: String,
        sigla: String,
        ordem: Int,
    ) {
        val agora = agoraIso()
        db.execute(
            """
            INSERT INTO teams (id, nome, cor_hex, sigla, ativo, ordem, created_at, updated_at)
            VALUES (?, ?, ?, ?, 1, ?, ?, ?)
            """.trimIndent(),
            listOf(novoId(), nome.trim(), corHex, sigla.trim().uppercase().take(2), ordem, agora, agora),
        )
    }

    suspend fun update(team: Team) {
        db.execute(
            """
            UPDATE teams
            SET nome = ?, cor_hex = ?, sigla = ?, ativo = ?, ordem = ?, updated_at = ?
            WHERE id = ?
            """.trimIndent(),
            listOf(
                team.nome.trim(),
                team.corHex,
                team.sigla.trim().uppercase().take(2),
                if (team.ativo) 1 else 0,
                team.ordem,
                agoraIso(),
                team.id,
            ),
        )
    }

    suspend fun delete(teamId: String) {
        db.writeTransactionAsync { tx ->
            tx.execute("DELETE FROM team_players WHERE team_id = ?", listOf(teamId))
            tx.execute("DELETE FROM teams WHERE id = ?", listOf(teamId))
        }
    }

    suspend fun replaceRosters(rosters: List<TeamRoster>) {
        db.writeTransactionAsync { tx ->
            tx.execute("DELETE FROM team_players", listOf())
            rosters.forEach { roster ->
                roster.players.forEach { jogador ->
                    tx.execute(
                        "INSERT INTO team_players (id, team_id, player_id) VALUES (?, ?, ?)",
                        listOf(novoId(), roster.team.id, jogador.id),
                    )
                }
            }
        }
    }
}
