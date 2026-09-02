package com.unidospelovolei.ui.membro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.StatusPresenca
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.RotuloPequeno
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun CartaoDaChamada(
    dataDoSabado: String,
    jogoHora: String?,
    jogoLocal: String?,
    minhaResposta: StatusPresenca?,
    salvando: Boolean,
    onResponder: (StatusPresenca) -> Unit,
    modifier: Modifier = Modifier,
) {
    val partes = dataDoSabado.take(10).split("-")
    val diaEMes = if (partes.size == 3) "${partes[2]}/${partes[1]}" else dataDoSabado

    Cartao(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RotuloPequeno("Sábado $diaEMes")

            Text(
                text = if (minhaResposta == null) "Você vai jogar?" else "Você respondeu: ${minhaResposta.rotulo}",
                color = VoleiColors.TextoPrimario,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            listOfNotNull(jogoHora?.let { "às $it" }, jogoLocal)
                .joinToString(" · ")
                .takeIf { it.isNotBlank() }
                ?.let { Text(it, color = VoleiColors.TextoSecundario, fontSize = 12.sp) }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPresenca.entries.forEach { opcao ->
                    BotaoDeResposta(
                        rotulo = opcao.rotulo,
                        selecionado = minhaResposta == opcao,
                        cor = corDaResposta(opcao),
                        habilitado = !salvando,
                        onClick = { onResponder(opcao) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BotaoDeResposta(
    rotulo: String,
    selecionado: Boolean,
    cor: androidx.compose.ui.graphics.Color,
    habilitado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (selecionado) cor else VoleiColors.CartaoInterno)
                .border(
                    width = 1.dp,
                    color = if (selecionado) cor else VoleiColors.Borda,
                    shape = RoundedCornerShape(10.dp),
                ).clickable(enabled = habilitado, onClick = onClick)
                .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rotulo,
            color = if (selecionado) VoleiColors.Fundo else VoleiColors.TextoSecundario,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun corDaResposta(status: StatusPresenca) =
    when (status) {
        StatusPresenca.VOU -> VoleiColors.VerdeClaro
        StatusPresenca.TALVEZ -> VoleiColors.Dourado
        StatusPresenca.NAO_VOU -> VoleiColors.Vermelho
    }
