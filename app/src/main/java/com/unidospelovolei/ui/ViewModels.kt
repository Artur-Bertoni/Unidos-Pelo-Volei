package com.unidospelovolei.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.unidospelovolei.AppContainer
import com.unidospelovolei.ui.evolucao.EvolucaoViewModel
import com.unidospelovolei.ui.financeiro.FinanceiroViewModel
import com.unidospelovolei.ui.games.GamesViewModel
import com.unidospelovolei.ui.grupo.GrupoViewModel
import com.unidospelovolei.ui.main.MainViewModel
import com.unidospelovolei.ui.membro.MembroViewModel
import com.unidospelovolei.ui.players.PlayersViewModel
import com.unidospelovolei.ui.standings.StandingsViewModel
import com.unidospelovolei.ui.teams.TeamsViewModel

@Composable
fun rememberVoleiViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    remember(container) {
        viewModelFactory {
            initializer {
                MainViewModel(
                    authRepository = container.authRepository,
                    profileRepository = container.profileRepository,
                    matchesRepository = container.matchesRepository,
                    syncService = container.syncService,
                )
            }
            initializer {
                GamesViewModel(
                    matchesRepository = container.matchesRepository,
                    teamsRepository = container.teamsRepository,
                    gameDaysRepository = container.gameDaysRepository,
                    playersRepository = container.playersRepository,
                )
            }
            initializer {
                StandingsViewModel(
                    standingsRepository = container.standingsRepository,
                    matchesRepository = container.matchesRepository,
                )
            }
            initializer {
                TeamsViewModel(
                    teamsRepository = container.teamsRepository,
                    playersRepository = container.playersRepository,
                    gameDaysRepository = container.gameDaysRepository,
                )
            }
            initializer {
                PlayersViewModel(
                    playersRepository = container.playersRepository,
                    gameDaysRepository = container.gameDaysRepository,
                )
            }
            initializer {
                MembroViewModel(
                    authRepository = container.authRepository,
                    profileRepository = container.profileRepository,
                    playersRepository = container.playersRepository,
                    gameDaysRepository = container.gameDaysRepository,
                    membroRepository = container.membroRepository,
                    chamadaRepository = container.chamadaRepository,
                )
            }
            initializer {
                GrupoViewModel(
                    grupoRepository = container.grupoRepository,
                    chamadaRepository = container.chamadaRepository,
                    playersRepository = container.playersRepository,
                )
            }
            initializer {
                FinanceiroViewModel(
                    financeiroRepository = container.financeiroRepository,
                    playersRepository = container.playersRepository,
                )
            }
            initializer {
                EvolucaoViewModel(avaliacaoRepository = container.avaliacaoRepository)
            }
        }
    }
