package com.github.fusuma.uithemescreenshot.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.github.fusuma.uithemescreenshot.model.UiTheme

@Composable
fun ScreenshotContainerRow(
    modifier: Modifier = Modifier,
    lightScreenShot: ImageBitmap?,
    isLightScreenShotProcessing: Boolean,
    isDarkScreenShotProcessing: Boolean,
    darkScreenShot: ImageBitmap?,
    onSave: (ImageBitmap, UiTheme) -> Unit
) {
    Row(modifier = modifier) {
        ScreenshotContainer(
            modifier = Modifier.weight(1f),
            isLightScreenShotProcessing,
            lightScreenShot,
            onSave = { image ->
                onSave(image, UiTheme.LIGHT)
            }
        )
        Spacer(Modifier.width(20.dp))
        ScreenshotContainer(
            modifier = Modifier.weight(1f),
            isDarkScreenShotProcessing,
            darkScreenShot,
            onSave = { image ->
                onSave(image, UiTheme.DARK)
            }
        )
    }
}