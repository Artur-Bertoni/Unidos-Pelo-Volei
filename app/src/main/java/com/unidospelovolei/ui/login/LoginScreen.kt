package com.unidospelovolei.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.ui.components.LogoUpv
import com.unidospelovolei.ui.theme.VoleiColors

@Composable
fun LoginScreen(
    entrando: Boolean,
    erro: String?,
    onEntrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(VoleiColors.Fundo)
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LogoUpv(tamanho = 128.dp)

        Text(
            text = "UNIDOS PELO VÔLEI",
            color = VoleiColors.TextoPrimario,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = "Entre com a sua conta Google para ver os jogos e a classificação do grupo.",
            color = VoleiColors.TextoSecundario,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )

        Button(
            onClick = onEntrar,
            enabled = !entrando,
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VoleiColors.Verde),
        ) {
            Text(
                if (entrando) "Entrando..." else "Entrar com Google",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        erro?.let { mensagem ->
            Text(
                text = mensagem,
                color = VoleiColors.Vermelho,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
fun ConfiguracaoPendenteScreen(
    chavesFaltando: List<String>,
    ambiente: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(VoleiColors.Fundo)
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Configuração pendente",
            color = VoleiColors.TextoPrimario,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text =
                "Preencha as chaves abaixo em local.properties com o prefixo " +
                    "\"$ambiente.\" e rode o build de novo:",
            color = VoleiColors.TextoSecundario,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        chavesFaltando.forEach { chave ->
            Text(
                text = "$ambiente.$chave",
                color = VoleiColors.Dourado,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Text(
            text = "O passo a passo está na seção \"Configuração\" do README.",
            color = VoleiColors.TextoTerciario,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}
