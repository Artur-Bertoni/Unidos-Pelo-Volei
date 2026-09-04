package com.unidospelovolei.ui.membro

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.StatusPresenca
import com.unidospelovolei.ui.evolucao.CartaoDaEvolucao
import com.unidospelovolei.ui.evolucao.EvolucaoUiState
import com.unidospelovolei.ui.financeiro.CartaoDoExtrato
import com.unidospelovolei.ui.financeiro.FinanceiroUiState
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.CampoBusca
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.RotuloPequeno
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun EuScreen(
    estado: MembroUiState,
    financeiro: FinanceiroUiState,
    evolucao: EvolucaoUiState,
    onBuscar: (String) -> Unit,
    onPedirVinculo: (String) -> Unit,
    onCancelarPedido: () -> Unit,
    onEditarFicha: () -> Unit,
    onAbrirAprovacoes: () -> Unit,
    onResponderChamada: (StatusPresenca) -> Unit,
    onAbrirPainelFinanceiro: () -> Unit,
    onAbrirAvaliacao: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (estado.carregando) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VoleiColors.Verde)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (estado.isAdmin) {
            item {
                CartaoDaFila(quantidade = estado.fila.size, onAbrir = onAbrirAprovacoes)
            }
        }

        when {
            estado.aguardando ->
                item {
                    CartaoAguardando(
                        nomeEscolhido = estado.fila.firstOrNull { it.pedido.id == estado.meuPedido?.id }?.jogador?.nome,
                        salvando = estado.salvando,
                        onCancelar = onCancelarPedido,
                    )
                }

            estado.meuJogador != null -> {
                item {
                    CartaoDaChamada(
                        dataDoSabado = estado.dataDoSabado,
                        jogoHora = estado.config?.jogoHora,
                        jogoLocal = estado.config?.jogoLocal,
                        minhaResposta = estado.minhaResposta,
                        salvando = estado.salvando,
                        onResponder = onResponderChamada,
                    )
                }
                item {
                    MinhaFicha(
                        estado = estado,
                        onEditar = onEditarFicha,
                    )
                }
                item {
                    CartaoDoExtrato(
                        estado = financeiro,
                        onAbrirPainel = onAbrirPainelFinanceiro,
                        isAdmin = estado.isAdmin,
                    )
                }
                item {
                    CartaoDaEvolucao(
                        estado = evolucao,
                        onAvaliar = onAbrirAvaliacao,
                    )
                }
            }

            estado.precisaEscolher -> {
                item {
                    ConviteParaSeIdentificar(recusado = estado.recusado)
                }
                item {
                    CampoBusca(
                        valor = estado.busca,
                        onMudar = onBuscar,
                        dica = "Buscar meu nome",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (estado.candidatos.isEmpty()) {
                    item {
                        EstadoVazio(
                            titulo = "Nenhum nome disponível",
                            descricao = "Todos os jogadores da lista já têm dono.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    items(estado.candidatos, key = { it.id }) { jogador ->
                        LinhaDeCandidato(
                            jogador = jogador,
                            habilitado = !estado.salvando,
                            onEscolher = { onPedirVinculo(jogador.id) },
                        )
                    }
                }
                item { RecadoDaDiretoria() }
            }
        }
    }
}

@Composable
private fun CartaoDaFila(
    quantidade: Int,
    onAbrir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier.fillMaxWidth().clickable(onClick = onAbrir)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.HowToReg,
                contentDescription = null,
                tint = VoleiColors.Dourado,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text =
                        when (quantidade) {
                            0 -> "Contas e jogadores"
                            1 -> "1 pedido aguardando"
                            else -> "$quantidade pedidos aguardando"
                        },
                    color = VoleiColors.TextoPrimario,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text =
                        if (quantidade == 0) {
                            "Ligue um jogador a uma conta na mão, ou desfaça um vínculo"
                        } else {
                            "Confirme quem é quem para liberar o acesso"
                        },
                    color = VoleiColors.TextoSecundario,
                    fontSize = 12.sp,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = VoleiColors.TextoTerciario,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun RecadoDaDiretoria(modifier: Modifier = Modifier) {
    Cartao(modifier = modifier.fillMaxWidth(), cor = VoleiColors.CartaoInterno) {
        Text(
            text = "Não encontrou seu nome na lista? Entre em contato com a diretoria para adicioná-lo aqui!",
            color = VoleiColors.TextoSecundario,
            fontSize = 13.sp,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Composable
private fun ConviteParaSeIdentificar(
    recusado: Boolean,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Quem é você?",
                color = VoleiColors.TextoPrimario,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text =
                    if (recusado) {
                        "A diretoria não confirmou o pedido anterior. " +
                            "Escolha o seu nome de novo ou fale com quem organiza."
                    } else {
                        "Ache o seu nome na lista do grupo. A diretoria confirma, " +
                            "e a partir daí a sua ficha e o seu histórico ficam aqui."
                    },
                color = VoleiColors.TextoSecundario,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun LinhaDeCandidato(
    jogador: Player,
    habilitado: Boolean,
    onEscolher: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier.fillMaxWidth(), cor = VoleiColors.CartaoInterno) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = jogador.nome,
                    color = VoleiColors.TextoPrimario,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = jogador.genero.rotulo,
                    color = VoleiColors.TextoTerciario,
                    fontSize = 11.sp,
                )
            }
            Button(
                onClick = onEscolher,
                enabled = habilitado,
                shape = RoundedCornerShape(10.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = VoleiColors.Verde,
                        contentColor = Color.White,
                        disabledContainerColor = VoleiColors.Borda,
                        disabledContentColor = VoleiColors.TextoTerciario,
                    ),
            ) {
                Text("Sou eu", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CartaoAguardando(
    nomeEscolhido: String?,
    salvando: Boolean,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = VoleiColors.Dourado,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Aguardando a diretoria",
                    color = VoleiColors.TextoPrimario,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text =
                    if (nomeEscolhido != null) {
                        "Você pediu para ser $nomeEscolhido. Assim que alguém da diretoria confirmar, " +
                            "a sua ficha aparece aqui."
                    } else {
                        "Assim que alguém da diretoria confirmar, a sua ficha aparece aqui."
                    },
                color = VoleiColors.TextoSecundario,
                fontSize = 13.sp,
            )
            TextButton(onClick = onCancelar, enabled = !salvando) {
                Text("Escolher outro nome", color = VoleiColors.Azul, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun MinhaFicha(
    estado: MembroUiState,
    onEditar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val jogador = estado.meuJogador ?: return

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Cartao(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(VoleiColors.SeloFaseFundo),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = jogador.nome.take(2).uppercase(),
                            color = VoleiColors.SeloFaseTexto,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = jogador.nome,
                            color = VoleiColors.TextoPrimario,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text =
                                listOfNotNull(
                                    estado.profile?.papel?.rotulo,
                                    jogador.genero.rotulo,
                                ).joinToString(" · "),
                            color = VoleiColors.TextoSecundario,
                            fontSize = 12.sp,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Editar minha ficha",
                        tint = VoleiColors.Azul,
                        modifier = Modifier.size(20.dp).clickable(onClick = onEditar),
                    )
                }

                LinhaDeDado(
                    icone = Icons.Filled.Payments,
                    rotulo = "Como eu pago",
                    valor = jogador.regime.rotulo,
                )
                LinhaDeDado(
                    icone = Icons.Filled.Cake,
                    rotulo = "Aniversário",
                    valor = jogador.aniversario ?: "Não informado",
                )
                LinhaDeDado(
                    icone = Icons.Filled.Phone,
                    rotulo = "Telefone",
                    valor = estado.meuContato?.telefone ?: "Não informado",
                )
                LinhaDeDado(
                    icone = Icons.Filled.HowToReg,
                    rotulo = "No grupo desde",
                    valor = jogador.entrouEm ?: "Não informado",
                )
            }
        }

        val desempenho = estado.meuDesempenho
        if (desempenho != null && desempenho.dias > 0) {
            Cartao(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RotuloPequeno("Meu histórico")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Numero("Sábados", desempenho.dias)
                        Numero("Jogos", desempenho.jogos)
                        Numero("Vitórias", desempenho.vitorias)
                        Numero("Saldo", desempenho.saldoPontos)
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaDeDado(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    rotulo: String,
    valor: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = VoleiColors.TextoTerciario,
            modifier = Modifier.size(16.dp),
        )
        Text(rotulo, color = VoleiColors.TextoSecundario, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(valor, color = VoleiColors.TextoPrimario, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Numero(
    rotulo: String,
    valor: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = valor.toString(),
            color = VoleiColors.TextoPrimario,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        RotuloPequeno(rotulo)
    }
}
