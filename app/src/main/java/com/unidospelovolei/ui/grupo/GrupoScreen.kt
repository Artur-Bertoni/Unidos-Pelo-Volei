package com.unidospelovolei.ui.grupo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.Evento
import com.unidospelovolei.domain.model.Marco
import com.unidospelovolei.domain.model.Pagina
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.Post
import com.unidospelovolei.domain.model.StatusPresenca
import com.unidospelovolei.domain.model.TipoMarco
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.RotuloPequeno
import com.unidospelovolei.ui.components.Selo
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun GrupoScreen(
    estado: GrupoUiState,
    isAdmin: Boolean,
    onSecao: (SecaoDoGrupo) -> Unit,
    onNovoPost: () -> Unit,
    onExcluirPost: (String) -> Unit,
    onReagir: (String) -> Unit,
    onNovoEvento: () -> Unit,
    onEditarEvento: (Evento) -> Unit,
    onExcluirEvento: (String) -> Unit,
    onAbrirPagina: (Pagina) -> Unit,
    onResponderPor: (String, StatusPresenca) -> Unit,
    onTrazerConfirmados: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecaoDoGrupo.entries.forEach { opcao ->
                ChipDeSecao(
                    rotulo = opcao.rotulo,
                    selecionado = opcao == estado.secao,
                    onClick = { onSecao(opcao) },
                )
            }
        }

        when (estado.secao) {
            SecaoDoGrupo.MURAL ->
                Mural(
                    posts = estado.posts,
                    isAdmin = isAdmin,
                    onNovoPost = onNovoPost,
                    onExcluir = onExcluirPost,
                    onReagir = onReagir,
                )

            SecaoDoGrupo.AGENDA ->
                Agenda(
                    eventos = estado.eventos,
                    marcos = estado.marcos,
                    isAdmin = isAdmin,
                    onNovo = onNovoEvento,
                    onEditar = onEditarEvento,
                    onExcluir = onExcluirEvento,
                )

            SecaoDoGrupo.CHAMADA ->
                Chamada(
                    estado = estado,
                    isAdmin = isAdmin,
                    onResponderPor = onResponderPor,
                    onTrazerConfirmados = onTrazerConfirmados,
                )

            SecaoDoGrupo.REGRAS -> Regras(paginas = estado.paginas, onAbrir = onAbrirPagina)
        }
    }
}

@Composable
private fun ChipDeSecao(
    rotulo: String,
    selecionado: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (selecionado) VoleiColors.SeloFaseFundo else VoleiColors.CartaoInterno)
                .border(
                    width = 1.dp,
                    color = if (selecionado) VoleiColors.Azul else VoleiColors.Borda,
                    shape = RoundedCornerShape(20.dp),
                ).clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = rotulo,
            color = if (selecionado) VoleiColors.SeloFaseTexto else VoleiColors.TextoSecundario,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Mural(
    posts: List<Post>,
    isAdmin: Boolean,
    onNovoPost: () -> Unit,
    onExcluir: (String) -> Unit,
    onReagir: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isAdmin) {
            item {
                BotaoDeAcao(texto = "Publicar no mural", onClick = onNovoPost)
            }
        }

        if (posts.isEmpty()) {
            item {
                EstadoVazio(
                    titulo = "Mural vazio",
                    descricao = "Quando a diretoria publicar um recado, ele aparece aqui.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        items(posts, key = { it.id }) { post ->
            Cartao(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (post.fixado) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = "Fixado",
                                tint = VoleiColors.Dourado,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                        Text(
                            text = post.titulo,
                            color = VoleiColors.TextoPrimario,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        if (isAdmin) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Excluir publicação",
                                tint = VoleiColors.TextoTerciario,
                                modifier = Modifier.size(17.dp).clickable { onExcluir(post.id) },
                            )
                        }
                    }

                    if (post.corpo.isNotBlank()) {
                        Text(post.corpo, color = VoleiColors.TextoSecundario, fontSize = 14.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = post.autorNome ?: "Diretoria",
                            color = VoleiColors.TextoTerciario,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (post.reagi) VoleiColors.SeloVitoriaFundo else VoleiColors.CartaoInterno,
                                    ).clickable { onReagir(post.id) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = if (post.reacoes > 0) "👏 ${post.reacoes}" else "👏",
                                fontSize = 13.sp,
                                color = if (post.reagi) VoleiColors.VerdeClaro else VoleiColors.TextoSecundario,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Agenda(
    eventos: List<Evento>,
    marcos: List<Marco>,
    isAdmin: Boolean,
    onNovo: () -> Unit,
    onEditar: (Evento) -> Unit,
    onExcluir: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isAdmin) {
            item { BotaoDeAcao(texto = "Novo evento", onClick = onNovo) }
        }

        if (eventos.isEmpty()) {
            item {
                EstadoVazio(
                    titulo = "Nada marcado",
                    descricao = "O sábado é toda semana. Aqui entram confraternização, amistoso e campeonato.",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        items(eventos, key = { it.id }) { evento ->
            Cartao(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Filled.Event,
                        contentDescription = null,
                        tint = VoleiColors.Azul,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            evento.titulo,
                            color = VoleiColors.TextoPrimario,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            listOfNotNull(dataLegivel(evento.inicio), evento.local).joinToString(" · "),
                            color = VoleiColors.TextoSecundario,
                            fontSize = 12.sp,
                        )
                    }
                    Selo(evento.tipo.rotulo, VoleiColors.SeloFaseTexto, VoleiColors.SeloFaseFundo)
                    if (isAdmin) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Editar evento",
                            tint = VoleiColors.TextoTerciario,
                            modifier = Modifier.size(17.dp).clickable { onEditar(evento) },
                        )
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Excluir evento",
                            tint = VoleiColors.TextoTerciario,
                            modifier = Modifier.size(17.dp).clickable { onExcluir(evento.id) },
                        )
                    }
                }
            }
        }

        if (marcos.isNotEmpty()) {
            item { RotuloPequeno("Datas do grupo", modifier = Modifier.padding(top = 8.dp)) }
        }

        items(marcos, key = { "${it.playerId}-${it.tipo}" }) { marco ->
            Cartao(modifier = Modifier.fillMaxWidth(), cor = VoleiColors.CartaoInterno) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Filled.Cake,
                        contentDescription = null,
                        tint = if (marco.tipo == TipoMarco.ANIVERSARIO) VoleiColors.Dourado else VoleiColors.VerdeClaro,
                        modifier = Modifier.size(17.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(marco.nome, color = VoleiColors.TextoPrimario, fontSize = 14.sp)
                        Text(
                            text =
                                if (marco.tipo == TipoMarco.ANIVERSARIO) {
                                    "Aniversário em %02d/%02d".format(marco.dia, marco.mes)
                                } else {
                                    "${marco.anos} anos de Unidos em %02d/%02d".format(marco.dia, marco.mes)
                                },
                            color = VoleiColors.TextoTerciario,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Chamada(
    estado: GrupoUiState,
    isAdmin: Boolean,
    onResponderPor: (String, StatusPresenca) -> Unit,
    onTrazerConfirmados: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Cartao(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RotuloPequeno("Sábado ${dataLegivel(estado.dataDoSabado)}")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NumeroDaChamada("Vou", estado.resumo.vou, VoleiColors.VerdeClaro)
                        NumeroDaChamada("Talvez", estado.resumo.talvez, VoleiColors.Dourado)
                        NumeroDaChamada("Não vou", estado.resumo.naoVou, VoleiColors.Vermelho)
                        NumeroDaChamada("Sem resposta", estado.resumo.semResposta, VoleiColors.TextoTerciario)
                    }
                    if (isAdmin) {
                        BotaoDeAcao(
                            texto = "Trazer confirmados para a lista de hoje",
                            onClick = onTrazerConfirmados,
                            habilitado = !estado.salvando && estado.resumo.vou > 0,
                        )
                    }
                }
            }
        }

        items(estado.chamada, key = { it.jogador.id }) { linha ->
            Cartao(modifier = Modifier.fillMaxWidth(), cor = VoleiColors.CartaoInterno) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(linha.jogador.nome, color = VoleiColors.TextoPrimario, fontSize = 14.sp)
                            if (linha.origem == "diretoria") {
                                Text(
                                    "respondido pela diretoria",
                                    color = VoleiColors.TextoTerciario,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                        linha.status?.let { status ->
                            Selo(status.rotulo, corDoStatus(status), VoleiColors.Cartao)
                        }
                    }

                    if (isAdmin) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPresenca.entries.forEach { opcao ->
                                ChipDeSecao(
                                    rotulo = opcao.rotulo,
                                    selecionado = linha.status == opcao,
                                    onClick = { onResponderPor(linha.jogador.id, opcao) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Regras(
    paginas: List<Pagina>,
    onAbrir: (Pagina) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(paginas, key = { it.id }) { pagina ->
            Cartao(modifier = Modifier.fillMaxWidth().clickable { onAbrir(pagina) }) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    RotuloPequeno(pagina.categoria.rotulo)
                    Text(
                        pagina.titulo,
                        color = VoleiColors.TextoPrimario,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        pagina.corpo.lineSequence().firstOrNull { it.isNotBlank() }?.removePrefix("# ")
                            ?: "Página em branco",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun NumeroDaChamada(
    rotulo: String,
    valor: Int,
    cor: androidx.compose.ui.graphics.Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor.toString(), color = cor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        RotuloPequeno(rotulo)
    }
}

@Composable
internal fun BotaoDeAcao(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (habilitado) VoleiColors.Verde else VoleiColors.Borda)
                .clickable(enabled = habilitado, onClick = onClick)
                .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = texto,
            color = if (habilitado) androidx.compose.ui.graphics.Color.White else VoleiColors.TextoTerciario,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun corDoStatus(status: StatusPresenca) =
    when (status) {
        StatusPresenca.VOU -> VoleiColors.VerdeClaro
        StatusPresenca.TALVEZ -> VoleiColors.Dourado
        StatusPresenca.NAO_VOU -> VoleiColors.Vermelho
    }

internal fun dataLegivel(iso: String): String {
    val partes = iso.take(10).split("-")
    return if (partes.size == 3) "${partes[2]}/${partes[1]}" else iso
}
