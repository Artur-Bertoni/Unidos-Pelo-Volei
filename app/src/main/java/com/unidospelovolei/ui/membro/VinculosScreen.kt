package com.unidospelovolei.ui.membro

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.data.ContaDoGrupo
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.ui.components.CampoBusca
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.Selo
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun VinculosScreen(
    jogadores: List<Player>,
    contas: List<ContaDoGrupo>,
    carregandoContas: Boolean,
    salvando: Boolean,
    onVoltar: () -> Unit,
    onCarregarContas: () -> Unit,
    onVincular: (String, String) -> Unit,
    onDesvincular: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var busca by remember { mutableStateOf("") }
    var escolhendoPara by remember { mutableStateOf<Player?>(null) }
    var desvinculando by remember { mutableStateOf<Player?>(null) }

    LaunchedEffect(Unit) { onCarregarContas() }

    val porPerfil = contas.associateBy { it.id }
    val termo = busca.trim().lowercase()
    val visiveis =
        jogadores
            .filter { termo.isEmpty() || it.nome.lowercase().contains(termo) }
            .sortedWith(compareBy({ it.vinculado }, { it.nome.lowercase() }))

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
                        "Vincular jogadores",
                        color = VoleiColors.TextoPrimario,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${jogadores.count { it.vinculado }} de ${jogadores.size} com conta",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                    )
                }
                if (carregandoContas) {
                    CircularProgressIndicator(
                        color = VoleiColors.Verde,
                        modifier = Modifier.size(18.dp).padding(end = 8.dp),
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                CampoBusca(
                    valor = busca,
                    onMudar = { busca = it },
                    dica = "Buscar jogador pelo nome",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (visiveis.isEmpty()) {
                EstadoVazio(
                    titulo = "Nenhum jogador",
                    descricao = "Cadastre os jogadores do grupo antes de ligar as contas.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visiveis, key = { it.id }) { jogador ->
                    LinhaDeVinculo(
                        jogador = jogador,
                        conta = jogador.profileId?.let { porPerfil[it] },
                        habilitado = !salvando,
                        onVincular = { escolhendoPara = jogador },
                        onDesvincular = { desvinculando = jogador },
                    )
                }
            }
        }
    }

    escolhendoPara?.let { jogador ->
        EscolhaDeConta(
            jogador = jogador,
            contas = contas.filter { conta -> jogadores.none { it.profileId == conta.id } },
            carregando = carregandoContas,
            onEscolher = { conta ->
                onVincular(jogador.id, conta.id)
                escolhendoPara = null
            },
            onFechar = { escolhendoPara = null },
        )
    }

    desvinculando?.let { jogador ->
        AlertDialog(
            onDismissRequest = { desvinculando = null },
            containerColor = VoleiColors.Cartao,
            titleContentColor = VoleiColors.TextoPrimario,
            textContentColor = VoleiColors.TextoSecundario,
            title = { Text("Desvincular ${jogador.nome}?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "A conta perde o acesso à ficha, ao extrato e às avaliações desse jogador. " +
                        "Os dados do jogador continuam no grupo.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDesvincular(jogador.id)
                        desvinculando = null
                    },
                ) {
                    Text("Desvincular", color = VoleiColors.Vermelho)
                }
            },
            dismissButton = {
                TextButton(onClick = { desvinculando = null }) {
                    Text("Cancelar", color = VoleiColors.TextoSecundario)
                }
            },
        )
    }
}

@Composable
private fun LinhaDeVinculo(
    jogador: Player,
    conta: ContaDoGrupo?,
    habilitado: Boolean,
    onVincular: () -> Unit,
    onDesvincular: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    jogador.nome,
                    color = VoleiColors.TextoPrimario,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                        when {
                            !jogador.vinculado -> "Sem conta ligada"
                            conta != null -> conta.rotulo
                            else -> "Conta ligada"
                        },
                    color = if (jogador.vinculado) VoleiColors.VerdeClaro else VoleiColors.TextoTerciario,
                    fontSize = 12.sp,
                )
            }
            TextButton(
                onClick = if (jogador.vinculado) onDesvincular else onVincular,
                enabled = habilitado,
            ) {
                Text(
                    text = if (jogador.vinculado) "Desvincular" else "Vincular",
                    color = if (jogador.vinculado) VoleiColors.Vermelho else VoleiColors.Azul,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun EscolhaDeConta(
    jogador: Player,
    contas: List<ContaDoGrupo>,
    carregando: Boolean,
    onEscolher: (ContaDoGrupo) -> Unit,
    onFechar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = VoleiColors.Cartao,
        titleContentColor = VoleiColors.TextoPrimario,
        textContentColor = VoleiColors.TextoSecundario,
        title = { Text("Conta de ${jogador.nome}", fontWeight = FontWeight.Bold) },
        text = {
            when {
                carregando ->
                    Text("Carregando as contas que já entraram no app…")

                contas.isEmpty() ->
                    Text(
                        "Nenhuma conta livre. Quem for usar o app precisa entrar com o Google " +
                            "pelo menos uma vez para aparecer aqui.",
                    )

                else ->
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(contas, key = { it.id }) { conta ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onEscolher(conta) }
                                        .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        conta.rotulo,
                                        color = VoleiColors.TextoPrimario,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    conta.email?.takeIf { it != conta.rotulo }?.let { email ->
                                        Text(email, color = VoleiColors.TextoTerciario, fontSize = 11.sp)
                                    }
                                }
                                if (conta.isAdmin) {
                                    Selo("Diretoria", VoleiColors.SeloFaseTexto, VoleiColors.SeloFaseFundo)
                                }
                            }
                        }
                    }
            }
        },
        confirmButton = {
            TextButton(onClick = onFechar) {
                Text("Fechar", color = VoleiColors.TextoSecundario)
            }
        },
    )
}
