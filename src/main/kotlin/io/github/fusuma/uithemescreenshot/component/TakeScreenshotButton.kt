package io.github.fusuma.uithemescreenshot.component

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.fusuma.uithemescreenshot.theme.ScreenshotThemePreview

@Composable
fun TakeScreenshotButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
    ) {
        Text("Take Screenshot")
    }
}

@Preview
@Composable
fun TakeScreenshotButtonPreview() {
    ScreenshotThemePreview {
        TakeScreenshotButton(
            enabled = true,
            onClick = {},
        )
    }
}