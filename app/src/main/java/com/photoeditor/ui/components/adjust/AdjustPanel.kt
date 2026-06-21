package com.photoeditor.ui.components.adjust

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vignette
import androidx.compose.material.icons.filled.WbShade
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.photoeditor.ui.theme.DarkGray
import com.photoeditor.ui.theme.LightGray
import com.photoeditor.ui.theme.SubtleGray
import com.photoeditor.ui.theme.TextGray
import com.photoeditor.ui.theme.iOSYellow
import kotlin.math.roundToInt

@Composable
fun AdjustPanel(
    editState: EditState,
    selectedAdjustment: AdjustmentType?,
    onAdjustmentSelected: (AdjustmentType?) -> Unit,
    onValueChanged: (AdjustmentType, Float) -> Unit,
    onReset: (AdjustmentType) -> Unit,
    onAutoEnhance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxWidth()) {
        // Auto-enhance + value display row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAutoEnhance,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = "Auto Enhance",
                    tint = iOSYellow,
                    modifier = Modifier.size(22.dp)
                )
            }

            selectedAdjustment?.let { type ->
                val value = editState.getAdjustmentValue(type)
                val config = type.config()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = value.roundToInt().toString(),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onReset(type) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = if (value != config.defaultValue) iOSYellow else SubtleGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } ?: Text("", color = Color.Transparent, fontSize = 17.sp)
        }

        // Slider for selected adjustment
        AnimatedVisibility(
            visible = selectedAdjustment != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            selectedAdjustment?.let { type ->
                val config = type.config()
                HorizontalValueSlider(
                    value = editState.getAdjustmentValue(type),
                    onValueChange = { onValueChanged(type, it) },
                    valueRange = config.minValue..config.maxValue,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontally scrollable adjustment icons
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(allAdjustments) { config ->
                val isSelected = selectedAdjustment == config.type
                val currentValue = editState.getAdjustmentValue(config.type)
                val isModified = currentValue != config.defaultValue

                AdjustmentIcon(
                    type = config.type,
                    label = config.label,
                    isSelected = isSelected,
                    isModified = isModified,
                    value = currentValue,
                    onClick = {
                        onAdjustmentSelected(if (isSelected) null else config.type)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AdjustmentIcon(
    type: AdjustmentType,
    label: String,
    isSelected: Boolean,
    isModified: Boolean,
    value: Float,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(
                if (isSelected) Color(0xFF2C2C2E) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> iOSYellow.copy(alpha = 0.15f)
                        isModified -> Color(0xFF2C2C2E)
                        else -> Color(0xFF1C1C1E)
                    }
                )
                .border(
                    width = if (isSelected) 1.5.dp else 0.dp,
                    color = if (isSelected) iOSYellow else Color.Transparent,
                    shape = CircleShape
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
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isModified && !isSelected) {
                val v = value.roundToInt()
                if (v >= 0) "+$v" else "$v"
            } else label,
            color = when {
                isSelected -> iOSYellow
                isModified -> Color.White
                else -> TextGray
            },
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
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
