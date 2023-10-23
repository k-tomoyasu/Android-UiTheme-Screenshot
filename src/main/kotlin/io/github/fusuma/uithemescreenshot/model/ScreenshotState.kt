package io.github.fusuma.uithemescreenshot.model

import androidx.compose.ui.graphics.ImageBitmap

data class ScreenshotState(
    val lightScreenshot: ImageBitmap? = null,
    val darkScreenshot: ImageBitmap? = null,
    val onScreenshot: ScreenshotTarget = ScreenshotTarget.NOT_PROCESSING,
) {

    val isScreenshotProcessing = onScreenshot != ScreenshotTarget.NOT_PROCESSING
    val isLightScreenshotProcessing = onScreenshot.isTarget(ScreenshotTarget.LIGHT)
    val isDarkScreenshotProcessing = onScreenshot.isTarget(ScreenshotTarget.DARK)

    private fun ScreenshotTarget.isTarget(target: ScreenshotTarget) = when (this) {
        ScreenshotTarget.LIGHT -> target == ScreenshotTarget.LIGHT
        ScreenshotTarget.DARK -> target == ScreenshotTarget.DARK
        ScreenshotTarget.BOTH -> true
        ScreenshotTarget.NOT_PROCESSING -> false
    }
}