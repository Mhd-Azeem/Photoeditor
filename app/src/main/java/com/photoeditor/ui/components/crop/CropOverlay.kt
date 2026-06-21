package com.photoeditor.ui.components.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.photoeditor.data.model.CropState
import kotlin.math.abs

private const val HANDLE_TOUCH_RADIUS = 40f

private enum class DragTarget {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    TOP, BOTTOM, LEFT, RIGHT, CENTER
}

@Composable
fun CropOverlay(
    cropState: CropState,
    onCropChanged: (CropState) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragTarget by remember { mutableStateOf(DragTarget.NONE) }
    var startDragOffset by remember { mutableStateOf(Offset.Zero) }
    var startCropState by remember { mutableStateOf(cropState) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(cropState) {
                detectDragGestures(
                    onDragStart = { touchOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val rect = Rect(
                            left = cropState.left * w,
                            top = cropState.top * h,
                            right = cropState.right * w,
                            bottom = cropState.bottom * h
                        )
                        dragTarget = findDragTarget(touchOffset, rect)
                        startDragOffset = touchOffset
                        startCropState = cropState
                    },
                    onDragEnd = { dragTarget = DragTarget.NONE },
                    onDragCancel = { dragTarget = DragTarget.NONE },
                    onDrag = { change, _ ->
                        change.consume()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val dx = (change.position.x - startDragOffset.x) / w
                        val dy = (change.position.y - startDragOffset.y) / h

                        val minSize = 0.1f
                        val sc = startCropState

                        val newState = when (dragTarget) {
                            DragTarget.TOP_LEFT -> sc.copy(
                                left = (sc.left + dx).coerceIn(0f, sc.right - minSize),
                                top = (sc.top + dy).coerceIn(0f, sc.bottom - minSize)
                            )
                            DragTarget.TOP_RIGHT -> sc.copy(
                                right = (sc.right + dx).coerceIn(sc.left + minSize, 1f),
                                top = (sc.top + dy).coerceIn(0f, sc.bottom - minSize)
                            )
                            DragTarget.BOTTOM_LEFT -> sc.copy(
                                left = (sc.left + dx).coerceIn(0f, sc.right - minSize),
                                bottom = (sc.bottom + dy).coerceIn(sc.top + minSize, 1f)
                            )
                            DragTarget.BOTTOM_RIGHT -> sc.copy(
                                right = (sc.right + dx).coerceIn(sc.left + minSize, 1f),
                                bottom = (sc.bottom + dy).coerceIn(sc.top + minSize, 1f)
                            )
                            DragTarget.TOP -> sc.copy(
                                top = (sc.top + dy).coerceIn(0f, sc.bottom - minSize)
                            )
                            DragTarget.BOTTOM -> sc.copy(
                                bottom = (sc.bottom + dy).coerceIn(sc.top + minSize, 1f)
                            )
                            DragTarget.LEFT -> sc.copy(
                                left = (sc.left + dx).coerceIn(0f, sc.right - minSize)
                            )
                            DragTarget.RIGHT -> sc.copy(
                                right = (sc.right + dx).coerceIn(sc.left + minSize, 1f)
                            )
                            DragTarget.CENTER -> {
                                val cw = sc.right - sc.left
                                val ch = sc.bottom - sc.top
                                val newLeft = (sc.left + dx).coerceIn(0f, 1f - cw)
                                val newTop = (sc.top + dy).coerceIn(0f, 1f - ch)
                                sc.copy(left = newLeft, top = newTop, right = newLeft + cw, bottom = newTop + ch)
                            }
                            DragTarget.NONE -> sc
                        }

                        // Apply aspect ratio constraint if set
                        val constrained = applyAspectRatio(newState, dragTarget)
                        onCropChanged(constrained)
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val rect = Rect(
            left = cropState.left * w,
            top = cropState.top * h,
            right = cropState.right * w,
            bottom = cropState.bottom * h
        )

        // Dim outside crop
        drawDimOverlay(rect)

        // Grid lines (rule of thirds)
        drawGrid(rect)

        // Border
        drawRect(
            color = Color.White,
            topLeft = Offset(rect.left, rect.top),
            size = androidx.compose.ui.geometry.Size(rect.width, rect.height),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Corner handles (L-shaped)
        val handleLen = 22.dp.toPx()
        val handleWidth = 3.dp.toPx()
        drawCornerHandle(rect.left, rect.top, handleLen, handleWidth, 1f, 1f)
        drawCornerHandle(rect.right, rect.top, handleLen, handleWidth, -1f, 1f)
        drawCornerHandle(rect.left, rect.bottom, handleLen, handleWidth, 1f, -1f)
        drawCornerHandle(rect.right, rect.bottom, handleLen, handleWidth, -1f, -1f)
    }
}

private fun DrawScope.drawDimOverlay(cropRect: Rect) {
    val dimColor = Color.Black.copy(alpha = 0.55f)
    // Top
    drawRect(dimColor, Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(size.width, cropRect.top))
    // Bottom
    drawRect(dimColor, Offset(0f, cropRect.bottom), size = androidx.compose.ui.geometry.Size(size.width, size.height - cropRect.bottom))
    // Left
    drawRect(dimColor, Offset(0f, cropRect.top), size = androidx.compose.ui.geometry.Size(cropRect.left, cropRect.height))
    // Right
    drawRect(dimColor, Offset(cropRect.right, cropRect.top), size = androidx.compose.ui.geometry.Size(size.width - cropRect.right, cropRect.height))
}

private fun DrawScope.drawGrid(rect: Rect) {
    val gridColor = Color.White.copy(alpha = 0.3f)
    val stroke = Stroke(width = 0.5.dp.toPx())
    val thirdW = rect.width / 3f
    val thirdH = rect.height / 3f
    // Vertical lines
    for (i in 1..2) {
        val x = rect.left + thirdW * i
        drawLine(gridColor, Offset(x, rect.top), Offset(x, rect.bottom), strokeWidth = 0.5.dp.toPx())
    }
    // Horizontal lines
    for (i in 1..2) {
        val y = rect.top + thirdH * i
        drawLine(gridColor, Offset(rect.left, y), Offset(rect.right, y), strokeWidth = 0.5.dp.toPx())
    }
}

private fun DrawScope.drawCornerHandle(
    x: Float, y: Float, len: Float, width: Float, dx: Float, dy: Float
) {
    val color = Color.White
    drawLine(color, Offset(x, y), Offset(x + dx * len, y), strokeWidth = width, cap = StrokeCap.Square)
    drawLine(color, Offset(x, y), Offset(x, y + dy * len), strokeWidth = width, cap = StrokeCap.Square)
}

private fun findDragTarget(touch: Offset, rect: Rect): DragTarget {
    val r = HANDLE_TOUCH_RADIUS
    val cx = (rect.left + rect.right) / 2f
    val cy = (rect.top + rect.bottom) / 2f

    fun near(a: Float, b: Float) = abs(a - b) < r

    return when {
        near(touch.x, rect.left) && near(touch.y, rect.top)    -> DragTarget.TOP_LEFT
        near(touch.x, rect.right) && near(touch.y, rect.top)   -> DragTarget.TOP_RIGHT
        near(touch.x, rect.left) && near(touch.y, rect.bottom) -> DragTarget.BOTTOM_LEFT
        near(touch.x, rect.right) && near(touch.y, rect.bottom)-> DragTarget.BOTTOM_RIGHT
        near(touch.y, rect.top) && touch.x in rect.left..rect.right    -> DragTarget.TOP
        near(touch.y, rect.bottom) && touch.x in rect.left..rect.right -> DragTarget.BOTTOM
        near(touch.x, rect.left) && touch.y in rect.top..rect.bottom   -> DragTarget.LEFT
        near(touch.x, rect.right) && touch.y in rect.top..rect.bottom  -> DragTarget.RIGHT
        touch.x in rect.left..rect.right && touch.y in rect.top..rect.bottom -> DragTarget.CENTER
        else -> DragTarget.NONE
    }
}

private fun applyAspectRatio(state: CropState, dragTarget: DragTarget): CropState {
    val ratio = state.aspectRatio.ratio ?: return state
    val w = state.right - state.left
    val h = state.bottom - state.top
    val currentRatio = w / h
    return if (abs(currentRatio - ratio) > 0.01f) {
        val newH = w / ratio
        state.copy(bottom = (state.top + newH).coerceAtMost(1f))
    } else state
}
