package com.unidospelovolei.ui.evolucao

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.AvaliacaoPendente
import com.unidospelovolei.domain.model.Fundamento
import com.unidospelovolei.domain.model.NotasDaAvaliacao
import com.unidospelovolei.domain.model.NotasPorFundamento
import com.unidospelovolei.domain.model.OrigemDaNota
import com.unidospelovolei.domain.model.PeriodoDoGrafico
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.PontoDaNota
import com.unidospelovolei.domain.model.arredondar
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.Estrelas
import com.unidospelovolei.ui.components.RotuloPequeno
import com.unidospelovolei.ui.grupo.BotaoDeAcao
import com.unidospelovolei.ui.theme.VoleiColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun CartaoDaEvolucao(
    estado: EvolucaoUiState,
    jogador: Player?,
    modifier: Modifier = Modifier,
) {
    var periodo by remember { mutableStateOf(PeriodoDoGrafico.ANO) }
    var serie by remember { mutableStateOf<Fundamento?>(null) }
    var selecionado by remember { mutableStateOf<Int?>(null) }

    val pontos = remember(estado.historico, periodo) { estado.historico.noPeriodo(periodo) }
    val variacao = remember(pontos) { variacaoDe(pontos) }
    val notas = jogador?.notas ?: NotasPorFundamento()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Cartao(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    RotuloPequeno("Minha nota")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Estrelas(nivel = jogador?.media ?: 3.0)
                        Text(
                            "%.1f".format(jogador?.media ?: 3.0),
                            color = VoleiColors.TextoPrimario,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (jogador == null) {
                    Text(
                        "A nota aparece aqui assim que a diretoria ligar a sua conta a um jogador.",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 13.sp,
                    )
                    return@Column
                }

                Fundamento.entries.forEach { fundamento ->
                    BarraDoFundamento(
                        rotulo = fundamento.rotulo,
                        media = notas.de(fundamento),
                        delta = variacao[fundamento] ?: 0.0,
                        selecionado = serie == fundamento,
                        onClick = {
                            serie = if (serie == fundamento) null else fundamento
                            selecionado = null
                        },
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PeriodoDoGrafico.entries.forEach { opcao ->
                        ChipDoPeriodo(
                            rotulo = opcao.rotulo,
                            selecionado = opcao == periodo,
                            onClick = {
                                periodo = opcao
                                selecionado = null
                            },
                        )
                    }
                }

                if (pontos.size < 2) {
                    Text(
                        text =
                            if (estado.jaMoveu) {
                                "Nenhum ponto neste período. Escolha um período maior."
                            } else {
                                "O gráfico começa a desenhar quando o primeiro sábado avaliado entrar na conta."
                            },
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                    )
                } else {
                    Text(
                        "${serie?.rotulo ?: "Média geral"} • toque numa barra acima para trocar a linha",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                    )
                    GraficoDaNota(
                        pontos = pontos,
                        serie = serie,
                        selecionado = selecionado,
                        onSelecionar = { selecionado = if (it == selecionado) null else it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                    )
                    Text(
                        text = legendaDoGrafico(pontos, serie, selecionado),
                        color = VoleiColors.TextoTerciario,
                        fontSize = 11.sp,
                    )
                }

                estado.dicaPara(notas)?.let { dica ->
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

                Text(
                    text =
                        (
                            if (estado.totalAvaliacoes == 0) {
                                "Você ainda não recebeu avaliações. "
                            } else {
                                "${estado.totalAvaliacoes} avaliações recebidas até agora. "
                            }
                        ) +
                            "Cada sábado com pelo menos duas notas empurra a sua em até 0,25 " +
                            "na direção do que o time achou.",
                    color = VoleiColors.TextoTerciario,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

private fun List<PontoDaNota>.noPeriodo(periodo: PeriodoDoGrafico): List<PontoDaNota> {
    val corte =
        when (periodo) {
            PeriodoDoGrafico.TRES_MESES -> LocalDate.now().minusMonths(3)
            PeriodoDoGrafico.SEIS_MESES -> LocalDate.now().minusMonths(6)
            PeriodoDoGrafico.ANO -> LocalDate.of(LocalDate.now().year, 1, 1)
            PeriodoDoGrafico.TUDO -> return this
        }
    return filter { ponto -> ponto.dia()?.isBefore(corte) != true }
}

private fun PontoDaNota.dia(): LocalDate? =
    runCatching { Instant.parse(registradoEm).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()

private fun PontoDaNota.diaEMes(): String =
    dia()?.format(DateTimeFormatter.ofPattern("dd/MM")).orEmpty()

private fun PontoDaNota.valorDe(serie: Fundamento?): Double = if (serie == null) media else notas.de(serie)

private fun variacaoDe(pontos: List<PontoDaNota>): Map<Fundamento, Double> {
    if (pontos.size < 2) return emptyMap()
    val primeiro = pontos.first().notas
    val ultimo = pontos.last().notas
    return Fundamento.entries.associateWith { arredondar(ultimo.de(it) - primeiro.de(it)) }
}

private fun legendaDoGrafico(
    pontos: List<PontoDaNota>,
    serie: Fundamento?,
    selecionado: Int?,
): String {
    val ponto = selecionado?.let { pontos.getOrNull(it) }
        ?: return "Ponto dourado é ajuste da diretoria. Ponto verde é sábado avaliado."
    val complemento =
        if (ponto.origem == OrigemDaNota.DIRETORIA) {
            "ajuste da diretoria"
        } else {
            "${ponto.avaliadores} avaliações naquele dia"
        }
    return "${ponto.diaEMes()} • ${serie?.rotulo ?: "Média geral"} " +
        "%.2f".format(ponto.valorDe(serie)) + " • " + complemento
}

@Composable
private fun GraficoDaNota(
    pontos: List<PontoDaNota>,
    serie: Fundamento?,
    selecionado: Int?,
    onSelecionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val corDaLinha = VoleiColors.VerdeClaro
    val corDaGrade = VoleiColors.Borda
    val corDoAjuste = VoleiColors.Dourado
    val corDoFundo = VoleiColors.Cartao
    val medidor = rememberTextMeasurer()
    val estiloDoEixo = TextStyle(color = VoleiColors.TextoTerciario, fontSize = 9.sp)

    Canvas(
        modifier =
            modifier.pointerInput(pontos, serie) {
                detectTapGestures { toque ->
                    val esquerda = 26.dp.toPx()
                    val largura = (size.width - esquerda - 8.dp.toPx()).coerceAtLeast(1f)
                    val passo = if (pontos.size == 1) 0f else largura / (pontos.size - 1)
                    val indice =
                        if (passo == 0f) 0 else ((toque.x - esquerda) / passo).roundToInt()
                    if (indice in pontos.indices) onSelecionar(indice)
                }
            },
    ) {
        val esquerda = 26.dp.toPx()
        val topo = 8.dp.toPx()
        val baixo = size.height - 16.dp.toPx()
        val direita = size.width - 8.dp.toPx()
        val largura = (direita - esquerda).coerceAtLeast(1f)
        val altura = (baixo - topo).coerceAtLeast(1f)

        fun x(indice: Int): Float =
            if (pontos.size == 1) esquerda + largura / 2 else esquerda + largura * indice / (pontos.size - 1)

        fun y(valor: Double): Float = topo + altura * ((5.0 - valor.coerceIn(1.0, 5.0)) / 4.0).toFloat()

        (1..5).forEach { linha ->
            val altoDaLinha = y(linha.toDouble())
            drawLine(
                color = corDaGrade,
                start = Offset(esquerda, altoDaLinha),
                end = Offset(direita, altoDaLinha),
                strokeWidth = 1f,
            )
            val rotulo = medidor.measure(linha.toString(), estiloDoEixo)
            drawText(
                textLayoutResult = rotulo,
                topLeft = Offset(esquerda - rotulo.size.width - 6f, altoDaLinha - rotulo.size.height / 2f),
            )
        }

        val caminho =
            Path().apply {
                pontos.forEachIndexed { indice, ponto ->
                    val ponteiroX = x(indice)
                    val ponteiroY = y(ponto.valorDe(serie))
                    if (indice == 0) moveTo(ponteiroX, ponteiroY) else lineTo(ponteiroX, ponteiroY)
                }
            }
        drawPath(
            path = caminho,
            color = corDaLinha,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        if (pontos.size <= 26) {
            pontos.forEachIndexed { indice, ponto ->
                val centro = Offset(x(indice), y(ponto.valorDe(serie)))
                drawCircle(color = corDoFundo, radius = 6.dp.toPx() / 2 + 2f, center = centro)
                drawCircle(
                    color = if (ponto.origem == OrigemDaNota.DIRETORIA) corDoAjuste else corDaLinha,
                    radius = if (indice == selecionado) 5.dp.toPx() else 4.dp.toPx(),
                    center = centro,
                )
            }
        }

        val primeiro = medidor.measure(pontos.first().diaEMes(), estiloDoEixo)
        drawText(textLayoutResult = primeiro, topLeft = Offset(esquerda, baixo + 4f))
        val ultimo = medidor.measure(pontos.last().diaEMes(), estiloDoEixo)
        drawText(textLayoutResult = ultimo, topLeft = Offset(direita - ultimo.size.width, baixo + 4f))
    }
}

@Composable
private fun ChipDoPeriodo(
    rotulo: String,
    selecionado: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = rotulo,
        color = if (selecionado) VoleiColors.TextoPrimario else VoleiColors.TextoSecundario,
        fontSize = 12.sp,
        fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (selecionado) VoleiColors.CartaoInterno else VoleiColors.Cartao)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun BarraDoFundamento(
    rotulo: String,
    media: Double,
    delta: Double,
    selecionado: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                rotulo,
                color = if (selecionado) VoleiColors.TextoPrimario else VoleiColors.TextoSecundario,
                fontSize = 12.sp,
                fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            if (delta != 0.0) {
                Text(
                    (if (delta > 0) "+" else "−") + "%.2f".format(kotlin.math.abs(delta)),
                    color = if (delta > 0) VoleiColors.VerdeClaro else VoleiColors.Vermelho,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Text(
                "%.2f".format(media),
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
