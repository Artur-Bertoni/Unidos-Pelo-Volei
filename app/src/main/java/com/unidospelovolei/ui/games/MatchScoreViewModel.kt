package com.unidospelovolei.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.MatchesRepository
import com.unidospelovolei.domain.model.MatchCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MatchScoreUiState(
    val partida: MatchCard? = null,
    val salvando: Boolean = false,
    val erro: String? = null,
)

class MatchScoreViewModel(
    private val matchesRepository: MatchesRepository,
    private val matchId: String,
) : ViewModel() {
    private val salvando = MutableStateFlow(false)
    private val erro = MutableStateFlow<String?>(null)

    val estado: StateFlow<MatchScoreUiState> =
        combine(
            matchesRepository.observeMatch(matchId).map { it.firstOrNull() },
            salvando,
            erro,
        ) { partida, salvandoAgora, mensagem ->
            MatchScoreUiState(partida = partida, salvando = salvandoAgora, erro = mensagem)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MatchScoreUiState())

    fun somar(
        ladoA: Boolean,
        delta: Int,
    ) {
        val partida = estado.value.partida ?: return
        val novoA = (partida.scoreA + if (ladoA) delta else 0).coerceAtLeast(0)
        val novoB = (partida.scoreB + if (ladoA) 0 else delta).coerceAtLeast(0)
        executar { matchesRepository.updateScore(matchId, novoA, novoB) }
    }

    fun definirPlacar(
        ladoA: Boolean,
        valor: Int,
    ) {
        val partida = estado.value.partida ?: return
        val limitado = valor.coerceIn(0, PLACAR_MAXIMO)
        val novoA = if (ladoA) limitado else partida.scoreA
        val novoB = if (ladoA) partida.scoreB else limitado
        if (novoA == partida.scoreA && novoB == partida.scoreB) return
        executar { matchesRepository.updateScore(matchId, novoA, novoB) }
    }

    fun finalizar() {
        val partida = estado.value.partida ?: return
        if (partida.scoreA == partida.scoreB) {
            erro.value = "Empate nao finaliza: ajuste o placar antes."
            return
        }
        executar {
            matchesRepository.finish(
                matchId = matchId,
                scoreA = partida.scoreA,
                scoreB = partida.scoreB,
                teamAId = partida.teamA.id,
                teamBId = partida.teamB.id,
            )
        }
    }

    fun reabrir() = executar { matchesRepository.reopen(matchId) }

    fun limparErro() {
        erro.value = null
    }

    private fun executar(acao: suspend () -> Unit) {
        viewModelScope.launch {
            salvando.value = true
            runCatching { acao() }
                .onFailure { erro.value = it.message ?: "Nao foi possivel salvar." }
            salvando.value = false
        }
    }

    companion object {
        const val PLACAR_MAXIMO: Int = 199
    }
}
