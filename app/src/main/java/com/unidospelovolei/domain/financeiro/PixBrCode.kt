package com.unidospelovolei.domain.financeiro

import java.text.Normalizer

enum class TipoDaChavePix(
    val value: String,
    val rotulo: String,
    val dica: String,
) {
    CELULAR(
        "celular",
        "Celular",
        "Digite como quiser: o código do país entra sozinho. Precisa de DDD mais o número.",
    ),
    CPF("cpf", "CPF", "Onze dígitos. Ponto e traço podem ficar, saem sozinhos."),
    CNPJ("cnpj", "CNPJ", "Quatorze dígitos. Ponto, barra e traço podem ficar, saem sozinhos."),
    EMAIL("email", "E-mail", "O e-mail que você registrou como chave no banco."),
    ALEATORIA(
        "aleatoria",
        "Aleatória",
        "A chave aleatória tem 36 caracteres com hífens. Copie do app do banco.",
    ),
    ;

    companion object {
        fun from(value: String?): TipoDaChavePix = entries.firstOrNull { it.value == value } ?: ALEATORIA
    }
}

object PixBrCode {
    const val GUI: String = "br.gov.bcb.pix"

    private val UUID = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

    private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun normalizarChave(
        chave: String,
        tipo: TipoDaChavePix,
    ): String {
        val limpa = chave.trim()
        return when (tipo) {
            TipoDaChavePix.EMAIL, TipoDaChavePix.ALEATORIA -> limpa.lowercase()
            TipoDaChavePix.CPF, TipoDaChavePix.CNPJ -> limpa.filter(Char::isDigit)
            TipoDaChavePix.CELULAR -> {
                val digitos = limpa.filter(Char::isDigit)
                val semPais = if (digitos.startsWith("55") && digitos.length > 11) digitos.drop(2) else digitos
                if (semPais.isEmpty()) "" else "+55$semPais"
            }
        }
    }

    fun chaveValida(
        chave: String,
        tipo: TipoDaChavePix,
    ): Boolean {
        val pronta = normalizarChave(chave, tipo)
        if (pronta.isEmpty()) return false
        return when (tipo) {
            TipoDaChavePix.CPF -> pronta.length == 11
            TipoDaChavePix.CNPJ -> pronta.length == 14
            TipoDaChavePix.CELULAR -> pronta.length == 13 || pronta.length == 14
            TipoDaChavePix.EMAIL -> EMAIL.matches(pronta) && pronta.length <= 77
            TipoDaChavePix.ALEATORIA -> UUID.matches(pronta)
        }
    }

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
        tipo: TipoDaChavePix,
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
                append(campo("26", campo("00", GUI) + campo("01", normalizarChave(chave, tipo))))
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
