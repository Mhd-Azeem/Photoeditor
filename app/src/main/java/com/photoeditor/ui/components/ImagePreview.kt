package com.photoeditor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.photoeditor.data.model.CropState
import com.photoeditor.ui.components.crop.CropOverlay

@Composable
fun ImagePreview(
    bitmap: Bitmap?,
    showCropOverlay: Boolean = false,
    cropState: CropState? = null,
    onCropChanged: ((CropState) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            if (showCropOverlay && cropState != null && onCropChanged != null) {
                // Compute the actual image display rect so the crop overlay maps correctly
                // to image pixels (not the black-bar letterbox area).
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val containerW = maxWidth.value
                    val containerH = maxHeight.value
                    val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val containerAspect = containerW / containerH

                    val displayW: Float
                    val displayH: Float
                    val imgLeft: Float
                    val imgTop: Float

                    if (imageAspect > containerAspect) {
                        // Image is wider than container — fits by width, letterboxed top/bottom
                        displayW = containerW
                        displayH = containerW / imageAspect
                        imgLeft = 0f
                        imgTop = (containerH - displayH) / 2f
                    } else {
                        // Image is taller than container — fits by height, pillarboxed left/right
                        displayH = containerH
                        displayW = containerH * imageAspect
                        imgLeft = (containerW - displayW) / 2f
                        imgTop = 0f
                    }

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    CropOverlay(
                        cropState = cropState,
                        onCropChanged = onCropChanged,
                        modifier = Modifier
                            .offset(x = imgLeft.dp, y = imgTop.dp)
                            .size(displayW.dp, displayH.dp)
                    )
                }
            } else {
                // Normal mode with pinch-to-zoom
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    offsetX = (offsetX + pan.x).coerceIn(-500f, 500f)
                                    offsetY = (offsetY + pan.y).coerceIn(-500f, 500f)
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
