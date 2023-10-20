package com.github.fusuma.uithemescreenshot

import androidx.compose.ui.awt.ComposePanel
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.WindowManager
import org.jetbrains.android.sdk.AndroidSdkUtils
import javax.swing.JComponent

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
                    ScreenshotPanel(
                        project,
                        statusBar,
                        ::getDevice,
                        ::getConnectedDeviceNames,
                    )
                }
            }
        }

        private fun getDevice(index: Int) = bridge.devices.getOrNull(index)

        private fun getConnectedDeviceNames() = bridge.devices.map { it.name }
    }
}

