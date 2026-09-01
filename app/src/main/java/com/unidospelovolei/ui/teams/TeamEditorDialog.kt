package com.unidospelovolei.ui.teams

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
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
import com.unidospelovolei.domain.model.Team
import com.unidospelovolei.ui.components.CampoTexto
import com.unidospelovolei.ui.components.CoresDeTime
import com.unidospelovolei.ui.components.SeletorCor
import com.unidospelovolei.ui.components.TeamCircle
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun TeamEditorDialog(
    time: Team?,
    onSalvarNovo: (nome: String, corHex: String, sigla: String) -> Unit,
    onSalvarExistente: (Team) -> Unit,
    onExcluir: (String) -> Unit,
    onFechar: () -> Unit,
) {
    var nome by remember { mutableStateOf(time?.nome.orEmpty()) }
    var sigla by remember { mutableStateOf(time?.sigla.orEmpty()) }
    var cor by remember { mutableStateOf(time?.corHex ?: CoresDeTime.first()) }
    var ativo by remember { mutableStateOf(time?.ativo ?: true) }

    val valido = nome.isNotBlank() && sigla.trim().length == 2

    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = VoleiColors.Cartao,
        titleContentColor = VoleiColors.TextoPrimario,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TeamCircle(
                    sigla = sigla.ifBlank { "??" },
                    corHex = cor,
                    tamanho = 36.dp,
                )
                Text(
                    if (time == null) "Novo time" else "Editar time",
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CampoTexto(
                    valor = nome,
                    rotulo = "Nome",
                    onMudar = { nome = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                CampoTexto(
                    valor = sigla,
                    rotulo = "Sigla (2 letras)",
                    onMudar = { sigla = it.take(2) },
                    maiusculas = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Cor", color = VoleiColors.TextoSecundario, fontSize = 12.sp)
                SeletorCor(
                    corSelecionada = cor,
                    onMudar = { cor = it },
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                )
                if (time != null) {
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
                    TextButton(onClick = { onExcluir(time.id) }) {
                        Text("Excluir time", color = VoleiColors.Vermelho)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = {
                    if (time == null) {
                        onSalvarNovo(nome, cor, sigla)
                    } else {
                        onSalvarExistente(time.copy(nome = nome, corHex = cor, sigla = sigla, ativo = ativo))
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
