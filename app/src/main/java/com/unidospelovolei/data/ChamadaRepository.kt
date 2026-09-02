package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.unidospelovolei.domain.model.Aviso
import com.unidospelovolei.domain.model.ConfigGrupo
import com.unidospelovolei.domain.model.Presenca
import com.unidospelovolei.domain.model.StatusPresenca
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class ChamadaRepository(
    private val db: PowerSyncDatabase,
) {
    fun observeConfig(): Flow<ConfigGrupo?> =
        db
            .watch("SELECT id, jogo_hora, jogo_local FROM config_grupo LIMIT 1") { it.toConfigGrupo() }
            .map { it.firstOrNull() }

    fun observePresencas(data: String): Flow<List<Presenca>> =
        db.watch(
            """
            SELECT id, player_id, data, status, origem
            FROM presencas
            WHERE data = ?
            """.trimIndent(),
            listOf(data),
        ) { it.toPresenca() }

    fun observeAvisos(): Flow<List<Aviso>> =
        db.watch(
            """
            SELECT id, tipo, titulo, corpo, criado_em
            FROM avisos
            ORDER BY criado_em DESC
            LIMIT 30
            """.trimIndent(),
        ) { it.toAviso() }

    suspend fun responder(
        playerId: String,
        data: String,
        status: StatusPresenca,
        peloProprio: Boolean,
        registradoPor: String?,
    ) {
        val agora = agoraIso()
        val origem = if (peloProprio) "atleta" else "diretoria"

        db.writeTransactionAsync { tx ->
            val existente =
                tx
                    .getAll(
                        "SELECT id FROM presencas WHERE player_id = ? AND data = ?",
                        listOf(playerId, data),
                    ) { it.getString("id") }
                    .firstOrNull()

            if (existente == null) {
                tx.execute(
                    """
                    INSERT INTO presencas (
                        id, player_id, data, status, origem, registrado_por, atualizado_em
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    listOf(novoId(), playerId, data, status.value, origem, registradoPor, agora),
                )
            } else {
                tx.execute(
                    """
                    UPDATE presencas
                    SET status = ?, origem = ?, registrado_por = ?, atualizado_em = ?
                    WHERE id = ?
                    """.trimIndent(),
                    listOf(status.value, origem, registradoPor, agora, existente),
                )
            }
        }
    }

    suspend fun trazerConfirmados(data: String): Int =
        db.writeTransactionAsync { tx ->
            val confirmados =
                tx.getAll(
                    "SELECT player_id FROM presencas WHERE data = ? AND status = ?",
                    listOf(data, StatusPresenca.VOU.value),
                ) { it.getString("player_id") }

            if (confirmados.isNotEmpty()) {
                val agora = agoraIso()
                val marcadores = confirmados.joinToString(",") { "?" }
                tx.execute(
                    "UPDATE players SET ativo = 0, updated_at = ? WHERE ativo = 1",
                    listOf(agora),
                )
                tx.execute(
                    "UPDATE players SET ativo = 1, updated_at = ? WHERE id IN ($marcadores)",
                    listOf(agora) + confirmados,
                )
            }

            confirmados.size
        }

    suspend fun registrarDispositivo(
        profileId: String,
        token: String,
        plataforma: String,
    ) {
        val agora = agoraIso()
        db.writeTransactionAsync { tx ->
            val existente =
                tx
                    .getAll("SELECT id FROM dispositivos WHERE token = ?", listOf(token)) {
                        it.getString("id")
                    }.firstOrNull()

            if (existente == null) {
                tx.execute(
                    """
                    INSERT INTO dispositivos (
                        id, profile_id, token, plataforma, ativo, visto_em, criado_em
                    ) VALUES (?, ?, ?, ?, 1, ?, ?)
                    """.trimIndent(),
                    listOf(novoId(), profileId, token, plataforma, agora, agora),
                )
            } else {
                tx.execute(
                    "UPDATE dispositivos SET profile_id = ?, ativo = 1, visto_em = ? WHERE id = ?",
                    listOf(profileId, agora, existente),
                )
            }
        }
    }

    suspend fun salvarConfig(
        id: String,
        jogoHora: String,
        jogoLocal: String?,
    ) {
        db.execute(
            "UPDATE config_grupo SET jogo_hora = ?, jogo_local = ?, atualizado_em = ? WHERE id = ?",
            listOf(jogoHora, jogoLocal?.trim()?.ifBlank { null }, agoraIso(), id),
        )
    }

    companion object {
        private val ISO_DATA: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        fun proximoSabado(hoje: LocalDate = LocalDate.now()): String =
            if (hoje.dayOfWeek == DayOfWeek.SATURDAY) {
                hoje.format(ISO_DATA)
            } else {
                hoje.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).format(ISO_DATA)
            }
    }
}
