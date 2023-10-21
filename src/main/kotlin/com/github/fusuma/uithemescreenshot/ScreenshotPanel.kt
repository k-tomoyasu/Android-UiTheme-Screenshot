package com.github.fusuma.uithemescreenshot

import androidx.compose.runtime.Composable
import com.github.fusuma.uithemescreenshot.adb.AdbDeviceWrapper
import com.github.fusuma.uithemescreenshot.image.saveImage
import com.github.fusuma.uithemescreenshot.model.UiTheme
import com.github.fusuma.uithemescreenshot.model.useScreenshotState
import com.github.fusuma.uithemescreenshot.screen.ScreenshotScreen
import com.github.fusuma.uithemescreenshot.theme.ScreenshotTheme
import com.intellij.openapi.project.Project
import java.awt.image.BufferedImage
import java.time.LocalDateTime

@Composable
fun ScreenshotPanel(
    project: Project,
    getDevice: (Int) -> AdbDeviceWrapper,
    getConnectedDeviceNames: () -> List<String>
) {
    val stateController = useScreenshotState(
        getConnectedDeviceNames = getConnectedDeviceNames,
        getDevice  = getDevice,
        getLocalDateTime = { LocalDateTime.now() },
        saveImage = { image: BufferedImage, theme: UiTheme, datetime: LocalDateTime ->
            saveImage(image, theme, datetime, project)
        }
    )

    ScreenshotTheme {
        ScreenshotScreen(
            state = stateController.state,
            onSelectDevice = stateController.onSelectDevice,
            onResizeScaleChange = stateController.onResizeScaleChange,
            onClickTakeScreenshot = stateController.onTakeScreenshot,
            onClickRefreshDeviceList = stateController.onRefreshDeviceList,
            onSave = stateController.onSave,
            onCheckTakeBothTheme = stateController.onCheckTakeBothTheme,
            onToggleTheme = stateController.onToggleTheme
        )
    }
}

