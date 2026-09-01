package com.unidospelovolei.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.R
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.ui.theme.VoleiColors
import com.unidospelovolei.ui.theme.corDoTime

@Composable
fun LogoUpv(
    modifier: Modifier = Modifier,
    tamanho: Dp = 40.dp,
) {
    Image(
        painter = painterResource(R.drawable.logo_upv),
        contentDescription = null,
        modifier = modifier.size(tamanho),
    )
}

@Composable
fun TeamCircle(
    sigla: String,
    corHex: String,
    modifier: Modifier = Modifier,
    tamanho: Dp = 52.dp,
) {
    val cor = corDoTime(corHex)
    Box(
        modifier =
            modifier
                .size(tamanho)
                .clip(CircleShape)
                .background(cor)
                .border(width = 2.dp, color = Color.White.copy(alpha = 0.18f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = sigla.uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (tamanho.value * 0.36f).sp,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
fun TeamBadge(
    team: Team,
    modifier: Modifier = Modifier,
    tamanho: Dp = 52.dp,
) {
    Column(
        modifier = modifier.width(tamanho + 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TeamCircle(sigla = team.sigla, corHex = team.corHex, tamanho = tamanho)
        Text(
            text = team.nome.uppercase(),
            color = VoleiColors.TextoSecundario,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun ScoreBox(
    valor: Int?,
    modifier: Modifier = Modifier,
    destacado: Boolean = false,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(VoleiColors.Fundo)
                .border(
                    width = 1.dp,
                    color = if (destacado) VoleiColors.Verde else VoleiColors.Borda,
                    shape = RoundedCornerShape(10.dp),
                ).padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = valor?.toString() ?: "-",
            color = if (valor == null) VoleiColors.TextoTerciario else VoleiColors.TextoPrimario,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun Selo(
    texto: String,
    corTexto: Color,
    corFundo: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = texto.uppercase(),
        color = corTexto,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier =
            modifier
                .clip(RoundedCornerShape(6.dp))
                .background(corFundo)
                .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
fun SeloFase(
    fase: Int,
    modifier: Modifier = Modifier,
) = Selo("Fase $fase", VoleiColors.SeloFaseTexto, VoleiColors.SeloFaseFundo, modifier)

@Composable
fun SeloResultado(
    venceu: Boolean,
    modifier: Modifier = Modifier,
) = if (venceu) {
    Selo("Vitoria", VoleiColors.VerdeClaro, VoleiColors.SeloVitoriaFundo, modifier)
} else {
    Selo("Derrota", VoleiColors.Vermelho, VoleiColors.SeloDerrotaFundo, modifier)
}

@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    cor: Color = VoleiColors.Cartao,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(14.dp))
                .background(cor)
                .border(1.dp, VoleiColors.Borda, RoundedCornerShape(14.dp)),
    ) {
        content()
    }
}

@Composable
fun PontoDoTime(
    corHex: String,
    modifier: Modifier = Modifier,
    tamanho: Dp = 10.dp,
) {
    Box(
        modifier =
            modifier
                .size(tamanho)
                .clip(CircleShape)
                .background(corDoTime(corHex)),
    )
}

@Composable
fun RotuloPequeno(
    texto: String,
    modifier: Modifier = Modifier,
    cor: Color = VoleiColors.TextoTerciario,
) {
    Text(
        text = texto.uppercase(),
        color = cor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = modifier,
    )
}

@Composable
fun EstadoVazio(
    titulo: String,
    descricao: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(titulo, color = VoleiColors.TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            descricao,
            color = VoleiColors.TextoSecundario,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}
