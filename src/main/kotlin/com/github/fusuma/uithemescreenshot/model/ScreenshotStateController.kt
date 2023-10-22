package com.github.fusuma.uithemescreenshot.model

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import com.github.fusuma.uithemescreenshot.adb.AdbDeviceWrapper
import com.github.fusuma.uithemescreenshot.adb.AdbError
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
        state = state.copy(deviceNotFoundError = !deviceWrapper.hasDevice && state.deviceNameList.isNotEmpty())
        deviceWrapper
    }
    val adbError = deviceWrapper.errorFlow.collectAsState(null).value
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        state = state.copy(
            deviceNameList = getConnectedDeviceNames()
        )
    }

    adbError?.let { error ->
        when (error) {
            AdbError.TIMEOUT -> {
                //TODO
            }
            AdbError.NOT_FOUND -> {
                state = state.copy(deviceNotFoundError = true)
            }
        }
    }

    fun onRefreshDeviceList() {
        state = state.copy(
            deviceNameList = getConnectedDeviceNames(),
            deviceNotFoundError = false,
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
        saveImage(
            bitmap.toAwtImage(),
            theme,
            requireNotNull(screenshotTime) { "screenshotTime must not be null if it can save image." }
        )
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
        scope.launch {
            val initialTheme = deviceWrapper.getCurrentUiTheme()
            val refreshState = if (state.isTakeBothTheme) {
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
                state.resizeScale,
                state.isTakeBothTheme,
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
                        sleepTime = 0
                    )
                }
                state = state.copy(
                    onScreenshot = null,
                )
            }.launchIn(this)
        }
    }

    return remember(state) {
        ScreenshotStateController(
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
