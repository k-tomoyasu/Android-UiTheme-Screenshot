package com.github.fusuma.uithemescreenshot.model

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import com.android.ddmlib.AdbCommandRejectedException
import com.github.fusuma.uithemescreenshot.adb.AdbDeviceWrapper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage
import java.time.LocalDateTime

private const val sleepTime = 3000L

@Composable
fun useScreenshotScreenState(
    initialState: ScreenState = ScreenState(),
    getConnectedDeviceNames: () -> List<String>,
    getDevice: (Int) -> AdbDeviceWrapper,
    getLocalDateTime: () -> LocalDateTime,
    saveImage: (BufferedImage, UiTheme, LocalDateTime) -> Unit
): ScreenshotStateController {
    var state by remember { mutableStateOf(initialState) }
    var screenshotTime by remember { mutableStateOf<LocalDateTime?>(null) }
    val deviceWrapper = remember(state.selectedIndex, state.deviceNameList) {
        val deviceWrapper = getDevice(state.selectedIndex)
        state = state.copy(deviceNotFoundError = !deviceWrapper.hasDevice)
        deviceWrapper
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        state = state.copy(
            deviceNameList = getConnectedDeviceNames()
        )
    }

    fun onRefreshDeviceList() {
        state = state.copy(
            deviceNameList = getConnectedDeviceNames(),
            deviceNotFoundError = false,
            onRefreshDevice = null
        )
    }

    fun onSelectDevice(index: Int) {
        state = state.copy(
            selectedIndex = index
        )
    }

    fun onResizeScaleChange(scale: Float) {
        state = state.copy(
            resizeScale = scale,
        )
    }

    fun onSave(bitmap: ImageBitmap, theme: UiTheme) {
        screenshotTime?.let {
            saveImage(
                bitmap.toAwtImage(),
                theme,
                it,
            )
        }
    }

    fun onCheckTakeBothTheme(checked: Boolean) {
        state = state.copy(
            isTakeBothTheme = checked
        )
    }

    fun onToggleTheme() {
        state = state.copy(
            onToggleTheme = Unit,
        )
        scope.launch {
            deviceWrapper.changeUiTheme(
                deviceWrapper.getCurrentUiTheme().toggle(),
                0
            )
            state = state.copy(
                onToggleTheme = null
            )
        }
    }

    fun onTakeScreenshot() {
        state = state.copy(
            onScreenshot = Unit
        )
        scope.launch {
            val initialTheme = deviceWrapper.getCurrentUiTheme()
            val screenshotFlow = deviceWrapper.screenshotFlow(
                state.resizeScale,
                state.isTakeBothTheme,
                initialTheme
            )
            screenshotFlow.onStart {
                val refreshState = if (state.isTakeBothTheme) {
                    state.copy(
                        lightScreenshot = null,
                        darkScreenshot = null,
                        processingScreenshotTarget = ScreenshotTarget.BOTH
                    )
                } else {
                    when (initialTheme) {
                        UiTheme.LIGHT -> state.copy(
                            lightScreenshot = null,
                            processingScreenshotTarget = ScreenshotTarget.LIGHT
                        )
                        UiTheme.DARK -> state.copy(
                            darkScreenshot = null,
                            processingScreenshotTarget = ScreenshotTarget.DARK
                        )
                    }
                }
                state = refreshState
            }.onEach { screenshot ->
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
            }.onCompletion { throwable ->
                if (state.processingScreenshotTarget == ScreenshotTarget.BOTH) {
                    deviceWrapper.changeUiTheme(
                        uiTheme = initialTheme,
                        sleepTime = 0
                    )
                }
                state = state.copy(
                    deviceNotFoundError = throwable is AdbCommandRejectedException,
                    onScreenshot = null,
                    processingScreenshotTarget = null
                )
                screenshotTime = getLocalDateTime()
            }.launchIn(this)
        }
    }

    return ScreenshotStateController(
        state = state,
        onRefreshDeviceList = ::onRefreshDeviceList,
        onSelectDevice = ::onSelectDevice,
        onResizeScaleChange = ::onResizeScaleChange,
        onSave = ::onSave,
        onCheckTakeBothTheme = ::onCheckTakeBothTheme,
        onToggleTheme = ::onToggleTheme,
        onTakeScreenshot = ::onTakeScreenshot,
    )
}

data class ScreenshotStateController(
    val state: ScreenState,
    val onRefreshDeviceList: () -> Unit,
    val onSelectDevice: (Int) -> Unit,
    val onResizeScaleChange: (Float) -> Unit,
    val onSave: (ImageBitmap, UiTheme) -> Unit,
    val onCheckTakeBothTheme: (Boolean) -> Unit,
    val onToggleTheme: () -> Unit,
    val onTakeScreenshot: () -> Unit,
)
