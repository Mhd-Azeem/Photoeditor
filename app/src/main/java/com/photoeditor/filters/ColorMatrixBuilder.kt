package com.photoeditor.filters

import android.graphics.ColorMatrix
import kotlin.math.pow

object ColorMatrixBuilder {

    fun exposure(value: Float): ColorMatrix {
        val factor = 2f.pow(value / 100f * 3f)
        return scale(factor)
    }

    fun brightness(value: Float): ColorMatrix {
        val t = value * 1.28f
        return translate(t, t, t)
    }

    fun contrast(value: Float): ColorMatrix {
        val s = 1f + value / 100f
        val t = 128f * (1f - s)
        return ColorMatrix(floatArrayOf(
            s,  0f, 0f, 0f, t,
            0f, s,  0f, 0f, t,
            0f, 0f, s,  0f, t,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    fun saturation(value: Float): ColorMatrix {
        val factor = (value + 100f) / 100f
        val cm = ColorMatrix()
        cm.setSaturation(factor.coerceIn(0f, 4f))
        return cm
    }

    fun vibrance(value: Float): ColorMatrix {
        val v = value / 200f
        val inv = 1f - v * 0.3f
        val rw = 0.2126f; val gw = 0.7152f; val bw = 0.0722f
        return ColorMatrix(floatArrayOf(
            inv + v * (1f - rw), v * (0f - rw),       v * (0f - rw),       0f, 0f,
            v * (0f - gw),       inv + v * (1f - gw), v * (0f - gw),       0f, 0f,
            v * (0f - bw),       v * (0f - bw),       inv + v * (1f - bw), 0f, 0f,
            0f,                  0f,                  0f,                  1f, 0f
        ))
    }

    fun warmth(value: Float): ColorMatrix {
        val r = value * 0.4f
        val b = -value * 0.4f
        return translate(r, 0f, b)
    }

    fun tint(value: Float): ColorMatrix {
        val g = value * 0.3f
        return translate(0f, g, 0f)
    }

    fun highlights(value: Float): ColorMatrix {
        val v = value / 200f
        return ColorMatrix(floatArrayOf(
            1f + v, 0f,      0f,      0f, -v * 80f,
            0f,     1f + v,  0f,      0f, -v * 80f,
            0f,     0f,      1f + v,  0f, -v * 80f,
            0f,     0f,      0f,      1f, 0f
        ))
    }

    fun shadows(value: Float): ColorMatrix {
        val lift = value * 0.25f
        return ColorMatrix(floatArrayOf(
            1f - lift * 0.003f, 0f, 0f, 0f, lift,
            0f, 1f - lift * 0.003f, 0f, 0f, lift,
            0f, 0f, 1f - lift * 0.003f, 0f, lift,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    fun blackPoint(value: Float): ColorMatrix {
        val t = -value * 1.2f
        return translate(t, t, t)
    }

    fun brilliance(value: Float): ColorMatrix {
        val v = value / 200f
        val s = 1f + v * 0.5f
        val t = -v * 25f
        return ColorMatrix(floatArrayOf(
            s,  0f, 0f, 0f, t,
            0f, s,  0f, 0f, t,
            0f, 0f, s,  0f, t,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    fun grayscale(): ColorMatrix {
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        return cm
    }

    fun bwIntensity(intensity: Float): ColorMatrix {
        if (intensity <= 0f) return ColorMatrix()
        val factor = intensity / 100f
        val grayMix = grayscale()
        val identity = ColorMatrix()
        val result = FloatArray(20)
        val gv = grayMix.array
        val iv = identity.array
        for (i in 0 until 20) {
            result[i] = iv[i] * (1f - factor) + gv[i] * factor
        }
        return ColorMatrix(result)
    }

    fun bwTone(value: Float): ColorMatrix {
        return contrast(value * 0.5f)
    }

    fun bwNeutrals(value: Float): ColorMatrix {
        val v = value / 300f
        return ColorMatrix(floatArrayOf(
            1f + v * 0.3f, 0f, 0f, 0f, 0f,
            0f, 1f + v,    0f, 0f, 0f,
            0f, 0f, 1f - v * 0.3f, 0f, 0f,
            0f, 0f, 0f,    1f, 0f
        ))
    }

    fun definition(value: Float): ColorMatrix {
        val v = value / 100f
        val s = 1f + v * 0.3f
        val t = -v * 10f
        return ColorMatrix(floatArrayOf(
            s,  0f, 0f, 0f, t,
            0f, s,  0f, 0f, t,
            0f, 0f, s,  0f, t,
            0f, 0f, 0f, 1f, 0f
        ))
    }

    private fun scale(factor: Float): ColorMatrix = ColorMatrix(floatArrayOf(
        factor, 0f, 0f, 0f, 0f,
        0f, factor, 0f, 0f, 0f,
        0f, 0f, factor, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))

    private fun translate(r: Float, g: Float, b: Float): ColorMatrix = ColorMatrix(floatArrayOf(
        1f, 0f, 0f, 0f, r,
        0f, 1f, 0f, 0f, g,
        0f, 0f, 1f, 0f, b,
        0f, 0f, 0f, 1f, 0f
    ))
}
