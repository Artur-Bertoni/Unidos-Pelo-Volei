package com.unidospelovolei.ui

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
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.ui.components.AbaPrincipal
import com.unidospelovolei.ui.components.AppHeader
import com.unidospelovolei.ui.components.BarraDeAbas
import com.unidospelovolei.ui.components.SinalSync
import com.unidospelovolei.ui.games.GamesScreen
import com.unidospelovolei.ui.games.GamesViewModel
import com.unidospelovolei.ui.games.MatchScoreScreen
import com.unidospelovolei.ui.games.MatchScoreViewModel
import com.unidospelovolei.ui.login.ConfiguracaoPendenteScreen
import com.unidospelovolei.ui.login.LoginScreen
import com.unidospelovolei.ui.main.EstadoSincronizacao
import com.unidospelovolei.ui.main.MainViewModel
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

private sealed interface Destino {
    data object Abas : Destino

    data object Jogadores : Destino

    data object Distribuicao : Destino

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

        else ->
            HomeScreen(
                container = container,
                mainViewModel = mainViewModel,
                modifier = modifier,
            )
    }
}

@Composable
private fun HomeScreen(
    container: AppContainer,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val factory = rememberVoleiViewModelFactory(container)
    val estado by mainViewModel.estado.collectAsStateWithLifecycle()

    val gamesViewModel: GamesViewModel = viewModel(factory = factory)
    val standingsViewModel: StandingsViewModel = viewModel(factory = factory)
    val teamsViewModel: TeamsViewModel = viewModel(factory = factory)
    val playersViewModel: PlayersViewModel = viewModel(factory = factory)

    val jogos by gamesViewModel.estado.collectAsStateWithLifecycle()
    val classificacao by standingsViewModel.estado.collectAsStateWithLifecycle()
    val times by teamsViewModel.estado.collectAsStateWithLifecycle()
    val jogadores by playersViewModel.estado.collectAsStateWithLifecycle()

    var aba by rememberSaveable { mutableStateOf(AbaPrincipal.JOGOS) }
    var destino by remember { mutableStateOf<Destino>(Destino.Abas) }
    var historicoDoTime by remember { mutableStateOf<Team?>(null) }
    var editandoTime by remember { mutableStateOf<Team?>(null) }
    var criandoTime by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val mensagemDeErro =
        estado.erro ?: jogos.erro ?: classificacao.erro ?: times.erro ?: jogadores.erro

    LaunchedEffect(mensagemDeErro) {
        mensagemDeErro?.let {
            snackbar.showSnackbar(it)
            mainViewModel.limparErro()
            gamesViewModel.limparErro()
            standingsViewModel.limparErro()
            teamsViewModel.limparErro()
            playersViewModel.limparErro()
        }
    }

    LaunchedEffect(jogos.aviso) {
        jogos.aviso?.let {
            snackbar.showSnackbar(it)
            gamesViewModel.limparAviso()
        }
    }

    when (val atual = destino) {
        is Destino.Jogadores -> {
            PlayersScreen(
                estado = jogadores,
                onVoltar = { destino = Destino.Abas },
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
        bottomBar = { BarraDeAbas(abaAtual = aba, onSelecionar = { aba = it }) },
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
                        onNovoTime = { criandoTime = true },
                        onAbrirJogadores = { destino = Destino.Jogadores },
                        onDistribuir = {
                            teamsViewModel.calcularDistribuicao()
                            destino = Destino.Distribuicao
                        },
                    )
            }
        }
    }

    historicoDoTime?.let { time ->
        HistoricoDoTime(
            container = container,
            time = time,
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
