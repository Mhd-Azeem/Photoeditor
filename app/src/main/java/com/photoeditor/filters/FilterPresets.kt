package com.photoeditor.filters

import android.graphics.ColorMatrix
import com.photoeditor.data.model.FilterType

object FilterPresets {

    fun getMatrix(type: FilterType, intensity: Float = 100f): ColorMatrix {
        val factor = intensity / 100f
        val base = baseMatrix(type)
        if (factor >= 1f || type == FilterType.ORIGINAL) return base
        // Blend base with identity proportional to intensity
        val identity = ColorMatrix()
        val bv = base.array
        val iv = identity.array
        val blended = FloatArray(20) { i -> iv[i] + (bv[i] - iv[i]) * factor }
        return ColorMatrix(blended)
    }

    private fun baseMatrix(type: FilterType): ColorMatrix {
        return when (type) {
            FilterType.ORIGINAL      -> ColorMatrix()
            FilterType.VIVID         -> vivid()
            FilterType.VIVID_WARM    -> vividWarm()
            FilterType.VIVID_COOL    -> vividCool()
            FilterType.DRAMATIC      -> dramatic()
            FilterType.DRAMATIC_WARM -> dramaticWarm()
            FilterType.DRAMATIC_COOL -> dramaticCool()
            FilterType.MONO          -> mono()
            FilterType.SILVERTONE    -> silvertone()
            FilterType.NOIR          -> noir()
            FilterType.FADE          -> fade()
            FilterType.CHROME        -> chrome()
            FilterType.INSTANT       -> instant()
            FilterType.TONAL         -> tonal()
            FilterType.TRANSFER      -> transfer()
            FilterType.PROCESS       -> process()
        }
    }

    private fun vivid(): ColorMatrix {
        val cm = ColorMatrix()
        cm.setSaturation(1.6f)
        val contrast = ColorMatrix(floatArrayOf(
            1.1f, 0f, 0f, 0f, -13f,
            0f, 1.1f, 0f, 0f, -13f,
            0f, 0f, 1.1f, 0f, -13f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrast)
        return cm
    }

    private fun vividWarm(): ColorMatrix {
        val cm = vivid()
        cm.postConcat(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, 30f,
            0f, 1f, 0f, 0f, 5f,
            0f, 0f, 1f, 0f, -20f,
            0f, 0f, 0f, 1f, 0f
        )))
        return cm
    }

    private fun vividCool(): ColorMatrix {
        val cm = vivid()
        cm.postConcat(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, -20f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 30f,
            0f, 0f, 0f, 1f, 0f
        )))
        return cm
    }

    private fun dramatic(): ColorMatrix {
        val sat = ColorMatrix()
        sat.setSaturation(0.8f)
        val contrast = ColorMatrix(floatArrayOf(
            1.3f, 0f, 0f, 0f, -38f,
            0f, 1.3f, 0f, 0f, -38f,
            0f, 0f, 1.3f, 0f, -38f,
            0f, 0f, 0f, 1f, 0f
        ))
        sat.postConcat(contrast)
        return sat
    }

    private fun dramaticWarm(): ColorMatrix {
        val cm = dramatic()
        cm.postConcat(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, 20f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, -15f,
            0f, 0f, 0f, 1f, 0f
        )))
        return cm
    }

    private fun dramaticCool(): ColorMatrix {
        val cm = dramatic()
        cm.postConcat(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, -15f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        )))
        return cm
    }

    private fun mono(): ColorMatrix {
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        return cm
    }

    private fun silvertone(): ColorMatrix {
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        cm.postConcat(ColorMatrix(floatArrayOf(
            1.05f, 0f, 0f, 0f, 10f,
            0f, 1.05f, 0f, 0f, 10f,
            0f, 0f, 1.05f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )))
        return cm
    }

    private fun noir(): ColorMatrix {
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        cm.postConcat(ColorMatrix(floatArrayOf(
            1.5f, 0f, 0f, 0f, -60f,
            0f, 1.5f, 0f, 0f, -60f,
            0f, 0f, 1.5f, 0f, -60f,
            0f, 0f, 0f, 1f, 0f
        )))
        return cm
    }

    private fun fade(): ColorMatrix {
        return ColorMatrix(floatArrayOf(
            0.8f, 0f,   0f,   0f, 30f,
            0f,   0.8f, 0f,   0f, 30f,
            0f,   0f,   0.8f, 0f, 30f,
            0f,   0f,   0f,   1f, 0f
        ))
    }

    private fun chrome(): ColorMatrix {
        val cm = ColorMatrix()
        cm.setSaturation(1.4f)
        cm.postConcat(ColorMatrix(floatArrayOf(
            1.1f, 0f, 0f, 0f, -15f,
            0f, 1.0f, 0f, 0f, 0f,
            0f, 0f, 1.2f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        )))
        return cm
    }

    private fun instant(): ColorMatrix {
        return ColorMatrix(floatArrayOf(
            1.05f, 0f,    0f,    0f, 20f,
            0f,    1.0f,  0f,    0f, 15f,
            0f,    0f,    0.9f,  0f, 10f,
            0f,    0f,    0f,    1f, 0f
        ))
    }

    private fun tonal(): ColorMatrix {
        val cm = ColorMatrix()
        cm.setSaturation(0.2f)
        cm.postConcat(ColorMatrix(floatArrayOf(
            1.1f, 0f, 0f, 0f, -5f,
            0f, 1.1f, 0f, 0f, -5f,
            0f, 0f, 1.1f, 0f, -5f,
            0f, 0f, 0f, 1f, 0f
        )))
        return cm
    }

    private fun transfer(): ColorMatrix {
        return ColorMatrix(floatArrayOf(
            0.9f, 0.1f, 0f,   0f, 15f,
            0f,   0.9f, 0.1f, 0f, 10f,
            0.1f, 0f,   0.9f, 0f, 5f,
            0f,   0f,   0f,   1f, 0f
        ))
    }

    private fun process(): ColorMatrix {
        val cm = ColorMatrix()
        cm.setSaturation(1.2f)
        cm.postConcat(ColorMatrix(floatArrayOf(
            1.1f, 0f,   0f,   0f, 20f,
            0f,   1.2f, 0f,   0f, -10f,
            0f,   0f,   0.9f, 0f, 10f,
            0f,   0f,   0f,   1f, 0f
        )))
        return cm
    }
}
