package com.github.fusuma.uithemescreenshot.component

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.fusuma.uithemescreenshot.theme.ScreenshotTheme

@Composable
fun ScaleSlider(
    modifier: Modifier = Modifier,
    scale: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    var position by remember { mutableFloatStateOf(scale) }

    Text(
        text = AnnotatedString.Builder("Screenshot Scale: ").also {
            it.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            it.append("${(scale * 100).toInt()}%")
            it.pop()
        }.toAnnotatedString()
    )
    Slider(
        modifier = modifier.padding(horizontal = 60.dp),
        value = position,
        enabled = enabled,
        onValueChange = { position = it },
        valueRange = 0.2f..1f,
        onValueChangeFinished = {
            onValueChange(position)
        },
        steps = 3
    )
}
@Preview
@Composable
private fun ScaleSliderPreview() {
    ScreenshotTheme {
        ScaleSlider(
            scale = 0.6f,
            enabled = true,
            onValueChange = {}
        )
    }
}

