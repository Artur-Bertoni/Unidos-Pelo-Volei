package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.unidospelovolei.domain.financeiro.PixBrCode
import com.unidospelovolei.domain.financeiro.TipoDaChavePix
import com.unidospelovolei.domain.model.Cobranca
import com.unidospelovolei.domain.model.ConfigFinanceiro
import com.unidospelovolei.domain.model.ItemDoExtrato
import com.unidospelovolei.domain.model.Pagamento
import com.unidospelovolei.domain.model.Regime
import com.unidospelovolei.domain.model.StatusPagamento
import com.unidospelovolei.domain.model.TipoCobranca
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject

data class LinhaDoPainel(
    val id: String,
    val cobrancaId: String,
    val playerId: String,
    val valorCentavos: Int,
    val status: String,
    val pagoEm: String? = null,
    val observacao: String? = null,
)

class FinanceiroRepository(
    private val db: PowerSyncDatabase,
    private val supabase: SupabaseClient,
    private val grupoAtivo: GrupoAtivo,
) {
    fun observeConfig(): Flow<ConfigFinanceiro?> =
        db
            .observarNoGrupo(
                grupoAtivo,
                """
                SELECT id, pix_chave, pix_tipo, pix_nome, pix_cidade, mensalidade_centavos, diaria_centavos
                FROM config_financeiro
                WHERE grupo_id = ?
                LIMIT 1
                """.trimIndent(),
            ) { it.toConfigFinanceiro() }
            .map { it.firstOrNull() }

    fun observeCobrancas(): Flow<List<Cobranca>> =
        db.observarNoGrupo(
            grupoAtivo,
            """
            SELECT id, titulo, tipo, valor_centavos, competencia, vence_em
            FROM cobrancas
            WHERE grupo_id = ?
            ORDER BY COALESCE(competencia, criado_em) DESC, criado_em DESC
            """.trimIndent(),
        ) { it.toCobranca() }

    fun observeMeusPagamentos(): Flow<List<Pagamento>> =
        db.observarNoGrupo(
            grupoAtivo,
            """
            SELECT id, cobranca_id, player_id, valor_centavos, status, pago_em, observacao
            FROM pagamentos
            WHERE grupo_id = ?
            ORDER BY criado_em DESC
            """.trimIndent(),
        ) { it.toPagamento() }

    fun observeMeuExtrato(): Flow<List<ItemDoExtrato>> =
        combine(observeMeusPagamentos(), observeCobrancas()) { pagamentos, cobrancas ->
            val porId = cobrancas.associateBy { it.id }
            pagamentos.map { ItemDoExtrato(pagamento = it, cobranca = porId[it.cobrancaId]) }
        }

    suspend fun lerPainel(): List<LinhaDoPainel> {
        val grupo = grupoAtivo.exigir()
        return supabase
            .from("pagamentos")
            .select { filter { eq("grupo_id", grupo) } }
            .decodeList<JsonObject>()
            .map { linha ->
                LinhaDoPainel(
                    id = linha.textoObrigatorio("id"),
                    cobrancaId = linha.texto("cobranca_id").orEmpty(),
                    playerId = linha.texto("player_id").orEmpty(),
                    valorCentavos = linha.inteiro("valor_centavos"),
                    status = linha.texto("status") ?: StatusPagamento.PENDENTE.value,
                    pagoEm = linha.texto("pago_em"),
                    observacao = linha.texto("observacao"),
                )
            }
    }

    suspend fun salvarConfig(
        id: String,
        pixChave: String?,
        pixTipo: TipoDaChavePix,
        pixNome: String?,
        pixCidade: String?,
        mensalidadeCentavos: Int,
        diariaCentavos: Int,
    ) {
        db.execute(
            """
            UPDATE config_financeiro
            SET pix_chave = ?, pix_tipo = ?, pix_nome = ?, pix_cidade = ?,
                mensalidade_centavos = ?, diaria_centavos = ?, atualizado_em = ?
            WHERE id = ?
            """.trimIndent(),
            listOf(
                pixChave?.let { PixBrCode.normalizarChave(it, pixTipo) }?.ifBlank { null },
                pixTipo.value,
                pixNome?.trim()?.ifBlank { null },
                pixCidade?.trim()?.ifBlank { null },
                mensalidadeCentavos,
                diariaCentavos,
                agoraIso(),
                id,
            ),
        )
    }

    suspend fun gerarMensalidade(
        competencia: YearMonth,
        valorCentavos: Int,
        criadoPor: String,
    ): Int =
        gerar(
            titulo = "Mensalidade ${MESES[competencia.monthValue - 1]} de ${competencia.year}",
            tipo = TipoCobranca.MENSALIDADE,
            valorCentavos = valorCentavos,
            competencia = competencia.atDay(1).toString(),
            venceEm = competencia.atDay(10).toString(),
            criadoPor = criadoPor,
            regimes = listOf(Regime.MENSALISTA.value),
            somentePresentes = false,
        )

    suspend fun gerarDiaria(
        dia: LocalDate,
        valorCentavos: Int,
        criadoPor: String,
    ): Int =
        gerar(
            titulo = "Diária de ${dia.dayOfMonth}/${dia.monthValue}",
            tipo = TipoCobranca.DIARIA,
            valorCentavos = valorCentavos,
            competencia = dia.toString(),
            venceEm = dia.toString(),
            criadoPor = criadoPor,
            regimes = listOf(Regime.DIARISTA.value),
            somentePresentes = true,
        )

    private suspend fun gerar(
        titulo: String,
        tipo: TipoCobranca,
        valorCentavos: Int,
        competencia: String,
        venceEm: String,
        criadoPor: String,
        regimes: List<String>,
        somentePresentes: Boolean,
    ): Int {
        if (valorCentavos <= 0) return 0
        val grupo = grupoAtivo.exigir()

        return db.writeTransactionAsync { tx ->
            val jaExiste =
                tx
                    .getAll(
                        "SELECT id FROM cobrancas WHERE grupo_id = ? AND tipo = ? AND competencia = ?",
                        listOf(grupo, tipo.value, competencia),
                    ) { it.getString("id") }
                    .firstOrNull()

            if (jaExiste != null) {
                0
            } else {
                val marcadores = regimes.joinToString(",") { "?" }
                val filtroPresenca = if (somentePresentes) " AND ativo = 1" else ""
                val alvos =
                    tx.getAll(
                        "SELECT id FROM players WHERE grupo_id = ? AND regime IN ($marcadores)$filtroPresenca",
                        listOf(grupo) + regimes,
                    ) { it.getString("id") }

                if (alvos.isEmpty()) {
                    0
                } else {
                    val cobrancaId = novoId()
                    val agora = agoraIso()
                    tx.execute(
                        """
                        INSERT INTO cobrancas (
                            id, grupo_id, titulo, tipo, valor_centavos, competencia,
                            vence_em, criado_por, criado_em
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                        listOf(
                            cobrancaId,
                            grupo,
                            titulo,
                            tipo.value,
                            valorCentavos,
                            competencia,
                            venceEm,
                            criadoPor,
                            agora,
                        ),
                    )
                    alvos.forEach { playerId ->
                        tx.execute(
                            """
                            INSERT INTO pagamentos (
                                id, grupo_id, cobranca_id, player_id, valor_centavos, status, criado_em
                            ) VALUES (?, ?, ?, ?, ?, ?, ?)
                            """.trimIndent(),
                            listOf(
                                novoId(),
                                grupo,
                                cobrancaId,
                                playerId,
                                valorCentavos,
                                StatusPagamento.PENDENTE.value,
                                agora,
                            ),
                        )
                    }
                    alvos.size
                }
            }
        }
    }

    suspend fun definirStatus(
        pagamentoId: String,
        status: StatusPagamento,
        registradoPor: String,
    ) {
        db.execute(
            """
            UPDATE pagamentos
            SET status = ?, pago_em = ?, registrado_por = ?
            WHERE id = ?
            """.trimIndent(),
            listOf(
                status.value,
                if (status == StatusPagamento.PAGO) agoraIso() else null,
                registradoPor,
                pagamentoId,
            ),
        )
    }

    suspend fun excluirCobranca(cobrancaId: String) {
        db.writeTransactionAsync { tx ->
            tx.execute("DELETE FROM pagamentos WHERE cobranca_id = ?", listOf(cobrancaId))
            tx.execute("DELETE FROM cobrancas WHERE id = ?", listOf(cobrancaId))
        }
    }

    private companion object {
        val MESES =
            listOf(
                "janeiro", "fevereiro", "março", "abril", "maio", "junho",
                "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
            )
    }
}
