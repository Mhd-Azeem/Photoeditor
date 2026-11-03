package com.photoeditor.ai

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/** No network operations. Every output tile must pass actual NCNN inference. */
class OfflineUpscaler(private val context: Context) {
    companion object {
        private val inferenceMutex = Mutex()
        init { System.loadLibrary("az_upscaler") }
    }
    private external fun nativeCreate(param: String, model: String, gpu: Boolean): Long
    private external fun nativeUsesGpu(handle: Long): Boolean
    private external fun nativeTile(handle: Long, input: Bitmap, output: Bitmap, x: Int, y: Int)
    private external fun nativeDestroy(handle: Long)

    suspend fun enhance(source: Bitmap, cancelled: AtomicBoolean, progress: (Float, String) -> Unit): Bitmap =
        withContext(Dispatchers.Default) {
            inferenceMutex.withLock {
                fun checkCancel() { if (cancelled.get()) throw CancellationException("Enhancement cancelled") }
                checkCancel()
                progress(0f, "Checking bundled photo model…")
                val param = modelFile("realesrgan-x4plus.param", "35330ececcea33b6c397a72548e788d5d53becee4734c50b7fada36e89f10a86")
                val model = modelFile("realesrgan-x4plus.bin", "713ee713b0353afaa27976f0563a64a5043bd70b9bd8936c2e26e25ebcdbcddf")
                checkCancel()
                // Fit the inference input to a 960-pixel long edge. The model produces 4x this size.
                // Pre-normalization is explicit; no resize ever substitutes for failed inference.
                val factor = 960.0 / maxOf(source.width, source.height)
                val w = (source.width * factor).roundToInt().coerceIn(1, 960)
                val h = (source.height * factor).roundToInt().coerceIn(1, 960)
                val scaled = Bitmap.createScaledBitmap(source, w, h, true)
                val input = if (scaled.config == Bitmap.Config.ARGB_8888) scaled
                    else scaled.copy(Bitmap.Config.ARGB_8888, false).also { if (scaled !== source) scaled.recycle() }
                var output: Bitmap? = null
                var handle = 0L
                var completed = false
                try {
                    val result = Bitmap.createBitmap(w * 4, h * 4, Bitmap.Config.ARGB_8888)
                    output = result
                    handle = try { nativeCreate(param.path, model.path, true) } catch (_: Exception) {
                        nativeCreate(param.path, model.path, false)
                    }
                    check(handle != 0L) { "AI model initialization failed" }
                    var gpu = nativeUsesGpu(handle)
                    val total = ((w + 63) / 64) * ((h + 63) / 64)
                    var done = 0
                    for (y in 0 until h step 64) for (x in 0 until w step 64) {
                        checkCancel()
                        try { nativeTile(handle, input, result, x, y) }
                        catch (failure: Exception) {
                            if (!gpu) throw failure
                            nativeDestroy(handle); handle = 0L
                            progress(done.toFloat() / total, "Vulkan failed; retrying with CPU…")
                            handle = nativeCreate(param.path, model.path, false)
                            gpu = false
                            checkCancel()
                            nativeTile(handle, input, result, x, y)
                        }
                        done++
                        progress(done.toFloat() / total, "RealESRGAN • ${if (gpu) "Vulkan" else "CPU"} • $done/$total tiles")
                    }
                    checkCancel()
                    completed = true
                    result
                } finally {
                    if (handle != 0L) nativeDestroy(handle)
                    if (input !== source) input.recycle()
                    if (!completed) output?.recycle()
                }
            }
        }

    private fun modelFile(name: String, expected: String): File {
        val dir = File(context.filesDir, "ai-model-v2").apply { mkdirs() }
        val file = File(dir, name)
        fun valid(): Boolean {
            if (!file.isFile) return false
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(64 * 1024)
                while (true) { val count = stream.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
            }
            return digest.digest().joinToString("") { "%02x".format(it) } == expected
        }
        if (!valid()) {
            val temp = File(dir, "$name.tmp")
            try {
                context.assets.open("models/$name").use { input -> temp.outputStream().use { input.copyTo(it) } }
                check(temp.renameTo(file)) { "Cannot install bundled AI model" }
                check(valid()) { "Bundled AI model checksum failed. No enhancement was produced." }
            } finally { temp.delete() }
        }
        return file
    }
}
