package com.unidospelovolei.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.GameDaysRepository
import com.unidospelovolei.data.PlayersRepository
import com.unidospelovolei.domain.model.Genero
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.PlayerPerformance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayersUiState(
    val carregando: Boolean = true,
    val jogadores: List<Player> = emptyList(),
    val desempenho: Map<String, PlayerPerformance> = emptyMap(),
    val salvando: Boolean = false,
    val erro: String? = null,
) {
    val ativos: Int get() = jogadores.count { it.ativo }

    val homensAtivos: Int get() = jogadores.count { it.ativo && it.genero == Genero.MASCULINO }

    val mulheresAtivas: Int get() = jogadores.count { it.ativo && it.genero == Genero.FEMININO }
}

class PlayersViewModel(
    private val playersRepository: PlayersRepository,
    gameDaysRepository: GameDaysRepository,
) : ViewModel() {
    private val salvando = MutableStateFlow(false)
    private val erro = MutableStateFlow<String?>(null)

    val estado: StateFlow<PlayersUiState> =
        combine(
            playersRepository.observePlayers(),
            gameDaysRepository.observePerformances(),
            salvando,
            erro,
        ) { jogadores, desempenho, salvandoAgora, mensagem ->
            PlayersUiState(
                carregando = false,
                jogadores = jogadores,
                desempenho = desempenho,
                salvando = salvandoAgora,
                erro = mensagem,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayersUiState())

    fun criar(
        nome: String,
        skillLevel: Int,
        genero: Genero,
        ativo: Boolean,
    ) = executar { playersRepository.create(nome, skillLevel.coerceIn(1, 5), genero, ativo) }

    fun salvar(player: Player) =
        executar {
            playersRepository.update(player.copy(skillLevel = player.skillLevel.coerceIn(1, 5)))
        }

    fun excluir(playerId: String) = executar { playersRepository.delete(playerId) }

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
