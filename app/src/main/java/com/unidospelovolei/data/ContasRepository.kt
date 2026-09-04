package com.unidospelovolei.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContaDoGrupo(
    val id: String,
    val nome: String? = null,
    val email: String? = null,
    @SerialName("is_admin")
    val isAdmin: Boolean = false,
) {
    val rotulo: String get() = nome?.takeIf { it.isNotBlank() } ?: email ?: "Conta sem nome"
}

class ContasRepository(
    private val supabase: SupabaseClient,
) {
    suspend fun listar(): List<ContaDoGrupo> =
        supabase
            .from("profiles")
            .select()
            .decodeList<ContaDoGrupo>()
            .sortedBy { it.rotulo.lowercase() }
}
