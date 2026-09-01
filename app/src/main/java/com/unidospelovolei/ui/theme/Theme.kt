package com.unidospelovolei.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object VoleiColors {
    val Fundo = Color(0xFF0B0F17)
    val FundoCabecalho = Color(0xFF131B2B)
    val Cartao = Color(0xFF161B24)
    val CartaoInterno = Color(0xFF11161E)
    val Borda = Color(0xFF262D3A)

    val TextoPrimario = Color(0xFFE6EDF3)
    val TextoSecundario = Color(0xFF8B98A9)
    val TextoTerciario = Color(0xFF5C6675)

    val Verde = Color(0xFF16A34A)
    val VerdeClaro = Color(0xFF4ADE80)
    val Azul = Color(0xFF4A9EFF)
    val Vermelho = Color(0xFFF87171)
    val Dourado = Color(0xFFF5C518)

    val SeloFaseFundo = Color(0xFF1B2536)
    val SeloFaseTexto = Color(0xFF7FA8D9)
    val SeloVitoriaFundo = Color(0xFF12301F)
    val SeloDerrotaFundo = Color(0xFF33161A)
}

private val EsquemaEscuro =
    darkColorScheme(
        primary = VoleiColors.Verde,
        onPrimary = Color.White,
        secondary = VoleiColors.Azul,
        background = VoleiColors.Fundo,
        onBackground = VoleiColors.TextoPrimario,
        surface = VoleiColors.Cartao,
        onSurface = VoleiColors.TextoPrimario,
        surfaceVariant = VoleiColors.CartaoInterno,
        onSurfaceVariant = VoleiColors.TextoSecundario,
        outline = VoleiColors.Borda,
        error = VoleiColors.Vermelho,
    )

private val TipografiaVolei =
    Typography().run {
        copy(
            titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
            labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        )
    }

@Composable
fun VoleiTheme(
    escuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = EsquemaEscuro,
        typography = TipografiaVolei,
        content = content,
    )
}

fun corDoTime(hex: String): Color =
    runCatching {
        val limpo = hex.trim().removePrefix("#")
        val valor = limpo.toLong(16)
        if (limpo.length == 6) Color(valor or 0xFF000000L) else Color(valor)
    }.getOrElse { Color(0xFF6B7280) }
