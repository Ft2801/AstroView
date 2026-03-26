package com.astroview.decoder

import android.content.Context
import android.net.Uri
import com.astroview.model.AstroImage
import java.io.BufferedInputStream

object ImageDecoder {

    fun decode(context: Context, uri: Uri): AstroImage {
        val name = getFileName(context, uri).lowercase()
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open file")

        return inputStream.buffered(65536).use { bis ->
            // Read magic bytes to determine format
            bis.mark(16)
            val magic = ByteArray(16)
            val read = bis.read(magic)
            bis.reset()

            when {
                isFits(magic, read, name) -> FitsDecoder.decode(bis)
                isXisf(magic, read) -> XisfDecoder.decode(bis)
                isTiff(magic, read, name) -> TiffDecoder.decode(bis)
                name.endsWith(".fits") || name.endsWith(".fit") ||
                        name.endsWith(".fts") -> FitsDecoder.decode(bis)
                name.endsWith(".xisf") -> XisfDecoder.decode(bis)
                name.endsWith(".tiff") || name.endsWith(".tif") -> TiffDecoder.decode(bis)
                else -> throw IllegalArgumentException(
                    "Unsupported format. Only FITS, XISF and TIFF are supported."
                )
            }
        }
    }

    private fun isFits(magic: ByteArray, len: Int, name: String): Boolean {
        if (len < 6) return false
        val header = String(magic, 0, 6)
        return header == "SIMPLE" || name.endsWith(".fits") ||
                name.endsWith(".fit") || name.endsWith(".fts")
    }

    private fun isXisf(magic: ByteArray, len: Int): Boolean {
        if (len < 8) return false
        return String(magic, 0, 8) == "XISF0100"
    }

    private fun isTiff(magic: ByteArray, len: Int, name: String): Boolean {
        if (len < 4) return false
        // II (little-endian) or MM (big-endian) followed by 42
        val isLE = magic[0] == 0x49.toByte() && magic[1] == 0x49.toByte() &&
                magic[2] == 0x2A.toByte() && magic[3] == 0x00.toByte()
        val isBE = magic[0] == 0x4D.toByte() && magic[1] == 0x4D.toByte() &&
                magic[2] == 0x00.toByte() && magic[3] == 0x2A.toByte()
        return isLE || isBE || name.endsWith(".tiff") || name.endsWith(".tif")
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(idx) ?: ""
            }
        }
        if (name.isEmpty()) {
            name = uri.lastPathSegment ?: "unknown"
        }
        return name
    }
}