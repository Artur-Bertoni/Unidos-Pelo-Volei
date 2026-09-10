package com.unidospelovolei.ui.grupos

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.ChaveDeAcesso
import com.unidospelovolei.domain.model.MembroDoGrupo
import com.unidospelovolei.domain.model.Papel
import com.unidospelovolei.ui.components.CampoTexto
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.Selo
import com.unidospelovolei.ui.theme.VoleiColors
import java.time.LocalDate

private fun convite(
    nomeDoGrupo: String,
    codigo: String,
): String =
    "Entra no nosso grupo do UP Vôlei! Baixe o app, faça login com o Google e use a chave " +
        "$codigo para entrar no $nomeDoGrupo."

@Composable
fun ChavesScreen(
    nomeDoGrupo: String,
    chaves: List<ChaveDeAcesso>,
    carregando: Boolean,
    salvando: Boolean,
    onVoltar: () -> Unit,
    onCarregar: () -> Unit,
    onCriar: (String?, Papel, Int?, String?) -> Unit,
    onAlternar: (ChaveDeAcesso) -> Unit,
    onExcluir: (ChaveDeAcesso) -> Unit,
    modifier: Modifier = Modifier,
    falha: String? = null,
) {
    var criando by remember { mutableStateOf(false) }
    var excluindo by remember { mutableStateOf<ChaveDeAcesso?>(null) }
    val contexto = LocalContext.current
    val area = LocalClipboardManager.current

    LaunchedEffect(Unit) { onCarregar() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = VoleiColors.Fundo,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { criando = true },
                containerColor = VoleiColors.Verde,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nova chave")
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Chaves de acesso",
                        color = VoleiColors.TextoPrimario,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Quem tem a chave entra no $nomeDoGrupo",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                    )
                }
                if (carregando) {
                    CircularProgressIndicator(
                        color = VoleiColors.Verde,
                        modifier = Modifier.size(18.dp).padding(end = 8.dp),
                    )
                }
            }

            if (chaves.isEmpty() && !carregando) {
                EstadoVazio(
                    titulo = if (falha == null) "Nenhuma chave por aqui" else "Não deu para ler as chaves",
                    descricao =
                        falha
                            ?: ("Crie uma chave e mande para quem vai entrar. Você pode desligar a " +
                                "chave a qualquer momento, sem tirar ninguém que já entrou."),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(chaves, key = { it.id }) { chave ->
                    LinhaDaChave(
                        chave = chave,
                        habilitado = !salvando,
                        onCopiar = { area.setText(AnnotatedString(chave.codigoFormatado)) },
                        onCompartilhar = {
                            val envio =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, convite(nomeDoGrupo, chave.codigoFormatado))
                                }
                            contexto.startActivity(Intent.createChooser(envio, "Convidar para o grupo"))
                        },
                        onAlternar = { onAlternar(chave) },
                        onExcluir = { excluindo = chave },
                    )
                }
            }
        }
    }

    if (criando) {
        NovaChaveDialog(
            salvando = salvando,
            onCriar = { rotulo, papel, usos, expira ->
                onCriar(rotulo, papel, usos, expira)
                criando = false
            },
            onFechar = { criando = false },
        )
    }

    excluindo?.let { chave ->
        AlertDialog(
            onDismissRequest = { excluindo = null },
            containerColor = VoleiColors.Cartao,
            titleContentColor = VoleiColors.TextoPrimario,
            textContentColor = VoleiColors.TextoSecundario,
            title = { Text("Apagar a chave ${chave.codigoFormatado}?", fontWeight = FontWeight.Bold) },
            text = { Text("Quem já entrou com ela continua no grupo. A chave é que deixa de funcionar.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onExcluir(chave)
                        excluindo = null
                    },
                ) {
                    Text("Apagar", color = VoleiColors.Vermelho)
                }
            },
            dismissButton = {
                TextButton(onClick = { excluindo = null }) {
                    Text("Cancelar", color = VoleiColors.TextoSecundario)
                }
            },
        )
    }
}

@Composable
private fun LinhaDaChave(
    chave: ChaveDeAcesso,
    habilitado: Boolean,
    onCopiar: () -> Unit,
    onCompartilhar: () -> Unit,
    onAlternar: () -> Unit,
    onExcluir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    chave.codigoFormatado,
                    color = VoleiColors.TextoPrimario,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onCopiar, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copiar chave",
                        tint = VoleiColors.TextoSecundario,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onCompartilhar, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "Compartilhar convite",
                        tint = VoleiColors.TextoSecundario,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onExcluir, enabled = habilitado, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Apagar chave",
                        tint = VoleiColors.TextoTerciario,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (chave.papel == Papel.DIRETORIA) {
                    Selo("Entra como diretoria", VoleiColors.Dourado, VoleiColors.SeloFaseFundo)
                }
                Text(
                    listOfNotNull(
                        chave.rotulo,
                        if (chave.usosMax == null) {
                            "${chave.usos} usos"
                        } else {
                            "${chave.usos} de ${chave.usosMax} usos"
                        },
                        chave.expiraEm?.take(10)?.let { "vence em $it" },
                    ).joinToString(" · "),
                    color = VoleiColors.TextoSecundario,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = chave.ativa,
                    onCheckedChange = { onAlternar() },
                    enabled = habilitado,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = VoleiColors.Fundo,
                            checkedTrackColor = VoleiColors.VerdeClaro,
                            uncheckedTrackColor = VoleiColors.CartaoInterno,
                        ),
                )
            }
        }
    }
}

@Composable
private fun NovaChaveDialog(
    salvando: Boolean,
    onCriar: (String?, Papel, Int?, String?) -> Unit,
    onFechar: () -> Unit,
) {
    var rotulo by remember { mutableStateOf("") }
    var papel by remember { mutableStateOf(Papel.ATLETA) }
    var usos by remember { mutableStateOf("") }
    var dias by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = VoleiColors.Cartao,
        titleContentColor = VoleiColors.TextoPrimario,
        textContentColor = VoleiColors.TextoSecundario,
        title = { Text("Nova chave", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(
                    valor = rotulo,
                    rotulo = "Para que é esta chave (opcional)",
                    onMudar = { rotulo = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Quem entrar com ela vira:", fontSize = 12.sp)
                SeletorDePapel(papel = papel, onMudar = { papel = it })
                CampoTexto(
                    valor = usos,
                    rotulo = "Limite de usos (vazio = sem limite)",
                    onMudar = { texto -> usos = texto.filter { it.isDigit() }.take(4) },
                    modifier = Modifier.fillMaxWidth(),
                )
                CampoTexto(
                    valor = dias,
                    rotulo = "Vence em quantos dias (vazio = nunca)",
                    onMudar = { texto -> dias = texto.filter { it.isDigit() }.take(4) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !salvando,
                onClick = {
                    onCriar(
                        rotulo.trim().ifBlank { null },
                        papel,
                        usos.toIntOrNull()?.takeIf { it > 0 },
                        dias.toIntOrNull()?.takeIf { it > 0 }?.let {
                            LocalDate.now().plusDays(it.toLong()).toString()
                        },
                    )
                },
            ) {
                Text("Criar chave", color = VoleiColors.VerdeClaro, fontWeight = FontWeight.Bold)
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
fun MembrosScreen(
    nomeDoGrupo: String,
    membros: List<MembroDoGrupo>,
    meuId: String?,
    carregando: Boolean,
    salvando: Boolean,
    onVoltar: () -> Unit,
    onCarregar: () -> Unit,
    onDefinirPapel: (MembroDoGrupo, Papel) -> Unit,
    onRemover: (MembroDoGrupo) -> Unit,
    modifier: Modifier = Modifier,
    falha: String? = null,
) {
    var removendo by remember { mutableStateOf<MembroDoGrupo?>(null) }

    LaunchedEffect(Unit) { onCarregar() }

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
                        "Membros do grupo",
                        color = VoleiColors.TextoPrimario,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${membros.size} contas no $nomeDoGrupo",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                    )
                }
                if (carregando) {
                    CircularProgressIndicator(
                        color = VoleiColors.Verde,
                        modifier = Modifier.size(18.dp).padding(end = 8.dp),
                    )
                }
            }

            if (membros.isEmpty() && !carregando) {
                EstadoVazio(
                    titulo = if (falha == null) "Ninguém por aqui ainda" else "Não deu para ler os membros",
                    descricao = falha ?: "Mande a chave de acesso para a galera entrar no grupo.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(membros, key = { it.profileId }) { membro ->
                    Cartao(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        membro.rotulo,
                                        color = VoleiColors.TextoPrimario,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    membro.email?.takeIf { it != membro.rotulo }?.let { email ->
                                        Text(email, color = VoleiColors.TextoTerciario, fontSize = 11.sp)
                                    }
                                }
                                if (membro.profileId != meuId) {
                                    IconButton(
                                        onClick = { removendo = membro },
                                        enabled = !salvando,
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Tirar do grupo",
                                            tint = VoleiColors.TextoTerciario,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                            SeletorDePapel(
                                papel = membro.papel,
                                onMudar = { onDefinirPapel(membro, it) },
                            )
                        }
                    }
                }
            }
        }
    }

    removendo?.let { membro ->
        AlertDialog(
            onDismissRequest = { removendo = null },
            containerColor = VoleiColors.Cartao,
            titleContentColor = VoleiColors.TextoPrimario,
            textContentColor = VoleiColors.TextoSecundario,
            title = { Text("Tirar ${membro.rotulo} do grupo?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "A conta perde o acesso ao grupo. A ficha do jogador continua aqui, " +
                        "só fica sem dono.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemover(membro)
                        removendo = null
                    },
                ) {
                    Text("Tirar do grupo", color = VoleiColors.Vermelho)
                }
            },
            dismissButton = {
                TextButton(onClick = { removendo = null }) {
                    Text("Cancelar", color = VoleiColors.TextoSecundario)
                }
            },
        )
    }
}
