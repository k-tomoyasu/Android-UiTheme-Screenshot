package com.github.fusuma.uithemescreenshot

import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.android.ddmlib.AdbCommandRejectedException
import com.android.ddmlib.IDevice
import com.android.ddmlib.NullOutputReceiver
import com.github.fusuma.uithemescreenshot.image.resizeImage
import com.github.fusuma.uithemescreenshot.image.saveImage
import com.github.fusuma.uithemescreenshot.model.*
import com.github.fusuma.uithemescreenshot.receiver.UIThemeDetectReceiver
import com.github.fusuma.uithemescreenshot.theme.ScreenshotTheme
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.swing.JComponent

private const val sleepTime = 3000L
class ScreenshotAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {
            ScreenshotDialog(it).show()
        }
    }

    class ScreenshotDialog(private val project: Project) : DialogWrapper(project, false, IdeModalityType.MODELESS) {
        private val bridge get() = AndroidSdkUtils.getDebugBridge(project)

        private var screenshotTime = ""
        override fun createActions() = arrayOf(cancelAction)

        init {
            title = "UiTheme Screenshot"
            init()
        }

        override fun createCenterPanel(): JComponent {
            return ComposePanel().apply {
                setBounds(0, 0, 800, 800)
                setContent {
                    val state = remember { mutableStateOf(ScreenState()) }
                    val selectedDevice = remember(state.value.selectedIndex) {
                        val device = getDevice(state.value.selectedIndex)
                        if (device == null) {
                            state.value = state.value.copy(
                                deviceNotFoundError = true
                            )
                        }
                        device
                    }

                    LaunchedEffect(Unit) {
                        state.value = state.value.copy(
                            deviceNameList = getConnectedDeviceNames()
                        )
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
                                    screenshotTime,
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

                    state.value.onRefreshDevice?.let {
                        LaunchedEffect(Unit) {
                            state.value = state.value.copy(
                                deviceNameList = getConnectedDeviceNames(),
                                deviceNotFoundError = false,
                            )
                            state.value = state.value.copy(
                                onRefreshDevice = null
                            )
                        }
                    }

                    state.value.onToggleTheme?.let {
                        LaunchedEffect(Unit) {
                            state.value = state.value.copy(
                                deviceNotFoundError = false,
                            )
                            selectedDevice?.let {
                                try {
                                    changeUiTheme(
                                        it,
                                        getCurrentUiTheme(it).toggle(),
                                        0
                                    )
                                } catch (e: AdbCommandRejectedException) {
                                    state.value = state.value.copy(
                                        deviceNotFoundError = true
                                    )
                                }
                            }
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
                            val device = getDevice(state.value.selectedIndex)
                            if (device == null || device.serialNumber != selectedDevice?.serialNumber) {
                                state.value = state.value.copy(
                                    deviceNotFoundError = true,
                                    onScreenshot = null,
                                )
                                return@LaunchedEffect
                            }
                            val screenshotFlow = if (state.value.isTakeBothTheme) {
                                state.value = state.value.copy(
                                    lightScreenshot = null,
                                    darkScreenshot = null,
                                    processingScreenshotTarget = ScreenshotTarget.BOTH
                                )
                                takeScreenshots(
                                    device,
                                    state.value.resizeScale,
                                    getCurrentUiTheme(device),
                                )
                            } else {
                                val currentUiTheme = getCurrentUiTheme(device)
                                when (currentUiTheme) {
                                    UiTheme.LIGHT -> state.value = state.value.copy(
                                        lightScreenshot = null,
                                        processingScreenshotTarget = ScreenshotTarget.LIGHT
                                    )
                                    UiTheme.DARK -> state.value = state.value.copy(
                                        darkScreenshot = null,
                                        processingScreenshotTarget = ScreenshotTarget.DARK
                                    )
                                }
                                takeScreenshot(device, state.value.resizeScale, currentUiTheme, null)
                            }
                            screenshotFlow.onEach { screenshot ->
                                val nextTarget = screenshot.nextTargetTheme
                                if (state.value.processingScreenshotTarget == ScreenshotTarget.BOTH) {
                                    changeUiTheme(
                                        device = device,
                                        uiTheme = nextTarget ?: screenshot.theme.toggle(),
                                        sleepTime = if (nextTarget == null)
                                            0
                                        else
                                            sleepTime,
                                    )
                                }
                            }.onCompletion { throwable ->
                                state.value = state.value.copy(
                                    deviceNotFoundError = throwable is AdbCommandRejectedException,
                                    onScreenshot = null,
                                    processingScreenshotTarget = null
                                )
                                val datetime = LocalDateTime.now().let { datetime ->
                                    val pattern = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                                    pattern.format(datetime)
                                }
                                screenshotTime = datetime
                            }.collect { screenshot ->
                                when (screenshot.theme) {
                                    UiTheme.LIGHT -> state.value = state.value.copy(
                                        lightScreenshot = screenshot.image
                                    )
                                    UiTheme.DARK -> state.value = state.value.copy(
                                        darkScreenshot = screenshot.image
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        private suspend fun refreshPreScreenshot(device: IDevice, isTakeBoth: Boolean) {

        }

        private suspend fun getCurrentUiTheme(device: IDevice) : UiTheme {
            return withContext(Dispatchers.IO) {
                val receiver = UIThemeDetectReceiver()
                device.executeShellCommand(
                    "cmd uimode night",
                    receiver,
                    10L,
                    TimeUnit.SECONDS
                )
                requireNotNull(receiver.currentUiTheme())
            }
        }

        private fun takeScreenshots(
            device: IDevice,
            scale: Float,
            currentUIMode: UiTheme,
        ): Flow<ScreenshotResult> {
            val targetThemes = when (currentUIMode) {
                UiTheme.LIGHT -> {
                    UiTheme.LIGHT to UiTheme.DARK
                }
                UiTheme.DARK -> {
                    UiTheme.DARK to UiTheme.LIGHT
                }
            }
            return flowOf(
                takeScreenshot(device, scale, targetThemes.first, targetThemes.first.toggle()),
                takeScreenshot(device, scale, targetThemes.second, null),
            ).flattenConcat()
        }

        private fun takeScreenshot(device: IDevice, scale: Float, theme: UiTheme, nextTargetTheme: UiTheme?) : Flow<ScreenshotResult> = flow {
            val screenshot = device.getScreenshot(
                10,
                TimeUnit.SECONDS
            ).asBufferedImage()
            emit(
                ScreenshotResult(
                    theme,
                    resizeImage(screenshot, scale).toComposeImageBitmap(),
                    nextTargetTheme
                )
            )
        }.flowOn(Dispatchers.IO)

        private fun getDevice(index: Int) = bridge?.devices?.getOrNull(index)

        private fun getConnectedDeviceNames() = bridge?.devices?.map { it.name }.orEmpty().toImmutableList()

        private suspend fun changeUiTheme(device: IDevice, uiTheme: UiTheme, sleepTime: Long) {
            withContext(Dispatchers.IO) {
                device.executeShellCommand(
                    "cmd uimode night ${uiTheme.nightYesNo}",
                    NullOutputReceiver(),
                    10,
                    TimeUnit.SECONDS
                )
                delay(sleepTime)
            }
        }
    }
}

