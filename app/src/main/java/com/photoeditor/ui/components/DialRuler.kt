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
    var isDragging by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth().height(52.dp)) {
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
                            // Drag right = increase value
                            val pixelsPerUnit = 4.5f
                            val delta = dragAmount.x / pixelsPerUnit
                            val newValue = (value + delta).coerceIn(
                                valueRange.start,
                                valueRange.endInclusive
                            )
                            onValueChange(newValue)
                        }
                    )
                }
        ) {
            val centerX = size.width / 2f
            val pixelsPerUnit = 4.5f

            // --- Center indicator dot ---
            drawCircle(
                color = Color.White,
                radius = 3.5.dp.toPx(),
                center = Offset(centerX, 8.dp.toPx())
            )

            // --- Tick marks ---
            // For tick at integer value v:
            //   screenX = centerX + (v - currentValue) * pixelsPerUnit
            val halfVisible = (size.width / 2f / pixelsPerUnit).toInt() + 4
            val baseValue = value.toInt()

            for (v in (baseValue - halfVisible)..(baseValue + halfVisible)) {
                if (v < valueRange.start - 1 || v > valueRange.endInclusive + 1) continue
                val screenX = centerX + (v.toFloat() - value) * pixelsPerUnit
                if (screenX < -4 || screenX > size.width + 4) continue

                val isZero = v == 0
                val isMajor = v % 10 == 0
                val isMid = v % 5 == 0

                val tickHeight = when {
                    isZero -> 18.dp.toPx()
                    isMajor -> 13.dp.toPx()
                    isMid -> 8.dp.toPx()
                    else -> 4.dp.toPx()
                }
                val strokeWidth = when {
                    isZero -> 2.dp.toPx()
                    isMajor -> 1.2.dp.toPx()
                    else -> 0.8.dp.toPx()
                }
                val color = when {
                    isZero -> Color.White
                    isMajor -> Color(0xFFAAAAAA)
                    isMid -> Color(0xFF777777)
                    else -> Color(0xFF444444)
                }

                // Ticks grow upward from the baseline
                val baselineY = size.height * 0.78f
                drawLine(
                    color = color,
                    start = Offset(screenX, baselineY - tickHeight),
                    end = Offset(screenX, baselineY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // --- Center guide line (subtle) ---
            val baselineY = size.height * 0.78f
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, baselineY),
                end = Offset(size.width, baselineY),
                strokeWidth = 0.5.dp.toPx()
            )
        }
    }
}
