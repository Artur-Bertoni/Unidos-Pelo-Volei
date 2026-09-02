package com.unidospelovolei.ui.membro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.VinculoPedido
import com.unidospelovolei.ui.components.Cartao
import com.unidospelovolei.ui.components.EstadoVazio
import com.unidospelovolei.ui.components.RotuloPequeno
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun AprovacoesScreen(
    fila: List<PedidoNaFila>,
    salvando: Boolean,
    onVoltar: () -> Unit,
    onDecidir: (VinculoPedido, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = VoleiColors.Fundo,
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
                        "Pedidos de acesso",
                        color = VoleiColors.TextoPrimario,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (fila.size == 1) "1 aguardando" else "${fila.size} aguardando",
                        color = VoleiColors.TextoSecundario,
                        fontSize = 12.sp,
                    )
                }
            }

            if (fila.isEmpty()) {
                EstadoVazio(
                    titulo = "Nenhum pedido na fila",
                    descricao = "Quando alguém entrar e escolher o próprio nome, o pedido aparece aqui.",
                    modifier = Modifier.fillMaxWidth(),
                )
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(fila, key = { it.pedido.id }) { item ->
                    LinhaDePedido(
                        item = item,
                        salvando = salvando,
                        onDecidir = { aprovado -> onDecidir(item.pedido, aprovado) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaDePedido(
    item: PedidoNaFila,
    salvando: Boolean,
    onDecidir: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                RotuloPequeno("Entrou com a conta")
                Text(
                    text = item.pedido.profileNome ?: "Conta sem nome",
                    color = VoleiColors.TextoPrimario,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                RotuloPequeno("Diz ser o jogador")
                Text(
                    text = item.jogador?.nome ?: "Jogador removido da lista",
                    color = if (item.jogador != null) VoleiColors.VerdeClaro else VoleiColors.Vermelho,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (item.jogador != null) {
                    Text(
                        text = item.jogador.genero.rotulo,
                        color = VoleiColors.TextoTerciario,
                        fontSize = 11.sp,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onDecidir(true) },
                    enabled = !salvando && item.jogador != null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = VoleiColors.Verde,
                            contentColor = Color.White,
                            disabledContainerColor = VoleiColors.Borda,
                            disabledContentColor = VoleiColors.TextoTerciario,
                        ),
                ) {
                    Text("Confirmar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onDecidir(false) },
                    enabled = !salvando,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Recusar", color = VoleiColors.Vermelho, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
