package io.github.fusuma.uithemescreenshot.model

import androidx.compose.ui.graphics.ImageBitmap

class ScreenshotResult(
    val theme: UiTheme,
    val image: ImageBitmap,
    val hasNextTarget: Boolean
)
