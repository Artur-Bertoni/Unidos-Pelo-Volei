package com.unidospelovolei.ui.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.MatchesRepository
import com.unidospelovolei.data.TeamsRepository
import com.unidospelovolei.domain.model.MatchCard
import com.unidospelovolei.domain.model.Player
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TeamDetalheUiState(
    val elenco: List<Player> = emptyList(),
    val partidas: List<MatchCard> = emptyList(),
)

class TeamHistoryViewModel(
    matchesRepository: MatchesRepository,
    teamsRepository: TeamsRepository,
    teamId: String,
) : ViewModel() {
    val estado: StateFlow<TeamDetalheUiState> =
        combine(
            teamsRepository.observeRoster(teamId),
            matchesRepository.observeTeamHistory(teamId),
        ) { elenco, partidas ->
            TeamDetalheUiState(elenco = elenco, partidas = partidas)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TeamDetalheUiState())
}
