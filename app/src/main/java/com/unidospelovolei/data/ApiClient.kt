package com.unidospelovolei.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import java.io.IOException

class ErroDaApi(
    val status: Int,
    mensagem: String,
    val codigo: String? = null,
) : IllegalStateException(mensagem)

class ApiClient(
    baseUrl: String,
    private val tokens: suspend (forcarRenovacao: Boolean) -> String?,
) {
    private val base = baseUrl.trimEnd('/')

    private val http =
        HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }
        }

    suspend fun get(caminho: String): JsonElement = enviar(HttpMethod.Get, caminho)

    suspend fun post(
        caminho: String,
        corpo: JsonElement = JsonObject(emptyMap()),
        autenticado: Boolean = true,
    ): JsonElement = enviar(HttpMethod.Post, caminho, corpo, autenticado)

    suspend fun patch(
        caminho: String,
        corpo: JsonElement,
    ): JsonElement = enviar(HttpMethod.Patch, caminho, corpo)

    suspend fun delete(caminho: String): JsonElement = enviar(HttpMethod.Delete, caminho)

    suspend fun enviarArquivo(
        caminho: String,
        bytes: ByteArray,
        tipo: String,
    ): JsonElement =
        executar(autenticado = true) { token ->
            http.request(base + caminho) {
                method = HttpMethod.Put
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                contentType(ContentType.parse(tipo))
                setBody(bytes)
            }
        }

    private suspend fun enviar(
        metodo: HttpMethod,
        caminho: String,
        corpo: JsonElement? = null,
        autenticado: Boolean = true,
    ): JsonElement =
        executar(autenticado) { token ->
            http.request(base + caminho) {
                method = metodo
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                if (corpo != null) {
                    contentType(ContentType.Application.Json)
                    setBody(corpo.toString())
                }
            }
        }

    private suspend fun executar(
        autenticado: Boolean,
        chamada: suspend (token: String?) -> HttpResponse,
    ): JsonElement {
        val primeira = chamarComRede { chamada(if (autenticado) tokens(false) else null) }
        val resposta =
            if (autenticado && primeira.status.value == 401) {
                chamarComRede { chamada(tokens(true)) }
            } else {
                primeira
            }
        val texto = resposta.bodyAsText()
        val json = if (texto.isBlank()) JsonNull else runCatching { Json.parseToJsonElement(texto) }.getOrDefault(JsonNull)

        if (!resposta.status.isSuccess()) {
            val objeto = json as? JsonObject
            throw ErroDaApi(
                status = resposta.status.value,
                mensagem = objeto?.texto("erro") ?: "O servidor respondeu ${resposta.status.value}.",
                codigo = objeto?.texto("codigo"),
            )
        }
        return json
    }

    private suspend fun chamarComRede(chamada: suspend () -> HttpResponse): HttpResponse =
        try {
            chamada()
        } catch (e: IOException) {
            throw IOException("Sem conexão com o servidor. Precisa de internet.", e)
        }
}

internal fun JsonElement.comoLista(): List<JsonObject> = (this as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

internal fun JsonElement.comoObjeto(): JsonObject? = this as? JsonObject
