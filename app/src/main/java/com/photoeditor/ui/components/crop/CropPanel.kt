package com.photoeditor.ui.components.crop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photoeditor.data.model.AspectRatio
import com.photoeditor.data.model.CropState
import com.photoeditor.ui.components.adjust.HorizontalValueSlider
import com.photoeditor.ui.theme.LightGray
import com.photoeditor.ui.theme.SubtleGray
import com.photoeditor.ui.theme.TextGray
import com.photoeditor.ui.theme.iOSYellow

@Composable
fun CropPanel(
    cropState: CropState,
    onCropChanged: (CropState) -> Unit,
    onAspectRatioChanged: (AspectRatio) -> Unit,
    onRotationChanged: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Rotation controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onRotationChanged(cropState.rotation - 90f) }) {
                Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Straighten",
                    color = TextGray,
                    fontSize = 12.sp
                )
                Text(
                    text = "${cropState.rotation.toInt()}°",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(onClick = { onRotationChanged(cropState.rotation + 90f) }) {
                Icon(Icons.Default.RotateRight, contentDescription = "Rotate Right", tint = Color.White)
            }
        }

        // Straighten slider
        HorizontalValueSlider(
            value = cropState.rotation % 360f,
            onValueChange = { onRotationChanged(it) },
            valueRange = -45f..45f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Aspect ratio chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AspectRatio.entries) { ratio ->
                AspectRatioChip(
                    ratio = ratio,
                    isSelected = cropState.aspectRatio == ratio,
                    onClick = { onAspectRatioChanged(ratio) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Reset button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onReset) {
                Text(
                    text = "Reset",
                    color = iOSYellow,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun AspectRatioChip(
    ratio: AspectRatio,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) iOSYellow.copy(alpha = 0.15f) else Color(0xFF2C2C2E),
                RoundedCornerShape(20.dp)
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) iOSYellow else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ratio.label,
            color = if (isSelected) iOSYellow else SubtleGray,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
