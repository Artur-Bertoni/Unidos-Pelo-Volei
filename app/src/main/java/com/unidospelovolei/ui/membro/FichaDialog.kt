package com.unidospelovolei.ui.membro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.domain.model.Player
import com.unidospelovolei.domain.model.PlayerContato
import com.unidospelovolei.domain.model.Regime
import com.unidospelovolei.ui.components.CampoTexto
import com.unidospelovolei.ui.components.RotuloPequeno
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun FichaDialog(
    jogador: Player,
    contato: PlayerContato?,
    salvando: Boolean,
    onSalvar: (String, Int?, Int?, String?, String?, Int?, Regime) -> Unit,
    onFechar: () -> Unit,
) {
    var nome by remember(jogador.id) { mutableStateOf(jogador.nome) }
    var dia by remember(jogador.id) { mutableStateOf(jogador.nascimentoDia?.toString().orEmpty()) }
    var mes by remember(jogador.id) { mutableStateOf(jogador.nascimentoMes?.toString().orEmpty()) }
    var regime by remember(jogador.id) { mutableStateOf(jogador.regime) }
    var ano by remember(contato?.id) { mutableStateOf(contato?.nascimentoAno?.toString().orEmpty()) }
    var telefone by remember(contato?.id) { mutableStateOf(contato?.telefone.orEmpty()) }
    var emergencia by remember(contato?.id) { mutableStateOf(contato?.contatoEmergencia.orEmpty()) }

    val diaValido = dia.isBlank() || dia.toIntOrNull()?.let { it in 1..31 } == true
    val mesValido = mes.isBlank() || mes.toIntOrNull()?.let { it in 1..12 } == true
    val aniversarioCompleto = dia.isBlank() == mes.isBlank()
    val podeSalvar =
        nome.isNotBlank() && diaValido && mesValido && aniversarioCompleto && !salvando

    AlertDialog(
        onDismissRequest = onFechar,
        containerColor = VoleiColors.Cartao,
        titleContentColor = VoleiColors.TextoPrimario,
        textContentColor = VoleiColors.TextoSecundario,
        title = { Text("Minha ficha", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CampoTexto(
                    valor = nome,
                    rotulo = "Nome",
                    onMudar = { nome = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (jogador.regime == Regime.ISENTO) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RotuloPequeno("Como eu pago")
                        Text(
                            text = "A diretoria te deixou isento. Fale com quem organiza para mudar.",
                            color = VoleiColors.TextoTerciario,
                            fontSize = 11.sp,
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RotuloPequeno("Como eu pago")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Regime.escolhiveisPeloAtleta.forEach { opcao ->
                                ChipDeEscolha(
                                    rotulo = opcao.rotulo,
                                    selecionado = opcao == regime,
                                    onClick = { regime = opcao },
                                )
                            }
                        }
                        Text(
                            text =
                                if (regime == Regime.MENSALISTA) {
                                    "Mensalista paga o mês inteiro e joga todo sábado."
                                } else {
                                    "Diarista paga só a diária dos sábados em que aparecer."
                                },
                            color = VoleiColors.TextoTerciario,
                            fontSize = 11.sp,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RotuloPequeno("Aniversário")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampoTexto(
                            valor = dia,
                            rotulo = "Dia",
                            onMudar = { dia = it.filter(Char::isDigit).take(2) },
                            modifier = Modifier.weight(1f),
                        )
                        CampoTexto(
                            valor = mes,
                            rotulo = "Mês",
                            onMudar = { mes = it.filter(Char::isDigit).take(2) },
                            modifier = Modifier.weight(1f),
                        )
                        CampoTexto(
                            valor = ano,
                            rotulo = "Ano",
                            onMudar = { ano = it.filter(Char::isDigit).take(4) },
                            modifier = Modifier.weight(1.2f),
                        )
                    }
                    Text(
                        text = "O grupo vê só o dia e o mês. O ano fica com a diretoria.",
                        color = VoleiColors.TextoTerciario,
                        fontSize = 11.sp,
                    )
                }

                CampoTexto(
                    valor = telefone,
                    rotulo = "Telefone",
                    onMudar = { telefone = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                CampoTexto(
                    valor = emergencia,
                    rotulo = "Contato de emergência",
                    onMudar = { emergencia = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSalvar(
                        nome.trim(),
                        dia.toIntOrNull(),
                        mes.toIntOrNull(),
                        telefone,
                        emergencia,
                        ano.toIntOrNull(),
                        regime,
                    )
                },
                enabled = podeSalvar,
            ) {
                Text(
                    "Salvar",
                    color = if (podeSalvar) VoleiColors.VerdeClaro else VoleiColors.TextoTerciario,
                )
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
fun ChipDeEscolha(
    rotulo: String,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (selecionado) VoleiColors.SeloFaseFundo else VoleiColors.CartaoInterno)
                .border(
                    width = 1.dp,
                    color = if (selecionado) VoleiColors.Azul else VoleiColors.Borda,
                    shape = RoundedCornerShape(8.dp),
                ).clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rotulo,
            color = if (selecionado) VoleiColors.SeloFaseTexto else VoleiColors.TextoSecundario,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
