package com.unidospelovolei.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun JsonObject.texto(campo: String): String? =
    runCatching { this[campo]?.jsonPrimitive?.contentOrNull }.getOrNull()

internal fun JsonObject.textoObrigatorio(campo: String): String =
    texto(campo) ?: throw IllegalStateException("O servidor devolveu $campo vazio.")

internal fun JsonObject.inteiro(
    campo: String,
    padrao: Int = 0,
): Int = runCatching { this[campo]?.jsonPrimitive?.intOrNull }.getOrNull() ?: padrao

internal fun JsonObject.inteiroOuNulo(campo: String): Int? =
    runCatching { this[campo]?.jsonPrimitive?.intOrNull }.getOrNull()

internal fun JsonObject.booleano(
    campo: String,
    padrao: Boolean = false,
): Boolean = runCatching { this[campo]?.jsonPrimitive?.booleanOrNull }.getOrNull() ?: padrao
