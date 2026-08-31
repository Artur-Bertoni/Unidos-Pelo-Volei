package com.unidospelovolei.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.unidospelovolei.domain.model.MatchCard
import com.unidospelovolei.domain.model.MatchStatus
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.ui.components.BotaoRedondo
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.RotuloPequeno
import com.unidospelovolei.ui.components.Selo
import com.unidospelovolei.ui.components.TeamCircle
import com.unidospelovolei.ui.theme.VoleiColors

/**
 * Placar ao vivo.
 *
 * A tela lê a partida do banco local, então o placar registrado por outra pessoa
 * na mesma quadra aparece aqui assim que o sync chega. Sem internet os botões
 * continuam funcionando e a alteração sobe depois.
 */
@Composable
fun MatchScoreScreen(
    estado: MatchScoreUiState,
    isAdmin: Boolean,
    onVoltar: () -> Unit,
    onSomar: (Boolean, Int) -> Unit,
    onFinalizar: () -> Unit,
    onReabrir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val partida = estado.partida

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
                text = "Placar",
                color = VoleiColors.TextoPrimario,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        if (partida == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VoleiColors.Verde)
            }
            return@Column
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RotuloPequeno(
                "Fase ${partida.fase} • Rodada ${partida.roundNumero} • Quadra ${partida.quadra}",
            )

            if (partida.status == MatchStatus.FINALIZADO) {
                Selo(
                    texto = "Finalizada",
                    corTexto = VoleiColors.VerdeClaro,
                    corFundo = VoleiColors.SeloVitoriaFundo,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PainelDeTime(
                    time = partida.teamA,
                    placar = partida.scoreA,
                    vencedor = partida.winnerId == partida.teamA.id,
                    editavel = isAdmin && partida.status == MatchStatus.AGENDADO,
                    onSomar = { delta -> onSomar(true, delta) },
                    modifier = Modifier.weight(1f),
                )
                PainelDeTime(
                    time = partida.teamB,
                    placar = partida.scoreB,
                    vencedor = partida.winnerId == partida.teamB.id,
                    editavel = isAdmin && partida.status == MatchStatus.AGENDADO,
                    onSomar = { delta -> onSomar(false, delta) },
                    modifier = Modifier.weight(1f),
                )
            }

            when {
                !isAdmin ->
                    Text(
                        "Somente administradores registram placar.",
                        color = VoleiColors.TextoTerciario,
                        fontSize = 12.sp,
                    )

                partida.status == MatchStatus.AGENDADO ->
                    Button(
                        onClick = onFinalizar,
                        enabled = !estado.salvando,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = VoleiColors.Verde),
                    ) {
                        Text("Finalizar partida", fontWeight = FontWeight.Bold)
                    }

                else ->
                    OutlinedButton(
                        onClick = onReabrir,
                        enabled = !estado.salvando,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Reabrir partida", color = VoleiColors.TextoPrimario)
                    }
            }

            estado.erro?.let { mensagem ->
                Text(mensagem, color = VoleiColors.Vermelho, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PainelDeTime(
    time: Team,
    placar: Int,
    vencedor: Boolean,
    editavel: Boolean,
    onSomar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TeamCircle(sigla = time.sigla, corHex = time.corHex, tamanho = 60.dp)
            Text(
                text = time.nome.uppercase(),
                color = VoleiColors.TextoSecundario,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
            Text(
                text = placar.toString(),
                color = if (vencedor) VoleiColors.VerdeClaro else VoleiColors.TextoPrimario,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
            )
            if (editavel) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BotaoRedondo(
                        icone = Icons.Filled.Remove,
                        descricao = "Menos um ponto",
                        habilitado = placar > 0,
                        onClick = { onSomar(-1) },
                        tamanho = 44.dp,
                    )
                    BotaoRedondo(
                        icone = Icons.Filled.Add,
                        descricao = "Mais um ponto",
                        onClick = { onSomar(1) },
                        tamanho = 44.dp,
                        cor = VoleiColors.Verde,
                    )
                }
            } else {
                Box(modifier = Modifier.size(44.dp))
            }
        }
    }
}
