package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.unidospelovolei.domain.model.PlayerContato
import com.unidospelovolei.domain.model.StatusVinculo
import com.unidospelovolei.domain.model.VinculoPedido
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MembroRepository(
    private val db: PowerSyncDatabase,
) {
    fun observeMeuPedido(profileId: String): Flow<VinculoPedido?> =
        db
            .watch(
                """
                SELECT $COLUNAS_PEDIDO
                FROM vinculo_pedidos
                WHERE profile_id = ?
                ORDER BY criado_em DESC
                LIMIT 1
                """.trimIndent(),
                listOf(profileId),
            ) { it.toVinculoPedido() }
            .map { it.firstOrNull() }

    fun observePedidosPendentes(): Flow<List<VinculoPedido>> =
        db.watch(
            """
            SELECT $COLUNAS_PEDIDO
            FROM vinculo_pedidos
            WHERE status = ?
            ORDER BY criado_em
            """.trimIndent(),
            listOf(StatusVinculo.PENDENTE.value),
        ) { it.toVinculoPedido() }

    fun observeContato(playerId: String): Flow<PlayerContato?> =
        db
            .watch(
                """
                SELECT id, player_id, telefone, contato_emergencia, nascimento_ano
                FROM player_contatos
                WHERE player_id = ?
                """.trimIndent(),
                listOf(playerId),
            ) { it.toPlayerContato() }
            .map { it.firstOrNull() }

    suspend fun pedirVinculo(
        profileId: String,
        profileNome: String?,
        playerId: String,
    ) {
        db.writeTransactionAsync { tx ->
            tx.execute(
                "DELETE FROM vinculo_pedidos WHERE profile_id = ? AND status = ?",
                listOf(profileId, StatusVinculo.PENDENTE.value),
            )
            tx.execute(
                """
                INSERT INTO vinculo_pedidos (
                    id, profile_id, player_id, profile_nome, status, criado_em
                ) VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                listOf(
                    novoId(),
                    profileId,
                    playerId,
                    profileNome,
                    StatusVinculo.PENDENTE.value,
                    agoraIso(),
                ),
            )
        }
    }

    suspend fun cancelarPedido(pedidoId: String) {
        db.execute("DELETE FROM vinculo_pedidos WHERE id = ?", listOf(pedidoId))
    }

    suspend fun decidir(
        pedido: VinculoPedido,
        aprovado: Boolean,
        decididoPor: String,
    ) {
        val status = if (aprovado) StatusVinculo.APROVADO else StatusVinculo.RECUSADO
        db.writeTransactionAsync { tx ->
            tx.execute(
                """
                UPDATE vinculo_pedidos
                SET status = ?, decidido_por = ?, decidido_em = ?
                WHERE id = ?
                """.trimIndent(),
                listOf(status.value, decididoPor, agoraIso(), pedido.id),
            )
            if (aprovado) {
                tx.execute(
                    "UPDATE players SET profile_id = NULL, updated_at = ? WHERE profile_id = ? AND id <> ?",
                    listOf(agoraIso(), pedido.profileId, pedido.playerId),
                )
                tx.execute(
                    "UPDATE players SET profile_id = ?, updated_at = ? WHERE id = ?",
                    listOf(pedido.profileId, agoraIso(), pedido.playerId),
                )
            }
        }
    }

    suspend fun salvarContato(
        playerId: String,
        profileId: String?,
        telefone: String?,
        contatoEmergencia: String?,
        nascimentoAno: Int?,
    ) {
        val fone = telefone?.trim()?.ifBlank { null }
        val emergencia = contatoEmergencia?.trim()?.ifBlank { null }
        val agora = agoraIso()

        db.writeTransactionAsync { tx ->
            val existente =
                tx
                    .getAll(
                        "SELECT id FROM player_contatos WHERE player_id = ?",
                        listOf(playerId),
                    ) { it.getString("id") }
                    .firstOrNull()

            if (existente == null) {
                tx.execute(
                    """
                    INSERT INTO player_contatos (
                        id, player_id, profile_id, telefone, contato_emergencia,
                        nascimento_ano, atualizado_em
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    listOf(novoId(), playerId, profileId, fone, emergencia, nascimentoAno, agora),
                )
            } else {
                tx.execute(
                    """
                    UPDATE player_contatos
                    SET telefone = ?, contato_emergencia = ?, nascimento_ano = ?, atualizado_em = ?
                    WHERE id = ?
                    """.trimIndent(),
                    listOf(fone, emergencia, nascimentoAno, agora, existente),
                )
            }
        }
    }

    private companion object {
        const val COLUNAS_PEDIDO =
            "id, profile_id, player_id, profile_nome, status, criado_em"
    }
}
