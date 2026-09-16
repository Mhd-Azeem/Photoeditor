package com.photoeditor.data.model

enum class AdjustmentGroup(val label: String) {
    LIGHT("Light"),
    COLOR("Color"),
    BLACK_WHITE("B&W"),
    DETAIL("Detail")
}

data class AdjustmentConfig(
    val type: AdjustmentType,
    val label: String,
    val group: AdjustmentGroup,
    val defaultValue: Float = 0f,
    val minValue: Float = -100f,
    val maxValue: Float = 100f
)

enum class AdjustmentType {
    EXPOSURE, BRILLIANCE, HIGHLIGHTS, SHADOWS, CONTRAST, BRIGHTNESS, BLACK_POINT,
    SATURATION, VIBRANCE, WARMTH, TINT,
    BW_INTENSITY, BW_NEUTRALS, BW_TONE, BW_GRAIN,
    SHARPNESS, DEFINITION, NOISE_REDUCTION,
    VIGNETTE
}

val allAdjustments = listOf(
    AdjustmentConfig(AdjustmentType.EXPOSURE,       "Exposure",        AdjustmentGroup.LIGHT),
    AdjustmentConfig(AdjustmentType.BRILLIANCE,     "Brilliance",      AdjustmentGroup.LIGHT),
    AdjustmentConfig(AdjustmentType.HIGHLIGHTS,     "Highlights",      AdjustmentGroup.LIGHT),
    AdjustmentConfig(AdjustmentType.SHADOWS,        "Shadows",         AdjustmentGroup.LIGHT),
    AdjustmentConfig(AdjustmentType.CONTRAST,       "Contrast",        AdjustmentGroup.LIGHT),
    AdjustmentConfig(AdjustmentType.BRIGHTNESS,     "Brightness",      AdjustmentGroup.LIGHT),
    AdjustmentConfig(AdjustmentType.BLACK_POINT,    "Black Point",     AdjustmentGroup.LIGHT),
    AdjustmentConfig(AdjustmentType.SATURATION,     "Saturation",      AdjustmentGroup.COLOR),
    AdjustmentConfig(AdjustmentType.VIBRANCE,       "Vibrance",        AdjustmentGroup.COLOR),
    AdjustmentConfig(AdjustmentType.WARMTH,         "Warmth",          AdjustmentGroup.COLOR),
    AdjustmentConfig(AdjustmentType.TINT,           "Tint",            AdjustmentGroup.COLOR),
    AdjustmentConfig(AdjustmentType.BW_INTENSITY,   "Intensity",       AdjustmentGroup.BLACK_WHITE),
    AdjustmentConfig(AdjustmentType.BW_NEUTRALS,    "Neutrals",        AdjustmentGroup.BLACK_WHITE),
    AdjustmentConfig(AdjustmentType.BW_TONE,        "Tone",            AdjustmentGroup.BLACK_WHITE),
    AdjustmentConfig(AdjustmentType.BW_GRAIN,       "Grain",           AdjustmentGroup.BLACK_WHITE, 0f, 0f, 100f),
    AdjustmentConfig(AdjustmentType.SHARPNESS,      "Sharpness",       AdjustmentGroup.DETAIL),
    AdjustmentConfig(AdjustmentType.DEFINITION,     "Definition",      AdjustmentGroup.DETAIL),
    AdjustmentConfig(AdjustmentType.NOISE_REDUCTION,"Noise Reduction", AdjustmentGroup.DETAIL, 0f, 0f, 100f),
    AdjustmentConfig(AdjustmentType.VIGNETTE,       "Vignette",        AdjustmentGroup.LIGHT, 0f, 0f, 100f)
)

fun AdjustmentType.config(): AdjustmentConfig =
    allAdjustments.first { it.type == this }
