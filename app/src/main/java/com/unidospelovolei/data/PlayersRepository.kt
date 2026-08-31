package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.unidospelovolei.domain.model.Player
import kotlinx.coroutines.flow.Flow

class PlayersRepository(
    private val db: PowerSyncDatabase,
) {
    fun observePlayers(): Flow<List<Player>> =
        db.watch(
            """
            SELECT id, nome, skill_level, ativo
            FROM players
            ORDER BY ativo DESC, nome COLLATE NOCASE
            """.trimIndent(),
        ) { it.toPlayer() }

    fun observeActivePlayers(): Flow<List<Player>> =
        db.watch(
            """
            SELECT id, nome, skill_level, ativo
            FROM players
            WHERE ativo = 1
            ORDER BY skill_level DESC, nome COLLATE NOCASE
            """.trimIndent(),
        ) { it.toPlayer() }

    suspend fun create(
        nome: String,
        skillLevel: Int,
        ativo: Boolean,
    ) {
        val agora = agoraIso()
        db.execute(
            """
            INSERT INTO players (id, nome, skill_level, ativo, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            listOf(novoId(), nome.trim(), skillLevel, if (ativo) 1 else 0, agora, agora),
        )
    }

    suspend fun update(player: Player) {
        db.execute(
            """
            UPDATE players
            SET nome = ?, skill_level = ?, ativo = ?, updated_at = ?
            WHERE id = ?
            """.trimIndent(),
            listOf(player.nome.trim(), player.skillLevel, if (player.ativo) 1 else 0, agoraIso(), player.id),
        )
    }

    suspend fun delete(playerId: String) {
        db.writeTransactionAsync { tx ->
            tx.execute("DELETE FROM team_players WHERE player_id = ?", listOf(playerId))
            tx.execute("DELETE FROM players WHERE id = ?", listOf(playerId))
        }
    }
}
