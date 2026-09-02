package com.unidospelovolei.ui.evolucao

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.AvaliacaoPendente
import com.unidospelovolei.domain.model.Fundamento
import com.unidospelovolei.domain.model.NotasDaAvaliacao
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.RotuloPequeno
import com.unidospelovolei.ui.grupo.BotaoDeAcao
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun CartaoDaEvolucao(
    estado: EvolucaoUiState,
    onAvaliar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (estado.pendentes.isNotEmpty()) {
            Cartao(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text =
                            if (estado.pendentes.size == 1) {
                                "1 companheiro esperando a sua nota"
                            } else {
                                "${estado.pendentes.size} companheiros esperando a sua nota"
                            },
                        color = VoleiColors.TextoPrimario,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "A nota é anônima: ninguém, nem a diretoria, vê quem deu qual nota.",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                    )
                    BotaoDeAcao(texto = "Avaliar agora", onClick = onAvaliar)
                }
            }
        }

        Cartao(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RotuloPequeno("Minha evolução")

                if (!estado.liberado) {
                    Text(
                        text =
                            if (estado.faltam > 0) {
                                "Faltam ${estado.faltam} avaliações para o painel acender."
                            } else {
                                "O painel acende assim que chegarem avaliações suficientes."
                            },
                        color = VoleiColors.TextoSecundario,
                        fontSize = 13.sp,
                    )
                    Text(
                        "O mínimo existe para o anonimato: com poucas notas dá para adivinhar de quem vieram.",
                        color = VoleiColors.TextoTerciario,
                        fontSize = 11.sp,
                    )
                } else {
                    val medias = estado.evolucao?.medias.orEmpty()
                    Fundamento.entries.forEach { fundamento ->
                        val media = medias[fundamento] ?: return@forEach
                        BarraDoFundamento(rotulo = fundamento.rotulo, media = media)
                    }

                    estado.dicasDoPontoFraco.forEach { dica ->
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(VoleiColors.CartaoInterno)
                                    .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            RotuloPequeno("Para treinar: ${dica.fundamento.rotulo}")
                            Text(
                                dica.titulo,
                                color = VoleiColors.TextoPrimario,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(dica.texto, color = VoleiColors.TextoSecundario, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarraDoFundamento(
    rotulo: String,
    media: Double,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(rotulo, color = VoleiColors.TextoSecundario, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(
                "%.1f".format(media),
                color = VoleiColors.TextoPrimario,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(VoleiColors.CartaoInterno),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth((media / 5.0).toFloat().coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (media >= 3.5) VoleiColors.Verde else VoleiColors.Dourado),
            )
        }
    }
}

@Composable
fun AvaliacaoScreen(
    pendentes: List<AvaliacaoPendente>,
    salvando: Boolean,
    onVoltar: () -> Unit,
    onEnviar: (AvaliacaoPendente, NotasDaAvaliacao) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize(), containerColor = VoleiColors.Fundo) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                Column {
                    Text(
                        "Avaliar companheiros",
                        color = VoleiColors.TextoPrimario,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Anônimo, e não mexe no sorteio", color = VoleiColors.TextoSecundario, fontSize = 12.sp)
                }
            }

            if (pendentes.isEmpty()) {
                EstadoVazio(
                    titulo = "Nada para avaliar",
                    descricao = "Depois do próximo sábado encerrado, os seus companheiros de time aparecem aqui.",
                    modifier = Modifier.fillMaxWidth(),
                )
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(pendentes, key = { "${it.dayId}-${it.avaliadoPlayerId}" }) { pendente ->
                    FichaDeAvaliacao(
                        pendente = pendente,
                        salvando = salvando,
                        onEnviar = { notas -> onEnviar(pendente, notas) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FichaDeAvaliacao(
    pendente: AvaliacaoPendente,
    salvando: Boolean,
    onEnviar: (NotasDaAvaliacao) -> Unit,
) {
    var notas by remember(pendente.avaliadoPlayerId) { mutableStateOf(NotasDaAvaliacao()) }

    Cartao(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                pendente.avaliadoNome,
                color = VoleiColors.TextoPrimario,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )

            Fundamento.entries.forEach { fundamento ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        fundamento.rotulo,
                        color = VoleiColors.TextoSecundario,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { nota ->
                            NotaSelecionavel(
                                nota = nota,
                                selecionada = notas.de(fundamento) == nota,
                                onClick = { notas = notas.com(fundamento, nota) },
                            )
                        }
                    }
                }
            }

            BotaoDeAcao(
                texto = "Enviar avaliação",
                onClick = { onEnviar(notas) },
                habilitado = !salvando,
            )
        }
    }
}

@Composable
private fun NotaSelecionavel(
    nota: Int,
    selecionada: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .width(30.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (selecionada) VoleiColors.Verde else VoleiColors.CartaoInterno)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            nota.toString(),
            color = if (selecionada) androidx.compose.ui.graphics.Color.White else VoleiColors.TextoSecundario,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
