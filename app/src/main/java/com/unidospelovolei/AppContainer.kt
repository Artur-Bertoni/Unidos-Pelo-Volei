package com.unidospelovolei

import android.content.Context
import com.powersync.DatabaseDriverFactory
import com.powersync.PowerSyncDatabase
import com.unidospelovolei.data.ApiClient
import com.unidospelovolei.data.AppSchema
import com.unidospelovolei.data.AuthRepository
import com.unidospelovolei.data.AvaliacaoRepository
import com.unidospelovolei.data.BackendConnector
import com.unidospelovolei.data.ChamadaRepository
import com.unidospelovolei.data.EnderecoRepository
import com.unidospelovolei.data.FinanceiroRepository
import com.unidospelovolei.data.GameDaysRepository
import com.unidospelovolei.data.GrupoAtivo
import com.unidospelovolei.data.GrupoRepository
import com.unidospelovolei.data.GrupoStorage
import com.unidospelovolei.data.MatchesRepository
import com.unidospelovolei.data.MembroRepository
import com.unidospelovolei.data.MeusGruposRepository
import com.unidospelovolei.data.MuralStorage
import com.unidospelovolei.data.PlayersRepository
import com.unidospelovolei.data.ProfileRepository
import com.unidospelovolei.data.StandingsRepository
import com.unidospelovolei.data.SyncService
import com.unidospelovolei.data.TeamsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val chavesFaltando: List<String> =
        buildList {
            if (BuildConfig.API_URL.isBlank()) add("API_URL")
            if (BuildConfig.POWERSYNC_URL.isBlank()) add("POWERSYNC_URL")
            if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) add("GOOGLE_WEB_CLIENT_ID")
        }

    val configurado: Boolean get() = chavesFaltando.isEmpty()

    private val api: ApiClient by lazy {
        ApiClient(BuildConfig.API_URL) { forcar -> authRepository.tokenDeAcesso(forcar) }
    }

    val database: PowerSyncDatabase by lazy {
        PowerSyncDatabase(
            factory = DatabaseDriverFactory(appContext),
            schema = AppSchema,
            dbFilename = "unidos-pelo-volei.db",
            scope = scope,
        )
    }

    private val connector by lazy {
        BackendConnector(
            api = api,
            authRepository = authRepository,
            powerSyncUrl = BuildConfig.POWERSYNC_URL,
        )
    }

    val grupoAtivo by lazy { GrupoAtivo(appContext) }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            apiProvider = { api },
            appContext = appContext,
            googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
        )
    }

    val meusGruposRepository by lazy { MeusGruposRepository(database, api, grupoAtivo) }
    val grupoStorage by lazy { GrupoStorage(api) }
    val profileRepository by lazy { ProfileRepository(database, grupoAtivo) }
    val membroRepository by lazy { MembroRepository(database, grupoAtivo) }
    val chamadaRepository by lazy { ChamadaRepository(database, grupoAtivo) }
    val grupoRepository by lazy { GrupoRepository(database, grupoAtivo) }
    val muralStorage by lazy { MuralStorage(api, grupoAtivo) }
    val enderecoRepository by lazy { EnderecoRepository() }
    val financeiroRepository by lazy { FinanceiroRepository(database, api, grupoAtivo) }
    val avaliacaoRepository by lazy { AvaliacaoRepository(database, grupoAtivo) }
    val playersRepository by lazy { PlayersRepository(database, grupoAtivo) }
    val teamsRepository by lazy { TeamsRepository(database, grupoAtivo) }
    val matchesRepository by lazy { MatchesRepository(database, grupoAtivo) }
    val standingsRepository by lazy { StandingsRepository(database, grupoAtivo) }
    val gameDaysRepository by lazy { GameDaysRepository(database, grupoAtivo) }

    val syncService by lazy {
        SyncService(
            db = database,
            connector = connector,
            authRepository = authRepository,
            grupoAtivo = grupoAtivo,
            scope = scope,
        )
    }

    fun iniciarSync() {
        if (configurado) syncService.start()
    }
}
