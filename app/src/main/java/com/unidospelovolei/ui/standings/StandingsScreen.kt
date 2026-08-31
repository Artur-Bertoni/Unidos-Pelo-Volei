package com.unidospelovolei.ui.standings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.Standing
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.DialogoConfirmacao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.PontoDoTime
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun StandingsScreen(
    estado: StandingsUiState,
    isAdmin: Boolean,
    onApagarResultados: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmando by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Cartao(modifier = Modifier.fillMaxWidth()) {
                Column {
                    CabecalhoClassificacao()
                    HorizontalDivider(color = VoleiColors.Borda)
                    CabecalhoColunas()
                    if (estado.linhas.isEmpty()) {
                        EstadoVazio(
                            titulo = "Sem classificação",
                            descricao = "Cadastre os times e finalize partidas para ver a tabela.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        estado.linhas.forEachIndexed { indice, linha ->
                            LinhaClassificacao(posicao = indice + 1, linha = linha)
                            if (indice < estado.linhas.lastIndex) {
                                HorizontalDivider(color = VoleiColors.Borda.copy(alpha = 0.5f))
                            }
                        }
                    }
                    HorizontalDivider(color = VoleiColors.Borda)
                    Text(
                        text = "Critérios: 1. Vitórias (V)  |  2. Saldo de Pontos (S)  |  3. Pontos Pró (PP)",
                        color = VoleiColors.TextoTerciario,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    )
                }
            }
        }

        if (isAdmin && estado.linhas.isNotEmpty()) {
            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { confirmando = true }
                            .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        tint = VoleiColors.TextoTerciario,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "  Apagar todos os resultados",
                        color = VoleiColors.TextoTerciario,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }

    if (confirmando) {
        DialogoConfirmacao(
            titulo = "Apagar todos os resultados?",
            mensagem = "Os placares voltam a zero e as partidas voltam para agendadas. O chaveamento é mantido.",
            textoConfirmar = "Apagar",
            onConfirmar = {
                confirmando = false
                onApagarResultados()
            },
            onCancelar = { confirmando = false },
        )
    }
}

@Composable
private fun CabecalhoClassificacao(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(VoleiColors.FundoCabecalho)
                .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = VoleiColors.Dourado,
            modifier = Modifier.size(18.dp),
        )
        Text(
            "  Classificação Geral",
            color = VoleiColors.TextoPrimario,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CabecalhoColunas(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextoColuna("#", Modifier.width(28.dp))
        TextoColuna("TIME", Modifier.weight(1f), alinhamento = TextAlign.Start)
        TextoColuna("V", Modifier.width(36.dp))
        TextoColuna("S", Modifier.width(44.dp))
        TextoColuna("PP", Modifier.width(40.dp))
    }
}

@Composable
private fun TextoColuna(
    texto: String,
    modifier: Modifier = Modifier,
    alinhamento: TextAlign = TextAlign.Center,
) {
    Text(
        text = texto,
        color = VoleiColors.TextoTerciario,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        textAlign = alinhamento,
        modifier = modifier,
    )
}

@Composable
private fun LinhaClassificacao(
    posicao: Int,
    linha: Standing,
    modifier: Modifier = Modifier,
) {
    val destaque =
        when (posicao) {
            1 -> VoleiColors.Dourado.copy(alpha = 0.07f)
            2, 3 -> VoleiColors.Dourado.copy(alpha = 0.035f)
            else -> Color.Transparent
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(destaque)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${posicao}º",
            color = if (posicao <= 3) VoleiColors.Dourado else VoleiColors.TextoSecundario,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PontoDoTime(linha.corHex)
            Text(
                text = linha.nome,
                color = VoleiColors.TextoPrimario,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        Text(
            text = linha.vitorias.toString(),
            color = VoleiColors.TextoPrimario,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(36.dp),
        )
        Text(
            text = if (linha.saldoPontos > 0) "+${linha.saldoPontos}" else linha.saldoPontos.toString(),
            color = VoleiColors.TextoSecundario,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(44.dp),
        )
        Text(
            text = linha.pontosPro.toString(),
            color = VoleiColors.TextoTerciario,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp),
        )
    }
}
