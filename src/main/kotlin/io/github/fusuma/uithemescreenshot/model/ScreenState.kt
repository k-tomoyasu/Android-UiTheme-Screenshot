package io.github.fusuma.uithemescreenshot.model

data class ScreenState(
    // Preview failed when use ImmutableList(kotlinx)
    val deviceNameList: List<String> = emptyList(),
    val selectedIndex: Int = 0,
    val resizeScale: Float = 0.4f,
    val isTakeBothTheme: Boolean = true,
    val onToggleTheme: Unit? = null,
    val screenshotState: ScreenshotState = ScreenshotState(),
    val deviceNotFoundError: Boolean = false,
) {
    private val isScreenshotProcessing = screenshotState.isScreenshotProcessing
    val isLightScreenshotProcessing = screenshotState.isLightScreenshotProcessing
    val isDarkScreenshotProcessing = screenshotState.isDarkScreenshotProcessing
    private val isToggleThemeProcessing = onToggleTheme != null
    private val isInvalidResizeScale = resizeScale <= 0f
    private val deviceExists = deviceNameList.isNotEmpty()
    val scaleSliderEnabled = !isScreenshotProcessing
    val screenshotButtonEnabled = !isScreenshotProcessing && !isToggleThemeProcessing && !isInvalidResizeScale && deviceExists
    val toggleButtonEnabled = !isScreenshotProcessing && !isToggleThemeProcessing && deviceExists
    val targetCheckboxEnabled = !isScreenshotProcessing
    val deviceListSelectable = !isScreenshotProcessing && !isToggleThemeProcessing
}
