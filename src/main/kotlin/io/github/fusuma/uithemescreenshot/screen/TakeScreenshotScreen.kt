package io.github.fusuma.uithemescreenshot.screen

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import io.github.fusuma.uithemescreenshot.component.*
import io.github.fusuma.uithemescreenshot.model.ScreenState
import io.github.fusuma.uithemescreenshot.model.UiTheme
import io.github.fusuma.uithemescreenshot.theme.ScreenshotThemePreview

@Composable
fun ScreenshotScreen(
    state: ScreenState,
    onSave: (ImageBitmap, UiTheme) -> Unit,
    onResizeScaleChange: (Float) -> Unit,
    onSelectDevice: (Int) -> Unit,
    onClickRefreshDeviceList: () -> Unit,
    onCheckTakeBothTheme: (Boolean) -> Unit,
    onToggleTheme: () -> Unit,
    onClickTakeScreenshot: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DeviceList(
                enabled = state.deviceListSelectable,
                deviceNameList = state.deviceNameList,
                deviceNotFoundError = state.deviceNotFoundError,
                selectedIndex = state.selectedIndex,
                onSelect = onSelectDevice,
                onRefresh = onClickRefreshDeviceList
            )
            Spacer(Modifier.height(10.dp))
            ScaleSlider(
                scale = state.resizeScale,
                enabled = state.scaleSliderEnabled,
                onValueChange = onResizeScaleChange
            )
            Spacer(Modifier.height(10.dp))
            Row {
                ToggleUiThemeButton(
                    enabled = state.toggleButtonEnabled,
                    onClick = onToggleTheme,
                )
                Spacer(Modifier.width(10.dp))
                TakeScreenshotButton(
                    enabled = state.screenshotButtonEnabled,
                    onClick = onClickTakeScreenshot,
                )
            }
            TargetBothThemeCheckbox(
                enabled = state.targetCheckboxEnabled,
                isTakeBothTheme = state.isTakeBothTheme,
                onCheck = onCheckTakeBothTheme,
            )
            Spacer(Modifier.height(10.dp))
            ScreenshotContainerRow(
                isLightScreenShotProcessing = state.isLightScreenshotProcessing,
                isDarkScreenShotProcessing = state.isDarkScreenshotProcessing,
                lightScreenShot = state.lightScreenshot,
                darkScreenShot = state.darkScreenshot,
                onSave = onSave
            )
        }
    }
}

@Preview
@Composable
fun ScreenPreview() {
    ScreenshotThemePreview {
        ScreenshotScreen(
            ScreenState(
                deviceNameList = listOf("emulator1", "emulator2"),
            ),
            { _, _ -> },
            {},
            {},
            {},
            {},
            {},
            {},
        )
    }
}