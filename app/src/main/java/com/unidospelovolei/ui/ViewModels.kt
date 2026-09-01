package com.unidospelovolei.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.unidospelovolei.AppContainer
import com.unidospelovolei.ui.games.GamesViewModel
import com.unidospelovolei.ui.main.MainViewModel
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
        }
    }
