package com.photoeditor.ui.components.adjust

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vignette
import androidx.compose.material.icons.filled.WbShade
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photoeditor.data.model.AdjustmentType
import com.photoeditor.data.model.EditState
import com.photoeditor.data.model.allAdjustments
import com.photoeditor.data.model.config
import com.photoeditor.ui.theme.SubtleGray
import com.photoeditor.ui.theme.TextGray
import com.photoeditor.ui.theme.iOSYellow
import kotlin.math.roundToInt

@Composable
fun AdjustPanel(
    editState: EditState,
    selectedAdjustment: AdjustmentType?,
    onAdjustmentSelected: (AdjustmentType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Adjustment name + value display row (iOS-style: centered label above dial)
        val selValue = selectedAdjustment?.let { editState.getAdjustmentValue(it) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(28.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedAdjustment != null && selValue != null) {
                Text(
                    text = selectedAdjustment.config().label.uppercase(),
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                val v = selValue.roundToInt()
                Text(
                    text = if (v >= 0) "+$v" else "$v",
                    color = if (v != 0) Color.White else TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Horizontally scrollable adjustment icon strip (compact iOS style)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(allAdjustments) { config ->
                val isSelected = selectedAdjustment == config.type
                val currentValue = editState.getAdjustmentValue(config.type)
                val isModified = currentValue != config.defaultValue

                CompactAdjustmentIcon(
                    type = config.type,
                    label = config.label,
                    isSelected = isSelected,
                    isModified = isModified,
                    onClick = { onAdjustmentSelected(config.type) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun CompactAdjustmentIcon(
    type: AdjustmentType,
    label: String,
    isSelected: Boolean,
    isModified: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .background(
                if (isSelected) Color(0xFF1C1C1E) else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .padding(vertical = 6.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> Color(0xFF2C2C2E)
                        isModified -> Color(0xFF1C1C1E)
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconForAdjustment(type),
                contentDescription = label,
                tint = when {
                    isSelected -> iOSYellow
                    isModified -> Color.White
                    else -> SubtleGray
                },
                modifier = Modifier.size(20.dp)
            )

            // Small yellow dot indicator when modified
            if (isModified && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(iOSYellow)
                        .align(Alignment.TopEnd)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label.split(" ").first(), // Shorten "Black Point" to "Black" etc.
            color = if (isSelected) iOSYellow else SubtleGray,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            letterSpacing = (-0.2).sp
        )
    }
}

private fun iconForAdjustment(type: AdjustmentType): ImageVector = when (type) {
    AdjustmentType.EXPOSURE        -> Icons.Default.Exposure
    AdjustmentType.BRILLIANCE      -> Icons.Default.WbSunny
    AdjustmentType.HIGHLIGHTS      -> Icons.Default.Brightness5
    AdjustmentType.SHADOWS         -> Icons.Default.WbShade
    AdjustmentType.CONTRAST        -> Icons.Default.Contrast
    AdjustmentType.BRIGHTNESS      -> Icons.Default.Brightness6
    AdjustmentType.BLACK_POINT     -> Icons.Default.Brightness4
    AdjustmentType.SATURATION      -> Icons.Default.Palette
    AdjustmentType.VIBRANCE        -> Icons.Default.Colorize
    AdjustmentType.WARMTH          -> Icons.Default.Thermostat
    AdjustmentType.TINT            -> Icons.Default.InvertColors
    AdjustmentType.BW_INTENSITY    -> Icons.Default.FilterBAndW
    AdjustmentType.BW_NEUTRALS     -> Icons.Default.Tonality
    AdjustmentType.BW_TONE         -> Icons.Default.Gradient
    AdjustmentType.BW_GRAIN        -> Icons.Default.Grain
    AdjustmentType.SHARPNESS       -> Icons.Default.Lens
    AdjustmentType.DEFINITION      -> Icons.Default.Tune
    AdjustmentType.NOISE_REDUCTION -> Icons.Default.FilterBAndW
    AdjustmentType.VIGNETTE        -> Icons.Default.Vignette
}
