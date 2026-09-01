package com.unidospelovolei.ui.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.MatchesRepository
import com.unidospelovolei.data.StandingsRepository
import com.unidospelovolei.domain.model.Standing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StandingsUiState(
    val carregando: Boolean = true,
    val linhas: List<Standing> = emptyList(),
    val erro: String? = null,
)

class StandingsViewModel(
    standingsRepository: StandingsRepository,
    private val matchesRepository: MatchesRepository,
) : ViewModel() {
    private val erro = MutableStateFlow<String?>(null)

    val estado: StateFlow<StandingsUiState> =
        combine(standingsRepository.observeStandings(), erro) { linhas, mensagem ->
            StandingsUiState(carregando = false, linhas = linhas, erro = mensagem)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StandingsUiState())

    fun apagarResultados() {
        viewModelScope.launch {
            runCatching { matchesRepository.clearResults() }
                .onFailure { erro.value = it.message ?: "Nao foi possivel apagar os resultados." }
        }
    }

    fun limparErro() {
        erro.value = null
    }
}
