package com.github.fusuma.uithemescreenshot.component

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.fusuma.uithemescreenshot.theme.ScreenshotTheme

@Composable
fun TakeScreenshotButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled,
        ) {
            Text("Take Screenshot")
        }
    }
}

@Preview
@Composable
fun TakeScreenshotButtonPreview() {
    Text("aa")
}