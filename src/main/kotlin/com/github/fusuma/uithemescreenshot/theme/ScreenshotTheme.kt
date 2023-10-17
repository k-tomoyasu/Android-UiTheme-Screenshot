package com.github.fusuma.uithemescreenshot.theme

import androidx.compose.foundation.border
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ScreenshotTheme(
    content: @Composable () -> Unit
) {
    val swingColor = SwingColor()

    MaterialTheme(
        colors = darkColors().copy(
            primary = Color(0xff80deea),
            background = swingColor.background,
            onBackground = swingColor.onBackground,
            surface = swingColor.background,
            onSurface = swingColor.onBackground,
        ),
        content = content
    )
}