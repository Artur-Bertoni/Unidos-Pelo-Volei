package com.unidospelovolei.ui.evolucao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.AvaliacaoRepository
import com.unidospelovolei.domain.model.AvaliacaoPendente
import com.unidospelovolei.domain.model.Dica
import com.unidospelovolei.domain.model.Fundamento
import com.unidospelovolei.domain.model.NotasDaAvaliacao
import com.unidospelovolei.domain.model.NotasPorFundamento
import com.unidospelovolei.domain.model.OrigemDaNota
import com.unidospelovolei.domain.model.PontoDaNota
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EvolucaoUiState(
    val totalAvaliacoes: Int = 0,
    val historico: List<PontoDaNota> = emptyList(),
    val pendentes: List<AvaliacaoPendente> = emptyList(),
    val dicas: List<Dica> = emptyList(),
    val salvando: Boolean = false,
    val erro: String? = null,
    val aviso: String? = null,
) {
    val jaMoveu: Boolean get() = historico.any { it.origem == OrigemDaNota.AVALIACAO }

    fun dicaPara(notas: NotasPorFundamento): Dica? {
        val fraco: Fundamento = notas.maisFraco
        val nota = notas.de(fraco)
        val doFundamento = dicas.filter { it.fundamento == fraco }
        return doFundamento.sortedBy { it.faixaMax }.firstOrNull { nota <= it.faixaMax }
            ?: doFundamento.firstOrNull()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EvolucaoViewModel(
    private val avaliacaoRepository: AvaliacaoRepository,
) : ViewModel() {
    private val meuPlayerId = MutableStateFlow<String?>(null)
    private val salvando = MutableStateFlow(false)
    private val erro = MutableStateFlow<String?>(null)
    private val aviso = MutableStateFlow<String?>(null)

    private val pendentes =
        meuPlayerId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else avaliacaoRepository.observePendentes(id).catch { emit(emptyList()) }
        }

    private val historico =
        meuPlayerId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else avaliacaoRepository.observeHistorico(id).catch { emit(emptyList()) }
        }

    val estado: StateFlow<EvolucaoUiState> =
        combine(
            avaliacaoRepository.observeEvolucao().catch { emit(null) },
            historico,
            pendentes,
            avaliacaoRepository.observeDicas().catch { emit(emptyList()) },
            combine(salvando, erro, aviso) { gravando, falha, mensagem -> Triple(gravando, falha, mensagem) },
        ) { evolucao, linha, fila, dicas, situacao ->
            EvolucaoUiState(
                totalAvaliacoes = evolucao?.totalAvaliacoes ?: 0,
                historico = linha,
                pendentes = fila,
                dicas = dicas,
                salvando = situacao.first,
                erro = situacao.second,
                aviso = situacao.third,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EvolucaoUiState())

    fun definirMeuJogador(playerId: String?) {
        meuPlayerId.value = playerId
    }

    fun limparErro() {
        erro.value = null
    }

    fun limparAviso() {
        aviso.value = null
    }

    fun avaliar(
        pendente: AvaliacaoPendente,
        notas: NotasDaAvaliacao,
    ) {
        val avaliador = meuPlayerId.value ?: return
        if (salvando.value) return
        viewModelScope.launch {
            salvando.value = true
            erro.value = null
            runCatching {
                avaliacaoRepository.avaliar(
                    dayId = pendente.dayId,
                    avaliadorPlayerId = avaliador,
                    avaliadoPlayerId = pendente.avaliadoPlayerId,
                    notas = notas,
                )
            }.onSuccess { aviso.value = "Avaliação de ${pendente.avaliadoNome} enviada. Ela é anônima." }
                .onFailure { erro.value = it.message ?: "Não foi possível enviar a avaliação." }
            salvando.value = false
        }
    }

    companion object {
        val FUNDAMENTOS: List<Fundamento> = Fundamento.entries
    }
}
