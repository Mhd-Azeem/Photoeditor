package com.photoeditor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photoeditor.data.model.EditTab
import com.photoeditor.ui.theme.SubtleGray
import com.photoeditor.ui.theme.iOSYellow

@Composable
fun EditBottomBar(
    selectedTab: EditTab,
    onTabSelected: (EditTab) -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Cancel button
        TextButton(onClick = onCancel) {
            Text(
                text = "Cancel",
                color = iOSYellow,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // Center: 3 icon tabs (no text labels — exact iOS style)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TabIconButton(
                icon = Icons.Outlined.WbSunny,
                isSelected = selectedTab == EditTab.ADJUST,
                onClick = { onTabSelected(EditTab.ADJUST) }
            )
            TabIconButton(
                icon = Icons.Outlined.FilterVintage,
                isSelected = selectedTab == EditTab.FILTERS,
                onClick = { onTabSelected(EditTab.FILTERS) }
            )
            TabIconButton(
                icon = Icons.Default.Crop,
                isSelected = selectedTab == EditTab.CROP,
                onClick = { onTabSelected(EditTab.CROP) }
            )
        }

        // Done button
        TextButton(onClick = onDone) {
            Text(
                text = "Done",
                color = iOSYellow,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TabIconButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) iOSYellow else SubtleGray,
            modifier = Modifier.size(26.dp)
        )
    }
}
