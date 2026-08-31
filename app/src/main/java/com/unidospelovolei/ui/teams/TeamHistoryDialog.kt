package com.unidospelovolei.ui.teams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.unidospelovolei.domain.model.MatchCard
import com.unidospelovolei.domain.model.MatchStatus
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.PontoDoTime
import com.unidospelovolei.ui.components.RotuloPequeno
import com.unidospelovolei.ui.components.ScoreBox
import com.unidospelovolei.ui.components.SeloResultado
import com.unidospelovolei.ui.components.TeamCircle
import com.unidospelovolei.ui.theme.VoleiColors

/**
 * Agenda de jogos do time: todas as partidas dele com fase, rodada, quadra,
 * placar e o resultado quando já finalizada.
 */
@Composable
fun TeamHistoryDialog(
    time: Team,
    partidas: List<MatchCard>,
    onFechar: () -> Unit,
) {
    Dialog(onDismissRequest = onFechar) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VoleiColors.Fundo),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(VoleiColors.FundoCabecalho)
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TeamCircle(sigla = time.sigla, corHex = time.corHex, tamanho = 40.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Time ${time.nome}",
                        color = VoleiColors.TextoPrimario,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Agenda de Jogos",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                    )
                }
                IconButton(onClick = onFechar, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Fechar",
                        tint = VoleiColors.TextoSecundario,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (partidas.isEmpty()) {
                EstadoVazio(
                    titulo = "Sem jogos",
                    descricao = "Este time ainda não aparece em nenhuma rodada.",
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(partidas, key = { it.id }) { partida ->
                        LinhaHistorico(partida = partida, timeId = time.id)
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaHistorico(
    partida: MatchCard,
    timeId: String,
    modifier: Modifier = Modifier,
) {
    val souOTimeA = partida.teamA.id == timeId
    val meuTime = if (souOTimeA) partida.teamA else partida.teamB
    val adversario = if (souOTimeA) partida.teamB else partida.teamA
    val meuPlacar = if (souOTimeA) partida.scoreA else partida.scoreB
    val placarAdversario = if (souOTimeA) partida.scoreB else partida.scoreA
    val finalizada = partida.status == MatchStatus.FINALIZADO

    Cartao(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RotuloPequeno(
                    "Fase ${partida.fase} • Rodada ${partida.roundNumero} • Quadra ${partida.quadra}",
                )
                if (finalizada) {
                    SeloResultado(venceu = partida.winnerId == timeId)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    PontoDoTime(meuTime.corHex, tamanho = 9.dp)
                    Text(
                        meuTime.nome,
                        color = VoleiColors.TextoPrimario,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val jaTemPlacar = finalizada || meuPlacar > 0 || placarAdversario > 0
                    ScoreBox(valor = meuPlacar.takeIf { jaTemPlacar })
                    Text("x", color = VoleiColors.TextoTerciario, fontSize = 12.sp)
                    ScoreBox(valor = placarAdversario.takeIf { jaTemPlacar })
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(
                        adversario.nome,
                        color = VoleiColors.TextoPrimario,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    PontoDoTime(adversario.corHex, tamanho = 9.dp)
                }
            }
        }
    }
}
