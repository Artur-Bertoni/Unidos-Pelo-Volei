package com.unidospelovolei.data

class GrupoStorage(
    private val api: ApiClient,
) {
    suspend fun enviarLogo(
        grupoId: String,
        bytes: ByteArray,
        extensao: String,
    ): String {
        require(bytes.isNotEmpty()) { "Não foi possível ler a imagem escolhida." }
        require(bytes.size <= LIMITE_DE_BYTES) { "A logo passa de 2 MB. Escolha uma menor." }

        val final = extensao.lowercase().ifBlank { "jpg" }
        return enviarImagemPublica(api, "grupos", grupoId, "logo-${novoId()}.$final", bytes, final)
    }

    private companion object {
        const val LIMITE_DE_BYTES = 2 * 1024 * 1024
    }
}

internal suspend fun enviarImagemPublica(
    api: ApiClient,
    bucket: String,
    pasta: String,
    nome: String,
    bytes: ByteArray,
    extensao: String,
): String {
    val tipo =
        when (extensao) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "image/jpeg"
        }
    return api
        .enviarArquivo("/arquivos/$bucket/$pasta/$nome", bytes, tipo)
        .comoObjeto()
        ?.texto("url")
        ?: throw IllegalStateException("O servidor não devolveu o endereço da imagem.")
}
