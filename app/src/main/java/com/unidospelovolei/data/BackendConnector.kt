package com.unidospelovolei.data

import android.util.Log
import com.powersync.PowerSyncDatabase
import com.powersync.connectors.PowerSyncBackendConnector
import com.powersync.connectors.PowerSyncCredentials
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BackendConnector(
    private val api: ApiClient,
    private val authRepository: AuthRepository,
    private val powerSyncUrl: String,
) : PowerSyncBackendConnector() {
    override suspend fun fetchCredentials(): PowerSyncCredentials? {
        val token = authRepository.tokenDeAcesso() ?: return null
        return PowerSyncCredentials(endpoint = powerSyncUrl, token = token)
    }

    override suspend fun uploadData(database: PowerSyncDatabase) {
        val transacao = database.getNextCrudTransaction() ?: return

        val operacoes =
            buildJsonArray {
                transacao.crud.forEach { entrada ->
                    add(
                        buildJsonObject {
                            put("op", entrada.op.name)
                            put("tabela", entrada.table)
                            put("id", entrada.id)
                            entrada.opData?.let { put("dados", it.jsonValues) }
                        },
                    )
                }
            }

        try {
            val resposta = api.post("/sync/upload", buildJsonObject { put("operacoes", operacoes) })
            val descartadas = resposta.comoObjeto()?.get("descartadas") as? JsonArray
            if (!descartadas.isNullOrEmpty()) Log.w(TAG, "O servidor recusou alterações: $descartadas")
        } catch (e: ErroDaApi) {
            if (e.status != 400) throw e
            Log.e(TAG, "Descartando transação que o servidor recusou: ${e.message}")
        }
        transacao.complete(null)
    }

    private companion object {
        const val TAG = "BackendConnector"
    }
}
