package com.unidospelovolei.ui.grupos

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.MeuGrupo
import com.unidospelovolei.domain.model.Papel
import com.unidospelovolei.ui.components.AvatarRemoto
import com.unidospelovolei.ui.components.CampoTexto
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.Selo
import com.unidospelovolei.ui.grupo.ImagemEscolhida
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun GruposScreen(
    estado: GruposUiState,
    onVoltar: (() -> Unit)?,
    onSelecionar: (MeuGrupo) -> Unit,
    onEntrarComChave: (String) -> Unit,
    onCriarGrupo: (String, String?) -> Unit,
    onSair: (MeuGrupo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var entrando by remember { mutableStateOf(false) }
    var criando by remember { mutableStateOf(false) }
    var saindoDe by remember { mutableStateOf<MeuGrupo?>(null) }

    Scaffold(modifier = modifier.fillMaxSize(), containerColor = VoleiColors.Fundo) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onVoltar != null) {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = VoleiColors.TextoPrimario,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = if (onVoltar == null) 12.dp else 0.dp)) {
                    Text(
                        "Meus grupos",
                        color = VoleiColors.TextoPrimario,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (estado.grupos.isEmpty()) {
                            "Entre com a chave que a diretoria passou"
                        } else {
                            "Toque para trocar o grupo que você está usando"
                        },
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                    )
                }
            }

            if (estado.grupos.isEmpty()) {
                EstadoVazio(
                    titulo = "Você ainda não está em nenhum grupo",
                    descricao =
                        "Cada grupo de jogo tem os seus jogadores, times, placar e financeiro. " +
                            "Peça a chave de acesso a quem organiza, ou crie o seu grupo agora.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(estado.grupos, key = { it.id }) { grupo ->
                    CartaoDoGrupo(
                        grupo = grupo,
                        atual = grupo.id == estado.grupoAtual?.id,
                        onSelecionar = { onSelecionar(grupo) },
                        onSair = { saindoDe = grupo },
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { entrando = true },
                    enabled = !estado.salvando,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VoleiColors.Verde),
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Entrar com uma chave", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { criando = true },
                    enabled = !estado.salvando,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = VoleiColors.TextoPrimario,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("  Criar um grupo", color = VoleiColors.TextoPrimario)
                }
            }
        }
    }

    if (entrando) {
        EntrarNoGrupoDialog(
            salvando = estado.salvando,
            onEntrar = {
                onEntrarComChave(it)
                entrando = false
            },
            onFechar = { entrando = false },
        )
    }

    if (criando) {
        CriarGrupoDialog(
            salvando = estado.salvando,
            onCriar = { nome, cidade ->
                onCriarGrupo(nome, cidade)
                criando = false
            },
            onFechar = { criando = false },
        )
    }

    saindoDe?.let { grupo ->
        AlertDialog(
            onDismissRequest = { saindoDe = null },
            containerColor = VoleiColors.Cartao,
            titleContentColor = VoleiColors.TextoPrimario,
            textContentColor = VoleiColors.TextoSecundario,
            title = { Text("Sair do ${grupo.nome}?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Você perde o acesso ao grupo neste aparelho e a sua ficha fica sem dono. " +
                        "Para voltar, vai precisar da chave de acesso de novo.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSair(grupo)
                        saindoDe = null
                    },
                ) {
                    Text("Sair do grupo", color = VoleiColors.Vermelho)
                }
            },
            dismissButton = {
                TextButton(onClick = { saindoDe = null }) {
                    Text("Cancelar", color = VoleiColors.TextoSecundario)
                }
            },
        )
    }
}

@Composable
private fun CartaoDoGrupo(
    grupo: MeuGrupo,
    atual: Boolean,
    onSelecionar: () -> Unit,
    onSair: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier.fillMaxWidth().clickable(onClick = onSelecionar)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = if (atual) "Grupo em uso" else null,
                tint = if (atual) VoleiColors.VerdeClaro else VoleiColors.Borda,
                modifier = Modifier.size(22.dp),
            )
            LogoDoGrupo(grupo = grupo, tamanho = 36.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    grupo.nome,
                    color = VoleiColors.TextoPrimario,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    listOfNotNull(
                        grupo.cidade,
                        if (grupo.souDiretoria) "Você é da diretoria" else "Você é atleta",
                    ).joinToString(" · "),
                    color = VoleiColors.TextoSecundario,
                    fontSize = 12.sp,
                )
            }
            if (atual) {
                Selo("Em uso", VoleiColors.VerdeClaro, VoleiColors.SeloVitoriaFundo)
            }
            IconButton(onClick = onSair, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Logout,
                    contentDescription = "Sair do grupo",
                    tint = VoleiColors.TextoTerciario,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun LogoDoGrupo(
    grupo: MeuGrupo,
    modifier: Modifier = Modifier,
    tamanho: Dp = 40.dp,
) {
    AvatarRemoto(
        url = grupo.logoUrl,
        iniciais = grupo.iniciais,
        descricao = null,
        modifier = modifier,
        tamanho = tamanho,
    )
}

@Composable
fun EditarGrupoDialog(
    grupo: MeuGrupo,
    salvando: Boolean,
    onSalvar: (String, String?, ImagemEscolhida?, Boolean) -> Unit,
    onFechar: () -> Unit,
) {
    var nome by remember { mutableStateOf(grupo.nome) }
    var cidade by remember { mutableStateOf(grupo.cidade.orEmpty()) }
    var logo by remember { mutableStateOf<ImagemEscolhida?>(null) }
    var removerLogo by remember { mutableStateOf(false) }
    var recado by remember { mutableStateOf<String?>(null) }
    var falhou by remember { mutableStateOf(false) }

    val contexto = LocalContext.current
    val escolherLogo =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            val lida = runCatching { lerLogo(contexto.contentResolver, uri) }.getOrNull()
            if (lida == null) {
                falhou = true
                recado = "Não deu para ler essa imagem. Tente outra."
            } else {
                falhou = false
                logo = lida
                removerLogo = false
                recado = "Logo nova de ${lida.bytes.size / 1024} KB"
            }
        }

    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = VoleiColors.Cartao,
        titleContentColor = VoleiColors.TextoPrimario,
        textContentColor = VoleiColors.TextoSecundario,
        title = { Text("Nome e logo do grupo", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LogoDoGrupo(
                        grupo = if (removerLogo) grupo.copy(logoUrl = null) else grupo,
                        tamanho = 52.dp,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(
                            onClick = {
                                escolherLogo.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                        ) {
                            Text(
                                if (grupo.logoUrl == null && logo == null) "Escolher logo" else "Trocar logo",
                                color = VoleiColors.Azul,
                                fontSize = 13.sp,
                            )
                        }
                        if (grupo.logoUrl != null || logo != null) {
                            TextButton(
                                onClick = {
                                    logo = null
                                    removerLogo = true
                                    recado = "A logo vai sair e voltam as iniciais."
                                    falhou = false
                                },
                            ) {
                                Text("Remover logo", color = VoleiColors.Vermelho, fontSize = 13.sp)
                            }
                        }
                    }
                }

                recado?.let { texto ->
                    Text(
                        text = texto,
                        color = if (falhou) VoleiColors.Vermelho else VoleiColors.TextoTerciario,
                        fontSize = 11.sp,
                    )
                }

                CampoTexto(
                    valor = nome,
                    rotulo = "Nome do grupo",
                    onMudar = { nome = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                CampoTexto(
                    valor = cidade,
                    rotulo = "Cidade (opcional)",
                    onMudar = { cidade = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !salvando && nome.isNotBlank(),
                onClick = { onSalvar(nome.trim(), cidade.trim().ifBlank { null }, logo, removerLogo) },
            ) {
                Text("Salvar", color = VoleiColors.VerdeClaro, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) {
                Text("Cancelar", color = VoleiColors.TextoSecundario)
            }
        },
    )
}

private fun lerLogo(
    resolver: ContentResolver,
    uri: Uri,
): ImagemEscolhida {
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Sem acesso à imagem.")
    val extensao =
        when (resolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    return ImagemEscolhida(bytes = bytes, extensao = extensao)
}

@Composable
fun EntrarNoGrupoDialog(
    salvando: Boolean,
    onEntrar: (String) -> Unit,
    onFechar: () -> Unit,
) {
    var codigo by remember { mutableStateOf("") }
    val limpo = codigo.filter { it.isLetterOrDigit() }

    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = VoleiColors.Cartao,
        titleContentColor = VoleiColors.TextoPrimario,
        textContentColor = VoleiColors.TextoSecundario,
        title = { Text("Entrar em um grupo", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "A chave de acesso é um código de 8 letras e números que a diretoria do grupo " +
                        "manda para quem vai entrar.",
                    fontSize = 13.sp,
                )
                CampoTexto(
                    valor = codigo,
                    rotulo = "Chave de acesso",
                    onMudar = { codigo = it },
                    maiusculas = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEntrar(limpo) },
                enabled = !salvando && limpo.length >= 4,
            ) {
                Text("Entrar", color = VoleiColors.VerdeClaro, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) {
                Text("Cancelar", color = VoleiColors.TextoSecundario)
            }
        },
    )
}

@Composable
fun CriarGrupoDialog(
    salvando: Boolean,
    onCriar: (String, String?) -> Unit,
    onFechar: () -> Unit,
) {
    var nome by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = VoleiColors.Cartao,
        titleContentColor = VoleiColors.TextoPrimario,
        textContentColor = VoleiColors.TextoSecundario,
        title = { Text("Criar um grupo", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Você entra como diretoria e o grupo já nasce com os times coloridos, " +
                        "as páginas de regras e uma chave de acesso para convidar a galera.",
                    fontSize = 13.sp,
                )
                CampoTexto(
                    valor = nome,
                    rotulo = "Nome do grupo",
                    onMudar = { nome = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                CampoTexto(
                    valor = cidade,
                    rotulo = "Cidade (opcional)",
                    onMudar = { cidade = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCriar(nome.trim(), cidade.trim().ifBlank { null }) },
                enabled = !salvando && nome.isNotBlank(),
            ) {
                Text("Criar grupo", color = VoleiColors.VerdeClaro, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onFechar) {
                Text("Cancelar", color = VoleiColors.TextoSecundario)
            }
        },
    )
}

@Composable
fun SeletorDePapel(
    papel: Papel,
    onMudar: (Papel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Papel.entries.forEach { opcao ->
            val escolhido = opcao == papel
            Text(
                text = opcao.rotulo,
                color = if (escolhido) VoleiColors.Fundo else VoleiColors.TextoSecundario,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (escolhido) VoleiColors.VerdeClaro else VoleiColors.CartaoInterno)
                        .clickable { onMudar(opcao) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
