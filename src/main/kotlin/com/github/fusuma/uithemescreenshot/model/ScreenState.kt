package com.github.fusuma.uithemescreenshot.model

import androidx.compose.ui.graphics.ImageBitmap

data class ScreenState(
    // Preview failed when use ImmutableList(kotlinx)
    val deviceNameList: List<String> = emptyList(),
    val selectedIndex: Int = 0,
    val resizeScale: Float = 0.4f,
    val lightScreenshot: ImageBitmap? = null,
    val darkScreenshot: ImageBitmap? = null,
    val onRefreshDevice: Unit? = null,
    val onScreenshot: Unit? = null,
    val deviceNotFoundError: Unit? = null,
) {
    val isScreenshotProcessing = onScreenshot != null
    val isInvalidResizeScale = resizeScale <= 0f
    val deviceExists = deviceNameList.isNotEmpty()
    val isLightScreenshotProcessing = isScreenshotProcessing && lightScreenshot == null
    val isDarkScreenshotProcessing = isScreenshotProcessing && darkScreenshot == null
}
