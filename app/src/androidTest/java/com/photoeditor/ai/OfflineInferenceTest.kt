package com.photoeditor.ai

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.lang.reflect.InvocationTargetException

@RunWith(AndroidJUnit4::class)
class OfflineInferenceTest {
    @Test fun bundledPhotoModelRunsRealCpuInference() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = OfflineUpscaler(context)
        val type = OfflineUpscaler::class.java
        fun invoke(name: String, types: Array<Class<*>>, vararg args: Any): Any? {
            return type.getDeclaredMethod(name, *types).apply { isAccessible = true }.invoke(engine, *args)
        }
        val param = File(context.cacheDir, "test.param")
        val model = File(context.cacheDir, "test.bin")
        context.assets.open("models/realesrgan-x4plus.param").use { input -> param.outputStream().use { input.copyTo(it) } }
        context.assets.open("models/realesrgan-x4plus.bin").use { input -> model.outputStream().use { input.copyTo(it) } }
        val createTypes = arrayOf(String::class.java, String::class.java, java.lang.Boolean.TYPE)
        val handle = invoke("nativeCreate", createTypes, param.path, model.path, false) as Long
        assertTrue(handle != 0L)
        val input = Bitmap.createBitmap(17, 13, Bitmap.Config.ARGB_8888)
        for (y in 0 until 13) for (x in 0 until 17) input.setPixel(x, y, Color.rgb((x*31+y*13)%256, (x*11+y*23)%256, (x*19+y*7)%256))
        val output = Bitmap.createBitmap(68, 52, Bitmap.Config.ARGB_8888)
        try {
            assertEquals(false, invoke("nativeUsesGpu", arrayOf(java.lang.Long.TYPE), handle))
            invoke("nativeTile", arrayOf(java.lang.Long.TYPE, Bitmap::class.java, Bitmap::class.java, Integer.TYPE, Integer.TYPE), handle, input, output, 0, 0)
            val resized = Bitmap.createScaledBitmap(input, 68, 52, true)
            var difference = 0L
            for (y in 0 until 52) for (x in 0 until 68) {
                val actual = output.getPixel(x, y)
                assertEquals(255, Color.alpha(actual))
                difference += kotlin.math.abs(Color.red(actual)-Color.red(resized.getPixel(x,y)))
            }
            assertTrue("Inference must differ from ordinary resizing", difference > 68*52)
            resized.recycle()
        } finally {
            invoke("nativeDestroy", arrayOf(java.lang.Long.TYPE), handle)
            input.recycle(); output.recycle(); param.delete(); model.delete()
        }
        try {
            invoke("nativeCreate", createTypes, "/missing.param", "/missing.bin", false)
            fail("Missing model must not fall back to resizing")
        } catch (expected: InvocationTargetException) {
            assertTrue(expected.cause is IllegalStateException)
        }
    }
}
