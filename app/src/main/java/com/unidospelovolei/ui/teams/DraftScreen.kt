package com.unidospelovolei.ui.teams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.TeamRoster
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.TeamCircle
import com.unidospelovolei.ui.theme.VoleiColors

/**
 * Prévia do snake draft.
 *
 * Mostra como os jogadores ativos ficariam distribuídos e a força total de cada
 * time, para conferir o equilíbrio antes de gravar.
 */
@Composable
fun DraftScreen(
    previa: List<TeamRoster>?,
    salvando: Boolean,
    onVoltar: () -> Unit,
    onRecalcular: () -> Unit,
    onAplicar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(VoleiColors.Fundo)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onVoltar) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = VoleiColors.TextoPrimario,
                )
            }
            Text(
                "Distribuição por habilidade",
                color = VoleiColors.TextoPrimario,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        if (previa == null) {
            EstadoVazio(
                titulo = "Nada calculado ainda",
                descricao = "Toque em Calcular para montar os times com o snake draft.",
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            val forcas = previa.map { it.forcaTotal }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        text =
                            "${previa.sumOf { it.players.size }} jogadores ativos • " +
                                "força de ${forcas.minOrNull() ?: 0} a ${forcas.maxOrNull() ?: 0}",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                    )
                }
                items(previa, key = { it.team.id }) { elenco ->
                    CartaoElenco(elenco)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(onClick = onRecalcular, modifier = Modifier.weight(1f)) {
                Text(if (previa == null) "Calcular" else "Recalcular", color = VoleiColors.TextoPrimario)
            }
            Button(
                onClick = onAplicar,
                enabled = previa != null && !salvando,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = VoleiColors.Verde),
            ) {
                Text(if (salvando) "Salvando..." else "Aplicar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CartaoElenco(
    elenco: TeamRoster,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TeamCircle(sigla = elenco.team.sigla, corHex = elenco.team.corHex, tamanho = 34.dp)
                Text(
                    elenco.team.nome,
                    color = VoleiColors.TextoPrimario,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "força ${elenco.forcaTotal}",
                    color = VoleiColors.VerdeClaro,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = elenco.players.joinToString(", ") { "${it.nome} (${it.skillLevel})" },
                color = VoleiColors.TextoSecundario,
                fontSize = 12.sp,
            )
        }
    }
}
