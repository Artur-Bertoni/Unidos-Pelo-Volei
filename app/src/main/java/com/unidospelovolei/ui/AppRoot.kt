package com.unidospelovolei.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.unidospelovolei.AppContainer
import com.unidospelovolei.BuildConfig
import com.unidospelovolei.data.Push
import com.unidospelovolei.data.TokenPendente
import com.unidospelovolei.domain.model.Evento
import com.unidospelovolei.domain.model.Pagina
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.ui.components.AbaPrincipal
import com.unidospelovolei.ui.components.AppHeader
import com.unidospelovolei.ui.components.BarraDeAbas
import com.unidospelovolei.ui.components.SinalSync
import com.unidospelovolei.ui.evolucao.AvaliacaoScreen
import com.unidospelovolei.ui.evolucao.EvolucaoViewModel
import com.unidospelovolei.ui.financeiro.ConfigFinanceiroDialog
import com.unidospelovolei.ui.financeiro.FinanceiroViewModel
import com.unidospelovolei.ui.financeiro.PainelFinanceiroScreen
import com.unidospelovolei.ui.games.GamesScreen
import com.unidospelovolei.ui.games.GamesViewModel
import com.unidospelovolei.ui.games.MatchScoreScreen
import com.unidospelovolei.ui.games.MatchScoreViewModel
import com.unidospelovolei.ui.grupo.EventoDialog
import com.unidospelovolei.ui.grupo.GrupoScreen
import com.unidospelovolei.ui.grupo.GrupoViewModel
import com.unidospelovolei.ui.grupo.PaginaScreen
import com.unidospelovolei.ui.grupo.PostDialog
import com.unidospelovolei.ui.login.ConfiguracaoPendenteScreen
import com.unidospelovolei.ui.login.LoginScreen
import com.unidospelovolei.ui.main.EstadoSincronizacao
import com.unidospelovolei.ui.main.MainViewModel
import com.unidospelovolei.ui.membro.AprovacoesScreen
import com.unidospelovolei.ui.membro.EuScreen
import com.unidospelovolei.ui.membro.FichaDialog
import com.unidospelovolei.ui.membro.MembroViewModel
import com.unidospelovolei.ui.membro.VinculosScreen
import com.unidospelovolei.ui.players.PlayersScreen
import com.unidospelovolei.ui.players.PlayersViewModel
import com.unidospelovolei.ui.standings.StandingsScreen
import com.unidospelovolei.ui.standings.StandingsViewModel
import com.unidospelovolei.ui.teams.DraftScreen
import com.unidospelovolei.ui.teams.TeamEditorDialog
import com.unidospelovolei.ui.teams.TeamHistoryDialog
import com.unidospelovolei.ui.teams.TeamHistoryViewModel
import com.unidospelovolei.ui.teams.TeamsScreen
import com.unidospelovolei.ui.teams.TeamsViewModel
import com.unidospelovolei.ui.theme.VoleiColors
import com.unidospelovolei.ui.tour.Tour
import com.unidospelovolei.ui.tour.TourScreen

private sealed interface Destino {
    data object Abas : Destino

    data object Jogadores : Destino

    data object Distribuicao : Destino

    data object Aprovacoes : Destino

    data object Vinculos : Destino

    data object PainelFinanceiro : Destino

    data object Avaliacao : Destino

    data class Conteudo(
        val pagina: Pagina,
    ) : Destino

    data class Partida(
        val matchId: String,
    ) : Destino
}

@Composable
fun AppRoot(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    if (!container.configurado) {
        ConfiguracaoPendenteScreen(
            chavesFaltando = container.chavesFaltando,
            ambiente = BuildConfig.ENVIRONMENT,
            modifier = modifier,
        )
        return
    }

    val factory = rememberVoleiViewModelFactory(container)
    val mainViewModel: MainViewModel = viewModel(factory = factory)
    val estado by mainViewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    when {
        estado.carregandoSessao ->
            Box(
                modifier = modifier.fillMaxSize().background(VoleiColors.Fundo),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = VoleiColors.Verde)
            }

        !estado.logado ->
            LoginScreen(
                entrando = estado.entrando,
                erro = estado.erro,
                onEntrar = { mainViewModel.entrarComGoogle(contexto) },
                modifier = modifier,
            )

        else -> {
            var mostrarTour by rememberSaveable { mutableStateOf(!Tour.jaViu(contexto)) }
            var veioDoTour by rememberSaveable { mutableStateOf(false) }

            if (mostrarTour) {
                TourScreen(
                    onConcluir = {
                        Tour.marcarComoVisto(contexto)
                        veioDoTour = true
                        mostrarTour = false
                    },
                    modifier = modifier,
                )
            } else {
                HomeScreen(
                    container = container,
                    mainViewModel = mainViewModel,
                    abaInicial = if (veioDoTour) AbaPrincipal.EU else AbaPrincipal.SOCIAL,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    container: AppContainer,
    mainViewModel: MainViewModel,
    abaInicial: AbaPrincipal,
    modifier: Modifier = Modifier,
) {
    val factory = rememberVoleiViewModelFactory(container)
    val estado by mainViewModel.estado.collectAsStateWithLifecycle()

    val gamesViewModel: GamesViewModel = viewModel(factory = factory)
    val standingsViewModel: StandingsViewModel = viewModel(factory = factory)
    val teamsViewModel: TeamsViewModel = viewModel(factory = factory)
    val playersViewModel: PlayersViewModel = viewModel(factory = factory)
    val membroViewModel: MembroViewModel = viewModel(factory = factory)
    val grupoViewModel: GrupoViewModel = viewModel(factory = factory)
    val financeiroViewModel: FinanceiroViewModel = viewModel(factory = factory)
    val evolucaoViewModel: EvolucaoViewModel = viewModel(factory = factory)

    val jogos by gamesViewModel.estado.collectAsStateWithLifecycle()
    val classificacao by standingsViewModel.estado.collectAsStateWithLifecycle()
    val times by teamsViewModel.estado.collectAsStateWithLifecycle()
    val jogadores by playersViewModel.estado.collectAsStateWithLifecycle()
    val membro by membroViewModel.estado.collectAsStateWithLifecycle()
    val grupo by grupoViewModel.estado.collectAsStateWithLifecycle()
    val financeiro by financeiroViewModel.estado.collectAsStateWithLifecycle()
    val evolucao by evolucaoViewModel.estado.collectAsStateWithLifecycle()

    val contas by membroViewModel.contasDoGrupo.collectAsStateWithLifecycle()
    val carregandoContas by membroViewModel.buscandoContas.collectAsStateWithLifecycle()
    val sugestoesDeEndereco by grupoViewModel.sugestoesDeEndereco.collectAsStateWithLifecycle()

    val perfilId = membro.profile?.id
    val meuPlayerId = membro.meuJogador?.id

    LaunchedEffect(perfilId) {
        perfilId?.let(grupoViewModel::definirPerfil)
    }

    LaunchedEffect(meuPlayerId) {
        evolucaoViewModel.definirMeuJogador(meuPlayerId)
    }

    RegistroDePush(container = container, perfilId = perfilId)

    var aba by rememberSaveable { mutableStateOf(abaInicial) }
    var destino by remember { mutableStateOf<Destino>(Destino.Abas) }
    var historicoDoTime by remember { mutableStateOf<Team?>(null) }
    var editandoTime by remember { mutableStateOf<Team?>(null) }
    var criandoTime by remember { mutableStateOf(false) }
    var editandoFicha by remember { mutableStateOf(false) }
    var criandoPost by remember { mutableStateOf(false) }
    var editandoEvento by remember { mutableStateOf<Evento?>(null) }
    var criandoEvento by remember { mutableStateOf(false) }
    var configurandoFinanceiro by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val mensagemDeErro =
        estado.erro ?: jogos.erro ?: classificacao.erro ?: times.erro ?: jogadores.erro
            ?: membro.erro ?: grupo.erro ?: financeiro.erro ?: evolucao.erro

    LaunchedEffect(mensagemDeErro) {
        mensagemDeErro?.let {
            snackbar.showSnackbar(it)
            mainViewModel.limparErro()
            gamesViewModel.limparErro()
            standingsViewModel.limparErro()
            teamsViewModel.limparErro()
            playersViewModel.limparErro()
            membroViewModel.limparErro()
            grupoViewModel.limparErro()
            financeiroViewModel.limparErro()
            evolucaoViewModel.limparErro()
        }
    }

    val mensagemDeAviso = jogos.aviso ?: grupo.aviso ?: financeiro.aviso ?: evolucao.aviso

    LaunchedEffect(mensagemDeAviso) {
        mensagemDeAviso?.let {
            snackbar.showSnackbar(it)
            gamesViewModel.limparAviso()
            grupoViewModel.limparAviso()
            financeiroViewModel.limparAviso()
            evolucaoViewModel.limparAviso()
        }
    }

    when (val atual = destino) {
        is Destino.Jogadores -> {
            PlayersScreen(
                estado = jogadores,
                isAdmin = estado.isAdmin,
                onVoltar = { destino = Destino.Abas },
                onBuscar = playersViewModel::buscar,
                onFiltrar = playersViewModel::filtrar,
                onAlternarPresenca = playersViewModel::alternarPresenca,
                onMarcarTodosPresentes = playersViewModel::marcarTodosPresentes,
                onLimparPresencas = playersViewModel::limparPresencas,
                onCriar = playersViewModel::criar,
                onSalvar = playersViewModel::salvar,
                onExcluir = playersViewModel::excluir,
                modifier = modifier,
            )
            return
        }

        is Destino.Distribuicao -> {
            DraftScreen(
                previa = times.previaDistribuicao,
                salvando = times.salvando,
                onVoltar = {
                    teamsViewModel.descartarDistribuicao()
                    destino = Destino.Abas
                },
                onRecalcular = teamsViewModel::calcularDistribuicao,
                onAplicar = teamsViewModel::aplicarDistribuicao,
                modifier = modifier,
            )
            return
        }

        is Destino.Aprovacoes -> {
            AprovacoesScreen(
                fila = membro.fila,
                salvando = membro.salvando,
                onVoltar = { destino = Destino.Abas },
                onDecidir = membroViewModel::decidir,
                onAbrirVinculos = { destino = Destino.Vinculos },
                modifier = modifier,
            )
            return
        }

        is Destino.Vinculos -> {
            VinculosScreen(
                jogadores = grupo.membros,
                contas = contas,
                carregandoContas = carregandoContas,
                salvando = membro.salvando,
                onVoltar = { destino = Destino.Abas },
                onCarregarContas = membroViewModel::carregarContas,
                onVincular = membroViewModel::vincularManualmente,
                onDesvincular = membroViewModel::desvincular,
                modifier = modifier,
            )
            return
        }

        is Destino.PainelFinanceiro -> {
            PainelFinanceiroScreen(
                estado = financeiro,
                onVoltar = { destino = Destino.Abas },
                onRecarregar = financeiroViewModel::carregarPainel,
                onDefinirStatus = { pagamentoId, status ->
                    perfilId?.let { financeiroViewModel.definirStatus(pagamentoId, status, it) }
                },
                onGerarMensalidade = { perfilId?.let(financeiroViewModel::gerarMensalidadeDoMes) },
                onGerarDiaria = { perfilId?.let(financeiroViewModel::gerarDiariaDeHoje) },
                onConfigurar = { configurandoFinanceiro = true },
                modifier = modifier,
            )
            if (configurandoFinanceiro) {
                ConfigFinanceiroDialog(
                    estado = financeiro,
                    onSalvar = { chave, nome, cidade, mensalidade, diaria ->
                        financeiroViewModel.salvarConfig(chave, nome, cidade, mensalidade, diaria)
                        configurandoFinanceiro = false
                    },
                    onFechar = { configurandoFinanceiro = false },
                )
            }
            return
        }

        is Destino.Avaliacao -> {
            AvaliacaoScreen(
                pendentes = evolucao.pendentes,
                salvando = evolucao.salvando,
                onVoltar = { destino = Destino.Abas },
                onEnviar = evolucaoViewModel::avaliar,
                modifier = modifier,
            )
            return
        }

        is Destino.Conteudo -> {
            PaginaScreen(
                pagina = grupo.paginas.firstOrNull { it.id == atual.pagina.id } ?: atual.pagina,
                isAdmin = estado.isAdmin,
                salvando = grupo.salvando,
                onVoltar = { destino = Destino.Abas },
                onSalvar = { titulo, corpo -> grupoViewModel.salvarPagina(atual.pagina.id, titulo, corpo) },
                modifier = modifier,
            )
            return
        }

        is Destino.Partida -> {
            PartidaScreen(
                container = container,
                matchId = atual.matchId,
                isAdmin = estado.isAdmin,
                onVoltar = { destino = Destino.Abas },
                modifier = modifier,
            )
            return
        }

        Destino.Abas -> Unit
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = VoleiColors.Fundo,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            AppHeader(
                subtitulo = subtituloDoFormato(estado.formato.times, estado.formato.quadras),
                sinal =
                    when (estado.sincronizacao) {
                        EstadoSincronizacao.ONLINE -> SinalSync.ONLINE
                        EstadoSincronizacao.CONECTANDO -> SinalSync.CONECTANDO
                        EstadoSincronizacao.OFFLINE -> SinalSync.OFFLINE
                    },
                onSair = mainViewModel::sair,
            )
        },
        bottomBar = {
            BarraDeAbas(
                abaAtual = aba,
                onSelecionar = { aba = it },
                abasComAviso =
                    if (evolucao.pendentes.isNotEmpty()) setOf(AbaPrincipal.EU) else emptySet(),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (aba) {
                AbaPrincipal.JOGOS ->
                    GamesScreen(
                        estado = jogos,
                        isAdmin = estado.isAdmin,
                        onAbrirPartida = { destino = Destino.Partida(it) },
                        onGerarChaveamento = gamesViewModel::gerarChaveamento,
                        onEncerrarDia = gamesViewModel::encerrarDia,
                    )

                AbaPrincipal.CLASSIFICACAO ->
                    StandingsScreen(
                        estado = classificacao,
                        isAdmin = estado.isAdmin,
                        onApagarResultados = standingsViewModel::apagarResultados,
                    )

                AbaPrincipal.TIMES ->
                    TeamsScreen(
                        estado = times,
                        isAdmin = estado.isAdmin,
                        onAbrirTime = { historicoDoTime = it },
                        onEditarTime = { editandoTime = it },
                        onAlternarAtivoTime = teamsViewModel::alternarAtivo,
                        onAjustarTimes = teamsViewModel::ajustarTimesAosPresentes,
                        onNovoTime = { criandoTime = true },
                        onAbrirJogadores = { destino = Destino.Jogadores },
                        onDistribuir = {
                            teamsViewModel.calcularDistribuicao()
                            destino = Destino.Distribuicao
                        },
                    )

                AbaPrincipal.SOCIAL ->
                    GrupoScreen(
                        estado = grupo,
                        isAdmin = estado.isAdmin,
                        onSecao = grupoViewModel::abrir,
                        onNovoPost = { criandoPost = true },
                        onExcluirPost = grupoViewModel::excluirPost,
                        onReagir = grupoViewModel::alternarReacao,
                        onNovoEvento = { criandoEvento = true },
                        onEditarEvento = { editandoEvento = it },
                        onExcluirEvento = grupoViewModel::excluirEvento,
                        onAbrirPagina = { destino = Destino.Conteudo(it) },
                        onResponderPor = grupoViewModel::responderPor,
                        onTrazerConfirmados = grupoViewModel::trazerConfirmados,
                    )

                AbaPrincipal.EU ->
                    EuScreen(
                        estado = membro,
                        financeiro = financeiro,
                        evolucao = evolucao,
                        onBuscar = membroViewModel::buscar,
                        onPedirVinculo = membroViewModel::pedirVinculo,
                        onCancelarPedido = membroViewModel::cancelarPedido,
                        onEditarFicha = { editandoFicha = true },
                        onAbrirAprovacoes = { destino = Destino.Aprovacoes },
                        onResponderChamada = membroViewModel::responderChamada,
                        onAbrirPainelFinanceiro = {
                            financeiroViewModel.carregarPainel()
                            destino = Destino.PainelFinanceiro
                        },
                        onAbrirAvaliacao = { destino = Destino.Avaliacao },
                    )
            }
        }
    }

    if (criandoPost) {
        PostDialog(
            salvando = grupo.salvando,
            onSalvar = { titulo, corpo, fixado, imagem, emoji ->
                grupoViewModel.publicar(titulo, corpo, fixado, membro.profile?.nome, imagem, emoji)
                criandoPost = false
            },
            onFechar = { criandoPost = false },
        )
    }

    if (criandoEvento || editandoEvento != null) {
        EventoDialog(
            evento = editandoEvento,
            salvando = grupo.salvando,
            sugestoes = sugestoesDeEndereco,
            onBuscarEndereco = grupoViewModel::buscarEnderecos,
            onLimparSugestoes = grupoViewModel::limparEnderecos,
            onSalvar = { id, titulo, descricao, tipo, inicio, local ->
                grupoViewModel.salvarEvento(id, titulo, descricao, tipo, inicio, local)
                criandoEvento = false
                editandoEvento = null
            },
            onFechar = {
                criandoEvento = false
                editandoEvento = null
            },
        )
    }

    val meuJogador = membro.meuJogador
    if (editandoFicha && meuJogador != null) {
        FichaDialog(
            jogador = meuJogador,
            contato = membro.meuContato,
            salvando = membro.salvando,
            onSalvar = { nome, dia, mes, telefone, emergencia, ano, regime ->
                membroViewModel.salvarFicha(nome, dia, mes, telefone, emergencia, ano, regime)
                editandoFicha = false
            },
            onFechar = { editandoFicha = false },
        )
    }

    historicoDoTime?.let { time ->
        HistoricoDoTime(
            container = container,
            time = time,
            isAdmin = estado.isAdmin,
            onFechar = { historicoDoTime = null },
        )
    }

    if (criandoTime || editandoTime != null) {
        TeamEditorDialog(
            time = editandoTime,
            onSalvarNovo = { nome, cor, sigla ->
                teamsViewModel.criarTime(nome, cor, sigla)
                criandoTime = false
            },
            onSalvarExistente = {
                teamsViewModel.salvarTime(it)
                editandoTime = null
            },
            onExcluir = {
                teamsViewModel.excluirTime(it)
                editandoTime = null
            },
            onFechar = {
                criandoTime = false
                editandoTime = null
            },
        )
    }
}

@Composable
private fun PartidaScreen(
    container: AppContainer,
    matchId: String,
    isAdmin: Boolean,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val factory =
        remember(matchId) {
            viewModelFactory {
                initializer { MatchScoreViewModel(container.matchesRepository, matchId) }
            }
        }
    val viewModel: MatchScoreViewModel = viewModel(key = matchId, factory = factory)
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    MatchScoreScreen(
        estado = estado,
        isAdmin = isAdmin,
        onVoltar = onVoltar,
        onSomar = viewModel::somar,
        onDefinirPlacar = viewModel::definirPlacar,
        onFinalizar = viewModel::finalizar,
        onReabrir = viewModel::reabrir,
        modifier = modifier,
    )
}

@Composable
private fun HistoricoDoTime(
    container: AppContainer,
    time: Team,
    isAdmin: Boolean,
    onFechar: () -> Unit,
) {
    val factory =
        remember(time.id) {
            viewModelFactory {
                initializer {
                    TeamHistoryViewModel(
                        matchesRepository = container.matchesRepository,
                        teamsRepository = container.teamsRepository,
                        teamId = time.id,
                    )
                }
            }
        }
    val viewModel: TeamHistoryViewModel = viewModel(key = "historico-${time.id}", factory = factory)
    val detalhe by viewModel.estado.collectAsStateWithLifecycle()

    TeamHistoryDialog(
        time = time,
        isAdmin = isAdmin,
        elenco = detalhe.elenco,
        partidas = detalhe.partidas,
        onFechar = onFechar,
    )
}

private fun subtituloDoFormato(
    times: Int,
    quadras: Int,
): String =
    when {
        times == 0 -> "Configure os times para começar"
        quadras == 0 -> "$times times - chaveamento pendente"
        else -> "Jogos de Sábado - $times Times ($quadras Quadras)"
    }

@Composable
private fun RegistroDePush(
    container: AppContainer,
    perfilId: String?,
) {
    val contexto = LocalContext.current
    var permitido by remember { mutableStateOf(Push.temPermissao(contexto)) }

    val pedirPermissao =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
            permitido = concedida
        }

    LaunchedEffect(Unit) {
        if (Push.configurado) Push.iniciar(contexto)
    }

    LaunchedEffect(perfilId, permitido) {
        if (perfilId == null || !Push.configurado) return@LaunchedEffect

        if (!permitido) {
            val permissao = Push.permissaoNecessaria()
            if (permissao != null) {
                pedirPermissao.launch(permissao)
                return@LaunchedEffect
            }
            permitido = true
        }

        val token = TokenPendente.ultimo ?: Push.token() ?: return@LaunchedEffect
        runCatching {
            container.chamadaRepository.registrarDispositivo(
                profileId = perfilId,
                token = token,
                plataforma = "android",
            )
        }
    }
}
