package io.github.fusuma.uithemescreenshot.model

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import io.github.fusuma.uithemescreenshot.adb.AdbDeviceWrapper
import io.github.fusuma.uithemescreenshot.adb.AdbError
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage
import java.time.LocalDateTime
import kotlin.time.Duration

@Composable
fun useScreenState(
    initialState: ScreenState = ScreenState(),
    getConnectedDeviceNames: () -> List<String>,
    getDevice: (Int) -> AdbDeviceWrapper,
    getLocalDateTime: () -> LocalDateTime,
    saveImage: (BufferedImage, UiTheme, LocalDateTime) -> Unit
): ScreenStateController {
    val screenshotStateController = useScreenshotState(
        getLocalDateTime = getLocalDateTime,
        saveImage = saveImage
    )
    var state by remember {
        mutableStateOf(
            initialState.copy(screenshotState = screenshotStateController.state)
        )
    }
    val deviceWrapper = remember(state.selectedIndex, state.deviceNameList) {
        val deviceWrapper = getDevice(state.selectedIndex)
        state = state.copy(deviceNotFoundError = !deviceWrapper.hasDevice && state.deviceNameList.isNotEmpty())
        deviceWrapper
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        state = state.copy(
            deviceNameList = getConnectedDeviceNames()
        )
    }

    LaunchedEffect(deviceWrapper) {
        deviceWrapper.errorFlow.collect {
            when (it) {
                AdbError.TIMEOUT -> {
                    //TODO
                }
                AdbError.NOT_FOUND -> {
                    state = state.copy(deviceNotFoundError = true)
                }
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
                Duration.ZERO
            )
            state = state.copy(
                onToggleTheme = null
            )
        }
    }

    fun onTakeScreenshot() {
        screenshotStateController.onTakeScreenshot(
            deviceWrapper,
            state.isTakeBothTheme,
            state.resizeScale
        )
    }

    return remember(state, screenshotStateController.state) {
        ScreenStateController(
            state = state.copy(
                screenshotState = screenshotStateController.state
            ),
            onRefreshDeviceList = ::onRefreshDeviceList,
            onSelectDevice = ::onSelectDevice,
            onResizeScaleChange = ::onResizeScaleChange,
            onSave = screenshotStateController.onSave,
            onCheckTakeBothTheme = ::onCheckTakeBothTheme,
            onToggleTheme = ::onToggleTheme,
            onTakeScreenshot = ::onTakeScreenshot,
        )
    }
}

data class ScreenStateController(
    val state: ScreenState,
    val onRefreshDeviceList: () -> Unit,
    val onSelectDevice: (Int) -> Unit,
    val onResizeScaleChange: (Float) -> Unit,
    val onSave: (ImageBitmap, UiTheme) -> Unit,
    val onCheckTakeBothTheme: (Boolean) -> Unit,
    val onToggleTheme: () -> Unit,
    val onTakeScreenshot: () -> Unit,
)
