package io.github.fusuma.uithemescreenshot.component

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.fusuma.uithemescreenshot.ifTrue
import io.github.fusuma.uithemescreenshot.theme.ScreenshotThemePreview

@Composable
fun DeviceList(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    deviceNameList: List<String>,
    deviceNotFoundError: Boolean,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Connected Device",
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(6.dp))
        OutlinedButton(
            onClick = onRefresh
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    if (deviceNotFoundError) {
        Text(
            "Selected Device Not Connected.",
            color = Color.Red
        )
    }
    if (deviceNameList.isEmpty()) {
        Text(
            "No Device Detected.",
            color = Color.Gray
        )
    } else {
        DeviceNameSelection(
            enabled = enabled,
            deviceNameList = deviceNameList,
            selectedIndex = selectedIndex,
            onSelect = onSelect
        )
    }
}

@Composable
private fun DeviceNameSelection(
    enabled: Boolean,
    deviceNameList: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column {
        deviceNameList.forEachIndexed { i, deviceName ->
            Row(
                Modifier
                    .ifTrue(enabled) {
                        Modifier.selectable(
                            selected = i == selectedIndex,
                            onClick = { onSelect(i) }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    enabled = enabled,
                    selected = i == selectedIndex,
                    onClick = { onSelect(i) }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = deviceName,
                )
            }
        }
    }
}

@Preview
@Composable
fun SelectionPreview() {
    ScreenshotThemePreview {
        DeviceNameSelection(
            true,
            listOf("aaa", "bbbb", "cccc"),
            1,
            {}
        )
    }
}