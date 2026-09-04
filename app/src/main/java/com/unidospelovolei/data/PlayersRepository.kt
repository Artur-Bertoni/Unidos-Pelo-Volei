package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.unidospelovolei.domain.model.Genero
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.Regime
import com.unidospelovolei.domain.model.StatusVinculo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlayersRepository(
    private val db: PowerSyncDatabase,
) {
    fun observePlayers(): Flow<List<Player>> =
        db.watch(
            """
            SELECT $COLUNAS
            FROM players
            ORDER BY nome COLLATE NOCASE
            """.trimIndent(),
        ) { it.toPlayer() }

    fun observeActivePlayers(): Flow<List<Player>> =
        db.watch(
            """
            SELECT $COLUNAS
            FROM players
            WHERE ativo = 1
            ORDER BY skill_level DESC, nome COLLATE NOCASE
            """.trimIndent(),
        ) { it.toPlayer() }

    fun observePlayerDoPerfil(profileId: String): Flow<Player?> =
        db
            .watch(
                "SELECT $COLUNAS FROM players WHERE profile_id = ?",
                listOf(profileId),
            ) { it.toPlayer() }
            .map { it.firstOrNull() }

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
            SET nome = ?, skill_level = ?, genero = ?, ativo = ?, regime = ?, updated_at = ?
            WHERE id = ?
            """.trimIndent(),
            listOf(
                player.nome.trim(),
                player.skillLevel,
                player.genero.value,
                if (player.ativo) 1 else 0,
                player.regime.value,
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

    suspend fun salvarFicha(
        playerId: String,
        nome: String,
        nascimentoDia: Int?,
        nascimentoMes: Int?,
        regime: Regime,
    ) {
        db.execute(
            """
            UPDATE players
            SET nome = ?, nascimento_dia = ?, nascimento_mes = ?, regime = ?, updated_at = ?
            WHERE id = ?
            """.trimIndent(),
            listOf(nome.trim(), nascimentoDia, nascimentoMes, regime.value, agoraIso(), playerId),
        )
    }

    suspend fun definirEntrada(
        playerId: String,
        entrouEm: String?,
    ) {
        db.execute(
            "UPDATE players SET entrou_em = ?, updated_at = ? WHERE id = ?",
            listOf(entrouEm, agoraIso(), playerId),
        )
    }

    suspend fun vincular(
        playerId: String,
        profileId: String?,
    ) {
        db.writeTransactionAsync { tx ->
            if (profileId != null) {
                tx.execute(
                    "UPDATE players SET profile_id = NULL, updated_at = ? WHERE profile_id = ? AND id <> ?",
                    listOf(agoraIso(), profileId, playerId),
                )
                tx.execute(
                    "DELETE FROM vinculo_pedidos WHERE profile_id = ? AND status = ?",
                    listOf(profileId, StatusVinculo.PENDENTE.value),
                )
            }
            tx.execute(
                "UPDATE players SET profile_id = ?, updated_at = ? WHERE id = ?",
                listOf(profileId, agoraIso(), playerId),
            )
        }
    }

    suspend fun delete(playerId: String) {
        db.writeTransactionAsync { tx ->
            tx.execute("DELETE FROM team_players WHERE player_id = ?", listOf(playerId))
            tx.execute("DELETE FROM player_day_stats WHERE player_id = ?", listOf(playerId))
            tx.execute("DELETE FROM player_contatos WHERE player_id = ?", listOf(playerId))
            tx.execute("DELETE FROM vinculo_pedidos WHERE player_id = ?", listOf(playerId))
            tx.execute("DELETE FROM players WHERE id = ?", listOf(playerId))
        }
    }

    private companion object {
        const val COLUNAS =
            "id, nome, skill_level, genero, ativo, profile_id, foto_url, " +
                "nascimento_dia, nascimento_mes, entrou_em, regime"
    }
}
