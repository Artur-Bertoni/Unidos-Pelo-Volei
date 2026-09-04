package com.unidospelovolei.ui.tour

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.ui.theme.VoleiColors

private data class PassoDoTour(
    val icone: ImageVector,
    val aba: String?,
    val titulo: String,
    val texto: String,
)

private val PASSOS =
    listOf(
        PassoDoTour(
            icone = Icons.Filled.Forum,
            aba = "SOCIAL",
            titulo = "é onde o sábado começa",
            texto =
                "O mural traz os recados da diretoria, a agenda mostra jogos e confraternizações, " +
                    "e as regras do vôlei de areia ficam guardadas ali.",
        ),
        PassoDoTour(
            icone = Icons.Filled.Groups,
            aba = "TIMES",
            titulo = "monta o quarteto",
            texto =
                "A diretoria marca quem veio, sorteia os quartetos equilibrados e ativa " +
                    "os times do dia. Você acompanha o elenco de cada um.",
        ),
        PassoDoTour(
            icone = Icons.Filled.SportsVolleyball,
            aba = "JOGOS",
            titulo = "tem o chaveamento e o placar",
            texto =
                "Cada rodada enche todas as quadras. Toque em uma partida para ver ou " +
                    "digitar o placar ponto a ponto.",
        ),
        PassoDoTour(
            icone = Icons.Filled.EmojiEvents,
            aba = "CLASSIFICAÇÃO",
            titulo = "fecha a conta",
            texto = "Vitórias, saldo e pontos pró de todos os times do dia, atualizados na hora.",
        ),
        PassoDoTour(
            icone = Icons.Filled.Person,
            aba = "EU",
            titulo = "é a sua parte",
            texto =
                "Confirme presença no sábado, escolha se paga mensalidade ou diária, " +
                    "veja o seu extrato com Pix e avalie os companheiros de time.",
        ),
        PassoDoTour(
            icone = Icons.Filled.HowToReg,
            aba = null,
            titulo = "Falta só achar você na lista",
            texto =
                "Escolha o seu nome entre os jogadores do grupo. A diretoria confirma e, " +
                    "a partir daí, a sua ficha e o seu histórico ficam aqui dentro.",
        ),
    )

object Tour {
    private const val ARQUIVO = "tour"
    private const val CHAVE = "visto"

    fun jaViu(context: Context): Boolean =
        context
            .getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            .getBoolean(CHAVE, false)

    fun marcarComoVisto(context: Context) {
        context
            .getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(CHAVE, true)
            .apply()
    }
}

@Composable
fun TourScreen(
    onConcluir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passo by remember { mutableIntStateOf(0) }
    val atual = PASSOS[passo]
    val ultimo = passo == PASSOS.lastIndex

    Column(
        modifier = modifier.fillMaxSize().background(VoleiColors.Fundo).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(VoleiColors.SeloFaseFundo),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = atual.icone,
                contentDescription = null,
                tint = VoleiColors.SeloFaseTexto,
                modifier = Modifier.size(40.dp),
            )
        }

        Text(
            text =
                buildAnnotatedString {
                    atual.aba?.let { aba ->
                        withStyle(
                            SpanStyle(fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                        ) {
                            append(aba)
                        }
                        append(" ")
                    }
                    append(atual.titulo)
                },
            color = VoleiColors.TextoPrimario,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 28.dp),
        )

        Text(
            text = atual.texto,
            color = VoleiColors.TextoSecundario,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )

        Row(
            modifier = Modifier.padding(top = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PASSOS.indices.forEach { indice ->
                Box(
                    modifier =
                        Modifier
                            .size(if (indice == passo) 9.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (indice == passo) VoleiColors.Verde else VoleiColors.Borda,
                            ),
                )
            }
        }

        Button(
            onClick = { if (ultimo) onConcluir() else passo++ },
            shape = RoundedCornerShape(12.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = VoleiColors.Verde,
                    contentColor = Color.White,
                ),
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
        ) {
            Text(
                text = if (ultimo) "Escolher meu nome" else "Continuar",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }

        TextButton(onClick = onConcluir, modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = if (ultimo) "Agora não" else "Pular o tour",
                color = VoleiColors.TextoTerciario,
                fontSize = 13.sp,
            )
        }
    }
}
