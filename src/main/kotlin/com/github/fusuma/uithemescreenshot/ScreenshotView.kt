package com.github.fusuma.uithemescreenshot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toAwtImage
import com.android.ddmlib.AdbCommandRejectedException
import com.android.ddmlib.IDevice
import com.github.fusuma.uithemescreenshot.adb.AdbError
import com.github.fusuma.uithemescreenshot.adb.AdbWrapperImpl
import com.github.fusuma.uithemescreenshot.image.saveImage
import com.github.fusuma.uithemescreenshot.model.ScreenState
import com.github.fusuma.uithemescreenshot.model.ScreenshotTarget
import com.github.fusuma.uithemescreenshot.model.UiTheme
import com.github.fusuma.uithemescreenshot.screen.TakeScreenshotScreen
import com.github.fusuma.uithemescreenshot.theme.ScreenshotTheme
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val sleepTime = 3000L

@Composable
fun ScreenshotView(
    project: Project,
    statusBar: StatusBar,
    getDevice: (Int) -> IDevice?,
    getConnectedDeviceNames: () -> List<String>
) {
    val screenshotTime = remember { mutableStateOf("") }
    val state = remember { mutableStateOf(ScreenState()) }
    val deviceWrapper = remember(state.value.selectedIndex, state.value.deviceNameList) {
        val device = getDevice(state.value.selectedIndex)
        state.value = state.value.copy(
            deviceNotFoundError = device == null
        )
        AdbWrapperImpl(device)
    }

    ScreenshotTheme {
        TakeScreenshotScreen(
            state = state.value,
            onSelectDevice = { index ->
                state.value = state.value.copy(
                    selectedIndex = index
                )
            },
            onResizeScaleChange = { scale ->
                state.value = state.value.copy(
                    resizeScale = scale,
                )
            },
            onClickTakeScreenshot = {
                state.value = state.value.copy(
                    onScreenshot = Unit
                )
            },
            onClickRefreshDeviceList = {
                state.value = state.value.copy(
                    onRefreshDevice = Unit
                )
            },
            onSave = { imageBitmap, uiTheme ->
                saveImage(
                    imageBitmap.toAwtImage(),
                    uiTheme,
                    screenshotTime.value,
                    project
                )
            },
            onCheckTakeBothTheme = { checked ->
                state.value = state.value.copy(
                    isTakeBothTheme = checked
                )
            },
            onToggleTheme = {
                state.value = state.value.copy(
                    onToggleTheme = Unit
                )
            }
        )
    }

    LaunchedEffect(Unit) {
        state.value = state.value.copy(
            deviceNameList = getConnectedDeviceNames()
        )
    }
    LaunchedEffect(Unit) {
        deviceWrapper.errorFlow.collect {
            when (it) {
                AdbError.TIMEOUT -> {
                    ApplicationManager.getApplication().invokeLater {
                        statusBar.info = "ADB timeout."
                    }
                }
                AdbError.NOT_FOUND -> state.value = state.value.copy(
                    deviceNotFoundError = false
                )
            }
        }
    }

    state.value.onRefreshDevice?.let {
        state.value = state.value.copy(
            deviceNameList = getConnectedDeviceNames(),
            deviceNotFoundError = false,
            onRefreshDevice = null
        )
    }

    state.value.onToggleTheme?.let {
        LaunchedEffect(Unit) {
            state.value = state.value.copy(
                deviceNotFoundError = false,
            )
            deviceWrapper.changeUiTheme(
                deviceWrapper.getCurrentUiTheme().toggle(),
                0
            )
            state.value = state.value.copy(
                onToggleTheme = null
            )
        }
    }

    state.value.onScreenshot?.let {
        LaunchedEffect(Unit) {
            state.value = state.value.copy(
                deviceNotFoundError = false,
            )
            val initialTheme = deviceWrapper.getCurrentUiTheme()
            val screenshotFlow = deviceWrapper.screenshotFlow(
                state.value.resizeScale,
                state.value.isTakeBothTheme,
                initialTheme
            )
            screenshotFlow.onStart {
                val refreshState = if (state.value.isTakeBothTheme) {
                    state.value.copy(
                        lightScreenshot = null,
                        darkScreenshot = null,
                        processingScreenshotTarget = ScreenshotTarget.BOTH
                    )
                } else {
                    when (initialTheme) {
                        UiTheme.LIGHT -> state.value.copy(
                            lightScreenshot = null,
                            processingScreenshotTarget = ScreenshotTarget.LIGHT
                        )
                        UiTheme.DARK -> state.value.copy(
                            darkScreenshot = null,
                            processingScreenshotTarget = ScreenshotTarget.DARK
                        )
                    }
                }
                state.value = refreshState
            }.onEach { screenshot ->
                when (screenshot.theme) {
                    UiTheme.LIGHT -> state.value = state.value.copy(
                        lightScreenshot = screenshot.image
                    )
                    UiTheme.DARK -> state.value = state.value.copy(
                        darkScreenshot = screenshot.image
                    )
                }
                if (
                    state.value.processingScreenshotTarget == ScreenshotTarget.BOTH &&
                    screenshot.hasNextTarget
                ) {
                    deviceWrapper.changeUiTheme(
                        uiTheme = screenshot.theme.toggle(),
                        sleepTime = sleepTime
                    )
                }
            }.onCompletion { throwable ->
                if (state.value.processingScreenshotTarget == ScreenshotTarget.BOTH) {
                    deviceWrapper.changeUiTheme(
                        uiTheme = initialTheme,
                        sleepTime = 0
                    )
                }
                state.value = state.value.copy(
                    deviceNotFoundError = throwable is AdbCommandRejectedException,
                    onScreenshot = null,
                    processingScreenshotTarget = null
                )
                screenshotTime.value = getTimeString()
            }.launchIn(this)
        }
    }
}

private fun getTimeString(): String {
    return LocalDateTime.now().let { datetime ->
        val pattern = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        pattern.format(datetime)
    }
}
