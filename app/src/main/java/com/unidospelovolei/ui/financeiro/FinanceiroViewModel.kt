package com.unidospelovolei.ui.financeiro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.FinanceiroRepository
import com.unidospelovolei.data.PlayersRepository
import com.unidospelovolei.domain.financeiro.PixBrCode
import com.unidospelovolei.domain.model.Cobranca
import com.unidospelovolei.domain.model.ConfigFinanceiro
import com.unidospelovolei.domain.model.ItemDoExtrato
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.StatusPagamento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class LinhaDoPainelFinanceiro(
    val pagamentoId: String,
    val nome: String,
    val cobranca: String,
    val valorCentavos: Int,
    val status: StatusPagamento,
)

data class FinanceiroUiState(
    val config: ConfigFinanceiro? = null,
    val cobrancas: List<Cobranca> = emptyList(),
    val extrato: List<ItemDoExtrato> = emptyList(),
    val painel: List<LinhaDoPainelFinanceiro> = emptyList(),
    val carregandoPainel: Boolean = false,
    val salvando: Boolean = false,
    val erro: String? = null,
    val aviso: String? = null,
) {
    val emAbertoCentavos: Int
        get() = extrato.filter { it.pagamento.status == StatusPagamento.PENDENTE }
            .sumOf { it.pagamento.valorCentavos }

    fun pixCopiaECola(valorCentavos: Int): String? {
        val atual = config ?: return null
        val chave = atual.pixChave?.takeIf { it.isNotBlank() } ?: return null
        return PixBrCode.gerar(
            chave = chave,
            nome = atual.pixNome.orEmpty(),
            cidade = atual.pixCidade.orEmpty(),
            valorCentavos = valorCentavos,
        )
    }
}

class FinanceiroViewModel(
    private val financeiroRepository: FinanceiroRepository,
    playersRepository: PlayersRepository,
) : ViewModel() {
    private val painel = MutableStateFlow<List<LinhaDoPainelFinanceiro>>(emptyList())
    private val carregandoPainel = MutableStateFlow(false)
    private val salvando = MutableStateFlow(false)
    private val erro = MutableStateFlow<String?>(null)
    private val aviso = MutableStateFlow<String?>(null)

    private var jogadores: List<Player> = emptyList()
    private var cobrancasAtuais: List<Cobranca> = emptyList()
    private var configAtual: ConfigFinanceiro? = null

    private val dados =
        combine(
            financeiroRepository.observeConfig().catch { emit(null) },
            financeiroRepository.observeCobrancas().catch { emit(emptyList()) },
            financeiroRepository.observeMeuExtrato().catch { emit(emptyList()) },
            playersRepository.observePlayers().catch { emit(emptyList()) },
        ) { config, cobrancas, extrato, players ->
            configAtual = config
            cobrancasAtuais = cobrancas
            jogadores = players
            Quadro(config, cobrancas, extrato)
        }

    private val controles =
        combine(painel, carregandoPainel, salvando, erro, aviso) { linhas, carregando, gravando, falha, mensagem ->
            Controles(linhas, carregando, gravando, falha, mensagem)
        }

    val estado: StateFlow<FinanceiroUiState> =
        combine(dados, controles) { quadro, ui ->
            FinanceiroUiState(
                config = quadro.config,
                cobrancas = quadro.cobrancas,
                extrato = quadro.extrato,
                painel = ui.painel,
                carregandoPainel = ui.carregandoPainel,
                salvando = ui.salvando,
                erro = ui.erro,
                aviso = ui.aviso,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceiroUiState())

    fun limparErro() {
        erro.value = null
    }

    fun limparAviso() {
        aviso.value = null
    }

    fun carregarPainel() {
        if (carregandoPainel.value) return
        viewModelScope.launch {
            carregandoPainel.value = true
            erro.value = null
            runCatching { financeiroRepository.lerPainel() }
                .onSuccess { linhas ->
                    val porJogador = jogadores.associateBy { it.id }
                    val porCobranca = cobrancasAtuais.associateBy { it.id }
                    painel.value =
                        linhas
                            .map { linha ->
                                LinhaDoPainelFinanceiro(
                                    pagamentoId = linha.id,
                                    nome = porJogador[linha.playerId]?.nome ?: "Jogador removido",
                                    cobranca = porCobranca[linha.cobrancaId]?.titulo ?: "Cobrança",
                                    valorCentavos = linha.valorCentavos,
                                    status = StatusPagamento.from(linha.status),
                                )
                            }.sortedWith(compareBy({ it.status != StatusPagamento.PENDENTE }, { it.nome.lowercase() }))
                }.onFailure {
                    erro.value = "Não foi possível carregar o financeiro do grupo. Precisa de internet."
                }
            carregandoPainel.value = false
        }
    }

    fun salvarConfig(
        pixChave: String?,
        pixNome: String?,
        pixCidade: String?,
        mensalidadeCentavos: Int,
        diariaCentavos: Int,
    ) = executar {
        val atual = configAtual ?: return@executar
        financeiroRepository.salvarConfig(
            id = atual.id,
            pixChave = pixChave,
            pixNome = pixNome,
            pixCidade = pixCidade,
            mensalidadeCentavos = mensalidadeCentavos,
            diariaCentavos = diariaCentavos,
        )
    }

    fun gerarMensalidadeDoMes(perfilId: String) = executar {
        val valor = configAtual?.mensalidadeCentavos ?: 0
        if (valor <= 0) {
            aviso.value = "Defina o valor da mensalidade antes de gerar."
            return@executar
        }
        val quantos = financeiroRepository.gerarMensalidade(YearMonth.now(), valor, perfilId)
        aviso.value =
            if (quantos == 0) {
                "A mensalidade deste mês já tinha sido gerada."
            } else {
                "Mensalidade gerada para $quantos mensalistas."
            }
    }

    fun gerarDiariaDeHoje(perfilId: String) = executar {
        val valor = configAtual?.diariaCentavos ?: 0
        if (valor <= 0) {
            aviso.value = "Defina o valor da diária antes de gerar."
            return@executar
        }
        val quantos = financeiroRepository.gerarDiaria(LocalDate.now(), valor, perfilId)
        aviso.value =
            when (quantos) {
                0 -> "Nenhum diarista presente hoje, ou a diária de hoje já foi gerada."
                1 -> "Diária gerada para 1 diarista presente."
                else -> "Diária gerada para $quantos diaristas presentes."
            }
    }

    fun definirStatus(
        pagamentoId: String,
        status: StatusPagamento,
        perfilId: String,
    ) = executar {
        financeiroRepository.definirStatus(pagamentoId, status, perfilId)
        painel.value =
            painel.value.map { linha ->
                if (linha.pagamentoId == pagamentoId) linha.copy(status = status) else linha
            }
    }

    private fun executar(acao: suspend () -> Unit) {
        if (salvando.value) return
        viewModelScope.launch {
            salvando.value = true
            erro.value = null
            runCatching { acao() }
                .onFailure { erro.value = it.message ?: "Não foi possível salvar." }
            salvando.value = false
        }
    }

    private data class Quadro(
        val config: ConfigFinanceiro?,
        val cobrancas: List<Cobranca>,
        val extrato: List<ItemDoExtrato>,
    )

    private data class Controles(
        val painel: List<LinhaDoPainelFinanceiro>,
        val carregandoPainel: Boolean,
        val salvando: Boolean,
        val erro: String?,
        val aviso: String?,
    )
}
