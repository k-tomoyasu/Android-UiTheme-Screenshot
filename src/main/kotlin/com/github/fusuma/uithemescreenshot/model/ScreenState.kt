package com.github.fusuma.uithemescreenshot.model

import androidx.compose.ui.graphics.ImageBitmap

data class ScreenState(
    // Preview failed when use ImmutableList(kotlinx)
    val deviceNameList: List<String> = emptyList(),
    val selectedIndex: Int = 0,
    val resizeScale: Float = 0.4f,
    val isTakeBothTheme: Boolean = true,
    val lightScreenshot: ImageBitmap? = null,
    val darkScreenshot: ImageBitmap? = null,
    val onRefreshDevice: Unit? = null,
    val onScreenshot: Unit? = null,
    val processingScreenshotTarget: ScreenshotTarget? = null,
    val onToggleTheme: Unit? = null,
    val deviceNotFoundError: Unit? = null,
) {
    val isScreenshotProcessing = onScreenshot != null
    val isLightScreenshotProcessing = isScreenshotProcessing && (processingScreenshotTarget.isTarget(ScreenshotTarget.LIGHT))
    val isDarkScreenshotProcessing = isScreenshotProcessing && (processingScreenshotTarget.isTarget(ScreenshotTarget.DARK))
    val isToggleThemeProcessing = onToggleTheme != null
    val isInvalidResizeScale = resizeScale <= 0f
    val deviceExists = deviceNameList.isNotEmpty()
}

enum class ScreenshotTarget {
    LIGHT, DARK, BOTH;
}
private fun ScreenshotTarget?.isTarget(target: ScreenshotTarget) = when (this) {
    ScreenshotTarget.LIGHT -> target == ScreenshotTarget.LIGHT
    ScreenshotTarget.DARK -> target == ScreenshotTarget.DARK
    ScreenshotTarget.BOTH -> true
    null -> false
}
