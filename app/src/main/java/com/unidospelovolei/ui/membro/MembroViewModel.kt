package com.unidospelovolei.ui.membro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.AuthRepository
import com.unidospelovolei.data.ChamadaRepository
import com.unidospelovolei.data.ContaDoGrupo
import com.unidospelovolei.data.ContasRepository
import com.unidospelovolei.data.GameDaysRepository
import com.unidospelovolei.data.MembroRepository
import com.unidospelovolei.data.PlayersRepository
import com.unidospelovolei.data.ProfileRepository
import com.unidospelovolei.domain.model.ConfigGrupo
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.PlayerContato
import com.unidospelovolei.domain.model.PlayerPerformance
import com.unidospelovolei.domain.model.Regime
import com.unidospelovolei.domain.model.StatusPresenca
import com.unidospelovolei.domain.model.StatusVinculo
import com.unidospelovolei.domain.model.UserProfile
import com.unidospelovolei.domain.model.VinculoPedido
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PedidoNaFila(
    val pedido: VinculoPedido,
    val jogador: Player?,
)

data class MembroUiState(
    val carregando: Boolean = true,
    val profile: UserProfile? = null,
    val meuJogador: Player? = null,
    val meuPedido: VinculoPedido? = null,
    val meuContato: PlayerContato? = null,
    val minhaResposta: StatusPresenca? = null,
    val dataDoSabado: String = "",
    val config: ConfigGrupo? = null,
    val meuDesempenho: PlayerPerformance? = null,
    val candidatos: List<Player> = emptyList(),
    val fila: List<PedidoNaFila> = emptyList(),
    val busca: String = "",
    val salvando: Boolean = false,
    val erro: String? = null,
) {
    val isAdmin: Boolean get() = profile?.isAdmin == true

    val aguardando: Boolean
        get() = meuJogador == null && meuPedido?.status == StatusVinculo.PENDENTE

    val recusado: Boolean
        get() = meuJogador == null && meuPedido?.status == StatusVinculo.RECUSADO

    val precisaEscolher: Boolean
        get() = profile != null && meuJogador == null && !aguardando
}

@OptIn(ExperimentalCoroutinesApi::class)
class MembroViewModel(
    authRepository: AuthRepository,
    profileRepository: ProfileRepository,
    private val playersRepository: PlayersRepository,
    gameDaysRepository: GameDaysRepository,
    private val membroRepository: MembroRepository,
    private val chamadaRepository: ChamadaRepository,
    private val contasRepository: ContasRepository,
) : ViewModel() {
    private val sabado = ChamadaRepository.proximoSabado()
    private val busca = MutableStateFlow("")
    private val salvando = MutableStateFlow(false)
    private val erro = MutableStateFlow<String?>(null)
    private val contas = MutableStateFlow<List<ContaDoGrupo>>(emptyList())
    private val carregandoContas = MutableStateFlow(false)

    val contasDoGrupo: StateFlow<List<ContaDoGrupo>> = contas.asStateFlow()
    val buscandoContas: StateFlow<Boolean> = carregandoContas.asStateFlow()

    private val usuarioId =
        authRepository.sessionStatus.map { status ->
            (status as? SessionStatus.Authenticated)?.session?.user?.id
        }

    private val identidade: Flow<Identidade> =
        usuarioId.flatMapLatest { id ->
            if (id == null) {
                flowOf(Identidade())
            } else {
                combine(
                    profileRepository.observeProfile(id).catch { emit(null) },
                    playersRepository.observePlayerDoPerfil(id).catch { emit(null) },
                    membroRepository.observeMeuPedido(id).catch { emit(null) },
                ) { perfil, jogador, pedido ->
                    Identidade(carregando = false, profile = perfil, jogador = jogador, pedido = pedido)
                }
            }
        }

    private val pessoal: Flow<Pessoal> =
        identidade.flatMapLatest { atual ->
            val playerId = atual.jogador?.id
            val contato =
                if (playerId == null) flowOf(null) else membroRepository.observeContato(playerId).catch { emit(null) }
            combine(
                contato,
                chamadaRepository.observePresencas(sabado).catch { emit(emptyList()) },
                chamadaRepository.observeConfig().catch { emit(null) },
            ) { dados, presencas, config ->
                Pessoal(
                    contato = dados,
                    resposta = presencas.firstOrNull { it.playerId == playerId }?.status,
                    config = config,
                )
            }
        }

    private val grupo: Flow<Grupo> =
        combine(
            playersRepository.observePlayers().catch { emit(emptyList()) },
            membroRepository.observePedidosPendentes().catch { emit(emptyList()) },
            gameDaysRepository.observePerformances().catch { emit(emptyMap()) },
        ) { jogadores, pendentes, desempenho ->
            Grupo(jogadores = jogadores, pendentes = pendentes, desempenho = desempenho)
        }

    private val controles =
        combine(busca, salvando, erro) { texto, carregando, mensagem ->
            Controles(busca = texto, salvando = carregando, erro = mensagem)
        }

    val estado: StateFlow<MembroUiState> =
        combine(identidade, pessoal, grupo, controles) { atual, meu, dados, ui ->
            val porId = dados.jogadores.associateBy { it.id }
            val termo = ui.busca.trim().lowercase()

            MembroUiState(
                carregando = atual.carregando,
                profile = atual.profile,
                meuJogador = atual.jogador,
                meuPedido = atual.pedido,
                meuContato = meu.contato,
                minhaResposta = meu.resposta,
                dataDoSabado = sabado,
                config = meu.config,
                meuDesempenho = atual.jogador?.let { dados.desempenho[it.id] },
                candidatos =
                    dados.jogadores
                        .filter { !it.vinculado }
                        .filter { termo.isEmpty() || it.nome.lowercase().contains(termo) },
                fila = dados.pendentes.map { PedidoNaFila(pedido = it, jogador = porId[it.playerId]) },
                busca = ui.busca,
                salvando = ui.salvando,
                erro = ui.erro,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MembroUiState())

    fun buscar(texto: String) {
        busca.value = texto
    }

    fun limparErro() {
        erro.value = null
    }

    fun pedirVinculo(playerId: String) {
        val perfil = estado.value.profile ?: return
        executar {
            membroRepository.pedirVinculo(
                profileId = perfil.id,
                profileNome = perfil.nome ?: perfil.email,
                playerId = playerId,
            )
            busca.value = ""
        }
    }

    fun cancelarPedido() {
        val pedido = estado.value.meuPedido ?: return
        executar { membroRepository.cancelarPedido(pedido.id) }
    }

    fun decidir(
        pedido: VinculoPedido,
        aprovado: Boolean,
    ) {
        val perfil = estado.value.profile ?: return
        executar { membroRepository.decidir(pedido, aprovado, perfil.id) }
    }

    fun carregarContas() {
        if (carregandoContas.value) return
        viewModelScope.launch {
            carregandoContas.value = true
            runCatching { contasRepository.listar() }
                .onSuccess { contas.value = it }
                .onFailure {
                    erro.value = "Não foi possível listar as contas. Precisa de internet."
                }
            carregandoContas.value = false
        }
    }

    fun vincularManualmente(
        playerId: String,
        profileId: String,
    ) = executar { playersRepository.vincular(playerId, profileId) }

    fun desvincular(playerId: String) = executar { playersRepository.vincular(playerId, null) }

    fun salvarFicha(
        nome: String,
        nascimentoDia: Int?,
        nascimentoMes: Int?,
        telefone: String?,
        contatoEmergencia: String?,
        nascimentoAno: Int?,
        regime: Regime,
    ) {
        val jogador = estado.value.meuJogador ?: return
        val perfil = estado.value.profile ?: return
        executar {
            playersRepository.salvarFicha(
                playerId = jogador.id,
                nome = nome,
                nascimentoDia = nascimentoDia,
                nascimentoMes = nascimentoMes,
                regime = regime,
            )
            membroRepository.salvarContato(
                playerId = jogador.id,
                profileId = perfil.id,
                telefone = telefone,
                contatoEmergencia = contatoEmergencia,
                nascimentoAno = nascimentoAno,
            )
        }
    }

    private fun executar(acao: suspend () -> Unit) {
        if (salvando.value) return
        viewModelScope.launch {
            salvando.value = true
            erro.value = null
            runCatching { acao() }
                .onFailure { erro.value = it.message ?: "Não foi possível salvar." }
            salvando.value = false
        }
    }

    fun responderChamada(status: StatusPresenca) {
        val jogador = estado.value.meuJogador ?: return
        val perfil = estado.value.profile ?: return
        executar {
            chamadaRepository.responder(
                playerId = jogador.id,
                data = sabado,
                status = status,
                peloProprio = true,
                registradoPor = perfil.id,
            )
        }
    }

    private data class Pessoal(
        val contato: PlayerContato? = null,
        val resposta: StatusPresenca? = null,
        val config: ConfigGrupo? = null,
    )

    private data class Identidade(
        val carregando: Boolean = true,
        val profile: UserProfile? = null,
        val jogador: Player? = null,
        val pedido: VinculoPedido? = null,
    )

    private data class Grupo(
        val jogadores: List<Player> = emptyList(),
        val pendentes: List<VinculoPedido> = emptyList(),
        val desempenho: Map<String, PlayerPerformance> = emptyMap(),
    )

    private data class Controles(
        val busca: String = "",
        val salvando: Boolean = false,
        val erro: String? = null,
    )
}
