package com.unidospelovolei.data

import com.powersync.PowerSyncDatabase
import com.powersync.sync.SyncStatusData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncService(
    private val db: PowerSyncDatabase,
    private val connector: BackendConnector,
    private val authRepository: AuthRepository,
    private val grupoAtivo: GrupoAtivo,
    private val scope: CoroutineScope,
) {
    private val trava = Mutex()
    private var conectado = false

    val status: Flow<SyncStatusData> get() = db.currentStatus.asFlow()

    fun start() {
        scope.launch {
            var anterior: EstadoDaSessao? = null
            authRepository.estado.collect { sessao ->
                when (sessao) {
                    is EstadoDaSessao.Autenticado -> {
                        grupoAtivo.paraUsuario(sessao.usuarioId)
                        if (anterior is EstadoDaSessao.Desconectado) desconectar(limpar = true)
                        conectar()
                    }

                    is EstadoDaSessao.Desconectado -> {
                        grupoAtivo.paraUsuario(null)
                        desconectar(limpar = sessao.saiu)
                    }

                    EstadoDaSessao.Carregando -> Unit
                }
                anterior = sessao
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
