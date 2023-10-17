package com.github.fusuma.uithemescreenshot.component

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TakeScreenshotButton(
    modifier: Modifier = Modifier,
    isProcessingScreenshot: Boolean,
    isInvalidResizeScale: Boolean,
    deviceExists: Boolean,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        enabled = (!isProcessingScreenshot && !isInvalidResizeScale && deviceExists)
    ) {
        Text("Take Screenshot")
    }
}
