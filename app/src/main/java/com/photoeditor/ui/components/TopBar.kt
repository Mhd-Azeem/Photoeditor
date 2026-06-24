package com.photoeditor.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photoeditor.data.model.EditTab
import com.photoeditor.ui.theme.SubtleGray
import com.photoeditor.ui.theme.White

@Composable
fun EditTopBar(
    selectedTab: EditTab,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Centered section title — all caps, letter-spaced like iOS
        Text(
            text = when (selectedTab) {
                EditTab.ADJUST  -> "ADJUST"
                EditTab.FILTERS -> "FILTERS"
                EditTab.CROP    -> "CROP"
            },
            color = White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        // Left side: undo / redo
        Row(modifier = Modifier.align(Alignment.CenterStart)) {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) White else SubtleGray
                )
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    imageVector = Icons.Default.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) White else SubtleGray
                )
            }
        }

        // "..." overflow menu on right
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = onMore) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "More options",
                    tint = SubtleGray
                )
            }
        }
    }
}
