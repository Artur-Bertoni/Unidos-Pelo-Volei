package com.unidospelovolei.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.GameDaysRepository
import com.unidospelovolei.data.MatchesRepository
import com.unidospelovolei.data.PlayersRepository
import com.unidospelovolei.data.TeamsRepository
import com.unidospelovolei.domain.model.ResumoDoDia
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
    val temElencos: Boolean = false,
    val presentes: Int = 0,
    val gerando: Boolean = false,
    val encerrando: Boolean = false,
    val aviso: String? = null,
    val erro: String? = null,
)

class GamesViewModel(
    private val matchesRepository: MatchesRepository,
    private val teamsRepository: TeamsRepository,
    private val gameDaysRepository: GameDaysRepository,
    playersRepository: PlayersRepository,
) : ViewModel() {
    private val gerando = MutableStateFlow(false)
    private val encerrando = MutableStateFlow(false)
    private val aviso = MutableStateFlow<String?>(null)
    private val erro = MutableStateFlow<String?>(null)

    private val progresso = combine(gerando, encerrando, aviso, erro, ::Progresso)

    val estado: StateFlow<GamesUiState> =
        combine(
            matchesRepository.observeRounds(),
            matchesRepository.observeMatches(),
            teamsRepository.observeRosters(),
            playersRepository.observeActivePlayers(),
            progresso,
        ) { rodadas, partidas, elencos, presentes, emAndamento ->
            val times = elencos.map { it.team }
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
                temElencos = elencos.any { it.players.isNotEmpty() },
                presentes = presentes.size,
                gerando = emAndamento.gerando,
                encerrando = emAndamento.encerrando,
                aviso = emAndamento.aviso,
                erro = emAndamento.erro,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GamesUiState())

    fun gerarChaveamento(quadras: Int) {
        if (gerando.value) return
        viewModelScope.launch {
            gerando.value = true
            erro.value = null
            runCatching {
                val times = teamsRepository.observeTeams().first()
                require(times.size >= 2) { "Deixe pelo menos 2 times ativos para gerar o chaveamento." }
                matchesRepository.replaceSchedule(RoundRobinScheduler.generate(times, quadras))
            }.onFailure { erro.value = it.message ?: "Nao foi possivel gerar o chaveamento." }
            gerando.value = false
        }
    }

    fun encerrarDia() {
        if (encerrando.value) return
        viewModelScope.launch {
            encerrando.value = true
            erro.value = null
            runCatching { gameDaysRepository.encerrarDia() }
                .onSuccess { resumo ->
                    aviso.value = mensagemDoDia(resumo)
                }.onFailure { erro.value = it.message ?: "Nao foi possivel encerrar o dia." }
            encerrando.value = false
        }
    }

    fun limparErro() {
        erro.value = null
    }

    fun limparAviso() {
        aviso.value = null
    }

    private fun mensagemDoDia(resumo: ResumoDoDia): String =
        when {
            resumo.presencas == 0 -> "Ninguém estava marcado como presente: o dia foi apenas limpo."
            resumo.atletas == 0 ->
                "Dia encerrado: ${resumo.presencas} presenças guardadas, sem times montados. " +
                    "A lista de presença já está zerada."
            else ->
                "Dia encerrado: ${resumo.partidas} " +
                    (if (resumo.partidas == 1) "partida" else "partidas") +
                    " no desempenho de ${resumo.atletas} atletas e ${resumo.presencas} presenças guardadas. " +
                    "A lista de presença já está zerada."
        }

    private data class Progresso(
        val gerando: Boolean,
        val encerrando: Boolean,
        val aviso: String?,
        val erro: String?,
    )
}
