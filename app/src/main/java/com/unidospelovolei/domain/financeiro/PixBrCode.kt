package com.unidospelovolei.domain.financeiro

import java.text.Normalizer

object PixBrCode {
    const val GUI: String = "br.gov.bcb.pix"

    fun crc16(dados: String): Int {
        var crc = 0xFFFF
        dados.toByteArray(Charsets.UTF_8).forEach { byte ->
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc =
                    if (crc and 0x8000 != 0) {
                        ((crc shl 1) xor 0x1021) and 0xFFFF
                    } else {
                        (crc shl 1) and 0xFFFF
                    }
            }
        }
        return crc and 0xFFFF
    }

    fun campo(
        id: String,
        valor: String,
    ): String = id + "%02d".format(valor.length) + valor

    fun sanear(
        texto: String,
        limite: Int,
    ): String =
        Normalizer
            .normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^A-Za-z0-9 ]"), "")
            .trim()
            .uppercase()
            .take(limite)

    fun valorFormatado(centavos: Int): String = "%d.%02d".format(centavos / 100, centavos % 100)

    fun gerar(
        chave: String,
        nome: String,
        cidade: String,
        valorCentavos: Int = 0,
        identificador: String = "***",
    ): String {
        val nomeLimpo = sanear(nome, LIMITE_NOME).ifBlank { "RECEBEDOR" }
        val cidadeLimpa = sanear(cidade, LIMITE_CIDADE).ifBlank { "BRASIL" }
        val txid = sanear(identificador.replace("*", "A"), LIMITE_TXID).ifBlank { "***" }

        val corpo =
            buildString {
                append(campo("00", "01"))
                append(campo("26", campo("00", GUI) + campo("01", chave.trim())))
                append(campo("52", "0000"))
                append(campo("53", "986"))
                if (valorCentavos > 0) append(campo("54", valorFormatado(valorCentavos)))
                append(campo("58", "BR"))
                append(campo("59", nomeLimpo))
                append(campo("60", cidadeLimpa))
                append(campo("62", campo("05", txid)))
            }

        val comMarcador = corpo + "6304"
        return comMarcador + "%04X".format(crc16(comMarcador))
    }

    private const val LIMITE_NOME = 25
    private const val LIMITE_CIDADE = 15
    private const val LIMITE_TXID = 25
}
