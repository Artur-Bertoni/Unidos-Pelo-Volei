package com.unidospelovolei.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.MatchesRepository
import com.unidospelovolei.data.TeamsRepository
import com.unidospelovolei.domain.model.RoundSchedule
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.domain.scheduling.RoundRobinScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GamesUiState(
    val carregando: Boolean = true,
    val rodadas: List<RoundSchedule> = emptyList(),
    val times: List<Team> = emptyList(),
    val gerando: Boolean = false,
    val erro: String? = null,
)

class GamesViewModel(
    private val matchesRepository: MatchesRepository,
    private val teamsRepository: TeamsRepository,
) : ViewModel() {
    private val gerando = MutableStateFlow(false)
    private val erro = MutableStateFlow<String?>(null)

    val estado: StateFlow<GamesUiState> =
        combine(
            matchesRepository.observeRounds(),
            matchesRepository.observeMatches(),
            teamsRepository.observeTeams(),
            gerando,
            erro,
        ) { rodadas, partidas, times, gerandoAgora, mensagem ->
            val porRodada = partidas.groupBy { it.roundNumero }
            GamesUiState(
                carregando = false,
                rodadas =
                    rodadas.map { rodada ->
                        val doRodada = porRodada[rodada.numero].orEmpty()
                        val jogando = doRodada.flatMap { listOf(it.teamA.id, it.teamB.id) }.toSet()
                        RoundSchedule(
                            round = rodada,
                            matches = doRodada,
                            folgam = times.filter { it.id !in jogando },
                        )
                    },
                times = times,
                gerando = gerandoAgora,
                erro = mensagem,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GamesUiState())

    /**
     * Gera o chaveamento do zero. Apaga rodadas e partidas anteriores, entao a
     * tela pede confirmacao antes de chamar.
     */
    fun gerarChaveamento(quadras: Int) {
        if (gerando.value) return
        viewModelScope.launch {
            gerando.value = true
            erro.value = null
            runCatching {
                val times = teamsRepository.observeTeams().first()
                require(times.size >= 2) { "Cadastre pelo menos 2 times ativos." }
                matchesRepository.replaceSchedule(RoundRobinScheduler.generate(times, quadras))
            }.onFailure { erro.value = it.message ?: "Nao foi possivel gerar o chaveamento." }
            gerando.value = false
        }
    }

    fun limparErro() {
        erro.value = null
    }
}
