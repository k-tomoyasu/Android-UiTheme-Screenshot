package com.github.fusuma.uithemescreenshot.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DeviceList(
    modifier: Modifier = Modifier,
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
            text = "Connected Devices",
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
            deviceNameList = deviceNameList,
            selectedIndex = selectedIndex,
            onSelect = onSelect
        )
    }
}
@Composable
private fun DeviceNameSelection(
    deviceNameList: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    deviceNameList.forEachIndexed { i, deviceName ->
        Row(
            Modifier.selectable(
                selected = i == selectedIndex,
                onClick = { onSelect(i) }
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
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
