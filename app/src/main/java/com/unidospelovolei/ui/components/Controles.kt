package com.unidospelovolei.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.Genero
import com.unidospelovolei.ui.theme.VoleiColors
import com.unidospelovolei.ui.theme.corDoTime

@Composable
fun Contador(
    valor: Int,
    minimo: Int,
    maximo: Int,
    onMudar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BotaoRedondo(
            icone = Icons.Filled.Remove,
            descricao = "Diminuir",
            habilitado = valor > minimo,
            onClick = { onMudar((valor - 1).coerceAtLeast(minimo)) },
        )
        Text(
            text = valor.toString(),
            color = VoleiColors.TextoPrimario,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.size(width = 28.dp, height = 24.dp),
        )
        BotaoRedondo(
            icone = Icons.Filled.Add,
            descricao = "Aumentar",
            habilitado = valor < maximo,
            onClick = { onMudar((valor + 1).coerceAtMost(maximo)) },
        )
    }
}

@Composable
fun BotaoRedondo(
    icone: ImageVector,
    descricao: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    tamanho: Dp = 34.dp,
    cor: Color = VoleiColors.Cartao,
) {
    Box(
        modifier =
            modifier
                .size(tamanho)
                .clip(CircleShape)
                .background(if (habilitado) cor else VoleiColors.CartaoInterno)
                .border(1.dp, VoleiColors.Borda, CircleShape)
                .clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icone,
            contentDescription = descricao,
            tint = if (habilitado) VoleiColors.TextoPrimario else VoleiColors.TextoTerciario,
            modifier = Modifier.size(tamanho * 0.5f),
        )
    }
}

@Composable
fun CampoTexto(
    valor: String,
    rotulo: String,
    onMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
    maiusculas: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { if (maiusculas) onMudar(it.uppercase()) else onMudar(it) },
        label = { Text(rotulo) },
        singleLine = true,
        modifier = modifier,
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

@Composable
fun CampoBusca(
    valor: String,
    onMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
    dica: String = "Buscar",
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onMudar,
        placeholder = { Text(dica, color = VoleiColors.TextoTerciario, fontSize = 14.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = VoleiColors.TextoSecundario,
                modifier = Modifier.size(18.dp),
            )
        },
        trailingIcon = {
            if (valor.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Limpar busca",
                    tint = VoleiColors.TextoSecundario,
                    modifier =
                        Modifier
                            .size(18.dp)
                            .clickable { onMudar("") },
                )
            }
        },
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = VoleiColors.TextoPrimario,
                unfocusedTextColor = VoleiColors.TextoPrimario,
                focusedBorderColor = VoleiColors.Verde,
                unfocusedBorderColor = VoleiColors.Borda,
                cursorColor = VoleiColors.Verde,
            ),
    )
}

@Composable
fun Estrelas(
    nivel: Int,
    modifier: Modifier = Modifier,
    tamanho: Dp = 14.dp,
    espacamento: Dp = 2.dp,
    onMudar: ((Int) -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(espacamento),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (1..5).forEach { valor ->
            val preenchida = valor <= nivel
            Icon(
                imageVector = if (preenchida) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = onMudar?.let { "Nível $valor" },
                tint = if (preenchida) VoleiColors.Dourado else VoleiColors.TextoTerciario,
                modifier =
                    Modifier
                        .size(tamanho)
                        .then(
                            if (onMudar == null) {
                                Modifier
                            } else {
                                Modifier.clickable { onMudar(valor) }
                            },
                        ),
            )
        }
    }
}

@Composable
fun SeletorGenero(
    genero: Genero,
    onMudar: (Genero) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Genero.entries.forEach { opcao ->
            val selecionado = opcao == genero
            Row(
                modifier =
                    Modifier
                        .selectable(
                            selected = selecionado,
                            role = Role.RadioButton,
                            onClick = { onMudar(opcao) },
                        ).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selecionado,
                    onClick = null,
                    colors =
                        RadioButtonDefaults.colors(
                            selectedColor = VoleiColors.Verde,
                            unselectedColor = VoleiColors.TextoTerciario,
                        ),
                )
                Text(
                    text = opcao.rotulo,
                    color = if (selecionado) VoleiColors.TextoPrimario else VoleiColors.TextoSecundario,
                    fontSize = 13.sp,
                    fontWeight = if (selecionado) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

val CoresDeTime: List<String> =
    listOf(
        "#2F80ED", "#E8590C", "#3F444D", "#E8437F", "#8B5CF6",
        "#16A34A", "#9CA3AF", "#E23B3B", "#EAB308", "#06B6D4",
    )

@Composable
fun SeletorCor(
    corSelecionada: String,
    onMudar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CoresDeTime.forEach { hex ->
            val selecionada = hex.equals(corSelecionada, ignoreCase = true)
            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(corDoTime(hex))
                        .border(
                            width = if (selecionada) 3.dp else 1.dp,
                            color = if (selecionada) Color.White else VoleiColors.Borda,
                            shape = CircleShape,
                        ).clickable { onMudar(hex) },
            )
        }
    }
}

@Composable
fun DialogoConfirmacao(
    titulo: String,
    mensagem: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    destrutivo: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = VoleiColors.Cartao,
        titleContentColor = VoleiColors.TextoPrimario,
        textContentColor = VoleiColors.TextoSecundario,
        title = { Text(titulo, fontWeight = FontWeight.Bold) },
        text = { Text(mensagem) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(
                    textoConfirmar,
                    color = if (destrutivo) VoleiColors.Vermelho else VoleiColors.Verde,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar", color = VoleiColors.TextoSecundario)
            }
        },
    )
}
