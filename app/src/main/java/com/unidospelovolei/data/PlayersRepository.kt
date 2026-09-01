package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.unidospelovolei.domain.model.Genero
import com.unidospelovolei.domain.model.Player
import kotlinx.coroutines.flow.Flow

class PlayersRepository(
    private val db: PowerSyncDatabase,
) {
    fun observePlayers(): Flow<List<Player>> =
        db.watch(
            """
            SELECT id, nome, skill_level, genero, ativo
            FROM players
            ORDER BY nome COLLATE NOCASE
            """.trimIndent(),
        ) { it.toPlayer() }

    fun observeActivePlayers(): Flow<List<Player>> =
        db.watch(
            """
            SELECT id, nome, skill_level, genero, ativo
            FROM players
            WHERE ativo = 1
            ORDER BY skill_level DESC, nome COLLATE NOCASE
            """.trimIndent(),
        ) { it.toPlayer() }

    suspend fun create(
        nome: String,
        skillLevel: Int,
        genero: Genero,
        ativo: Boolean,
    ) {
        val agora = agoraIso()
        db.execute(
            """
            INSERT INTO players (id, nome, skill_level, genero, ativo, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            listOf(novoId(), nome.trim(), skillLevel, genero.value, if (ativo) 1 else 0, agora, agora),
        )
    }

    suspend fun update(player: Player) {
        db.execute(
            """
            UPDATE players
            SET nome = ?, skill_level = ?, genero = ?, ativo = ?, updated_at = ?
            WHERE id = ?
            """.trimIndent(),
            listOf(
                player.nome.trim(),
                player.skillLevel,
                player.genero.value,
                if (player.ativo) 1 else 0,
                agoraIso(),
                player.id,
            ),
        )
    }

    suspend fun setAtivo(
        playerId: String,
        ativo: Boolean,
    ) {
        db.execute(
            "UPDATE players SET ativo = ?, updated_at = ? WHERE id = ?",
            listOf(if (ativo) 1 else 0, agoraIso(), playerId),
        )
    }

    suspend fun definirPresencaDeTodos(presente: Boolean) {
        db.execute(
            "UPDATE players SET ativo = ?, updated_at = ? WHERE ativo <> ?",
            listOf(if (presente) 1 else 0, agoraIso(), if (presente) 1 else 0),
        )
    }

    suspend fun delete(playerId: String) {
        db.writeTransactionAsync { tx ->
            tx.execute("DELETE FROM team_players WHERE player_id = ?", listOf(playerId))
            tx.execute("DELETE FROM player_day_stats WHERE player_id = ?", listOf(playerId))
            tx.execute("DELETE FROM players WHERE id = ?", listOf(playerId))
        }
    }
}
