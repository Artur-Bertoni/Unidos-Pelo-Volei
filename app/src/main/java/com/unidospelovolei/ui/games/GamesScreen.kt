package com.unidospelovolei.ui.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.MatchCard
import com.unidospelovolei.domain.model.MatchStatus
import com.unidospelovolei.domain.model.RoundSchedule
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.Contador
import com.unidospelovolei.ui.components.DialogoConfirmacao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.ScoreBox
import com.unidospelovolei.ui.components.Selo
import com.unidospelovolei.ui.components.SeloFase
import com.unidospelovolei.ui.components.TeamBadge
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun GamesScreen(
    estado: GamesUiState,
    isAdmin: Boolean,
    onAbrirPartida: (String) -> Unit,
    onGerarChaveamento: (Int) -> Unit,
    onEncerrarDia: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandida by rememberSaveable { mutableStateOf<Int?>(1) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isAdmin) {
            item {
                CartaoChaveamento(
                    gerando = estado.gerando,
                    temChaveamento = estado.rodadas.isNotEmpty(),
                    onGerar = onGerarChaveamento,
                )
            }
            item {
                CartaoEncerrarDia(
                    encerrando = estado.encerrando,
                    temElencos = estado.temElencos,
                    temChaveamento = estado.rodadas.isNotEmpty(),
                    temPresentes = estado.presentes > 0,
                    onEncerrar = onEncerrarDia,
                )
            }
        }

        if (estado.rodadas.isEmpty() && !estado.carregando) {
            item {
                EstadoVazio(
                    titulo = "Nenhuma rodada ainda",
                    descricao =
                        if (isAdmin) {
                            "Cadastre os times e gere o chaveamento para começar."
                        } else {
                            "O chaveamento ainda não foi gerado por um administrador."
                        },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        items(estado.rodadas, key = { it.round.id }) { rodada ->
            CartaoRodada(
                rodada = rodada,
                expandida = expandida == rodada.round.numero,
                onAlternar = {
                    expandida = if (expandida == rodada.round.numero) null else rodada.round.numero
                },
                onAbrirPartida = onAbrirPartida,
            )
        }
    }
}

@Composable
private fun CartaoChaveamento(
    gerando: Boolean,
    temChaveamento: Boolean,
    onGerar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var quadras by rememberSaveable { mutableIntStateOf(3) }
    var confirmando by remember { mutableStateOf(false) }

    Cartao(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Chaveamento",
                color = VoleiColors.TextoPrimario,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Cada quadra recebe um trio, que joga entre si nas três rodadas da fase " +
                    "(A×B, A×C, B×C). Depois os times trocam de quadra até todos jogarem " +
                    "contra todos, e ninguém joga mais de duas partidas seguidas.",
                color = VoleiColors.TextoSecundario,
                fontSize = 12.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Quadras", color = VoleiColors.TextoSecundario, fontSize = 13.sp)
                Contador(
                    valor = quadras,
                    minimo = 1,
                    maximo = 12,
                    onMudar = { quadras = it },
                )
            }
            Button(
                onClick = { if (temChaveamento) confirmando = true else onGerar(quadras) },
                enabled = !gerando,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VoleiColors.Verde),
            ) {
                Text(if (gerando) "Gerando..." else "Gerar chaveamento", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (confirmando) {
        DialogoConfirmacao(
            titulo = "Gerar novo chaveamento?",
            mensagem = "As rodadas e os placares atuais serão apagados e substituídos.",
            textoConfirmar = "Gerar",
            onConfirmar = {
                confirmando = false
                onGerar(quadras)
            },
            onCancelar = { confirmando = false },
        )
    }
}

@Composable
private fun CartaoEncerrarDia(
    encerrando: Boolean,
    temElencos: Boolean,
    temChaveamento: Boolean,
    temPresentes: Boolean,
    onEncerrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmando by remember { mutableStateOf(false) }
    val temAlgoParaEncerrar = temElencos || temChaveamento || temPresentes

    Cartao(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Fim do dia",
                color = VoleiColors.TextoPrimario,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (temAlgoParaEncerrar) {
                    "Guarda os resultados e a presença de quem veio, desfaz os times, " +
                        "apaga o chaveamento e zera as presenças. O próximo sorteio usa " +
                        "esse histórico para evitar repetir os mesmos companheiros."
                } else {
                    "Nada a encerrar: ninguém presente, nenhum time montado e nenhum chaveamento."
                },
                color = VoleiColors.TextoSecundario,
                fontSize = 12.sp,
            )
            OutlinedButton(
                onClick = { confirmando = true },
                enabled = !encerrando && temAlgoParaEncerrar,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Filled.EventAvailable,
                    contentDescription = null,
                    tint = if (temAlgoParaEncerrar) VoleiColors.Dourado else VoleiColors.TextoTerciario,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    if (encerrando) "  Encerrando..." else "  Encerrar dia",
                    color = if (temAlgoParaEncerrar) VoleiColors.TextoPrimario else VoleiColors.TextoTerciario,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (confirmando) {
        DialogoConfirmacao(
            titulo = "Encerrar o dia?",
            mensagem =
                "Os resultados das partidas finalizadas vão para o desempenho dos atletas, " +
                    "e todo mundo que estava presente ganha mais um dia no histórico. " +
                    "Depois disso os times ficam sem elenco, o chaveamento é apagado e as " +
                    "presenças são zeradas para o próximo sábado. Não dá para desfazer.",
            textoConfirmar = "Encerrar dia",
            onConfirmar = {
                confirmando = false
                onEncerrar()
            },
            onCancelar = { confirmando = false },
        )
    }
}

@Composable
private fun CartaoRodada(
    rodada: RoundSchedule,
    expandida: Boolean,
    onAlternar: () -> Unit,
    onAbrirPartida: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAlternar)
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "RODADA ${rodada.round.numero}",
                            color = VoleiColors.TextoPrimario,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                        )
                        SeloFase(rodada.round.fase)
                    }
                    if (rodada.folgam.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Filled.Hotel,
                                contentDescription = null,
                                tint = VoleiColors.TextoTerciario,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                "Folgam: ${listarNomes(rodada.folgam.map { it.nome })}",
                                color = VoleiColors.TextoSecundario,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (expandida) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expandida) "Recolher" else "Expandir",
                    tint = VoleiColors.TextoSecundario,
                )
            }

            AnimatedVisibility(visible = expandida) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(VoleiColors.CartaoInterno)
                            .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rodada.matches.forEach { partida ->
                        CartaoPartida(partida = partida, onClick = { onAbrirPartida(partida.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun CartaoPartida(
    partida: MatchCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val finalizada = partida.status == MatchStatus.FINALIZADO
    Cartao(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Selo(
                texto = "Quadra ${partida.quadra}",
                corTexto = VoleiColors.TextoSecundario,
                corFundo = VoleiColors.Fundo,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TeamBadge(partida.teamA)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ScoreBox(
                        valor = if (finalizada || partida.scoreA > 0 || partida.scoreB > 0) partida.scoreA else null,
                        destacado = finalizada && partida.winnerId == partida.teamA.id,
                    )
                    Text("x", color = VoleiColors.TextoTerciario, fontSize = 13.sp)
                    ScoreBox(
                        valor = if (finalizada || partida.scoreA > 0 || partida.scoreB > 0) partida.scoreB else null,
                        destacado = finalizada && partida.winnerId == partida.teamB.id,
                    )
                }
                TeamBadge(partida.teamB)
            }
        }
    }
}

fun listarNomes(nomes: List<String>): String =
    when (nomes.size) {
        0 -> ""
        1 -> nomes.first()
        else -> nomes.dropLast(1).joinToString(", ") + " e " + nomes.last()
    }
