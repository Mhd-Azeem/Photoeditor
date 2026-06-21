package com.photoeditor.data.model

data class EditState(
    // Light
    val exposure: Float = 0f,
    val brilliance: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val contrast: Float = 0f,
    val brightness: Float = 0f,
    val blackPoint: Float = 0f,
    // Color
    val saturation: Float = 0f,
    val vibrance: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    // B&W
    val bwIntensity: Float = 0f,
    val bwNeutrals: Float = 0f,
    val bwTone: Float = 0f,
    val bwGrain: Float = 0f,
    // Detail
    val sharpness: Float = 0f,
    val definition: Float = 0f,
    val noiseReduction: Float = 0f,
    // Other
    val vignette: Float = 0f,
    // Filter
    val selectedFilter: FilterType = FilterType.ORIGINAL,
    val filterIntensity: Float = 100f,
    // Crop
    val cropState: CropState = CropState()
) {
    fun isModified(): Boolean =
        exposure != 0f || brilliance != 0f || highlights != 0f || shadows != 0f ||
        contrast != 0f || brightness != 0f || blackPoint != 0f || saturation != 0f ||
        vibrance != 0f || warmth != 0f || tint != 0f || bwIntensity != 0f ||
        bwNeutrals != 0f || bwTone != 0f || bwGrain != 0f || sharpness != 0f ||
        definition != 0f || noiseReduction != 0f || vignette != 0f ||
        selectedFilter != FilterType.ORIGINAL ||
        cropState != CropState()

    fun withAdjustment(type: AdjustmentType, value: Float): EditState = when (type) {
        AdjustmentType.EXPOSURE        -> copy(exposure = value)
        AdjustmentType.BRILLIANCE      -> copy(brilliance = value)
        AdjustmentType.HIGHLIGHTS      -> copy(highlights = value)
        AdjustmentType.SHADOWS         -> copy(shadows = value)
        AdjustmentType.CONTRAST        -> copy(contrast = value)
        AdjustmentType.BRIGHTNESS      -> copy(brightness = value)
        AdjustmentType.BLACK_POINT     -> copy(blackPoint = value)
        AdjustmentType.SATURATION      -> copy(saturation = value)
        AdjustmentType.VIBRANCE        -> copy(vibrance = value)
        AdjustmentType.WARMTH          -> copy(warmth = value)
        AdjustmentType.TINT            -> copy(tint = value)
        AdjustmentType.BW_INTENSITY    -> copy(bwIntensity = value)
        AdjustmentType.BW_NEUTRALS     -> copy(bwNeutrals = value)
        AdjustmentType.BW_TONE         -> copy(bwTone = value)
        AdjustmentType.BW_GRAIN        -> copy(bwGrain = value)
        AdjustmentType.SHARPNESS       -> copy(sharpness = value)
        AdjustmentType.DEFINITION      -> copy(definition = value)
        AdjustmentType.NOISE_REDUCTION -> copy(noiseReduction = value)
        AdjustmentType.VIGNETTE        -> copy(vignette = value)
    }

    fun getAdjustmentValue(type: AdjustmentType): Float = when (type) {
        AdjustmentType.EXPOSURE        -> exposure
        AdjustmentType.BRILLIANCE      -> brilliance
        AdjustmentType.HIGHLIGHTS      -> highlights
        AdjustmentType.SHADOWS         -> shadows
        AdjustmentType.CONTRAST        -> contrast
        AdjustmentType.BRIGHTNESS      -> brightness
        AdjustmentType.BLACK_POINT     -> blackPoint
        AdjustmentType.SATURATION      -> saturation
        AdjustmentType.VIBRANCE        -> vibrance
        AdjustmentType.WARMTH          -> warmth
        AdjustmentType.TINT            -> tint
        AdjustmentType.BW_INTENSITY    -> bwIntensity
        AdjustmentType.BW_NEUTRALS     -> bwNeutrals
        AdjustmentType.BW_TONE         -> bwTone
        AdjustmentType.BW_GRAIN        -> bwGrain
        AdjustmentType.SHARPNESS       -> sharpness
        AdjustmentType.DEFINITION      -> definition
        AdjustmentType.NOISE_REDUCTION -> noiseReduction
        AdjustmentType.VIGNETTE        -> vignette
    }
}

enum class EditTab { ADJUST, FILTERS, CROP }
