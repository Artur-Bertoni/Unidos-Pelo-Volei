package com.unidospelovolei.ui.grupo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.ChamadaRepository
import com.unidospelovolei.data.GrupoRepository
import com.unidospelovolei.data.PlayersRepository
import com.unidospelovolei.domain.model.CategoriaPagina
import com.unidospelovolei.domain.model.ConfigGrupo
import com.unidospelovolei.domain.model.Evento
import com.unidospelovolei.domain.model.Marco
import com.unidospelovolei.domain.model.Pagina
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.Post
import com.unidospelovolei.domain.model.Presenca
import com.unidospelovolei.domain.model.ResumoDaChamada
import com.unidospelovolei.domain.model.StatusPresenca
import com.unidospelovolei.domain.model.TipoEvento
import com.unidospelovolei.domain.model.TipoMarco
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class SecaoDoGrupo(
    val rotulo: String,
) {
    MURAL("Mural"),
    AGENDA("Agenda"),
    CHAMADA("Chamada"),
    REGRAS("Regras"),
}

data class LinhaDaChamada(
    val jogador: Player,
    val status: StatusPresenca?,
    val origem: String?,
)

data class GrupoUiState(
    val secao: SecaoDoGrupo = SecaoDoGrupo.MURAL,
    val posts: List<Post> = emptyList(),
    val eventos: List<Evento> = emptyList(),
    val marcos: List<Marco> = emptyList(),
    val paginas: List<Pagina> = emptyList(),
    val membros: List<Player> = emptyList(),
    val chamada: List<LinhaDaChamada> = emptyList(),
    val resumo: ResumoDaChamada = ResumoDaChamada(0, 0, 0, 0),
    val dataDoSabado: String = "",
    val config: ConfigGrupo? = null,
    val salvando: Boolean = false,
    val erro: String? = null,
    val aviso: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class GrupoViewModel(
    private val grupoRepository: GrupoRepository,
    private val chamadaRepository: ChamadaRepository,
    playersRepository: PlayersRepository,
) : ViewModel() {
    private val secao = MutableStateFlow(SecaoDoGrupo.MURAL)
    private val perfilId = MutableStateFlow("")
    private val salvando = MutableStateFlow(false)
    private val erro = MutableStateFlow<String?>(null)
    private val aviso = MutableStateFlow<String?>(null)

    private val sabado = ChamadaRepository.proximoSabado()

    private val posts: Flow<List<Post>> =
        perfilId.flatMapLatest { id ->
            grupoRepository.observePosts(id).catch { emit(emptyList()) }
        }

    private val conteudo =
        combine(
            posts,
            grupoRepository.observeEventos().catch { emit(emptyList()) },
            grupoRepository.observePaginas().catch { emit(emptyList()) },
        ) { mural, eventos, paginas -> Triple(mural, eventos, paginas) }

    private val chamada =
        combine(
            playersRepository.observePlayers().catch { emit(emptyList()) },
            chamadaRepository.observePresencas(sabado).catch { emit(emptyList()) },
            chamadaRepository.observeConfig().catch { emit(null) },
        ) { jogadores, presencas, config -> Triple(jogadores, presencas, config) }

    private val controles =
        combine(secao, salvando, combine(erro, aviso) { e, a -> e to a }) { atual, carregando, mensagens ->
            Triple(atual, carregando, mensagens)
        }

    val estado: StateFlow<GrupoUiState> =
        combine(conteudo, chamada, controles) { (mural, eventos, paginas), (jogadores, presencas, config), (atual, carregando, mensagens) ->
            val porJogador = presencas.associateBy { it.playerId }
            val linhas =
                jogadores.map { jogador ->
                    val presenca = porJogador[jogador.id]
                    LinhaDaChamada(jogador = jogador, status = presenca?.status, origem = presenca?.origem)
                }

            GrupoUiState(
                secao = atual,
                posts = mural,
                eventos = eventos,
                marcos = marcosDe(jogadores),
                paginas = paginas,
                membros = jogadores,
                chamada = linhas,
                resumo = resumoDe(presencas, jogadores.size),
                dataDoSabado = sabado,
                config = config,
                salvando = carregando,
                erro = mensagens.first,
                aviso = mensagens.second,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GrupoUiState())

    fun definirPerfil(id: String) {
        perfilId.value = id
    }

    fun abrir(nova: SecaoDoGrupo) {
        secao.value = nova
    }

    fun limparErro() {
        erro.value = null
    }

    fun limparAviso() {
        aviso.value = null
    }

    fun publicar(
        titulo: String,
        corpo: String,
        fixado: Boolean,
        autorNome: String?,
    ) = executar {
        grupoRepository.publicar(perfilId.value, autorNome, titulo, corpo, fixado)
    }

    fun excluirPost(postId: String) = executar { grupoRepository.excluirPost(postId) }

    fun alternarReacao(postId: String) = executar {
        grupoRepository.alternarReacao(postId, perfilId.value)
    }

    fun salvarEvento(
        eventoId: String?,
        titulo: String,
        descricao: String?,
        tipo: TipoEvento,
        inicio: String,
        local: String?,
    ) = executar {
        grupoRepository.salvarEvento(eventoId, titulo, descricao, tipo, inicio, local, perfilId.value)
    }

    fun excluirEvento(eventoId: String) = executar { grupoRepository.excluirEvento(eventoId) }

    fun salvarPagina(
        paginaId: String,
        titulo: String,
        corpo: String,
    ) = executar { grupoRepository.salvarPagina(paginaId, titulo, corpo, perfilId.value) }

    fun criarPagina(
        categoria: CategoriaPagina,
        titulo: String,
    ) = executar { grupoRepository.criarPagina(categoria, titulo, perfilId.value) }

    fun excluirPagina(paginaId: String) = executar { grupoRepository.excluirPagina(paginaId) }

    fun responderPor(
        playerId: String,
        status: StatusPresenca,
    ) = executar {
        chamadaRepository.responder(playerId, sabado, status, peloProprio = false, registradoPor = perfilId.value)
    }

    fun trazerConfirmados() = executar {
        val quantos = chamadaRepository.trazerConfirmados(sabado)
        aviso.value =
            when (quantos) {
                0 -> "Ninguém confirmou presença para este sábado ainda."
                1 -> "1 confirmado virou presença na lista de hoje."
                else -> "$quantos confirmados viraram presença na lista de hoje."
            }
    }

    fun salvarConfig(
        jogoHora: String,
        jogoLocal: String?,
    ) = executar {
        val atual = estado.value.config ?: return@executar
        chamadaRepository.salvarConfig(atual.id, jogoHora, jogoLocal)
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

    private fun resumoDe(
        presencas: List<Presenca>,
        totalDeJogadores: Int,
    ): ResumoDaChamada {
        val vou = presencas.count { it.status == StatusPresenca.VOU }
        val talvez = presencas.count { it.status == StatusPresenca.TALVEZ }
        val naoVou = presencas.count { it.status == StatusPresenca.NAO_VOU }
        return ResumoDaChamada(
            vou = vou,
            talvez = talvez,
            naoVou = naoVou,
            semResposta = (totalDeJogadores - vou - talvez - naoVou).coerceAtLeast(0),
        )
    }

    private fun marcosDe(jogadores: List<Player>): List<Marco> {
        val hoje = LocalDate.now()
        val aniversarios =
            jogadores.mapNotNull { jogador ->
                val dia = jogador.nascimentoDia ?: return@mapNotNull null
                val mes = jogador.nascimentoMes ?: return@mapNotNull null
                Marco(
                    playerId = jogador.id,
                    nome = jogador.nome,
                    tipo = TipoMarco.ANIVERSARIO,
                    dia = dia,
                    mes = mes,
                    anos = null,
                )
            }

        val tempoDeCasa =
            jogadores.mapNotNull { jogador ->
                val entrada = runCatching { LocalDate.parse(jogador.entrouEm) }.getOrNull()
                    ?: return@mapNotNull null
                val anos = hoje.year - entrada.year
                if (anos < 1) return@mapNotNull null
                Marco(
                    playerId = jogador.id,
                    nome = jogador.nome,
                    tipo = TipoMarco.TEMPO_DE_CASA,
                    dia = entrada.dayOfMonth,
                    mes = entrada.monthValue,
                    anos = anos,
                )
            }

        return (aniversarios + tempoDeCasa).sortedWith(
            compareBy({ diasAte(it, hoje) }, { it.nome.lowercase() }),
        )
    }

    private fun diasAte(
        marco: Marco,
        hoje: LocalDate,
    ): Int {
        val desteAno = runCatching { LocalDate.of(hoje.year, marco.mes, marco.dia) }.getOrNull()
            ?: return Int.MAX_VALUE
        val alvo = if (desteAno.isBefore(hoje)) desteAno.plusYears(1) else desteAno
        return (alvo.toEpochDay() - hoje.toEpochDay()).toInt()
    }
}
