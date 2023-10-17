package com.github.fusuma.uithemescreenshot

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.github.fusuma.uithemescreenshot.component.DeviceList
import com.github.fusuma.uithemescreenshot.component.ScaleSlider
import com.github.fusuma.uithemescreenshot.component.ScreenshotContainerRow
import com.github.fusuma.uithemescreenshot.component.TakeScreenshotButton
import com.github.fusuma.uithemescreenshot.model.ScreenState
import com.github.fusuma.uithemescreenshot.model.UiTheme
import com.github.fusuma.uithemescreenshot.theme.ScreenshotTheme

@Composable
fun TakeScreenshotScreen(
    state: ScreenState,
    onSave: (ImageBitmap, UiTheme) -> Unit,
    onResizeScaleChange: (Float) -> Unit,
    onSelectDevice: (Int) -> Unit,
    onClickRefreshDeviceList: () -> Unit,
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
                deviceNameList = state.deviceNameList,
                deviceNotFoundError = state.deviceNotFoundError != null,
                selectedIndex = state.selectedIndex,
                onSelect = onSelectDevice,
                onRefresh = onClickRefreshDeviceList
            )
            Spacer(Modifier.height(10.dp))
            ScaleSlider(
                scale = state.resizeScale,
                isProcessingScreenshot = state.isScreenshotProcessing,
                onValueChange = onResizeScaleChange
            )
            Spacer(Modifier.height(10.dp))
            TakeScreenshotButton(
                isProcessingScreenshot = state.isScreenshotProcessing,
                isInvalidResizeScale = state.isInvalidResizeScale,
                deviceExists = state.deviceExists,
                onClick = onClickTakeScreenshot
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
    ScreenshotTheme {
        TakeScreenshotScreen(
            ScreenState(
                deviceNameList = listOf("emulator1", "emulator2")
            ),
            { _, _ -> },
            {},
            {},
            {},
            {},
        )
    }
}