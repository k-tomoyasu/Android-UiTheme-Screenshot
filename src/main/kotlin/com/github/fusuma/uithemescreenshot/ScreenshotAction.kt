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
import com.github.fusuma.uithemescreenshot.model.ScreenState
import com.github.fusuma.uithemescreenshot.model.ScreenshotTarget
import com.github.fusuma.uithemescreenshot.model.UiTheme
import com.github.fusuma.uithemescreenshot.receiver.UIThemeDetectReceiver
import com.github.fusuma.uithemescreenshot.theme.ScreenshotTheme
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.awt.image.BufferedImage
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
                                deviceNotFoundError = Unit
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
                                deviceNotFoundError = null,
                            )
                            state.value = state.value.copy(
                                onRefreshDevice = null
                            )
                        }
                    }

                    state.value.onToggleTheme?.let {
                        LaunchedEffect(Unit) {
                            state.value = state.value.copy(
                                deviceNotFoundError = null,
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
                                        deviceNotFoundError = Unit
                                    )
                                }
                            }
                            state.value = state.value.copy(
                                onToggleTheme = null
                            )
                        }
                    }

                    state.value.onScreenshot?.let { targetUiTheme ->
                        LaunchedEffect(Unit) {
                            state.value = state.value.copy(
                                deviceNotFoundError = null,
                            )
                            val device = getDevice(state.value.selectedIndex)
                            if (device == null || device.serialNumber != selectedDevice?.serialNumber) {
                                state.value = state.value.copy(
                                    deviceNotFoundError = Unit,
                                    onScreenshot = null,
                                )
                                return@LaunchedEffect
                            }
                            try {
                                if (state.value.isTakeBothTheme) {
                                    state.value = state.value.copy(
                                        lightScreenshot = null,
                                        darkScreenshot = null,
                                        processingScreenshotTarget = ScreenshotTarget.BOTH
                                    )
                                    takeScreenshots(
                                        device,
                                        getCurrentUiTheme(device),
                                        state
                                    )
                                } else {
                                    val currentUiTheme = getCurrentUiTheme(device)
                                    when (currentUiTheme) {
                                        UiTheme.LIGHT -> {
                                            state.value = state.value.copy(
                                                lightScreenshot = null,
                                                processingScreenshotTarget = ScreenshotTarget.LIGHT
                                            )
                                            state.value = state.value.copy(
                                                lightScreenshot = takeScreenshot(device, state.value.resizeScale).toComposeImageBitmap()
                                            )
                                        }
                                        UiTheme.DARK -> {
                                            state.value = state.value.copy(
                                                darkScreenshot = null,
                                                processingScreenshotTarget = ScreenshotTarget.DARK
                                            )
                                            state.value = state.value.copy(
                                                darkScreenshot = takeScreenshot(device, state.value.resizeScale).toComposeImageBitmap()
                                            )
                                        }
                                    }
                                }
                            } catch (e: AdbCommandRejectedException) {
                                state.value = state.value.copy(
                                    deviceNotFoundError = Unit
                                )
                            } finally {
                                state.value = state.value.copy(
                                    onScreenshot = null,
                                    processingScreenshotTarget = null
                                )
                            }
                        }
                    }
                }
            }
        }

        private suspend fun getCurrentUiTheme(device: IDevice) : UiTheme {
            return withContext(Dispatchers.IO) {
                val receiver = UIThemeDetectReceiver()
                device.executeShellCommand(
                    "cmd uimode night",
                    receiver,
                )
                requireNotNull(receiver.currentUiTheme())
            }
        }
        private suspend fun takeScreenshots(
            device: IDevice,
            currentUIMode: UiTheme,
            uiState: MutableState<ScreenState>,
        ) {
            val scale = uiState.value.resizeScale
            when (currentUIMode) {
                UiTheme.LIGHT -> {
                    val light = takeScreenshot(device, scale).toComposeImageBitmap()
                    uiState.value = uiState.value.copy(
                        lightScreenshot = light
                    )
                    changeUiTheme(device, UiTheme.DARK, sleepTime)
                    val dark = takeScreenshot(device, scale).toComposeImageBitmap()
                    uiState.value = uiState.value.copy(
                        darkScreenshot = dark
                    )
                    changeUiTheme(device, UiTheme.LIGHT, 0)
                }
                UiTheme.DARK -> {
                    val dark = takeScreenshot(device, scale).toComposeImageBitmap()
                    uiState.value = uiState.value.copy(
                        darkScreenshot = dark
                    )
                    changeUiTheme(device, UiTheme.LIGHT, sleepTime)
                    val light = takeScreenshot(device, scale).toComposeImageBitmap()
                    uiState.value = uiState.value.copy(
                        lightScreenshot = light
                    )
                    changeUiTheme(device, UiTheme.DARK, 0)
                }
            }
            val datetime = LocalDateTime.now().let { datetime ->
                val pattern = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                pattern.format(datetime)
            }
            screenshotTime = datetime
        }

        private fun getDevice(index: Int) = bridge?.devices?.getOrNull(index)

        private fun getConnectedDeviceNames() = bridge?.devices?.map { it.name }.orEmpty().toImmutableList()
        private suspend fun takeScreenshot(device: IDevice, scale: Float) : BufferedImage {
            return withContext(Dispatchers.IO) {
                val screenshot = device.getScreenshot(
                    10,
                    TimeUnit.SECONDS
                )
                resizeImage(
                    screenshot.asBufferedImage(),
                    scale
                )
            }
        }

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

