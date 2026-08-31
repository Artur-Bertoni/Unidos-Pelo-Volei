package com.unidospelovolei.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import com.unidospelovolei.VoleiApplication
import com.unidospelovolei.ui.theme.VoleiColors
import com.unidospelovolei.ui.theme.VoleiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as VoleiApplication).container

        setContent {
            VoleiTheme {
                AppRoot(
                    container = container,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(VoleiColors.Fundo)
                            .safeDrawingPadding(),
                )
            }
        }
    }
}
