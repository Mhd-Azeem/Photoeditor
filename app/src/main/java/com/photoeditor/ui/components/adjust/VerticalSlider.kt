package com.photoeditor.ui.components.adjust

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun HorizontalValueSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = -100f..100f,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = if (isDragging) spring(stiffness = Spring.StiffnessHigh)
        else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "slider"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(valueRange) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val range = valueRange.endInclusive - valueRange.start
                            val delta = dragAmount.x / size.width.toFloat() * range
                            val newValue = (value + delta).coerceIn(valueRange.start, valueRange.endInclusive)
                            onValueChange(newValue)
                        }
                    )
                }
        ) {
            val centerY = size.height / 2f
            val trackHeight = 3.dp.toPx()
            val thumbRadius = 11.dp.toPx()
            val normalized = (animatedValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)
            val thumbX = normalized * size.width
            val midX = size.width / 2f

            // Full track (inactive)
            drawLine(
                color = Color(0xFF48484A),
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )

            // Filled segment from center (0-point) to thumb
            val fillStart = minOf(midX, thumbX)
            val fillEnd = maxOf(midX, thumbX)
            if (fillEnd > fillStart) {
                drawLine(
                    color = Color.White,
                    start = Offset(fillStart, centerY),
                    end = Offset(fillEnd, centerY),
                    strokeWidth = trackHeight,
                    cap = StrokeCap.Round
                )
            }

            // Center tick mark
            drawLine(
                color = Color(0xFF8E8E93),
                start = Offset(midX, centerY - 6.dp.toPx()),
                end = Offset(midX, centerY + 6.dp.toPx()),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Thumb
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(thumbX, centerY)
            )
            // Thumb shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = thumbRadius + 1.dp.toPx(),
                center = Offset(thumbX + 1.dp.toPx(), centerY + 1.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(thumbX, centerY)
            )
        }
    }
}
