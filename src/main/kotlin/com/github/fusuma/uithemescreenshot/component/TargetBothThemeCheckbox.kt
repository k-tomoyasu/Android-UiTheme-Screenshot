package com.github.fusuma.uithemescreenshot.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TargetBothThemeCheckbox(
    enabled: Boolean,
    isTakeBothTheme: Boolean,
    onCheck: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = "Capture in both theme?"
        )
        Checkbox(
            checked = isTakeBothTheme,
            onCheckedChange = onCheck,
            enabled = enabled
        )
    }
}
