package com.unidospelovolei.ui.players

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.Genero
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.PlayerPerformance
import com.unidospelovolei.ui.components.CampoTexto
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.SeletorGenero
import com.unidospelovolei.ui.components.SeletorNivel
import com.unidospelovolei.ui.components.Selo
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun PlayersScreen(
    estado: PlayersUiState,
    onVoltar: () -> Unit,
    onCriar: (String, Int, Genero, Boolean) -> Unit,
    onSalvar: (Player) -> Unit,
    onExcluir: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editando by remember { mutableStateOf<Player?>(null) }
    var criando by remember { mutableStateOf(false) }

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
                        "${estado.ativos} ativos de ${estado.jogadores.size} • " +
                            "${estado.homensAtivos}H / ${estado.mulheresAtivas}M",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                    )
                }
            }

            if (estado.jogadores.isEmpty() && !estado.carregando) {
                EstadoVazio(
                    titulo = "Nenhum jogador",
                    descricao = "Toque no botão + para cadastrar o primeiro jogador do grupo.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(estado.jogadores, key = { it.id }) { jogador ->
                    LinhaJogador(
                        jogador = jogador,
                        desempenho = estado.desempenho[jogador.id],
                        onClick = { editando = jogador },
                    )
                }
            }
        }
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

@Composable
private fun LinhaJogador(
    jogador: Player,
    desempenho: PlayerPerformance?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (jogador.ativo) VoleiColors.Verde else VoleiColors.CartaoInterno),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    jogador.skillLevel.toString(),
                    color = if (jogador.ativo) Color.White else VoleiColors.TextoTerciario,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                Text(
                    if (jogador.ativo) "Nível ${jogador.skillLevel}" else "Inativo",
                    color = VoleiColors.TextoTerciario,
                    fontSize = 11.sp,
                )
                if (desempenho != null && desempenho.jogos > 0) {
                    Text(
                        "${desempenho.dias} dias • ${desempenho.jogos} jogos • " +
                            "${desempenho.vitorias}V ${desempenho.derrotas}D • " +
                            "saldo ${desempenho.saldoPontos.comSinal()}",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

private fun Int.comSinal(): String = if (this > 0) "+$this" else toString()

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
    val valido = nome.isNotBlank()

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
                SeletorNivel(nivel = nivel, onMudar = { nivel = it })
                Text("Gênero", color = VoleiColors.TextoSecundario, fontSize = 12.sp)
                SeletorGenero(genero = genero, onMudar = { genero = it })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Ativo", color = VoleiColors.TextoSecundario, fontSize = 13.sp)
                    Switch(
                        checked = ativo,
                        onCheckedChange = { ativo = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = VoleiColors.Verde),
                    )
                }
                if (jogador != null) {
                    TextButton(onClick = { onExcluir(jogador.id) }) {
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
