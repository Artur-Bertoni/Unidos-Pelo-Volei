package com.unidospelovolei.ui.evolucao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.AvaliacaoRepository
import com.unidospelovolei.domain.model.AvaliacaoPendente
import com.unidospelovolei.domain.model.Dica
import com.unidospelovolei.domain.model.Evolucao
import com.unidospelovolei.domain.model.Fundamento
import com.unidospelovolei.domain.model.MINIMO_DE_AVALIACOES
import com.unidospelovolei.domain.model.NotasDaAvaliacao
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
    val evolucao: Evolucao? = null,
    val pendentes: List<AvaliacaoPendente> = emptyList(),
    val dicas: List<Dica> = emptyList(),
    val salvando: Boolean = false,
    val erro: String? = null,
    val aviso: String? = null,
) {
    val faltam: Int
        get() = (MINIMO_DE_AVALIACOES - (evolucao?.totalAvaliacoes ?: 0)).coerceAtLeast(0)

    val liberado: Boolean get() = evolucao?.liberado == true

    val dicasDoPontoFraco: List<Dica>
        get() {
            val fraco = evolucao?.maisFraco ?: return emptyList()
            val media = evolucao.medias[fraco] ?: return emptyList()
            return dicas
                .filter { it.fundamento == fraco }
                .sortedBy { it.faixaMax }
                .filter { media <= it.faixaMax }
                .take(1)
                .ifEmpty { dicas.filter { it.fundamento == fraco }.take(1) }
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

    val estado: StateFlow<EvolucaoUiState> =
        combine(
            avaliacaoRepository.observeEvolucao().catch { emit(null) },
            pendentes,
            avaliacaoRepository.observeDicas().catch { emit(emptyList()) },
            salvando,
            combine(erro, aviso) { falha, mensagem -> falha to mensagem },
        ) { evolucao, fila, dicas, gravando, mensagens ->
            EvolucaoUiState(
                evolucao = evolucao,
                pendentes = fila,
                dicas = dicas,
                salvando = gravando,
                erro = mensagens.first,
                aviso = mensagens.second,
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
