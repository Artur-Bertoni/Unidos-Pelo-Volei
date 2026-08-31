package com.unidospelovolei.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.PlayersRepository
import com.unidospelovolei.domain.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayersUiState(
    val carregando: Boolean = true,
    val jogadores: List<Player> = emptyList(),
    val salvando: Boolean = false,
    val erro: String? = null,
) {
    val ativos: Int get() = jogadores.count { it.ativo }
}

class PlayersViewModel(
    private val playersRepository: PlayersRepository,
) : ViewModel() {
    private val salvando = MutableStateFlow(false)
    private val erro = MutableStateFlow<String?>(null)

    val estado: StateFlow<PlayersUiState> =
        combine(playersRepository.observePlayers(), salvando, erro) { jogadores, salvandoAgora, mensagem ->
            PlayersUiState(
                carregando = false,
                jogadores = jogadores,
                salvando = salvandoAgora,
                erro = mensagem,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayersUiState())

    fun criar(
        nome: String,
        skillLevel: Int,
        ativo: Boolean,
    ) = executar { playersRepository.create(nome, skillLevel.coerceIn(1, 5), ativo) }

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
