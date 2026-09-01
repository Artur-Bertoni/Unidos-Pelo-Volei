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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun MatchScoreScreen(
    estado: MatchScoreUiState,
    isAdmin: Boolean,
    onVoltar: () -> Unit,
    onSomar: (Boolean, Int) -> Unit,
    onDefinirPlacar: (Boolean, Int) -> Unit,
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
                val editavel = isAdmin && partida.status == MatchStatus.AGENDADO
                PainelDeTime(
                    time = partida.teamA,
                    placar = partida.scoreA,
                    vencedor = partida.winnerId == partida.teamA.id,
                    editavel = editavel,
                    onSomar = { delta -> onSomar(true, delta) },
                    onDefinir = { valor -> onDefinirPlacar(true, valor) },
                    modifier = Modifier.weight(1f),
                )
                PainelDeTime(
                    time = partida.teamB,
                    placar = partida.scoreB,
                    vencedor = partida.winnerId == partida.teamB.id,
                    editavel = editavel,
                    onSomar = { delta -> onSomar(false, delta) },
                    onDefinir = { valor -> onDefinirPlacar(false, valor) },
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

                partida.status == MatchStatus.AGENDADO -> {
                    Text(
                        "Toque no número para digitar o resultado, ou use + e - para marcar ponto a ponto.",
                        color = VoleiColors.TextoTerciario,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onFinalizar,
                        enabled = !estado.salvando,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = VoleiColors.Verde),
                    ) {
                        Text("Finalizar partida", fontWeight = FontWeight.Bold)
                    }
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
    onDefinir: (Int) -> Unit,
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
            val corDoPlacar = if (vencedor) VoleiColors.VerdeClaro else VoleiColors.TextoPrimario
            if (editavel) {
                CampoDePlacar(
                    placar = placar,
                    cor = corDoPlacar,
                    descricao = "Placar do time ${time.nome}",
                    onDefinir = onDefinir,
                )
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
                Text(
                    text = placar.toString(),
                    color = corDoPlacar,
                    fontSize = PLACAR_SP.sp,
                    fontWeight = FontWeight.Black,
                )
                Box(modifier = Modifier.size(44.dp))
            }
        }
    }
}

@Composable
private fun CampoDePlacar(
    placar: Int,
    cor: Color,
    descricao: String,
    onDefinir: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var digitado by remember { mutableStateOf(placar.toString()) }
    var emFoco by remember { mutableStateOf(false) }
    val texto = if (emFoco) digitado else placar.toString()

    BasicTextField(
        value = texto,
        onValueChange = { entrada ->
            val limpo = entrada.filter { it.isDigit() }.trimStart('0').take(3)
            digitado = limpo
            onDefinir(limpo.toIntOrNull() ?: 0)
        },
        textStyle =
            TextStyle(
                color = cor,
                fontSize = PLACAR_SP.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            ),
        singleLine = true,
        cursorBrush = SolidColor(VoleiColors.Verde),
        keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier =
            modifier
                .width(110.dp)
                .onFocusChanged { estado ->
                    if (estado.isFocused) digitado = placar.toString()
                    emFoco = estado.isFocused
                }.semantics { contentDescription = descricao },
        decorationBox = { campo ->
            Box(contentAlignment = Alignment.Center) {
                if (texto.isEmpty()) {
                    Text(
                        "0",
                        color = VoleiColors.TextoTerciario,
                        fontSize = PLACAR_SP.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                campo()
            }
        },
    )
}

private const val PLACAR_SP = 52
