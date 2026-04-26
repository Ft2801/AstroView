package com.astroview.engine

import android.graphics.Bitmap
import com.astroview.model.AstroImage

/**
 * Converts a normalized AstroImage float array to an Android Bitmap for display.
 *
 * The input float data must be normalized to [0.0, 1.0].
 * Monochrome images (channels = 1) are expanded to grayscale RGB.
 * Color images (channels >= 3) use the first three channels as R, G, B.
 *
 * The output Bitmap uses ARGB_8888 configuration with full opacity (alpha = 255).
 */
object ImageProcessor {

    /**
     * Converts [image] to a Bitmap, optionally using [stretchedData] in place of image.data.
     *
     * @param image         The source AstroImage providing dimensions and channel count.
     * @param stretchedData Optional pre-computed stretch data. If null, image.data is used.
     * @return              An ARGB_8888 Bitmap ready for display.
     */
    fun toBitmap(image: AstroImage, stretchedData: FloatArray? = null): Bitmap {
        val width   = image.width
        val height  = image.height
        val channels = image.channels
        val data    = stretchedData ?: image.data
        val pixels  = IntArray(width * height)

        if (channels >= 3) {
            // Color image: pack R, G, B channels into ARGB integers.
            for (i in 0 until width * height) {
                val r = (data[i * channels]     * 255f).toInt().coerceIn(0, 255)
                val g = (data[i * channels + 1] * 255f).toInt().coerceIn(0, 255)
                val b = (data[i * channels + 2] * 255f).toInt().coerceIn(0, 255)
                pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        } else {
            // Monochrome image: replicate the single channel to R, G, and B.
            for (i in 0 until width * height) {
                val v = (data[i] * 255f).toInt().coerceIn(0, 255)
                pixels[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}