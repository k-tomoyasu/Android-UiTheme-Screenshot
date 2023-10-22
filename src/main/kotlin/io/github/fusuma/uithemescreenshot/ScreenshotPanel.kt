package io.github.fusuma.uithemescreenshot

import androidx.compose.runtime.Composable
import io.github.fusuma.uithemescreenshot.adb.AdbDeviceWrapper
import io.github.fusuma.uithemescreenshot.model.UiTheme
import io.github.fusuma.uithemescreenshot.model.useScreenshotScreenState
import io.github.fusuma.uithemescreenshot.screen.ScreenshotScreen
import io.github.fusuma.uithemescreenshot.theme.ScreenshotTheme
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

