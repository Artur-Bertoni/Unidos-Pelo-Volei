package com.unidospelovolei.ui.configuracoes

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.ui.components.CampoTexto
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.membro.CabecalhoDaSubtela
import com.unidospelovolei.ui.theme.VoleiColors

enum class AcaoDaDiretoria {
    APROVACOES,
    VINCULOS,
    CHAVES,
    MEMBROS,
    IDENTIDADE,
    PAINEL_FINANCEIRO,
    CONFIG_DO_JOGO,
}

@Composable
fun ConfiguracoesScreen(
    nomeDoGrupo: String,
    pedidosPendentes: Int,
    onVoltar: () -> Unit,
    onAbrir: (AcaoDaDiretoria) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize(), containerColor = VoleiColors.Fundo) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CabecalhoDaSubtela(
                titulo = "Configurações",
                subtitulo = "O que só a diretoria do $nomeDoGrupo mexe",
                onVoltar = onVoltar,
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Atalho(
                        icone = Icons.Filled.HowToReg,
                        titulo =
                            if (pedidosPendentes == 0) {
                                "Pedidos de vínculo"
                            } else {
                                "$pedidosPendentes " +
                                    if (pedidosPendentes == 1) "pedido aguardando" else "pedidos aguardando"
                            },
                        descricao = "Confirme quem é quem para liberar o acesso ao grupo",
                        selo = pedidosPendentes.takeIf { it > 0 },
                        onClick = { onAbrir(AcaoDaDiretoria.APROVACOES) },
                    )
                }
                item {
                    Atalho(
                        icone = Icons.Filled.Person,
                        titulo = "Contas e jogadores",
                        descricao = "Ligue um jogador a uma conta na mão, ou desfaça um vínculo",
                        onClick = { onAbrir(AcaoDaDiretoria.VINCULOS) },
                    )
                }
                item {
                    Atalho(
                        icone = Icons.Filled.Key,
                        titulo = "Chaves de acesso",
                        descricao = "Crie e revogue os códigos que liberam a entrada no grupo",
                        onClick = { onAbrir(AcaoDaDiretoria.CHAVES) },
                    )
                }
                item {
                    Atalho(
                        icone = Icons.Filled.Groups,
                        titulo = "Membros do grupo",
                        descricao = "Promova alguém à diretoria ou tire quem saiu",
                        onClick = { onAbrir(AcaoDaDiretoria.MEMBROS) },
                    )
                }
                item {
                    Atalho(
                        icone = Icons.Filled.Edit,
                        titulo = "Nome, cidade e logo",
                        descricao = "Troque como o $nomeDoGrupo aparece no app",
                        onClick = { onAbrir(AcaoDaDiretoria.IDENTIDADE) },
                    )
                }
                item {
                    Atalho(
                        icone = Icons.Filled.AccountBalanceWallet,
                        titulo = "Painel financeiro",
                        descricao = "Quem pagou, quem deve, e gerar mensalidade ou diária",
                        onClick = { onAbrir(AcaoDaDiretoria.PAINEL_FINANCEIRO) },
                    )
                }
                item {
                    Atalho(
                        icone = Icons.Filled.Schedule,
                        titulo = "Horário e local do sábado",
                        descricao = "O que aparece no cartão de presença de todo mundo",
                        onClick = { onAbrir(AcaoDaDiretoria.CONFIG_DO_JOGO) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Atalho(
    icone: ImageVector,
    titulo: String,
    descricao: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selo: Int? = null,
) {
    Cartao(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icone, contentDescription = null, tint = VoleiColors.Azul, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, color = VoleiColors.TextoPrimario, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(descricao, color = VoleiColors.TextoSecundario, fontSize = 12.sp)
            }
            if (selo != null) {
                Text(
                    selo.toString(),
                    color = VoleiColors.Vermelho,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("›", color = VoleiColors.TextoSecundario, fontSize = 18.sp)
        }
    }
}

private fun horaValida(valor: String): Boolean = Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(valor)

@Composable
fun ConfigDoJogoDialog(
    jogoHora: String?,
    jogoLocal: String?,
    salvando: Boolean,
    onSalvar: (String, String?) -> Unit,
    onFechar: () -> Unit,
) {
    var hora by remember { mutableStateOf(jogoHora ?: "09:00") }
    var local by remember { mutableStateOf(jogoLocal.orEmpty()) }
    val valido = horaValida(hora) && !salvando

    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = VoleiColors.Cartao,
        title = { Text("Horário e local do sábado", color = VoleiColors.TextoPrimario) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(
                    valor = hora,
                    rotulo = "Hora (HH:MM)",
                    onMudar = { hora = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!horaValida(hora)) {
                    Text(
                        "Use o formato 24 horas, como 09:00 ou 14:30.",
                        color = VoleiColors.Vermelho,
                        fontSize = 11.sp,
                    )
                }
                CampoTexto(
                    valor = local,
                    rotulo = "Local",
                    onMudar = { local = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Isso aparece no cartão de presença de todo mundo e no lembrete que sai antes do jogo.",
                    color = VoleiColors.TextoTerciario,
                    fontSize = 11.sp,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = { onSalvar(hora, local.trim().ifBlank { null }) },
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
