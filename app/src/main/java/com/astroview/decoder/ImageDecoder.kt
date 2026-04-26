package com.astroview.decoder

import android.content.Context
import android.net.Uri
import com.astroview.model.AstroImage
import java.io.BufferedInputStream

/**
 * Entry point for all image decoding operations.
 *
 * Responsibilities:
 * - Resolve the file name from the content URI.
 * - Enforce the file size limit (50 MB) and resolution limit (10 megapixels).
 * - Detect the image format via magic bytes or file extension.
 * - Delegate decoding to the appropriate format-specific decoder.
 *
 * Supported formats: FITS (.fits, .fit, .fts), XISF (.xisf).
 */
object ImageDecoder {

    /** Maximum allowed file size in bytes (50 MB). */
    private const val MAX_FILE_SIZE_BYTES = 50L * 1024L * 1024L

    /** Maximum allowed image resolution in total pixels (10 megapixels). */
    private const val MAX_PIXELS = 10_000_000L

    /**
     * Decodes the image at the given URI and returns a normalized AstroImage.
     *
     * @param context Android context used to access the content resolver.
     * @param uri     URI of the file to decode.
     * @return        A decoded and normalized AstroImage.
     * @throws IllegalArgumentException if the format is unsupported, the file is too large,
     *                                  or the resolution exceeds the allowed limit.
     */
    fun decode(context: Context, uri: Uri): AstroImage {
        val name = getFileName(context, uri).lowercase()

        // Enforce the 50 MB file size limit before reading any pixel data.
        val fileSize = getFileSize(context, uri)
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            val sizeMb = fileSize / (1024.0 * 1024.0)
            throw IllegalArgumentException(
                "File too large (%.1f MB). Maximum supported size is 50 MB.".format(sizeMb)
            )
        }

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open file.")

        val image = inputStream.buffered(65_536).use { bis ->
            // Peek at the first 16 bytes to detect format from magic bytes.
            bis.mark(16)
            val magic = ByteArray(16)
            val bytesRead = bis.read(magic)
            bis.reset()

            when {
                isFits(magic, bytesRead, name) -> FitsDecoder.decode(bis)
                isXisf(magic, bytesRead)       -> XisfDecoder.decode(bis)
                name.endsWith(".fits") || name.endsWith(".fit") ||
                        name.endsWith(".fts")  -> FitsDecoder.decode(bis)
                name.endsWith(".xisf")         -> XisfDecoder.decode(bis)
                else -> throw IllegalArgumentException(
                    "Unsupported format. AstroView supports FITS and XISF files only."
                )
            }
        }

        // Enforce the 10 megapixel resolution limit after decoding the header.
        val totalPixels = image.width.toLong() * image.height.toLong()
        if (totalPixels > MAX_PIXELS) {
            throw IllegalArgumentException(
                "Image resolution too large (%dx%d, %.1f MP). Maximum is 10 megapixels.".format(
                    image.width,
                    image.height,
                    totalPixels / 1_000_000.0
                )
            )
        }

        return image
    }

    // -------------------------------------------------------------------------
    // Format detection helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true if the magic bytes or file name match the FITS format.
     * FITS files begin with the ASCII string "SIMPLE".
     */
    private fun isFits(magic: ByteArray, len: Int, name: String): Boolean {
        if (len >= 6 && String(magic, 0, 6) == "SIMPLE") return true
        return name.endsWith(".fits") || name.endsWith(".fit") || name.endsWith(".fts")
    }

    /**
     * Returns true if the magic bytes match the XISF 1.0 signature.
     * XISF files begin with the 8-byte ASCII string "XISF0100".
     */
    private fun isXisf(magic: ByteArray, len: Int): Boolean {
        if (len < 8) return false
        return String(magic, 0, 8) == "XISF0100"
    }

    // -------------------------------------------------------------------------
    // URI metadata helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the display name of the file referenced by [uri], or a fallback
     * derived from the last path segment if the display name is unavailable.
     */
    private fun getFileName(context: Context, uri: Uri): String {
        var name = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(index) ?: ""
            }
        }
        if (name.isEmpty()) {
            name = uri.lastPathSegment ?: "unknown"
        }
        return name
    }

    /**
     * Returns the file size in bytes for the given URI, or Long.MAX_VALUE if
     * the size cannot be determined (which will trigger the size limit check).
     */
    private fun getFileSize(context: Context, uri: Uri): Long {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getLong(index)
            }
        }
        return Long.MAX_VALUE
    }
}