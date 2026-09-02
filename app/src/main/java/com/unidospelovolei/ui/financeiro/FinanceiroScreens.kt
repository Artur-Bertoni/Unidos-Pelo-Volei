package com.unidospelovolei.ui.financeiro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.StatusPagamento
import com.unidospelovolei.ui.components.CampoTexto
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.RotuloPequeno
import com.unidospelovolei.ui.components.Selo
import com.unidospelovolei.ui.grupo.BotaoDeAcao
import com.unidospelovolei.ui.theme.VoleiColors

fun reais(centavos: Int): String = "R$ %d,%02d".format(centavos / 100, centavos % 100)

@Composable
fun CartaoDoExtrato(
    estado: FinanceiroUiState,
    onAbrirPainel: () -> Unit,
    isAdmin: Boolean,
    modifier: Modifier = Modifier,
) {
    val copiar = LocalClipboardManager.current
    val emAberto = estado.emAbertoCentavos

    Cartao(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RotuloPequeno("Meu financeiro", modifier = Modifier.weight(1f))
                if (isAdmin) {
                    Text(
                        "Ver do grupo",
                        color = VoleiColors.Azul,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onAbrirPainel),
                    )
                }
            }

            Text(
                text = if (emAberto > 0) reais(emAberto) else "Tudo em dia",
                color = if (emAberto > 0) VoleiColors.Dourado else VoleiColors.VerdeClaro,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (emAberto > 0) "em aberto" else "nenhuma cobrança pendente",
                color = VoleiColors.TextoSecundario,
                fontSize = 12.sp,
            )

            if (emAberto > 0) {
                val codigo = estado.pixCopiaECola(emAberto)
                if (codigo != null) {
                    BotaoDeAcao(
                        texto = "Copiar Pix de ${reais(emAberto)}",
                        onClick = { copiar.setText(AnnotatedString(codigo)) },
                    )
                } else {
                    Text(
                        "A diretoria ainda não cadastrou a chave Pix.",
                        color = VoleiColors.TextoTerciario,
                        fontSize = 11.sp,
                    )
                }
            }

            estado.extrato.take(6).forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.titulo, color = VoleiColors.TextoPrimario, fontSize = 13.sp)
                        Text(
                            reais(item.pagamento.valorCentavos),
                            color = VoleiColors.TextoTerciario,
                            fontSize = 11.sp,
                        )
                    }
                    SeloDoStatus(item.pagamento.status)
                }
            }

            if (estado.extrato.isEmpty()) {
                Text(
                    "Nenhuma cobrança lançada para você ainda.",
                    color = VoleiColors.TextoTerciario,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SeloDoStatus(status: StatusPagamento) {
    when (status) {
        StatusPagamento.PAGO -> Selo(status.rotulo, VoleiColors.VerdeClaro, VoleiColors.SeloVitoriaFundo)
        StatusPagamento.PENDENTE -> Selo(status.rotulo, VoleiColors.Dourado, VoleiColors.Cartao)
        StatusPagamento.ISENTO -> Selo(status.rotulo, VoleiColors.TextoTerciario, VoleiColors.Cartao)
    }
}

@Composable
fun PainelFinanceiroScreen(
    estado: FinanceiroUiState,
    onVoltar: () -> Unit,
    onRecarregar: () -> Unit,
    onDefinirStatus: (String, StatusPagamento) -> Unit,
    onGerarMensalidade: () -> Unit,
    onGerarDiaria: () -> Unit,
    onConfigurar: () -> Unit,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Financeiro do grupo",
                        color = VoleiColors.TextoPrimario,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (estado.carregandoPainel) "Carregando do servidor..." else "Precisa de internet",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    "Recarregar",
                    color = VoleiColors.Azul,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onRecarregar).padding(8.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotaoDeAcao(texto = "Gerar mensalidade deste mês", onClick = onGerarMensalidade)
                        BotaoDeAcao(texto = "Gerar diária de hoje", onClick = onGerarDiaria)
                        BotaoDeAcao(texto = "Chave Pix e valores", onClick = onConfigurar)
                    }
                }

                if (estado.painel.isEmpty()) {
                    item {
                        EstadoVazio(
                            titulo = "Nada lançado ainda",
                            descricao = "Gere a mensalidade do mês ou a diária do sábado para começar.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                items(estado.painel, key = { it.pagamentoId }) { linha ->
                    Cartao(modifier = Modifier.fillMaxWidth(), cor = VoleiColors.CartaoInterno) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(linha.nome, color = VoleiColors.TextoPrimario, fontSize = 14.sp)
                                    Text(
                                        "${linha.cobranca} · ${reais(linha.valorCentavos)}",
                                        color = VoleiColors.TextoTerciario,
                                        fontSize = 11.sp,
                                    )
                                }
                                SeloDoStatus(linha.status)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPagamento.entries.forEach { opcao ->
                                    BotaoDeStatus(
                                        rotulo = opcao.rotulo,
                                        selecionado = linha.status == opcao,
                                        onClick = { onDefinirStatus(linha.pagamentoId, opcao) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BotaoDeStatus(
    rotulo: String,
    selecionado: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (selecionado) VoleiColors.SeloFaseFundo else VoleiColors.Cartao)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            rotulo,
            color = if (selecionado) VoleiColors.SeloFaseTexto else VoleiColors.TextoSecundario,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ConfigFinanceiroDialog(
    estado: FinanceiroUiState,
    onSalvar: (String, String, String, Int, Int) -> Unit,
    onFechar: () -> Unit,
) {
    val config = estado.config
    var chave by remember(config?.id) { mutableStateOf(config?.pixChave.orEmpty()) }
    var nome by remember(config?.id) { mutableStateOf(config?.pixNome.orEmpty()) }
    var cidade by remember(config?.id) { mutableStateOf(config?.pixCidade.orEmpty()) }
    var mensalidade by remember(config?.id) { mutableStateOf(emReais(config?.mensalidadeCentavos ?: 0)) }
    var diaria by remember(config?.id) { mutableStateOf(emReais(config?.diariaCentavos ?: 0)) }

    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = VoleiColors.Cartao,
        titleContentColor = VoleiColors.TextoPrimario,
        title = { Text("Chave Pix e valores", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(chave, "Chave Pix", { chave = it }, Modifier.fillMaxWidth())
                CampoTexto(nome, "Nome do recebedor", { nome = it }, Modifier.fillMaxWidth())
                CampoTexto(cidade, "Cidade", { cidade = it }, Modifier.fillMaxWidth())
                CampoTexto(mensalidade, "Mensalidade (R$)", { mensalidade = it }, Modifier.fillMaxWidth())
                CampoTexto(diaria, "Diária (R$)", { diaria = it }, Modifier.fillMaxWidth())
                Text(
                    "Os valores viram centavos inteiros no banco, então não há arredondamento.",
                    color = VoleiColors.TextoTerciario,
                    fontSize = 11.sp,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSalvar(chave, nome, cidade, emCentavos(mensalidade), emCentavos(diaria))
                },
            ) {
                Text("Salvar", color = VoleiColors.VerdeClaro)
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Cancelar", color = VoleiColors.TextoSecundario) }
        },
    )
}

private fun emReais(centavos: Int): String =
    if (centavos == 0) "" else "%d,%02d".format(centavos / 100, centavos % 100)

internal fun emCentavos(texto: String): Int {
    val limpo = texto.replace(Regex("[^0-9,.]"), "").replace(',', '.')
    val valor = limpo.toDoubleOrNull() ?: return 0
    return Math.round(valor * 100).toInt()
}
