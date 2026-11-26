package com.unidospelovolei.data

class MuralStorage(
    private val api: ApiClient,
    private val grupoAtivo: GrupoAtivo,
) {
    suspend fun enviarImagem(
        bytes: ByteArray,
        extensao: String,
    ): String {
        require(bytes.isNotEmpty()) { "Não foi possível ler a imagem escolhida." }
        require(bytes.size <= LIMITE_DE_BYTES) { "A imagem passa de 5 MB. Escolha uma menor." }

        val final = extensao.lowercase().ifBlank { "jpg" }
        return enviarImagemPublica(api, "mural", grupoAtivo.exigir(), "${novoId()}.$final", bytes, final)
    }

    private companion object {
        const val LIMITE_DE_BYTES = 5 * 1024 * 1024
    }
}
