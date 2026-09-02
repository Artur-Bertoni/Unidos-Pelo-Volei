package com.unidospelovolei.ui.grupo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.Evento
import com.unidospelovolei.domain.model.Pagina
import com.unidospelovolei.domain.model.TipoEvento
import com.unidospelovolei.ui.components.CampoTexto
import com.unidospelovolei.ui.components.RotuloPequeno
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun PostDialog(
    salvando: Boolean,
    onSalvar: (String, String, Boolean) -> Unit,
    onFechar: () -> Unit,
) {
    var titulo by remember { mutableStateOf("") }
    var corpo by remember { mutableStateOf("") }
    var fixado by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = VoleiColors.Cartao,
        titleContentColor = VoleiColors.TextoPrimario,
        title = { Text("Publicar no mural", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(titulo, "Título", { titulo = it }, Modifier.fillMaxWidth())
                CampoLongo(corpo, "Recado", { corpo = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fixar no topo", color = VoleiColors.TextoSecundario, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = fixado,
                        onCheckedChange = { fixado = it },
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = VoleiColors.Verde,
                                checkedTrackColor = VoleiColors.SeloVitoriaFundo,
                            ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSalvar(titulo, corpo, fixado) },
                enabled = titulo.isNotBlank() && !salvando,
            ) {
                Text(
                    "Publicar",
                    color = if (titulo.isNotBlank()) VoleiColors.VerdeClaro else VoleiColors.TextoTerciario,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Cancelar", color = VoleiColors.TextoSecundario) }
        },
    )
}

@Composable
fun EventoDialog(
    evento: Evento?,
    salvando: Boolean,
    onSalvar: (String?, String, String?, TipoEvento, String, String?) -> Unit,
    onFechar: () -> Unit,
) {
    var titulo by remember(evento?.id) { mutableStateOf(evento?.titulo.orEmpty()) }
    var descricao by remember(evento?.id) { mutableStateOf(evento?.descricao.orEmpty()) }
    var data by remember(evento?.id) { mutableStateOf(evento?.inicio?.take(10).orEmpty()) }
    var hora by remember(evento?.id) { mutableStateOf(evento?.inicio?.substring(11, 16) ?: "19:00") }
    var local by remember(evento?.id) { mutableStateOf(evento?.local.orEmpty()) }
    var tipo by remember(evento?.id) { mutableStateOf(evento?.tipo ?: TipoEvento.CONFRATERNIZACAO) }

    val dataValida = Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(data)
    val horaValida = Regex("^\\d{2}:\\d{2}$").matches(hora)
    val podeSalvar = titulo.isNotBlank() && dataValida && horaValida && !salvando

    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = VoleiColors.Cartao,
        titleContentColor = VoleiColors.TextoPrimario,
        title = { Text(if (evento == null) "Novo evento" else "Editar evento", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(titulo, "Título", { titulo = it }, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoTexto(data, "Data (AAAA-MM-DD)", { data = it }, Modifier.weight(1.6f))
                    CampoTexto(hora, "Hora", { hora = it }, Modifier.weight(1f))
                }
                CampoTexto(local, "Local", { local = it }, Modifier.fillMaxWidth())
                CampoLongo(descricao, "Descrição", { descricao = it })
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RotuloPequeno("Tipo")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TipoEvento.entries.forEach { opcao ->
                            ChipSimples(
                                rotulo = opcao.rotulo.take(6),
                                selecionado = opcao == tipo,
                                onClick = { tipo = opcao },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSalvar(evento?.id, titulo, descricao, tipo, "${data}T$hora:00Z", local) },
                enabled = podeSalvar,
            ) {
                Text("Salvar", color = if (podeSalvar) VoleiColors.VerdeClaro else VoleiColors.TextoTerciario)
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Cancelar", color = VoleiColors.TextoSecundario) }
        },
    )
}

@Composable
fun PaginaScreen(
    pagina: Pagina,
    isAdmin: Boolean,
    salvando: Boolean,
    onVoltar: () -> Unit,
    onSalvar: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editando by remember(pagina.id) { mutableStateOf(false) }
    var titulo by remember(pagina.id) { mutableStateOf(pagina.titulo) }
    var corpo by remember(pagina.id) { mutableStateOf(pagina.corpo) }

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
                        pagina.titulo,
                        color = VoleiColors.TextoPrimario,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(pagina.categoria.rotulo, color = VoleiColors.TextoSecundario, fontSize = 12.sp)
                }
                if (isAdmin) {
                    TextButton(
                        onClick = {
                            if (editando) {
                                onSalvar(titulo, corpo)
                                editando = false
                            } else {
                                editando = true
                            }
                        },
                        enabled = !salvando,
                    ) {
                        Text(if (editando) "Salvar" else "Editar", color = VoleiColors.Azul, fontSize = 13.sp)
                    }
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (editando) {
                    CampoTexto(titulo, "Título", { titulo = it }, Modifier.fillMaxWidth())
                    CampoLongo(corpo, "Conteúdo", { corpo = it }, linhas = 18)
                    Text(
                        "Use # no começo da linha para título e - para lista.",
                        color = VoleiColors.TextoTerciario,
                        fontSize = 11.sp,
                    )
                } else {
                    TextoFormatado(pagina.corpo)
                }
            }
        }
    }
}

@Composable
fun TextoFormatado(corpo: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        corpo.lines().forEach { linha ->
            val texto = linha.trim()
            when {
                texto.isEmpty() -> Unit

                texto.startsWith("# ") ->
                    Text(
                        texto.removePrefix("# "),
                        color = VoleiColors.TextoPrimario,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                texto.startsWith("- ") ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", color = VoleiColors.Verde, fontSize = 14.sp)
                        Text(
                            semNegrito(texto.removePrefix("- ")),
                            color = VoleiColors.TextoSecundario,
                            fontSize = 14.sp,
                        )
                    }

                else ->
                    Text(semNegrito(texto), color = VoleiColors.TextoSecundario, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ChipSimples(
    rotulo: String,
    selecionado: Boolean,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(
            rotulo,
            color = if (selecionado) VoleiColors.VerdeClaro else VoleiColors.TextoTerciario,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CampoLongo(
    valor: String,
    rotulo: String,
    onMudar: (String) -> Unit,
    linhas: Int = 5,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onMudar,
        label = { Text(rotulo) },
        minLines = linhas,
        modifier = Modifier.fillMaxWidth(),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = VoleiColors.TextoPrimario,
                unfocusedTextColor = VoleiColors.TextoPrimario,
                focusedBorderColor = VoleiColors.Verde,
                unfocusedBorderColor = VoleiColors.Borda,
                focusedLabelColor = VoleiColors.Verde,
                unfocusedLabelColor = VoleiColors.TextoSecundario,
                cursorColor = VoleiColors.Verde,
            ),
    )
}

private fun semNegrito(texto: String): String = texto.replace("**", "")
