package com.astroview.engine

import android.graphics.Bitmap
import com.astroview.model.AstroImage

/**
 * Converts AstroImage float data to an Android Bitmap for display.
 */
object ImageProcessor {

    fun toBitmap(image: AstroImage, stretchedData: FloatArray? = null): Bitmap {
        val w = image.width
        val h = image.height
        val ch = image.channels
        val data = stretchedData ?: image.data

        val pixels = IntArray(w * h)

        if (ch >= 3) {
            for (i in 0 until w * h) {
                val r = (data[i * ch] * 255f).toInt().coerceIn(0, 255)
                val g = (data[i * ch + 1] * 255f).toInt().coerceIn(0, 255)
                val b = (data[i * ch + 2] * 255f).toInt().coerceIn(0, 255)
                pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        } else {
            for (i in 0 until w * h) {
                val v = (data[i] * 255f).toInt().coerceIn(0, 255)
                pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
        }

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }
}