package com.unidospelovolei.ui.membro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.PlayerPerformance
import com.unidospelovolei.domain.model.StatusPresenca
import com.unidospelovolei.ui.components.CampoBusca
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.RotuloPequeno
import com.unidospelovolei.ui.theme.VoleiColors

enum class DestinoDaEu {
    GRUPOS,
    CONTA,
    FINANCEIRO,
    HISTORICO,
    AVALIACAO,
    CONFIGURACOES,
}

@Composable
fun EuScreen(
    estado: MembroUiState,
    nomeDoGrupo: String,
    quantosGrupos: Int,
    avaliacoesPendentes: Int,
    onBuscar: (String) -> Unit,
    onPedirVinculo: (String) -> Unit,
    onCancelarPedido: () -> Unit,
    onResponderChamada: (StatusPresenca) -> Unit,
    onAbrir: (DestinoDaEu) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (estado.carregando) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VoleiColors.Verde)
        }
        return
    }

    val temJogador = estado.meuJogador != null

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (temJogador) {
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
        }

        if (estado.aguardando) {
            item {
                CartaoAguardando(
                    nomeEscolhido = estado.fila.firstOrNull { it.pedido.id == estado.meuPedido?.id }?.jogador?.nome,
                    salvando = estado.salvando,
                    onCancelar = onCancelarPedido,
                )
            }
        }

        if (estado.precisaEscolher) {
            item { ConviteParaSeIdentificar(recusado = estado.recusado) }
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

        item {
            GradeDaEu(
                nomeDoGrupo = nomeDoGrupo,
                quantosGrupos = quantosGrupos,
                temJogador = temJogador,
                souDiretoria = estado.isAdmin,
                pedidosPendentes = estado.fila.size,
                avaliacoesPendentes = avaliacoesPendentes,
                onAbrir = onAbrir,
            )
        }

        if (!temJogador) {
            item {
                Text(
                    "Ficha, financeiro, histórico e avaliação abrem depois que a diretoria ligar a " +
                        "sua conta a um jogador da lista.",
                    color = VoleiColors.TextoTerciario,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun GradeDaEu(
    nomeDoGrupo: String,
    quantosGrupos: Int,
    temJogador: Boolean,
    souDiretoria: Boolean,
    pedidosPendentes: Int,
    avaliacoesPendentes: Int,
    onAbrir: (DestinoDaEu) -> Unit,
) {
    val blocos =
        buildList {
            add(
                BlocoDaEu(
                    destino = DestinoDaEu.GRUPOS,
                    icone = Icons.Filled.Groups,
                    titulo = "Grupos",
                    subtitulo =
                        if (quantosGrupos <= 1) {
                            "$nomeDoGrupo · entrar em outro"
                        } else {
                            "$nomeDoGrupo · trocar entre $quantosGrupos"
                        },
                ),
            )
            add(
                BlocoDaEu(
                    destino = DestinoDaEu.CONTA,
                    icone = Icons.Filled.Person,
                    titulo = "Detalhes da conta",
                    subtitulo = "Pagamento, aniversário e telefone",
                    habilitado = temJogador,
                ),
            )
            add(
                BlocoDaEu(
                    destino = DestinoDaEu.FINANCEIRO,
                    icone = Icons.Filled.AccountBalanceWallet,
                    titulo = "Financeiro",
                    subtitulo = "O que você já pagou e o que falta",
                    habilitado = temJogador,
                ),
            )
            add(
                BlocoDaEu(
                    destino = DestinoDaEu.HISTORICO,
                    icone = Icons.Filled.Insights,
                    titulo = "Histórico",
                    subtitulo = "Sábados, vitórias e a sua evolução",
                    habilitado = temJogador,
                ),
            )
            add(
                BlocoDaEu(
                    destino = DestinoDaEu.AVALIACAO,
                    icone = Icons.Filled.Star,
                    titulo = "Avaliar colegas",
                    subtitulo =
                        if (avaliacoesPendentes > 0) {
                            "$avaliacoesPendentes esperando a sua nota"
                        } else {
                            "Dê nota a quem jogou com você"
                        },
                    habilitado = temJogador,
                    selo = avaliacoesPendentes.takeIf { it > 0 },
                ),
            )
            if (souDiretoria) {
                add(
                    BlocoDaEu(
                        destino = DestinoDaEu.CONFIGURACOES,
                        icone = Icons.Filled.Settings,
                        titulo = "Configurações",
                        subtitulo = "Só a diretoria vê e mexe",
                        selo = pedidosPendentes.takeIf { it > 0 },
                    ),
                )
            }
        }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocos.chunked(2).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                linha.forEach { bloco ->
                    CartaoDoBloco(bloco = bloco, onClick = { onAbrir(bloco.destino) }, modifier = Modifier.weight(1f))
                }
                if (linha.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private data class BlocoDaEu(
    val destino: DestinoDaEu,
    val icone: ImageVector,
    val titulo: String,
    val subtitulo: String,
    val habilitado: Boolean = true,
    val selo: Int? = null,
)

@Composable
private fun CartaoDoBloco(
    bloco: BlocoDaEu,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier.height(122.dp).clickable(enabled = bloco.habilitado, onClick = onClick)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box {
                Icon(
                    imageVector = bloco.icone,
                    contentDescription = null,
                    tint = if (bloco.habilitado) VoleiColors.VerdeClaro else VoleiColors.TextoTerciario,
                    modifier = Modifier.size(26.dp),
                )
                if (bloco.selo != null) {
                    Text(
                        text = bloco.selo.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-6).dp)
                                .clip(CircleShape)
                                .background(VoleiColors.Vermelho)
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
            Text(
                bloco.titulo,
                color = if (bloco.habilitado) VoleiColors.TextoPrimario else VoleiColors.TextoTerciario,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                bloco.subtitulo,
                color = VoleiColors.TextoSecundario,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun CabecalhoDaSubtela(
    titulo: String,
    subtitulo: String,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
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
            Text(titulo, color = VoleiColors.TextoPrimario, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(subtitulo, color = VoleiColors.TextoSecundario, fontSize = 12.sp)
        }
    }
}

@Composable
fun ContaScreen(
    estado: MembroUiState,
    onEditarFicha: () -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize(), containerColor = VoleiColors.Fundo) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CabecalhoDaSubtela(
                titulo = "Detalhes da conta",
                subtitulo = "Como você aparece para o grupo",
                onVoltar = onVoltar,
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { MinhaFicha(estado = estado, onEditar = onEditarFicha) }
            }
        }
    }
}

@Composable
fun HistoricoScreen(
    desempenho: PlayerPerformance?,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    conteudo: @Composable () -> Unit,
) {
    Scaffold(modifier = modifier.fillMaxSize(), containerColor = VoleiColors.Fundo) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CabecalhoDaSubtela(
                titulo = "Histórico",
                subtitulo = "O que você jogou e como a sua nota andou",
                onVoltar = onVoltar,
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    if (desempenho != null && desempenho.dias > 0) {
                        CartaoDoDesempenho(desempenho)
                    } else {
                        EstadoVazio(
                            titulo = "Nenhum sábado ainda",
                            descricao = "Os números aparecem depois do primeiro dia encerrado com você em quadra.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item { conteudo() }
            }
        }
    }
}

@Composable
private fun CartaoDoDesempenho(
    desempenho: PlayerPerformance,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier.fillMaxWidth()) {
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
