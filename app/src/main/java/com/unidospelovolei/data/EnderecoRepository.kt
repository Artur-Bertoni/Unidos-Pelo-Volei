package com.unidospelovolei.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Endereco(
    val descricao: String,
)

class EnderecoRepository {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun buscar(termo: String): List<Endereco> {
        val consulta = termo.trim()
        if (consulta.length < MINIMO_DE_LETRAS) return emptyList()

        return withContext(Dispatchers.IO) {
            val conexao = abrir(consulta)
            try {
                val corpo = conexao.inputStream.use { it.readBytes().decodeToString() }
                json
                    .parseToJsonElement(corpo)
                    .jsonObject["features"]
                    ?.jsonArray
                    .orEmpty()
                    .mapNotNull { feature -> descricaoDe(feature.jsonObject["properties"] as? JsonObject) }
                    .distinct()
                    .map(::Endereco)
            } finally {
                conexao.disconnect()
            }
        }
    }

    private fun abrir(consulta: String): HttpURLConnection {
        val endereco = "$SERVICO?q=${URLEncoder.encode(consulta, "UTF-8")}&limit=$LIMITE&lang=pt"
        return (URL(endereco).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", AGENTE)
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun descricaoDe(propriedades: JsonObject?): String? {
        if (propriedades == null) return null

        fun campo(nome: String): String? =
            (propriedades[nome] as? JsonPrimitive)?.contentOrNull?.trim()?.ifBlank { null }

        val rua = listOfNotNull(campo("street"), campo("housenumber")).joinToString(", ")
        val cidade = campo("city") ?: campo("town") ?: campo("village") ?: campo("county")
        val cidadeComEstado = listOfNotNull(cidade, campo("state")).joinToString(" - ")

        return listOfNotNull(
            campo("name"),
            rua.ifBlank { null },
            campo("district"),
            cidadeComEstado.ifBlank { null },
        ).distinct()
            .joinToString(", ")
            .ifBlank { null }
    }

    companion object {
        const val MINIMO_DE_LETRAS = 3
        private const val LIMITE = 6
        private const val SERVICO = "https://photon.komoot.io/api/"
        private const val AGENTE = "UnidosPeloVolei/1.0 (app do grupo de volei)"
    }
}
