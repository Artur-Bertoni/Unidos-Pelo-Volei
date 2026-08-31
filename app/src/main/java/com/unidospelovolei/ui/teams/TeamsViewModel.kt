package com.unidospelovolei.ui.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.PlayersRepository
import com.unidospelovolei.data.TeamsRepository
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.domain.model.TeamRoster
import com.unidospelovolei.domain.scheduling.SnakeDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TeamsUiState(
    val carregando: Boolean = true,
    val times: List<Team> = emptyList(),
    val elencos: List<TeamRoster> = emptyList(),
    val previaDistribuicao: List<TeamRoster>? = null,
    val salvando: Boolean = false,
    val erro: String? = null,
)

class TeamsViewModel(
    private val teamsRepository: TeamsRepository,
    private val playersRepository: PlayersRepository,
) : ViewModel() {
    private val previa = MutableStateFlow<List<TeamRoster>?>(null)
    private val salvando = MutableStateFlow(false)
    private val erro = MutableStateFlow<String?>(null)

    val estado: StateFlow<TeamsUiState> =
        combine(
            teamsRepository.observeAllTeams(),
            teamsRepository.observeRosters(),
            previa,
            salvando,
            erro,
        ) { times, elencos, previaAtual, salvandoAgora, mensagem ->
            TeamsUiState(
                carregando = false,
                times = times,
                elencos = elencos,
                previaDistribuicao = previaAtual,
                salvando = salvandoAgora,
                erro = mensagem,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamsUiState())

    fun criarTime(
        nome: String,
        corHex: String,
        sigla: String,
    ) = executar {
        val proximaOrdem = (teamsRepository.observeAllTeams().first().maxOfOrNull { it.ordem } ?: 0) + 1
        teamsRepository.create(nome, corHex, sigla, proximaOrdem)
    }

    fun salvarTime(team: Team) = executar { teamsRepository.update(team) }

    fun excluirTime(teamId: String) = executar { teamsRepository.delete(teamId) }

    /**
     * Calcula a previa do snake draft sem gravar nada, para o usuario conferir a
     * forca de cada time antes de confirmar.
     */
    fun calcularDistribuicao() {
        viewModelScope.launch {
            runCatching {
                val jogadores = playersRepository.observeActivePlayers().first()
                val times = teamsRepository.observeTeams().first()
                require(times.isNotEmpty()) { "Cadastre ao menos um time ativo." }
                require(jogadores.isNotEmpty()) { "Cadastre jogadores ativos antes de distribuir." }
                SnakeDraft.distribute(jogadores, times)
            }.onSuccess { previa.value = it }
                .onFailure { erro.value = it.message ?: "Nao foi possivel calcular a distribuicao." }
        }
    }

    fun descartarDistribuicao() {
        previa.value = null
    }

    fun aplicarDistribuicao() {
        val distribuicao = previa.value ?: return
        executar {
            teamsRepository.replaceRosters(distribuicao)
            previa.value = null
        }
    }

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
}
