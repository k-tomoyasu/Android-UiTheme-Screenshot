package io.github.fusuma.uithemescreenshot.model

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import io.github.fusuma.uithemescreenshot.adb.AdbDeviceWrapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val sleepTime = 3.seconds

@Composable
fun useScreenshotState(
    initialState: ScreenshotState = ScreenshotState(),
    getLocalDateTime: () -> LocalDateTime,
    saveImage: (BufferedImage, UiTheme, LocalDateTime) -> Unit
): ScreenshotStateController {
    var state by remember { mutableStateOf(initialState) }
    var screenshotTime by remember { mutableStateOf<LocalDateTime?>(null) }
    val scope = rememberCoroutineScope()

    fun onSave(bitmap: ImageBitmap, theme: UiTheme) {
        saveImage(
            bitmap.toAwtImage(),
            theme,
            requireNotNull(screenshotTime) { "screenshotTime must not be null if it can save image." }
        )
    }

    fun onTakeScreenshot(deviceWrapper: AdbDeviceWrapper, isTakeBothTheme: Boolean, resizeScale: Float) {
        scope.launch {
            val initialTheme = deviceWrapper.getCurrentUiTheme()
            val refreshState = if (isTakeBothTheme) {
                state.copy(
                    lightScreenshot = null,
                    darkScreenshot = null,
                    onScreenshot = ScreenshotTarget.BOTH
                )
            } else {
                when (initialTheme) {
                    UiTheme.LIGHT -> state.copy(
                        lightScreenshot = null,
                        onScreenshot = ScreenshotTarget.LIGHT
                    )
                    UiTheme.DARK -> state.copy(
                        darkScreenshot = null,
                        onScreenshot = ScreenshotTarget.DARK
                    )
                }
            }
            state = refreshState
            screenshotTime = getLocalDateTime()

            val screenshotFlow = deviceWrapper.screenshotFlow(
                resizeScale,
                isTakeBothTheme,
                initialTheme
            )
            screenshotFlow.onEach { screenshot ->
                state = when (screenshot.theme) {
                    UiTheme.LIGHT -> state.copy(
                        lightScreenshot = screenshot.image
                    )
                    UiTheme.DARK -> state.copy(
                        darkScreenshot = screenshot.image
                    )
                }
                if (screenshot.hasNextTarget) {
                    deviceWrapper.changeUiTheme(
                        uiTheme = screenshot.theme.toggle(),
                        sleepTime = sleepTime
                    )
                }
            }.onCompletion {
                if (state.onScreenshot == ScreenshotTarget.BOTH) {
                    deviceWrapper.changeUiTheme(
                        uiTheme = initialTheme,
                        sleepTime = Duration.ZERO
                    )
                }
                state = state.copy(
                    onScreenshot = ScreenshotTarget.NOT_PROCESSING,
                )
            }.launchIn(this)
        }
    }

    return remember(state) {
        ScreenshotStateController(
            state = state,
            onTakeScreenshot = ::onTakeScreenshot,
            onSave = ::onSave
        )
    }
}

data class ScreenshotStateController(
    val state: ScreenshotState,
    val onTakeScreenshot: (deviceWrapper: AdbDeviceWrapper, isTakeBothTheme: Boolean, resizeScale: Float) -> Unit,
    val onSave: (image: ImageBitmap, theme: UiTheme) -> Unit,
)
