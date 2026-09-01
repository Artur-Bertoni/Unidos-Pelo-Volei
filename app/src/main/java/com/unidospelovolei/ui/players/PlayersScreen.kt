package com.unidospelovolei.ui.players

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.Genero
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.PlayerPerformance
import com.unidospelovolei.ui.components.CampoBusca
import com.unidospelovolei.ui.components.CampoTexto
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.DialogoConfirmacao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.Estrelas
import com.unidospelovolei.ui.components.RotuloPequeno
import com.unidospelovolei.ui.components.SeletorGenero
import com.unidospelovolei.ui.components.Selo
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun PlayersScreen(
    estado: PlayersUiState,
    onVoltar: () -> Unit,
    onBuscar: (String) -> Unit,
    onFiltrar: (FiltroPresenca) -> Unit,
    onAlternarPresenca: (Player) -> Unit,
    onMarcarTodosPresentes: () -> Unit,
    onLimparPresencas: () -> Unit,
    onCriar: (String, Int, Genero, Boolean) -> Unit,
    onSalvar: (Player) -> Unit,
    onExcluir: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editando by remember { mutableStateOf<Player?>(null) }
    var criando by remember { mutableStateOf(false) }
    var confirmandoLimpeza by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = VoleiColors.Fundo,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { criando = true },
                containerColor = VoleiColors.Verde,
                contentColor = Color.White,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Novo jogador")
            }
        },
    ) { padding ->
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
                        "Jogadores",
                        color = VoleiColors.TextoPrimario,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${estado.presentes} presentes de ${estado.total} • " +
                            "${estado.homensPresentes}H / ${estado.mulheresPresentes}M",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CampoBusca(
                    valor = estado.busca,
                    onMudar = onBuscar,
                    dica = "Buscar jogador pelo nome",
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FiltroPresenca.entries.forEach { opcao ->
                        ChipFiltro(
                            rotulo = opcao.rotulo,
                            selecionado = opcao == estado.filtro,
                            onClick = { onFiltrar(opcao) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotaoPresenca(
                        texto = "Marcar todos",
                        icone = Icons.Filled.DoneAll,
                        corDoIcone = VoleiColors.Verde,
                        habilitado = estado.presentes < estado.total,
                        onClick = onMarcarTodosPresentes,
                        modifier = Modifier.weight(1f),
                    )
                    BotaoPresenca(
                        texto = "Limpar presenças",
                        icone = Icons.Filled.PersonOff,
                        corDoIcone = VoleiColors.Vermelho,
                        habilitado = estado.presentes > 0,
                        onClick = { confirmandoLimpeza = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 20.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RotuloPequeno("Jogador", modifier = Modifier.weight(1f))
                RotuloPequeno("Presente?", cor = VoleiColors.TextoSecundario)
            }

            when {
                estado.total == 0 && !estado.carregando ->
                    EstadoVazio(
                        titulo = "Nenhum jogador",
                        descricao = "Toque no botão + para cadastrar o primeiro jogador do grupo.",
                        modifier = Modifier.fillMaxWidth(),
                    )

                estado.jogadores.isEmpty() && !estado.carregando ->
                    EstadoVazio(
                        titulo = "Nada nessa lista",
                        descricao = descricaoDaListaVazia(estado),
                        modifier = Modifier.fillMaxWidth(),
                    )
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(estado.jogadores, key = { it.id }) { jogador ->
                    LinhaJogador(
                        jogador = jogador,
                        desempenho = estado.desempenho[jogador.id],
                        onClick = { editando = jogador },
                        onAlternarPresenca = { onAlternarPresenca(jogador) },
                    )
                }
            }
        }
    }

    if (confirmandoLimpeza) {
        DialogoConfirmacao(
            titulo = "Limpar presenças?",
            mensagem =
                "Os ${estado.presentes} jogadores marcados como presentes ficam ausentes. " +
                    "Marque de novo quem chegou para jogar hoje.",
            textoConfirmar = "Limpar",
            onConfirmar = {
                onLimparPresencas()
                confirmandoLimpeza = false
            },
            onCancelar = { confirmandoLimpeza = false },
        )
    }

    if (criando) {
        PlayerEditorDialog(
            jogador = null,
            onSalvarNovo = { nome, nivel, genero, ativo ->
                onCriar(nome, nivel, genero, ativo)
                criando = false
            },
            onSalvarExistente = {},
            onExcluir = {},
            onFechar = { criando = false },
        )
    }

    editando?.let { jogador ->
        PlayerEditorDialog(
            jogador = jogador,
            onSalvarNovo = { _, _, _, _ -> },
            onSalvarExistente = {
                onSalvar(it)
                editando = null
            },
            onExcluir = {
                onExcluir(it)
                editando = null
            },
            onFechar = { editando = null },
        )
    }
}

private fun descricaoDaListaVazia(estado: PlayersUiState): String =
    when {
        estado.busca.isNotBlank() -> "Nenhum jogador do grupo combina com a busca."
        estado.filtro == FiltroPresenca.PRESENTES -> "Ninguém marcado como presente ainda."
        estado.filtro == FiltroPresenca.AUSENTES -> "Todo mundo está marcado como presente."
        else -> "Nenhum jogador para mostrar."
    }

@Composable
private fun ChipFiltro(
    rotulo: String,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selecionado,
        onClick = onClick,
        label = { Text(rotulo, fontSize = 12.sp) },
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = VoleiColors.CartaoInterno,
                labelColor = VoleiColors.TextoSecundario,
                selectedContainerColor = VoleiColors.Verde,
                selectedLabelColor = Color.White,
            ),
    )
}

@Composable
private fun BotaoPresenca(
    texto: String,
    icone: ImageVector,
    corDoIcone: Color,
    habilitado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = if (habilitado) corDoIcone else VoleiColors.TextoTerciario,
            modifier = Modifier.size(16.dp),
        )
        Text(
            "  $texto",
            color = if (habilitado) VoleiColors.TextoPrimario else VoleiColors.TextoTerciario,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun LinhaJogador(
    jogador: Player,
    desempenho: PlayerPerformance?,
    onClick: () -> Unit,
    onAlternarPresenca: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Cartao(
        modifier = modifier.fillMaxWidth(),
        cor = if (jogador.ativo) VoleiColors.Cartao else VoleiColors.CartaoInterno,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        jogador.nome,
                        color = if (jogador.ativo) VoleiColors.TextoPrimario else VoleiColors.TextoTerciario,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Selo(
                        texto = if (jogador.genero == Genero.MASCULINO) "H" else "M",
                        corTexto = VoleiColors.SeloFaseTexto,
                        corFundo = VoleiColors.SeloFaseFundo,
                    )
                }
                Estrelas(nivel = jogador.skillLevel)
                if (desempenho != null && desempenho.dias > 0) {
                    Text(
                        resumoDoDesempenho(desempenho),
                        color = VoleiColors.TextoSecundario,
                        fontSize = 11.sp,
                    )
                }
            }
            Switch(
                checked = jogador.ativo,
                onCheckedChange = { onAlternarPresenca() },
                colors =
                    SwitchDefaults.colors(
                        checkedTrackColor = VoleiColors.Verde,
                        checkedThumbColor = Color.White,
                        uncheckedTrackColor = VoleiColors.CartaoInterno,
                        uncheckedBorderColor = VoleiColors.Borda,
                    ),
            )
        }
    }
}

private fun Int.comSinal(): String = if (this > 0) "+$this" else toString()

private fun resumoDoDesempenho(desempenho: PlayerPerformance): String {
    val dias = "${desempenho.dias} " + if (desempenho.dias == 1) "dia" else "dias"
    return if (desempenho.jogos == 0) {
        "$dias de presença"
    } else {
        "$dias • ${desempenho.jogos} jogos • ${desempenho.vitorias}V ${desempenho.derrotas}D • " +
            "saldo ${desempenho.saldoPontos.comSinal()}"
    }
}

@Composable
private fun PlayerEditorDialog(
    jogador: Player?,
    onSalvarNovo: (String, Int, Genero, Boolean) -> Unit,
    onSalvarExistente: (Player) -> Unit,
    onExcluir: (String) -> Unit,
    onFechar: () -> Unit,
) {
    var nome by remember { mutableStateOf(jogador?.nome.orEmpty()) }
    var nivel by remember { mutableIntStateOf(jogador?.skillLevel ?: 3) }
    var genero by remember { mutableStateOf(jogador?.genero ?: Genero.MASCULINO) }
    var ativo by remember { mutableStateOf(jogador?.ativo ?: true) }
    var confirmandoExclusao by remember { mutableStateOf(false) }
    val valido = nome.isNotBlank()

    if (jogador != null && confirmandoExclusao) {
        DialogoConfirmacao(
            titulo = "Excluir ${jogador.nome}?",
            mensagem =
                "O jogador sai do grupo e o desempenho dele nos dias já encerrados é apagado. " +
                    "Para tirar alguém só de hoje, use o Presente? na listagem.",
            textoConfirmar = "Excluir",
            onConfirmar = { onExcluir(jogador.id) },
            onCancelar = { confirmandoExclusao = false },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = VoleiColors.Cartao,
        titleContentColor = VoleiColors.TextoPrimario,
        title = {
            Text(
                if (jogador == null) "Novo jogador" else "Editar jogador",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CampoTexto(
                    valor = nome,
                    rotulo = "Nome",
                    onMudar = { nome = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Nível de habilidade", color = VoleiColors.TextoSecundario, fontSize = 12.sp)
                Estrelas(
                    nivel = nivel,
                    tamanho = 38.dp,
                    espacamento = 8.dp,
                    onMudar = { nivel = it },
                )
                Text("Gênero", color = VoleiColors.TextoSecundario, fontSize = 12.sp)
                SeletorGenero(genero = genero, onMudar = { genero = it })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Presente hoje", color = VoleiColors.TextoSecundario, fontSize = 13.sp)
                    Switch(
                        checked = ativo,
                        onCheckedChange = { ativo = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = VoleiColors.Verde),
                    )
                }
                if (jogador != null) {
                    TextButton(onClick = { confirmandoExclusao = true }) {
                        Text("Excluir jogador", color = VoleiColors.Vermelho)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = {
                    if (jogador == null) {
                        onSalvarNovo(nome, nivel, genero, ativo)
                    } else {
                        onSalvarExistente(
                            jogador.copy(
                                nome = nome,
                                skillLevel = nivel,
                                genero = genero,
                                ativo = ativo,
                            ),
                        )
                    }
                },
            ) {
                Text("Salvar", color = if (valido) VoleiColors.Verde else VoleiColors.TextoTerciario)
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) {
                Text("Cancelar", color = VoleiColors.TextoSecundario)
            }
        },
    )
}
