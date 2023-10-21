package com.github.fusuma.uithemescreenshot

import androidx.compose.runtime.Composable
import com.github.fusuma.uithemescreenshot.adb.AdbDeviceWrapper
import com.github.fusuma.uithemescreenshot.model.UiTheme
import com.github.fusuma.uithemescreenshot.model.useScreenshotScreenState
import com.github.fusuma.uithemescreenshot.screen.ScreenshotScreen
import com.github.fusuma.uithemescreenshot.theme.ScreenshotTheme
import java.awt.image.BufferedImage
import java.time.LocalDateTime

@Composable
fun ScreenshotPanel(
    saveImage: (BufferedImage, UiTheme, LocalDateTime) -> Unit,
    getDevice: (Int) -> AdbDeviceWrapper,
    getConnectedDeviceNames: () -> List<String>
) {
    val stateController = useScreenshotScreenState(
        getConnectedDeviceNames = getConnectedDeviceNames,
        getDevice  = getDevice,
        saveImage = saveImage,
        getLocalDateTime = { LocalDateTime.now() },
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

