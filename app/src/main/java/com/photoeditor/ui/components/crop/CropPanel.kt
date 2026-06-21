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
import com.photoeditor.ui.theme.SubtleGray
import com.photoeditor.ui.theme.TextGray
import com.photoeditor.ui.theme.White
import com.photoeditor.ui.theme.iOSYellow
import kotlin.math.roundToInt

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
        // Rotation label (above the dial)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STRAIGHTEN",
                color = TextGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            )
            val deg = cropState.rotation.roundToInt()
            if (deg != 0) {
                Text(
                    text = "  ${if (deg > 0) "+$deg" else "$deg"}°",
                    color = White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Aspect ratio chips + rotate buttons in one row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { onRotationChanged(cropState.rotation - 90f) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.RotateLeft, contentDescription = "Rotate Left",
                    tint = SubtleGray, modifier = Modifier.size(22.dp))
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(AspectRatio.entries) { ratio ->
                    AspectRatioChip(
                        ratio = ratio,
                        isSelected = cropState.aspectRatio == ratio,
                        onClick = { onAspectRatioChanged(ratio) }
                    )
                }
            }

            IconButton(
                onClick = { onRotationChanged(cropState.rotation + 90f) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.RotateRight, contentDescription = "Rotate Right",
                    tint = SubtleGray, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
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
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Color(0xFF2C2C2E) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) iOSYellow else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = ratio.label,
            color = if (isSelected) iOSYellow else SubtleGray,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
