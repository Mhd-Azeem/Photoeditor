package com.photoeditor.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.photoeditor.data.model.EditState
import com.photoeditor.data.model.FilterType
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

object ImageProcessor {

    fun process(source: Bitmap, state: EditState): Bitmap {
        var result = applyColorMatrix(source, state)
        if (state.sharpness != 0f) result = applySharpness(result, state.sharpness)
        if (state.noiseReduction > 0f) result = applyBlur(result, state.noiseReduction)
        if (state.bwGrain > 0f) result = applyGrain(result, state.bwGrain)
        if (state.vignette != 0f) result = applyVignette(result, state.vignette)
        if (state.cropState.let { it.left != 0f || it.top != 0f || it.right != 1f || it.bottom != 1f || it.rotation != 0f }) {
            result = applyCrop(result, state)
        }
        return result
    }

    fun processForThumbnail(source: Bitmap, state: EditState): Bitmap {
        val scaled = Bitmap.createScaledBitmap(source, 150, 150, true)
        return applyColorMatrix(scaled, state)
    }

    fun processFilterPreview(source: Bitmap, filterType: FilterType, intensity: Float): Bitmap {
        val scaled = Bitmap.createScaledBitmap(source, 150, 150, true)
        val output = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(FilterPresets.getMatrix(filterType, intensity))
        }
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        return output
    }

    private fun applyColorMatrix(source: Bitmap, state: EditState): Bitmap {
        val combined = buildCombinedMatrix(state)
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(combined) }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    private fun buildCombinedMatrix(state: EditState): ColorMatrix {
        val cm = ColorMatrix()
        with(ColorMatrixBuilder) {
            if (state.selectedFilter != FilterType.ORIGINAL) {
                cm.postConcat(FilterPresets.getMatrix(state.selectedFilter, state.filterIntensity))
            }
            if (state.exposure != 0f)        cm.postConcat(exposure(state.exposure))
            if (state.brilliance != 0f)      cm.postConcat(brilliance(state.brilliance))
            if (state.highlights != 0f)      cm.postConcat(highlights(state.highlights))
            if (state.shadows != 0f)         cm.postConcat(shadows(state.shadows))
            if (state.contrast != 0f)        cm.postConcat(contrast(state.contrast))
            if (state.brightness != 0f)      cm.postConcat(brightness(state.brightness))
            if (state.blackPoint != 0f)      cm.postConcat(blackPoint(state.blackPoint))
            if (state.saturation != 0f)      cm.postConcat(saturation(state.saturation))
            if (state.vibrance != 0f)        cm.postConcat(vibrance(state.vibrance))
            if (state.warmth != 0f)          cm.postConcat(warmth(state.warmth))
            if (state.tint != 0f)            cm.postConcat(tint(state.tint))
            if (state.bwIntensity != 0f)     cm.postConcat(bwIntensity(state.bwIntensity))
            if (state.bwNeutrals != 0f)      cm.postConcat(bwNeutrals(state.bwNeutrals))
            if (state.bwTone != 0f)          cm.postConcat(bwTone(state.bwTone))
            if (state.definition != 0f)      cm.postConcat(definition(state.definition))
        }
        return cm
    }

    private fun applySharpness(source: Bitmap, amount: Float): Bitmap {
        val w = source.width; val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val strength = amount / 100f
        val output = IntArray(w * h)

        val kernel = if (amount > 0f) {
            floatArrayOf(
                -strength * 0.5f,  -strength,        -strength * 0.5f,
                -strength,          1f + 4f * strength, -strength,
                -strength * 0.5f,  -strength,        -strength * 0.5f
            )
        } else {
            val b = -strength * 0.15f
            floatArrayOf(b, b, b, b, 1f - 8f * b, b, b, b, b)
        }

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                var r = 0f; var g = 0f; var b = 0f
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val px = pixels[(y + ky) * w + (x + kx)]
                        val k = kernel[(ky + 1) * 3 + (kx + 1)]
                        r += ((px shr 16) and 0xFF) * k
                        g += ((px shr 8) and 0xFF) * k
                        b += (px and 0xFF) * k
                    }
                }
                val a = (pixels[y * w + x] shr 24) and 0xFF
                output[y * w + x] = (a shl 24) or
                    (r.toInt().coerceIn(0, 255) shl 16) or
                    (g.toInt().coerceIn(0, 255) shl 8) or
                    b.toInt().coerceIn(0, 255)
            }
        }
        // Copy border pixels unchanged
        for (x in 0 until w) { output[x] = pixels[x]; output[(h-1)*w+x] = pixels[(h-1)*w+x] }
        for (y in 0 until h) { output[y*w] = pixels[y*w]; output[y*w+w-1] = pixels[y*w+w-1] }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(output, 0, w, 0, 0, w, h)
        return result
    }

    private fun applyBlur(source: Bitmap, amount: Float): Bitmap {
        val radius = (amount / 100f * 5).roundToInt().coerceAtLeast(1)
        val w = source.width; val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val temp = IntArray(w * h)
        val out = IntArray(w * h)

        // Horizontal pass
        for (y in 0 until h) {
            for (x in 0 until w) {
                var r = 0; var g = 0; var b = 0; var count = 0
                for (kx in -radius..radius) {
                    val px = (x + kx).coerceIn(0, w - 1)
                    val c = pixels[y * w + px]
                    r += (c shr 16) and 0xFF
                    g += (c shr 8) and 0xFF
                    b += c and 0xFF
                    count++
                }
                val a = (pixels[y * w + x] shr 24) and 0xFF
                temp[y * w + x] = (a shl 24) or ((r/count) shl 16) or ((g/count) shl 8) or (b/count)
            }
        }
        // Vertical pass
        for (y in 0 until h) {
            for (x in 0 until w) {
                var r = 0; var g = 0; var b = 0; var count = 0
                for (ky in -radius..radius) {
                    val py = (y + ky).coerceIn(0, h - 1)
                    val c = temp[py * w + x]
                    r += (c shr 16) and 0xFF
                    g += (c shr 8) and 0xFF
                    b += c and 0xFF
                    count++
                }
                val a = (temp[y * w + x] shr 24) and 0xFF
                out[y * w + x] = (a shl 24) or ((r/count) shl 16) or ((g/count) shl 8) or (b/count)
            }
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    private fun applyGrain(source: Bitmap, amount: Float): Bitmap {
        val w = source.width; val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val strength = (amount / 100f * 40).roundToInt()
        val rng = Random(42)
        for (i in pixels.indices) {
            val c = pixels[i]
            val noise = rng.nextInt(-strength, strength + 1)
            val r = ((c shr 16) and 0xFF + noise).coerceIn(0, 255)
            val g = ((c shr 8) and 0xFF + noise).coerceIn(0, 255)
            val b = (c and 0xFF + noise).coerceIn(0, 255)
            pixels[i] = (c and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    private fun applyVignette(source: Bitmap, amount: Float): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val cx = output.width / 2f
        val cy = output.height / 2f
        val radius = max(output.width, output.height) * 0.7f
        val alpha = (amount.coerceIn(-100f, 100f) / 100f * 210).roundToInt().coerceIn(0, 210)
        val gradient = RadialGradient(
            cx, cy, radius,
            intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK),
            floatArrayOf(0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        val paint = Paint().apply { shader = gradient; this.alpha = alpha }
        canvas.drawRect(0f, 0f, output.width.toFloat(), output.height.toFloat(), paint)
        return output
    }

    private fun applyCrop(source: Bitmap, state: EditState): Bitmap {
        val crop = state.cropState
        val x = (crop.left * source.width).roundToInt().coerceIn(0, source.width - 1)
        val y = (crop.top * source.height).roundToInt().coerceIn(0, source.height - 1)
        val w = ((crop.right - crop.left) * source.width).roundToInt().coerceAtLeast(1)
        val h = ((crop.bottom - crop.top) * source.height).roundToInt().coerceAtLeast(1)
        val safeW = w.coerceAtMost(source.width - x)
        val safeH = h.coerceAtMost(source.height - y)
        val cropped = Bitmap.createBitmap(source, x, y, safeW, safeH)
        if (crop.rotation == 0f) return cropped
        val matrix = android.graphics.Matrix()
        matrix.postRotate(crop.rotation)
        return Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
    }

    fun autoEnhance(source: Bitmap): Map<String, Float> {
        // Analyse the bitmap to determine auto adjustments
        val scaled = Bitmap.createScaledBitmap(source, 64, 64, true)
        val pixels = IntArray(64 * 64)
        scaled.getPixels(pixels, 0, 64, 0, 0, 64, 64)

        var totalR = 0L; var totalG = 0L; var totalB = 0L
        var minLum = 255; var maxLum = 0
        for (px in pixels) {
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            totalR += r; totalG += g; totalB += b
            val lum = (0.299f * r + 0.587f * g + 0.114f * b).roundToInt()
            if (lum < minLum) minLum = lum
            if (lum > maxLum) maxLum = lum
        }
        val avgR = totalR / pixels.size.toFloat()
        val avgG = totalG / pixels.size.toFloat()
        val avgB = totalB / pixels.size.toFloat()
        val avgLum = (0.299f * avgR + 0.587f * avgG + 0.114f * avgB)

        val brightnessAdj = ((128f - avgLum) / 128f * 30f).coerceIn(-50f, 50f)
        val contrastAdj = ((maxLum - minLum) / 255f).let { range ->
            if (range < 0.5f) (0.5f - range) * 60f else 0f
        }.coerceIn(0f, 40f)
        val warmthAdj = ((avgR - avgB) / 255f * -30f).coerceIn(-30f, 30f)

        return mapOf(
            "brightness" to brightnessAdj,
            "contrast" to contrastAdj,
            "warmth" to warmthAdj
        )
    }
}
