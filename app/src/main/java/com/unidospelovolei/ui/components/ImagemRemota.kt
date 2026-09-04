package com.unidospelovolei.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unidospelovolei.ui.theme.VoleiColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val cache = LruCache<String, ImageBitmap>(24)

private sealed interface EstadoDaImagem {
    data object Carregando : EstadoDaImagem

    data class Pronta(
        val bitmap: ImageBitmap,
    ) : EstadoDaImagem

    data object Falhou : EstadoDaImagem
}

@Composable
fun ImagemRemota(
    url: String,
    descricao: String?,
    modifier: Modifier = Modifier,
    alturaMaxima: Int = 320,
) {
    var estado by remember(url) {
        mutableStateOf(cache.get(url)?.let(EstadoDaImagem::Pronta) ?: EstadoDaImagem.Carregando)
    }

    LaunchedEffect(url) {
        if (estado is EstadoDaImagem.Pronta) return@LaunchedEffect
        estado =
            runCatching { baixar(url) }
                .fold(
                    onSuccess = { bitmap ->
                        cache.put(url, bitmap)
                        EstadoDaImagem.Pronta(bitmap)
                    },
                    onFailure = { EstadoDaImagem.Falhou },
                )
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = alturaMaxima.dp)
                .background(VoleiColors.CartaoInterno),
        contentAlignment = Alignment.Center,
    ) {
        when (val atual = estado) {
            is EstadoDaImagem.Pronta ->
                Image(
                    bitmap = atual.bitmap,
                    contentDescription = descricao,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().heightIn(max = alturaMaxima.dp),
                )

            EstadoDaImagem.Carregando ->
                CircularProgressIndicator(
                    color = VoleiColors.Verde,
                    modifier = Modifier.size(24.dp),
                )

            EstadoDaImagem.Falhou ->
                Text(
                    text = "Imagem indisponível offline",
                    color = VoleiColors.TextoTerciario,
                    fontSize = 12.sp,
                )
        }
    }
}

private suspend fun baixar(url: String): ImageBitmap =
    withContext(Dispatchers.IO) {
        val conexao = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            val bytes = conexao.inputStream.use { it.readBytes() }
            val bitmap =
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: error("Formato de imagem não reconhecido.")
            bitmap.asImageBitmap()
        } finally {
            conexao.disconnect()
        }
    }
