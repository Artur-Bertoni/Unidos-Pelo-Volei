package com.unidospelovolei.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage

class MuralStorage(
    private val supabase: SupabaseClient,
) {
    suspend fun enviarImagem(
        bytes: ByteArray,
        extensao: String,
    ): String {
        require(bytes.isNotEmpty()) { "Não foi possível ler a imagem escolhida." }
        require(bytes.size <= LIMITE_DE_BYTES) { "A imagem passa de 5 MB. Escolha uma menor." }

        val caminho = "${novoId()}.${extensao.lowercase().ifBlank { "jpg" }}"
        val bucket = supabase.storage.from(BUCKET)
        bucket.upload(caminho, bytes) { upsert = false }
        return bucket.publicUrl(caminho)
    }

    private companion object {
        const val BUCKET = "mural"
        const val LIMITE_DE_BYTES = 5 * 1024 * 1024
    }
}
