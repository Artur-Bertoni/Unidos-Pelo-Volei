package com.unidospelovolei

import android.content.Context
import com.powersync.DatabaseDriverFactory
import com.powersync.PowerSyncDatabase
import com.powersync.connector.supabase.SupabaseConnector
import com.unidospelovolei.data.AppSchema
import com.unidospelovolei.data.AuthRepository
import com.unidospelovolei.data.GameDaysRepository
import com.unidospelovolei.data.MatchesRepository
import com.unidospelovolei.data.PlayersRepository
import com.unidospelovolei.data.ProfileRepository
import com.unidospelovolei.data.StandingsRepository
import com.unidospelovolei.data.SyncService
import com.unidospelovolei.data.TeamsRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
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
            if (BuildConfig.SUPABASE_URL.isBlank()) add("SUPABASE_URL")
            if (BuildConfig.SUPABASE_ANON_KEY.isBlank()) add("SUPABASE_ANON_KEY")
            if (BuildConfig.POWERSYNC_URL.isBlank()) add("POWERSYNC_URL")
            if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) add("GOOGLE_WEB_CLIENT_ID")
        }

    val configurado: Boolean get() = chavesFaltando.isEmpty()

    private val supabase by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
        }
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
        SupabaseConnector(
            supabaseClient = supabase,
            powerSyncEndpoint = BuildConfig.POWERSYNC_URL,
        )
    }

    val authRepository by lazy {
        AuthRepository(
            supabase = supabase,
            appContext = appContext,
            googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
        )
    }

    val profileRepository by lazy { ProfileRepository(database) }
    val playersRepository by lazy { PlayersRepository(database) }
    val teamsRepository by lazy { TeamsRepository(database) }
    val matchesRepository by lazy { MatchesRepository(database) }
    val standingsRepository by lazy { StandingsRepository(database) }
    val gameDaysRepository by lazy { GameDaysRepository(database) }

    val syncService by lazy {
        SyncService(
            db = database,
            connector = connector,
            authRepository = authRepository,
            scope = scope,
        )
    }

    fun iniciarSync() {
        if (configurado) syncService.start()
    }
}
