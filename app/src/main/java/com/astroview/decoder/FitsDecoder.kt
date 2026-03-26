package com.astroview.decoder

import com.astroview.model.AstroImage
import java.io.BufferedInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object FitsDecoder {

    fun decode(stream: BufferedInputStream): AstroImage {
        val headers = mutableMapOf<String, String>()
        var headerComplete = false

        // FITS headers are 2880-byte blocks of 80-char records
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
                    val valuePart = record.substring(10).split("/")[0].trim()
                        .removeSurrounding("'").trim()
                    headers[key] = valuePart
                }
            }
        }

        val bitpix = headers["BITPIX"]?.toIntOrNull()
            ?: throw IllegalArgumentException("Missing BITPIX in FITS header")
        val naxis = headers["NAXIS"]?.toIntOrNull()
            ?: throw IllegalArgumentException("Missing NAXIS in FITS header")

        if (naxis < 2) throw IllegalArgumentException("NAXIS must be >= 2, got $naxis")

        val naxis1 = headers["NAXIS1"]?.toIntOrNull() ?: throw IllegalArgumentException("Missing NAXIS1")
        val naxis2 = headers["NAXIS2"]?.toIntOrNull() ?: throw IllegalArgumentException("Missing NAXIS2")
        val naxis3 = if (naxis >= 3) headers["NAXIS3"]?.toIntOrNull() ?: 1 else 1

        val width = naxis1
        val height = naxis2
        val channels = if (naxis3 > 1) minOf(naxis3, 3) else 1

        val bzero = headers["BZERO"]?.toDoubleOrNull() ?: 0.0
        val bscale = headers["BSCALE"]?.toDoubleOrNull() ?: 1.0

        val bytesPerPixel = kotlin.math.abs(bitpix) / 8
        val totalPixels = width.toLong() * height.toLong() * channels.toLong()
        val dataSize = (totalPixels * bytesPerPixel).toInt()

        val rawBytes = ByteArray(dataSize)
        readFully(stream, rawBytes)

        // FITS is always big-endian
        val buffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.BIG_ENDIAN)

        val pixelCount = width * height * channels
        val floatData = FloatArray(pixelCount)

        // Read raw values
        when (bitpix) {
            8 -> {
                for (i in 0 until pixelCount) {
                    floatData[i] = ((buffer.get().toInt() and 0xFF) * bscale + bzero).toFloat()
                }
            }
            16 -> {
                for (i in 0 until pixelCount) {
                    floatData[i] = (buffer.short.toDouble() * bscale + bzero).toFloat()
                }
            }
            32 -> {
                for (i in 0 until pixelCount) {
                    floatData[i] = (buffer.int.toDouble() * bscale + bzero).toFloat()
                }
            }
            -32 -> {
                for (i in 0 until pixelCount) {
                    floatData[i] = (buffer.float.toDouble() * bscale + bzero).toFloat()
                }
            }
            -64 -> {
                for (i in 0 until pixelCount) {
                    floatData[i] = (buffer.double * bscale + bzero).toFloat()
                }
            }
            else -> throw IllegalArgumentException("Unsupported BITPIX: $bitpix")
        }

        // FITS stores planes sequentially: all R, then all G, then all B
        // Convert to interleaved and normalize
        val normalized: FloatArray
        if (channels > 1) {
            val planeSize = width * height
            normalized = FloatArray(pixelCount)
            // Find min/max across all data
            var min = Float.MAX_VALUE
            var max = Float.MIN_VALUE
            for (v in floatData) {
                if (v.isFinite()) {
                    if (v < min) min = v
                    if (v > max) max = v
                }
            }
            val range = if (max > min) max - min else 1f

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
            // Mono: normalize in-place
            var min = Float.MAX_VALUE
            var max = Float.MIN_VALUE
            for (v in floatData) {
                if (v.isFinite()) {
                    if (v < min) min = v
                    if (v > max) max = v
                }
            }
            val range = if (max > min) max - min else 1f
            normalized = FloatArray(pixelCount) { i ->
                ((floatData[i] - min) / range).coerceIn(0f, 1f)
            }
        }

        val actualBitDepth = when (bitpix) {
            8 -> 8
            16 -> 16
            32 -> 32
            -32 -> 32
            -64 -> 64
            else -> 32
        }

        return AstroImage(
            width = width,
            height = height,
            channels = channels,
            data = normalized,
            bitDepth = actualBitDepth,
            format = "FITS"
        )
    }

    private fun readFully(stream: BufferedInputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = stream.read(buffer, offset, buffer.size - offset)
            if (read < 0) break
            offset += read
        }
    }
}