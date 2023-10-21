package com.github.fusuma.uithemescreenshot

import androidx.compose.ui.awt.ComposePanel
import com.github.fusuma.uithemescreenshot.adb.AdbDeviceWrapperImpl
import com.github.fusuma.uithemescreenshot.image.saveImage
import com.github.fusuma.uithemescreenshot.model.UiTheme
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.android.sdk.AndroidSdkUtils
import java.awt.image.BufferedImage
import java.time.LocalDateTime
import javax.swing.JComponent

class ScreenshotAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {
            ScreenshotDialog(it).show()
        }
    }

    class ScreenshotDialog(private val project: Project) : DialogWrapper(project, false, IdeModalityType.MODELESS) {
        private val bridge = requireNotNull(AndroidSdkUtils.getDebugBridge(project))

        override fun createActions() = arrayOf(cancelAction)

        init {
            title = "UiTheme Screenshot"
            init()
        }

        override fun createCenterPanel(): JComponent {
            return ComposePanel().apply {
                setBounds(0, 0, 800, 800)
                setContent {
                    ScreenshotPanel(
                        saveImage = { image: BufferedImage, theme: UiTheme, datetime: LocalDateTime ->
                            saveImage(image, theme, datetime, project)
                        },
                        getDevice = { index ->
                            AdbDeviceWrapperImpl(bridge.devices.getOrNull(index))
                        },
                        getConnectedDeviceNames = {
                            bridge.devices.map { it.name }
                        }
                    )
                }
            }
        }
    }
}

