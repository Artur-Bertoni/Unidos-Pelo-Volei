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
import java.text.Normalizer

enum class FiltroPresenca(
    val rotulo: String,
) {
    TODOS("Todos"),
    PRESENTES("Presentes"),
    AUSENTES("Ausentes"),
}

data class PlayersUiState(
    val carregando: Boolean = true,
    val jogadores: List<Player> = emptyList(),
    val desempenho: Map<String, PlayerPerformance> = emptyMap(),
    val busca: String = "",
    val filtro: FiltroPresenca = FiltroPresenca.TODOS,
    val total: Int = 0,
    val presentes: Int = 0,
    val homensPresentes: Int = 0,
    val mulheresPresentes: Int = 0,
    val salvando: Boolean = false,
    val erro: String? = null,
)

class PlayersViewModel(
    private val playersRepository: PlayersRepository,
    gameDaysRepository: GameDaysRepository,
) : ViewModel() {
    private val busca = MutableStateFlow("")
    private val filtro = MutableStateFlow(FiltroPresenca.TODOS)
    private val salvando = MutableStateFlow(false)
    private val erro = MutableStateFlow<String?>(null)

    private val consulta = combine(busca, filtro, ::Consulta)

    val estado: StateFlow<PlayersUiState> =
        combine(
            playersRepository.observePlayers(),
            gameDaysRepository.observePerformances(),
            consulta,
            salvando,
            erro,
        ) { jogadores, desempenho, consultaAtual, salvandoAgora, mensagem ->
            PlayersUiState(
                carregando = false,
                jogadores = jogadores.filtrarPor(consultaAtual),
                desempenho = desempenho,
                busca = consultaAtual.busca,
                filtro = consultaAtual.filtro,
                total = jogadores.size,
                presentes = jogadores.count { it.ativo },
                homensPresentes = jogadores.count { it.ativo && it.genero == Genero.MASCULINO },
                mulheresPresentes = jogadores.count { it.ativo && it.genero == Genero.FEMININO },
                salvando = salvandoAgora,
                erro = mensagem,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayersUiState())

    fun buscar(texto: String) {
        busca.value = texto
    }

    fun filtrar(novoFiltro: FiltroPresenca) {
        filtro.value = novoFiltro
    }

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

    fun alternarPresenca(player: Player) = executar { playersRepository.setAtivo(player.id, !player.ativo) }

    fun marcarTodosPresentes() = executar { playersRepository.definirPresencaDeTodos(true) }

    fun limparPresencas() = executar { playersRepository.definirPresencaDeTodos(false) }

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

private data class Consulta(
    val busca: String,
    val filtro: FiltroPresenca,
)

private fun String.paraBusca(): String =
    Normalizer
        .normalize(trim(), Normalizer.Form.NFD)
        .filterNot { it.category == CharCategory.NON_SPACING_MARK }
        .lowercase()

private fun List<Player>.filtrarPor(consulta: Consulta): List<Player> {
    val alvo = consulta.busca.paraBusca()
    return filter { jogador ->
        val presencaCombina =
            when (consulta.filtro) {
                FiltroPresenca.TODOS -> true
                FiltroPresenca.PRESENTES -> jogador.ativo
                FiltroPresenca.AUSENTES -> !jogador.ativo
            }
        presencaCombina && (alvo.isEmpty() || jogador.nome.paraBusca().contains(alvo))
    }
}
