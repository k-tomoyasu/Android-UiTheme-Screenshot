package com.github.fusuma.uithemescreenshot

import androidx.compose.runtime.*
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.graphics.toAwtImage
import com.android.ddmlib.AdbCommandRejectedException
import com.github.fusuma.uithemescreenshot.adb.AdbDeviceWrapper
import com.github.fusuma.uithemescreenshot.adb.AdbError
import com.github.fusuma.uithemescreenshot.adb.AdbWrapperImpl
import com.github.fusuma.uithemescreenshot.image.saveImage
import com.github.fusuma.uithemescreenshot.model.*
import com.github.fusuma.uithemescreenshot.theme.ScreenshotTheme
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.WindowManager
import kotlinx.coroutines.flow.*
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JComponent

private const val sleepTime = 3000L
class ScreenshotAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {
            ScreenshotDialog(it).show()
        }
    }

    class ScreenshotDialog(private val project: Project) : DialogWrapper(project, false, IdeModalityType.MODELESS) {
        private val bridge get() = requireNotNull(AndroidSdkUtils.getDebugBridge(project))

        private val statusBar = WindowManager.getInstance().getStatusBar(project)

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
                    val deviceWrapper = remember(state.value.selectedIndex) {
                        val device = getDevice(state.value.selectedIndex)
                        if (device == null) {
                            state.value = state.value.copy(
                                deviceNotFoundError = true
                            )
                        }
                        AdbWrapperImpl(device)
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
                                onRefreshDevice = null
                            )
                        }
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
                            val currentUiTheme = deviceWrapper.getCurrentUiTheme()
                            val screenshotFlow = deviceWrapper.screenshotFlow(
                                state.value.resizeScale,
                                state.value.isTakeBothTheme,
                                currentUiTheme
                            )
                            screenshotFlow.onStart {
                                val refreshState = if (state.value.isTakeBothTheme) {
                                    state.value.copy(
                                        lightScreenshot = null,
                                        darkScreenshot = null,
                                        processingScreenshotTarget = ScreenshotTarget.BOTH
                                    )
                                } else {
                                    when (currentUiTheme) {
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
                                if (state.value.processingScreenshotTarget == ScreenshotTarget.BOTH) {
                                    val nextTarget = screenshot.nextTargetTheme
                                    deviceWrapper.changeUiTheme(
                                        uiTheme = nextTarget ?: screenshot.theme.toggle(),
                                        sleepTime = if (nextTarget == null) 0 else sleepTime,
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

        private fun getDevice(index: Int) = bridge.devices.getOrNull(index)

        private fun getConnectedDeviceNames() = bridge.devices.map { it.name }
    }
}

