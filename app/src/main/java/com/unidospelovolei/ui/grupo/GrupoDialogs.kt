package com.unidospelovolei.ui.grupo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.data.Endereco
import com.unidospelovolei.domain.model.EMOJIS_DE_REACAO
import com.unidospelovolei.domain.model.EMOJI_PADRAO
import com.unidospelovolei.domain.model.Evento
import com.unidospelovolei.domain.model.Pagina
import com.unidospelovolei.domain.model.TipoEvento
import com.unidospelovolei.ui.components.CampoTexto
import com.unidospelovolei.ui.components.RotuloPequeno
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun PostDialog(
    salvando: Boolean,
    onSalvar: (String, String, Boolean, ImagemEscolhida?, String) -> Unit,
    onFechar: () -> Unit,
) {
    var titulo by remember { mutableStateOf("") }
    var corpo by remember { mutableStateOf("") }
    var fixado by remember { mutableStateOf(false) }
    var emoji by remember { mutableStateOf(EMOJI_PADRAO) }
    var imagem by remember { mutableStateOf<ImagemEscolhida?>(null) }
    var nomeDaImagem by remember { mutableStateOf<String?>(null) }
    var falhaNaImagem by remember { mutableStateOf<String?>(null) }

    val contexto = LocalContext.current
    val escolherImagem =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            val lida = runCatching { lerImagem(contexto.contentResolver, uri) }.getOrNull()
            if (lida == null) {
                falhaNaImagem = "Não deu para ler essa imagem. Tente outra."
            } else {
                falhaNaImagem = null
                imagem = lida
                nomeDaImagem = "Imagem de ${lida.bytes.size / 1024} KB"
            }
        }

    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = VoleiColors.Cartao,
        titleContentColor = VoleiColors.TextoPrimario,
        title = { Text("Publicar no mural", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CampoTexto(titulo, "Título", { titulo = it }, Modifier.fillMaxWidth())
                CampoLongo(corpo, "Recado", { corpo = it })

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RotuloPequeno("Imagem")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = {
                                escolherImagem.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        ) {
                            Text(
                                if (imagem == null) "Anexar imagem" else "Trocar imagem",
                                color = VoleiColors.Azul,
                                fontSize = 13.sp,
                            )
                        }
                        if (imagem != null) {
                            TextButton(
                                onClick = {
                                    imagem = null
                                    nomeDaImagem = null
                                },
                            ) {
                                Text("Remover", color = VoleiColors.Vermelho, fontSize = 13.sp)
                            }
                        }
                    }
                    (falhaNaImagem ?: nomeDaImagem)?.let { aviso ->
                        Text(
                            text = aviso,
                            color = if (falhaNaImagem != null) VoleiColors.Vermelho else VoleiColors.TextoTerciario,
                            fontSize = 11.sp,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RotuloPequeno("Reação padrão")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        EMOJIS_DE_REACAO.forEach { opcao ->
                            BotaoDeEmoji(
                                emoji = opcao,
                                selecionado = opcao == emoji,
                                onClick = { emoji = opcao },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Text(
                        text = "É o emoji que o grupo toca para reagir a este recado.",
                        color = VoleiColors.TextoTerciario,
                        fontSize = 11.sp,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Fixar no topo",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
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
                onClick = { onSalvar(titulo, corpo, fixado, imagem, emoji) },
                enabled = titulo.isNotBlank() && !salvando,
            ) {
                Text(
                    if (salvando && imagem != null) "Enviando…" else "Publicar",
                    color = if (titulo.isNotBlank()) VoleiColors.VerdeClaro else VoleiColors.TextoTerciario,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Cancelar", color = VoleiColors.TextoSecundario) }
        },
    )
}

private fun lerImagem(
    resolver: android.content.ContentResolver,
    uri: Uri,
): ImagemEscolhida {
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Sem acesso à imagem.")
    val extensao =
        when (resolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
    return ImagemEscolhida(bytes = bytes, extensao = extensao)
}

@Composable
fun EventoDialog(
    evento: Evento?,
    salvando: Boolean,
    sugestoes: List<Endereco>,
    onBuscarEndereco: (String) -> Unit,
    onLimparSugestoes: () -> Unit,
    onSalvar: (String?, String, String?, TipoEvento, String, String?) -> Unit,
    onFechar: () -> Unit,
) {
    var titulo by remember(evento?.id) { mutableStateOf(evento?.titulo.orEmpty()) }
    var descricao by remember(evento?.id) { mutableStateOf(evento?.descricao.orEmpty()) }
    var data by remember(evento?.id) { mutableStateOf(paraDiaMesAno(evento?.inicio)) }
    var hora by remember(evento?.id) { mutableStateOf(evento?.inicio?.substring(11, 16) ?: "19:00") }
    var local by remember(evento?.id) { mutableStateOf(evento?.local.orEmpty()) }
    var tipo by remember(evento?.id) { mutableStateOf(evento?.tipo ?: TipoEvento.JOGO) }
    var buscandoLocal by remember(evento?.id) { mutableStateOf(false) }

    LaunchedEffect(Unit) { onLimparSugestoes() }

    val dataIso = paraIso(data)
    val horaValida = Regex("^\\d{2}:\\d{2}$").matches(hora)
    val podeSalvar = titulo.isNotBlank() && dataIso != null && horaValida && !salvando

    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = VoleiColors.Cartao,
        titleContentColor = VoleiColors.TextoPrimario,
        title = { Text(if (evento == null) "Novo evento" else "Editar evento", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CampoTexto(titulo, "Título", { titulo = it }, Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoTexto(
                        valor = data,
                        rotulo = "Data (dd/mm/aaaa)",
                        onMudar = { data = mascaraDeData(it) },
                        modifier = Modifier.weight(1.6f),
                    )
                    CampoTexto(
                        valor = hora,
                        rotulo = "Hora",
                        onMudar = { hora = mascaraDeHora(it) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (data.isNotBlank() && dataIso == null) {
                    Text("Data inválida. Use dd/mm/aaaa.", color = VoleiColors.Vermelho, fontSize = 11.sp)
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CampoTexto(
                        valor = local,
                        rotulo = "Local",
                        onMudar = {
                            local = it
                            buscandoLocal = true
                            onBuscarEndereco(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (buscandoLocal && sugestoes.isNotEmpty()) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .verticalScroll(rememberScrollState())
                                    .border(1.dp, VoleiColors.Borda, RoundedCornerShape(10.dp)),
                        ) {
                            sugestoes.forEach { sugestao ->
                                Text(
                                    text = sugestao.descricao,
                                    color = VoleiColors.TextoSecundario,
                                    fontSize = 12.sp,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                local = sugestao.descricao
                                                buscandoLocal = false
                                                onLimparSugestoes()
                                            }.padding(horizontal = 10.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = "Digite o endereço e toque em uma sugestão do mapa.",
                        color = VoleiColors.TextoTerciario,
                        fontSize = 11.sp,
                    )
                }

                CampoLongo(descricao, "Descrição", { descricao = it })

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RotuloPequeno("Tipo")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TipoEvento.entries.forEach { opcao ->
                            ChipDeTipo(
                                rotulo = opcao.rotulo,
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
                onClick = {
                    onLimparSugestoes()
                    onSalvar(evento?.id, titulo, descricao, tipo, "${dataIso}T$hora:00Z", local)
                },
                enabled = podeSalvar,
            ) {
                Text("Salvar", color = if (podeSalvar) VoleiColors.VerdeClaro else VoleiColors.TextoTerciario)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onLimparSugestoes()
                    onFechar()
                },
            ) { Text("Cancelar", color = VoleiColors.TextoSecundario) }
        },
    )
}

internal fun mascaraDeData(entrada: String): String {
    val digitos = entrada.filter(Char::isDigit).take(8)
    return buildString {
        digitos.forEachIndexed { indice, caractere ->
            if (indice == 2 || indice == 4) append('/')
            append(caractere)
        }
    }
}

internal fun mascaraDeHora(entrada: String): String {
    val digitos = entrada.filter(Char::isDigit).take(4)
    return buildString {
        digitos.forEachIndexed { indice, caractere ->
            if (indice == 2) append(':')
            append(caractere)
        }
    }
}

internal fun paraIso(diaMesAno: String): String? {
    val partes = diaMesAno.split("/")
    if (partes.size != 3) return null
    val dia = partes[0].toIntOrNull() ?: return null
    val mes = partes[1].toIntOrNull() ?: return null
    val ano = partes[2].toIntOrNull() ?: return null
    if (partes[2].length != 4 || dia !in 1..31 || mes !in 1..12) return null
    return runCatching { java.time.LocalDate.of(ano, mes, dia) }
        .getOrNull()
        ?.toString()
}

internal fun paraDiaMesAno(iso: String?): String {
    val data = iso?.take(10)?.split("-") ?: return ""
    if (data.size != 3) return ""
    return "${data[2]}/${data[1]}/${data[0]}"
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
private fun BotaoDeEmoji(
    emoji: String,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .border(
                    width = if (selecionado) 2.dp else 1.dp,
                    color = if (selecionado) VoleiColors.Verde else VoleiColors.Borda,
                    shape = RoundedCornerShape(8.dp),
                ).background(
                    color = if (selecionado) VoleiColors.SeloVitoriaFundo else VoleiColors.CartaoInterno,
                    shape = RoundedCornerShape(8.dp),
                ).clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 17.sp)
    }
}

@Composable
private fun ChipDeTipo(
    rotulo: String,
    selecionado: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .border(
                    width = 1.dp,
                    color = if (selecionado) VoleiColors.Verde else VoleiColors.Borda,
                    shape = RoundedCornerShape(8.dp),
                ).background(
                    color = if (selecionado) VoleiColors.SeloVitoriaFundo else VoleiColors.CartaoInterno,
                    shape = RoundedCornerShape(8.dp),
                ).clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(
            rotulo,
            color = if (selecionado) VoleiColors.VerdeClaro else VoleiColors.TextoSecundario,
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
