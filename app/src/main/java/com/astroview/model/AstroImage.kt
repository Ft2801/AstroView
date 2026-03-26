package com.astroview.model

/**
 * Represents a decoded astronomical image in normalized float [0..1] per channel.
 * Mono images have channels=1, color images channels=3 (R,G,B).
 * Data layout: FloatArray of size width * height * channels, row-major, channel-interleaved.
 */
data class AstroImage(
    val width: Int,
    val height: Int,
    val channels: Int,         // 1 = mono, 3 = RGB
    val data: FloatArray,      // normalized to [0,1]
    val bitDepth: Int,         // original bit depth for info display
    val format: String         // "FITS", "XISF", "TIFF"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AstroImage) return false
        return width == other.width && height == other.height &&
                channels == other.channels && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + channels
        result = 31 * result + data.contentHashCode()
        return result
    }
}