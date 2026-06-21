package com.photoeditor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.Image
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
    var offset by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            if (showCropOverlay && cropState != null && onCropChanged != null) {
                // Crop mode — show without zoom/pan, crop overlay handles gestures
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    CropOverlay(
                        cropState = cropState,
                        onCropChanged = onCropChanged,
                        modifier = Modifier.fillMaxSize()
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
                                    offset = (offset + pan.x).coerceIn(-500f, 500f)
                                    offsetY = (offsetY + pan.y).coerceIn(-500f, 500f)
                                } else {
                                    offset = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset
                            translationY = offsetY
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
