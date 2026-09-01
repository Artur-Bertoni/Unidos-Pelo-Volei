package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.powersync.connector.supabase.SupabaseConnector
import com.powersync.sync.SyncStatusData
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncService(
    private val db: PowerSyncDatabase,
    private val connector: SupabaseConnector,
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope,
) {
    private val trava = Mutex()
    private var conectado = false

    val status: Flow<SyncStatusData> get() = db.currentStatus.asFlow()

    fun start() {
        scope.launch {
            authRepository.sessionStatus.collect { sessao ->
                when (sessao) {
                    is SessionStatus.Authenticated -> conectar()
                    is SessionStatus.NotAuthenticated -> desconectar(limpar = sessao.isSignOut)
                    else -> Unit
                }
            }
        }
    }

    private suspend fun conectar() {
        trava.withLock {
            if (conectado) return
            runCatching { db.connect(connector) }
                .onSuccess { conectado = true }
        }
    }

    private suspend fun desconectar(limpar: Boolean) {
        trava.withLock {
            if (!conectado && !limpar) return
            runCatching {
                if (limpar) db.disconnectAndClear() else db.disconnect()
            }
            conectado = false
        }
    }
}
