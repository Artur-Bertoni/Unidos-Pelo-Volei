package com.unidospelovolei.domain

import com.unidospelovolei.domain.financeiro.PixBrCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixBrCodeTest {
    private val chave = "unidos@volei.com"

    @Test
    fun `crc16 bate com o vetor padrao`() {
        assertEquals(0x29B1, PixBrCode.crc16("123456789"))
    }

    @Test
    fun `crc16 muda quando o payload muda`() {
        assertNotEquals(PixBrCode.crc16("123456789"), PixBrCode.crc16("12345678A"))
    }

    @Test
    fun `campo prefixa o tamanho com dois digitos`() {
        assertEquals("000201", PixBrCode.campo("00", "01"))
        assertEquals("0117chave@exemplo.com", PixBrCode.campo("01", "chave@exemplo.com"))
    }

    @Test
    fun `sanear tira acento pontuacao e corta no limite`() {
        assertEquals("JOSE DA SILVAJUNIOR", PixBrCode.sanear("José da Silva-Júnior", 25))
        assertEquals("SAO PAULO", PixBrCode.sanear("São Paulo", 15))
        assertEquals("CIDADE MUITO CO", PixBrCode.sanear("Cidade Muito Comprida Demais", 15))
    }

    @Test
    fun `valor formatado escreve centavos com duas casas`() {
        assertEquals("50.00", PixBrCode.valorFormatado(5000))
        assertEquals("25.50", PixBrCode.valorFormatado(2550))
        assertEquals("0.05", PixBrCode.valorFormatado(5))
    }

    @Test
    fun `abre com o indicador de formato e fecha com o crc`() {
        val codigo = PixBrCode.gerar(chave, "Unidos Pelo Volei", "Blumenau")
        assertTrue(codigo.startsWith("000201"))
        assertEquals("6304", codigo.substring(codigo.length - 8, codigo.length - 4))
        assertTrue(Regex("^[0-9A-F]{4}$").matches(codigo.takeLast(4)))
    }

    @Test
    fun `carrega o gui e a chave do pix`() {
        val codigo = PixBrCode.gerar(chave, "Unidos Pelo Volei", "Blumenau")
        assertTrue(codigo.contains("0014br.gov.bcb.pix"))
        assertTrue(codigo.contains("0116$chave"))
    }

    @Test
    fun `so inclui o valor quando ele existe`() {
        assertFalse(PixBrCode.gerar(chave, "Unidos", "Blumenau", 0).contains("5405"))
        assertTrue(PixBrCode.gerar(chave, "Unidos", "Blumenau", 5000).contains("540550.00"))
    }

    @Test
    fun `o crc declarado confere com o corpo`() {
        val codigo = PixBrCode.gerar(chave, "Unidos Pelo Volei", "Blumenau", 2550)
        val corpo = codigo.dropLast(4)
        assertEquals(codigo.takeLast(4), "%04X".format(PixBrCode.crc16(corpo)))
    }

    @Test
    fun `cai para valores seguros quando nome e cidade vem vazios`() {
        val codigo = PixBrCode.gerar(chave, "   ", "")
        assertTrue(codigo.contains("5909RECEBEDOR"))
        assertTrue(codigo.contains("6006BRASIL"))
    }

    @Test
    fun `gera o mesmo codigo que o porte em typescript`() {
        val codigo = PixBrCode.gerar(chave, "Unidos Pelo Volei", "Blumenau", 2550)
        assertEquals(
            "00020126380014br.gov.bcb.pix0116unidos@volei.com52040000530398654" +
                "0525.505802BR5917UNIDOS PELO VOLEI6008BLUMENAU62070503AAA",
            codigo.dropLast(8),
        )
    }
}
