package com.photoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.photoeditor.ui.theme.TextGray
import com.photoeditor.ui.theme.iOSYellow

@Composable
fun EditTabBar(
    selectedTab: EditTab,
    onTabSelected: (EditTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(Color.Black)) {
        HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItem(
                icon = Icons.Default.Tune,
                label = "Adjust",
                selected = selectedTab == EditTab.ADJUST,
                onClick = { onTabSelected(EditTab.ADJUST) }
            )
            TabItem(
                icon = Icons.Default.FilterVintage,
                label = "Filters",
                selected = selectedTab == EditTab.FILTERS,
                onClick = { onTabSelected(EditTab.FILTERS) }
            )
            TabItem(
                icon = Icons.Default.Crop,
                label = "Crop",
                selected = selectedTab == EditTab.CROP,
                onClick = { onTabSelected(EditTab.CROP) }
            )
        }
    }
}

@Composable
private fun TabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (selected) iOSYellow else SubtleGray
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
