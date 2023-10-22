package io.github.fusuma.uithemescreenshot.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import io.github.fusuma.uithemescreenshot.theme.ScreenshotTheme

@Composable
fun ScreenshotContainer(
    modifier: Modifier,
    isProcessingScreenshot: Boolean,
    screenshot: ImageBitmap?,
    onSave: (ImageBitmap) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(2.dp, Color.Gray, RoundedCornerShape(4.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isProcessingScreenshot) {
            CircularProgressIndicator()
        }
        screenshot?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null
            )
            OutlinedButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp)
                ,
                onClick = {
                    onSave(image)
                },
                border = BorderStroke(1.dp,  MaterialTheme.colors.primary)
            ) {
                Text("Save")
            }
        }
    }
}