package com.photoeditor.data.model

data class CropState(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
    val rotation: Float = 0f,
    val aspectRatio: AspectRatio = AspectRatio.FREE
)

enum class AspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    ORIGINAL("Original", null),
    SQUARE("Square", 1f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f)
}
