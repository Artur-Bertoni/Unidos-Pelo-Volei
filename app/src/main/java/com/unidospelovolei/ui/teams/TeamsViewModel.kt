package com.unidospelovolei.ui.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.GameDaysRepository
import com.unidospelovolei.data.PlayersRepository
import com.unidospelovolei.data.TeamsRepository
import com.unidospelovolei.domain.model.Genero
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.domain.model.TeamRoster
import com.unidospelovolei.domain.scheduling.ElencoPassado
import com.unidospelovolei.domain.scheduling.HistoricoDeDuplas
import com.unidospelovolei.domain.scheduling.TeamDraft
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
    val presentes: Int = 0,
    val homensPresentes: Int = 0,
    val mulheresPresentes: Int = 0,
    val previaDistribuicao: List<TeamRoster>? = null,
    val salvando: Boolean = false,
    val erro: String? = null,
) {
    val timesAtivos: Int get() = times.count { it.ativo }

    val timesQueCabem: Int get() = presentes / TeamDraft.JOGADORES_POR_TIME
}

class TeamsViewModel(
    private val teamsRepository: TeamsRepository,
    private val playersRepository: PlayersRepository,
    private val gameDaysRepository: GameDaysRepository,
) : ViewModel() {
    private val previa = MutableStateFlow<List<TeamRoster>?>(null)
    private val salvando = MutableStateFlow(false)
    private val erro = MutableStateFlow<String?>(null)

    private val previasRecentes = ArrayDeque<List<List<String>>>()

    private val progresso = combine(previa, salvando, erro, ::Progresso)

    val estado: StateFlow<TeamsUiState> =
        combine(
            teamsRepository.observeAllTeams(),
            teamsRepository.observeRosters(),
            playersRepository.observeActivePlayers(),
            progresso,
        ) { times, elencos, presentes, emAndamento ->
            TeamsUiState(
                carregando = false,
                times = times,
                elencos = elencos,
                presentes = presentes.size,
                homensPresentes = presentes.count { it.genero == Genero.MASCULINO },
                mulheresPresentes = presentes.count { it.genero == Genero.FEMININO },
                previaDistribuicao = emAndamento.previa,
                salvando = emAndamento.salvando,
                erro = emAndamento.erro,
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

    fun alternarAtivo(team: Team) = executar { teamsRepository.setAtivo(team.id, !team.ativo) }

    fun ajustarTimesAosPresentes() =
        executar {
            val presentes = playersRepository.observeActivePlayers().first().size
            val times = teamsRepository.observeAllTeams().first()
            require(times.isNotEmpty()) { "Cadastre os times antes de ajustar." }
            require(presentes >= TeamDraft.JOGADORES_POR_TIME) {
                "Marque ao menos ${TeamDraft.JOGADORES_POR_TIME} presentes para montar um time."
            }
            val alvo = (presentes / TeamDraft.JOGADORES_POR_TIME).coerceAtMost(times.size)
            val porPrioridade = times.sortedWith(compareBy({ !it.ativo }, { it.ordem }, { it.nome.lowercase() }))
            teamsRepository.definirAtivos(
                porPrioridade.mapIndexed { indice, time -> time.id to (indice < alvo) }.toMap(),
            )
        }

    fun excluirTime(teamId: String) = executar { teamsRepository.delete(teamId) }

    fun calcularDistribuicao() {
        viewModelScope.launch {
            runCatching {
                val jogadores = playersRepository.observeActivePlayers().first()
                val times = teamsRepository.observeTeams().first()
                require(times.isNotEmpty()) { "Ative ao menos um time para hoje." }
                require(jogadores.isNotEmpty()) { "Marque quem veio jogar antes de distribuir." }

                val historico =
                    HistoricoDeDuplas.de(
                        gameDaysRepository.observeElencosPassados().first() +
                            elencoAtualComoHistorico() +
                            previasRecentesComoHistorico(),
                    )
                TeamDraft.distribute(jogadores, times, historico)
            }.onSuccess { distribuicao ->
                lembrarPrevia(distribuicao)
                previa.value = distribuicao
            }.onFailure { erro.value = it.message ?: "Nao foi possivel calcular a distribuicao." }
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

    private suspend fun elencoAtualComoHistorico(): List<ElencoPassado> =
        teamsRepository
            .observeRosters()
            .first()
            .filter { it.players.size > 1 }
            .map { roster -> ElencoPassado(roster.players.map { it.id }, PESO_ELENCO_ATUAL) }

    private fun previasRecentesComoHistorico(): List<ElencoPassado> =
        previasRecentes.flatMapIndexed { indice, elencos ->
            val peso = PESO_PREVIA_RECENTE / (1.0 + indice)
            elencos.filter { it.size > 1 }.map { ElencoPassado(it, peso) }
        }

    private fun lembrarPrevia(distribuicao: List<TeamRoster>) {
        previasRecentes.addFirst(distribuicao.map { roster -> roster.players.map { it.id } })
        while (previasRecentes.size > PREVIAS_LEMBRADAS) previasRecentes.removeLast()
    }

    private fun executar(acao: suspend () -> Unit) {
        viewModelScope.launch {
            salvando.value = true
            runCatching { acao() }
                .onFailure { erro.value = it.message ?: "Nao foi possivel salvar." }
            salvando.value = false
        }
    }

    private data class Progresso(
        val previa: List<TeamRoster>?,
        val salvando: Boolean,
        val erro: String?,
    )

    private companion object {
        const val PESO_ELENCO_ATUAL = 1.5
        const val PESO_PREVIA_RECENTE = 1.5
        const val PREVIAS_LEMBRADAS = 3
    }
}
