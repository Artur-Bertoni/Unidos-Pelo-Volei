package com.unidospelovolei.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage

class GrupoStorage(
    private val supabase: SupabaseClient,
) {
    suspend fun enviarLogo(
        grupoId: String,
        bytes: ByteArray,
        extensao: String,
    ): String {
        require(bytes.isNotEmpty()) { "Não foi possível ler a imagem escolhida." }
        require(bytes.size <= LIMITE_DE_BYTES) { "A logo passa de 2 MB. Escolha uma menor." }

        val caminho = "$grupoId/logo-${novoId()}.${extensao.lowercase().ifBlank { "jpg" }}"
        val bucket = supabase.storage.from(BUCKET)
        bucket.upload(caminho, bytes) { upsert = false }
        return bucket.publicUrl(caminho)
    }

    private companion object {
        const val BUCKET = "grupos"
        const val LIMITE_DE_BYTES = 2 * 1024 * 1024
    }
}
