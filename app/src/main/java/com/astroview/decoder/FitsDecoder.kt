package com.astroview.decoder

import com.astroview.model.AstroImage
import java.io.BufferedInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decoder for FITS (Flexible Image Transport System) files.
 *
 * Specification reference: https://fits.gsfc.nasa.gov/fits_standard.html
 *
 * Supported BITPIX values:
 *   8   - unsigned 8-bit integer
 *   16  - signed 16-bit integer
 *   32  - signed 32-bit integer
 *   -32 - 32-bit IEEE 754 float
 *   -64 - 64-bit IEEE 754 double
 *
 * Supports 2D (NAXIS=2, monochrome) and 3D (NAXIS=3, RGB) images.
 * BZERO and BSCALE are applied automatically.
 * Byte order is always big-endian per the FITS standard.
 *
 * The output float array is normalized to [0.0, 1.0].
 * For multi-channel images the output is channel-interleaved (RGBRGB...).
 */
object FitsDecoder {

    /**
     * Decodes a FITS file from the given buffered stream.
     *
     * @param stream A buffered input stream positioned at the beginning of the file.
     * @return       A normalized AstroImage.
     * @throws IllegalArgumentException on missing or invalid header keywords.
     */
    fun decode(stream: BufferedInputStream): AstroImage {
        val headers = mutableMapOf<String, String>()

        // FITS headers consist of 2880-byte blocks containing 36 records of 80 characters each.
        // Parsing continues until the END keyword is encountered.
        var headerComplete = false
        while (!headerComplete) {
            val block = ByteArray(2880)
            readFully(stream, block)
            for (i in 0 until 36) {
                val record = String(block, i * 80, 80).trimEnd()
                if (record.startsWith("END")) {
                    headerComplete = true
                    break
                }
                if (record.contains('=')) {
                    val key = record.substring(0, 8).trim()
                    val valuePart = record.substring(10)
                        .split("/")[0]
                        .trim()
                        .removeSurrounding("'")
                        .trim()
                    headers[key] = valuePart
                }
            }
        }

        // Extract mandatory header keywords.
        val bitpix = headers["BITPIX"]?.toIntOrNull()
            ?: throw IllegalArgumentException("Missing BITPIX in FITS header.")
        val naxis = headers["NAXIS"]?.toIntOrNull()
            ?: throw IllegalArgumentException("Missing NAXIS in FITS header.")

        if (naxis < 2) {
            throw IllegalArgumentException("NAXIS must be at least 2, got $naxis.")
        }

        val naxis1 = headers["NAXIS1"]?.toIntOrNull()
            ?: throw IllegalArgumentException("Missing NAXIS1 in FITS header.")
        val naxis2 = headers["NAXIS2"]?.toIntOrNull()
            ?: throw IllegalArgumentException("Missing NAXIS2 in FITS header.")
        val naxis3 = if (naxis >= 3) headers["NAXIS3"]?.toIntOrNull() ?: 1 else 1

        val width    = naxis1
        val height   = naxis2
        val channels = if (naxis3 > 1) minOf(naxis3, 3) else 1

        // Optional scaling keywords.
        val bzero  = headers["BZERO"]?.toDoubleOrNull()  ?: 0.0
        val bscale = headers["BSCALE"]?.toDoubleOrNull() ?: 1.0

        val bytesPerPixel = kotlin.math.abs(bitpix) / 8
        val pixelCount    = width * height * channels
        val dataSize      = pixelCount.toLong() * bytesPerPixel

        val rawBytes = ByteArray(dataSize.toInt())
        readFully(stream, rawBytes)

        // FITS is always big-endian.
        val buffer    = ByteBuffer.wrap(rawBytes).order(ByteOrder.BIG_ENDIAN)
        val floatData = FloatArray(pixelCount)

        // Convert raw bytes to float values, applying BSCALE and BZERO.
        when (bitpix) {
            8    -> for (i in 0 until pixelCount) {
                floatData[i] = ((buffer.get().toInt() and 0xFF) * bscale + bzero).toFloat()
            }
            16   -> for (i in 0 until pixelCount) {
                floatData[i] = (buffer.short.toDouble() * bscale + bzero).toFloat()
            }
            32   -> for (i in 0 until pixelCount) {
                floatData[i] = (buffer.int.toDouble() * bscale + bzero).toFloat()
            }
            -32  -> for (i in 0 until pixelCount) {
                floatData[i] = (buffer.float.toDouble() * bscale + bzero).toFloat()
            }
            -64  -> for (i in 0 until pixelCount) {
                floatData[i] = (buffer.double * bscale + bzero).toFloat()
            }
            else -> throw IllegalArgumentException("Unsupported BITPIX value: $bitpix.")
        }

        // Normalize to [0.0, 1.0] and convert from planar (all R, all G, all B)
        // to interleaved (RGBRGB...) layout.
        val normalized: FloatArray

        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        for (v in floatData) {
            if (v.isFinite()) {
                if (v < min) min = v
                if (v > max) max = v
            }
        }
        val range = if (max > min) max - min else 1f

        if (channels > 1) {
            val planeSize = width * height
            normalized = FloatArray(pixelCount)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    for (c in 0 until channels) {
                        val srcIdx = c * planeSize + y * width + x
                        val dstIdx = (y * width + x) * channels + c
                        normalized[dstIdx] = ((floatData[srcIdx] - min) / range).coerceIn(0f, 1f)
                    }
                }
            }
        } else {
            normalized = FloatArray(pixelCount) { i ->
                ((floatData[i] - min) / range).coerceIn(0f, 1f)
            }
        }

        val bitDepth = when (bitpix) {
            8    -> 8
            16   -> 16
            32   -> 32
            -32  -> 32
            -64  -> 64
            else -> 32
        }

        return AstroImage(
            width    = width,
            height   = height,
            channels = channels,
            data     = normalized,
            bitDepth = bitDepth,
            format   = "FITS"
        )
    }

    /**
     * Reads exactly [buffer.size] bytes from [stream] into [buffer].
     * Continues reading until the buffer is full or the stream is exhausted.
     */
    private fun readFully(stream: BufferedInputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = stream.read(buffer, offset, buffer.size - offset)
            if (read < 0) break
            offset += read
        }
    }
}