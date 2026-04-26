package com.astroview.model

/**
 * Represents a decoded astronomical image with all pixel data normalized to [0.0, 1.0].
 *
 * Data layout:
 *   - Monochrome images: channels = 1, data size = width * height.
 *   - Color images:      channels = 3, data size = width * height * 3.
 *   - Data is stored row-major with channels interleaved (RGBRGB...).
 *
 * @property width     Image width in pixels.
 * @property height    Image height in pixels.
 * @property channels  Number of color channels: 1 for monochrome, 3 for RGB.
 * @property data      Normalized float pixel data in [0.0, 1.0].
 * @property bitDepth  Original bit depth of the source file, used for display in the info bar.
 * @property format    Source format identifier: "FITS" or "XISF".
 */
data class AstroImage(
    val width:    Int,
    val height:   Int,
    val channels: Int,
    val data:     FloatArray,
    val bitDepth: Int,
    val format:   String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AstroImage) return false
        return width    == other.width    &&
               height   == other.height   &&
               channels == other.channels &&
               data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + channels
        result = 31 * result + data.contentHashCode()
        return result
    }
}