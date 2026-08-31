package com.unidospelovolei.ui.teams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.MatchesRepository
import com.unidospelovolei.domain.model.MatchCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Agenda de jogos de um time: todas as partidas dele, com fase, rodada e quadra. */
class TeamHistoryViewModel(
    matchesRepository: MatchesRepository,
    teamId: String,
) : ViewModel() {
    val partidas: StateFlow<List<MatchCard>> =
        matchesRepository
            .observeTeamHistory(teamId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
