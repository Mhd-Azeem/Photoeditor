package com.photoeditor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun DialRuler(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = -100f..100f,
    modifier: Modifier = Modifier
) {
    // rememberUpdatedState ensures the drag lambda always reads the latest
    // value and callback without needing to restart the pointerInput block.
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentRange by rememberUpdatedState(valueRange)

    var isDragging by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth().height(52.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val pixelsPerUnit = 4.5f
                            // Each delta is relative to the *current* (already updated) value
                            val delta = dragAmount.x / pixelsPerUnit
                            val newValue = (currentValue + delta).coerceIn(
                                currentRange.start,
                                currentRange.endInclusive
                            )
                            currentOnValueChange(newValue)
                        }
                    )
                }
        ) {
            val centerX = size.width / 2f
            val pixelsPerUnit = 4.5f

            // Center indicator dot (fixed, always at center)
            drawCircle(
                color = Color.White,
                radius = 3.5.dp.toPx(),
                center = Offset(centerX, 8.dp.toPx())
            )

            // Tick marks — position relative to current value
            // screenX(v) = centerX + (v - value) * pixelsPerUnit
            val halfVisible = (size.width / 2f / pixelsPerUnit).toInt() + 4
            val baseValue = value.toInt()

            val baselineY = size.height * 0.78f

            for (v in (baseValue - halfVisible)..(baseValue + halfVisible)) {
                if (v < valueRange.start - 1 || v > valueRange.endInclusive + 1) continue
                val screenX = centerX + (v.toFloat() - value) * pixelsPerUnit
                if (screenX < -4 || screenX > size.width + 4) continue

                val isZero  = v == 0
                val isMajor = v % 10 == 0
                val isMid   = v % 5  == 0

                val tickHeight = when {
                    isZero  -> 18.dp.toPx()
                    isMajor -> 13.dp.toPx()
                    isMid   ->  8.dp.toPx()
                    else    ->  4.dp.toPx()
                }
                val strokeWidth = when {
                    isZero  -> 2.0.dp.toPx()
                    isMajor -> 1.2.dp.toPx()
                    else    -> 0.8.dp.toPx()
                }
                val color = when {
                    isZero  -> Color.White
                    isMajor -> Color(0xFFAAAAAA)
                    isMid   -> Color(0xFF777777)
                    else    -> Color(0xFF444444)
                }

                drawLine(
                    color = color,
                    start = Offset(screenX, baselineY - tickHeight),
                    end = Offset(screenX, baselineY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // Subtle baseline
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, baselineY),
                end = Offset(size.width, baselineY),
                strokeWidth = 0.5.dp.toPx()
            )
        }
    }
}
