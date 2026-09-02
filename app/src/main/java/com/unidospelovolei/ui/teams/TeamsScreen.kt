package com.unidospelovolei.ui.teams

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.TeamCircle
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun TeamsScreen(
    estado: TeamsUiState,
    isAdmin: Boolean,
    onAbrirTime: (Team) -> Unit,
    onEditarTime: (Team) -> Unit,
    onAlternarAtivoTime: (Team) -> Unit,
    onAjustarTimes: () -> Unit,
    onNovoTime: () -> Unit,
    onAbrirJogadores: () -> Unit,
    onDistribuir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Equipes Participantes",
                        color = VoleiColors.TextoPrimario,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${estado.presentes} presentes " +
                            "(${estado.homensPresentes}H / ${estado.mulheresPresentes}M) • " +
                            "${estado.timesAtivos} times ativos",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                    )
                    if (isAdmin) {
                        dicaDeTimes(estado)?.let { dica ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    dica.texto,
                                    color = VoleiColors.Dourado,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                if (dica.ajustavel) {
                                    TextButton(
                                        onClick = onAjustarTimes,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Text("Ajustar", color = VoleiColors.Verde, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotaoAcao(
                        texto = "Jogadores",
                        icone = Icons.Filled.Group,
                        onClick = onAbrirJogadores,
                        modifier = Modifier.weight(1f),
                    )
                    if (isAdmin) {
                        BotaoAcao(
                            texto = "Distribuir",
                            icone = Icons.Filled.Shuffle,
                            onClick = onDistribuir,
                            modifier = Modifier.weight(1f),
                        )
                        BotaoAcao(
                            texto = "Novo time",
                            icone = Icons.Filled.Add,
                            onClick = onNovoTime,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (estado.times.isEmpty() && !estado.carregando) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EstadoVazio(
                    titulo = "Nenhum time cadastrado",
                    descricao =
                        if (isAdmin) {
                            "Toque em Novo time para criar as equipes coloridas."
                        } else {
                            "Um administrador ainda não cadastrou as equipes."
                        },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        items(estado.times, key = { it.id }) { time ->
            val elenco = estado.elencos.firstOrNull { it.team.id == time.id }
            CartaoTime(
                time = time,
                jogadores = elenco?.players?.size ?: 0,
                forca = elenco?.forcaTotal ?: 0,
                onClick = { onAbrirTime(time) },
                onEditar = if (isAdmin) ({ onEditarTime(time) }) else null,
                onAlternarAtivo = if (isAdmin) ({ onAlternarAtivoTime(time) }) else null,
            )
        }
    }
}

private data class DicaDoDia(
    val texto: String,
    val ajustavel: Boolean,
)

private fun dicaDeTimes(estado: TeamsUiState): DicaDoDia? {
    val cabem = estado.timesQueCabem.coerceAtMost(estado.times.size)
    val sobrando = estado.timesAtivos - cabem
    return when {
        estado.presentes == 0 -> DicaDoDia("Marque em Jogadores quem veio jogar hoje.", false)
        estado.timesQueCabem == 0 -> DicaDoDia("Menos de 4 presentes: ainda não dá para um time.", false)
        estado.timesQueCabem > estado.times.size ->
            DicaDoDia(
                "Dá para ${estado.timesQueCabem} times de 4: " +
                    "cadastre mais ${estado.timesQueCabem - estado.times.size}.",
                sobrando != 0,
            )
        sobrando > 0 -> DicaDoDia("Dá para $cabem times de 4: desative $sobrando.", true)
        sobrando < 0 -> DicaDoDia("Dá para $cabem times de 4: ative mais ${-sobrando}.", true)
        else -> null
    }
}

@Composable
private fun BotaoAcao(
    texto: String,
    icone: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Icon(icone, contentDescription = null, tint = VoleiColors.Verde, modifier = Modifier.size(16.dp))
        Text(
            "  $texto",
            color = VoleiColors.TextoPrimario,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun CartaoTime(
    time: Team,
    jogadores: Int,
    forca: Int,
    onClick: () -> Unit,
    onEditar: (() -> Unit)?,
    onAlternarAtivo: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Cartao(
        modifier = modifier.aspectRatio(0.86f),
        cor = if (time.ativo) VoleiColors.Cartao else VoleiColors.CartaoInterno,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .alpha(if (time.ativo) 1f else 0.45f)
                    .clickable(onClick = onClick)
                    .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            TeamCircle(sigla = time.sigla, corHex = time.corHex, tamanho = 52.dp)
            Text(
                text = time.nome,
                color = VoleiColors.TextoPrimario,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text =
                    when {
                        !time.ativo -> "fora de hoje"
                        jogadores > 0 -> "$jogadores jog. • força $forca"
                        else -> "sem elenco"
                    },
                color = VoleiColors.TextoTerciario,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        if (onAlternarAtivo != null) {
            Icon(
                imageVector = if (time.ativo) Icons.Filled.ToggleOn else Icons.Filled.ToggleOff,
                contentDescription = if (time.ativo) "Tirar o time de hoje" else "Colocar o time em hoje",
                tint = if (time.ativo) VoleiColors.Verde else VoleiColors.TextoTerciario,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .size(36.dp)
                        .clickable(onClick = onAlternarAtivo)
                        .padding(8.dp),
            )
        }
        if (onEditar != null) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Editar time",
                tint = VoleiColors.TextoTerciario,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(34.dp)
                        .clickable(onClick = onEditar)
                        .padding(9.dp),
            )
        }
    }
}
