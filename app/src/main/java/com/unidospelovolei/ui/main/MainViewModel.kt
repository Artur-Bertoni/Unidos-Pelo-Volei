package com.unidospelovolei.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unidospelovolei.data.AuthRepository
import com.unidospelovolei.data.Formato
import com.unidospelovolei.data.MatchesRepository
import com.unidospelovolei.data.ProfileRepository
import com.unidospelovolei.data.SyncService
import com.unidospelovolei.domain.model.UserProfile
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

enum class EstadoSincronizacao {
    ONLINE,
    CONECTANDO,
    OFFLINE,
}

data class MainUiState(
    val carregandoSessao: Boolean = true,
    val logado: Boolean = false,
    val profile: UserProfile? = null,
    val sincronizacao: EstadoSincronizacao = EstadoSincronizacao.OFFLINE,
    val formato: Formato = Formato(times = 0, quadras = 0),
    val entrando: Boolean = false,
    val erro: String? = null,
) {
    val isAdmin: Boolean get() = profile?.isAdmin == true
}

/**
 * Estado global da aplicacao: sessao, papel do usuario e situacao do sync.
 * As telas de conteudo tem os proprios ViewModels.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    matchesRepository: MatchesRepository,
    syncService: SyncService,
) : ViewModel() {
    private val entrando = MutableStateFlow(false)
    private val erro = MutableStateFlow<String?>(null)

    private val sessao = authRepository.sessionStatus

    private val profile =
        sessao.flatMapLatest { status ->
            when (status) {
                is SessionStatus.Authenticated ->
                    profileRepository
                        .observeProfile(status.session.user?.id.orEmpty())
                        .catch { emit(null) }

                else -> flowOf(null)
            }
        }

    private val sincronizacao =
        syncService.status
            .map { dados ->
                when {
                    dados.connected -> EstadoSincronizacao.ONLINE
                    dados.connecting -> EstadoSincronizacao.CONECTANDO
                    else -> EstadoSincronizacao.OFFLINE
                }
            }.catch { emit(EstadoSincronizacao.OFFLINE) }

    private val formato = matchesRepository.observeFormato().catch { emit(Formato(0, 0)) }

    val estado: StateFlow<MainUiState> =
        combine(
            sessao,
            profile,
            sincronizacao,
            formato,
            combine(entrando, erro) { carregando, mensagem -> carregando to mensagem },
        ) { statusSessao, perfil, sync, formatoAtual, (carregando, mensagem) ->
            MainUiState(
                carregandoSessao = statusSessao is SessionStatus.Initializing,
                logado = statusSessao is SessionStatus.Authenticated,
                profile = perfil,
                sincronizacao = sync,
                formato = formatoAtual,
                entrando = carregando,
                erro = mensagem,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    val erroVisivel: StateFlow<String?> = erro.asStateFlow()

    fun entrarComGoogle(activityContext: Context) {
        if (entrando.value) return
        viewModelScope.launch {
            entrando.value = true
            erro.value = null
            runCatching { authRepository.signInWithGoogle(activityContext) }
                .onFailure { erro.value = it.message ?: "Nao foi possivel entrar com o Google." }
            entrando.value = false
        }
    }

    fun sair() {
        viewModelScope.launch {
            runCatching { authRepository.signOut() }
                .onFailure { erro.value = it.message }
        }
    }

    fun limparErro() {
        erro.value = null
    }
}
