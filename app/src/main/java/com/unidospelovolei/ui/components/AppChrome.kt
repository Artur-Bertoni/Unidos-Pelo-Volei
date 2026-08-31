package com.unidospelovolei.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.ui.theme.VoleiColors

/** Abas da navegação inferior. */
enum class AbaPrincipal(
    val rotulo: String,
    val icone: ImageVector,
) {
    JOGOS("Jogos", Icons.Filled.SportsVolleyball),
    CLASSIFICACAO("Classificação", Icons.Filled.EmojiEvents),
    TIMES("Times", Icons.Filled.Groups),
}

/** Situação do sync mostrada no canto do cabeçalho. */
enum class SinalSync(
    val rotulo: String,
) {
    ONLINE("Online"),
    CONECTANDO("Conectando"),
    OFFLINE("Offline"),
}

@Composable
fun AppHeader(
    subtitulo: String,
    sinal: SinalSync,
    onSair: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(VoleiColors.FundoCabecalho)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LogoUpv()

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "UNIDOS PELO VÔLEI",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                )
                Text(
                    text = subtitulo,
                    color = VoleiColors.TextoSecundario,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }

            SinalDeSync(sinal)

            if (onSair != null) {
                IconButton(onClick = onSair, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sair",
                        tint = VoleiColors.TextoTerciario,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        HorizontalDivider(color = VoleiColors.Borda, thickness = 1.dp)
    }
}

@Composable
private fun LogoUpv(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(VoleiColors.Fundo)
                .border(2.dp, VoleiColors.Dourado, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "UPV",
            color = VoleiColors.Dourado,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun SinalDeSync(
    sinal: SinalSync,
    modifier: Modifier = Modifier,
) {
    val cor =
        when (sinal) {
            SinalSync.ONLINE -> VoleiColors.VerdeClaro
            SinalSync.CONECTANDO -> VoleiColors.Dourado
            SinalSync.OFFLINE -> VoleiColors.TextoTerciario
        }
    val icone =
        when (sinal) {
            SinalSync.ONLINE -> Icons.Filled.CloudDone
            SinalSync.CONECTANDO -> Icons.Filled.CloudSync
            SinalSync.OFFLINE -> Icons.Filled.CloudOff
        }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(15.dp))
        Text(sinal.rotulo, color = cor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BarraDeAbas(
    abaAtual: AbaPrincipal,
    onSelecionar: (AbaPrincipal) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        HorizontalDivider(color = VoleiColors.Borda, thickness = 1.dp)
        NavigationBar(
            containerColor = VoleiColors.FundoCabecalho,
            modifier = Modifier.height(72.dp),
        ) {
            AbaPrincipal.entries.forEach { aba ->
                NavigationBarItem(
                    selected = aba == abaAtual,
                    onClick = { onSelecionar(aba) },
                    icon = { Icon(aba.icone, contentDescription = null, modifier = Modifier.size(22.dp)) },
                    label = {
                        Text(
                            aba.rotulo.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    },
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = VoleiColors.Azul,
                            selectedTextColor = VoleiColors.Azul,
                            unselectedIconColor = VoleiColors.TextoTerciario,
                            unselectedTextColor = VoleiColors.TextoTerciario,
                            indicatorColor = Color.Transparent,
                        ),
                )
            }
        }
    }
}
