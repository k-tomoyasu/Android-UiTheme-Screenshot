package io.github.fusuma.uithemescreenshot.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces

private val primary = Color(0xff80deea)
private val darculaBackground = Color(0.23529412f, 0.24705882f, 0.25490198f, 1.0F, ColorSpaces.Srgb)
private val darculaOnBackground = Color(0.73333335f, 0.73333335f, 0.73333335f, 1.0f, ColorSpaces.Srgb)

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
 * and LocalInspectionMode.current does not work now. (https://github.com/JetBrains/compose-multiplatform/issues/2852)
 */
@Composable
fun ScreenshotThemePreview(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = darkColors().copy(
            primary = primary,
            background = darculaBackground,
            onBackground = darculaOnBackground,
            surface = darculaBackground,
            onSurface = darculaOnBackground,
        ),
        content = content
    )
}
