package com.github.fusuma.uithemescreenshot.theme

import androidx.compose.foundation.border
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode

private val primary = Color(0xff80deea)
@Composable
fun ScreenshotTheme(
    content: @Composable () -> Unit
) {

    val swingColor = SwingColor()

    MaterialTheme(
        colors = darkColors().copy(
            primary = primary,
            background = swingColor.background,
            onBackground = swingColor.onBackground,
            surface = swingColor.background,
            onSurface = swingColor.onBackground,
        ),
        content = content
    )
}

/**
 * SwingColor can not work in preview.
 * LocalInspectionMode.current does not work now. ()
 */
@Composable
fun ScreenshotThemePreview() {

}
