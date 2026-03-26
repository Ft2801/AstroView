package com.astroview.decoder

import com.astroview.model.AstroImage
import java.io.BufferedInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object TiffDecoder {

    private const val TAG_WIDTH = 256
    private const val TAG_HEIGHT = 257
    private const val TAG_BITS_PER_SAMPLE = 258
    private const val TAG_COMPRESSION = 259
    private const val TAG_PHOTOMETRIC = 262
    private const val TAG_STRIP_OFFSETS = 273
    private const val TAG_SAMPLES_PER_PIXEL = 277
    private const val TAG_ROWS_PER_STRIP = 278
    private const val TAG_STRIP_BYTE_COUNTS = 279
    private const val TAG_SAMPLE_FORMAT = 339

    fun decode(stream: BufferedInputStream): AstroImage {
        // Read entire file into memory for random access
        val allBytes = stream.readBytes()
        val buf = ByteBuffer.wrap(allBytes)

        // Determine byte order
        val order = when {
            allBytes[0] == 0x49.toByte() && allBytes[1] == 0x49.toByte() -> ByteOrder.LITTLE_ENDIAN
            allBytes[0] == 0x4D.toByte() && allBytes[1] == 0x4D.toByte() -> ByteOrder.BIG_ENDIAN
            else -> throw IllegalArgumentException("Invalid TIFF byte order")
        }
        buf.order(order)

        // Skip magic number check (bytes 2-3 = 42)
        buf.position(4)
        val ifdOffset = buf.int
        buf.position(ifdOffset)

        val numEntries = buf.short.toInt() and 0xFFFF

        var width = 0
        var height = 0
        var bitsPerSample = 16
        var samplesPerPixel = 1
        var sampleFormat = 1  // 1=uint, 3=float
        var stripOffsets = mutableListOf<Long>()
        var stripByteCounts = mutableListOf<Long>()
        var compression = 1
        var rowsPerStrip = Int.MAX_VALUE

        for (i in 0 until numEntries) {
            val tag = buf.short.toInt() and 0xFFFF
            val type = buf.short.toInt() and 0xFFFF
            val count = buf.int
            val valueOffset = buf.position()

            val value = readTagValue(buf, allBytes, type, count, order)

            when (tag) {
                TAG_WIDTH -> width = value[0].toInt()
                TAG_HEIGHT -> height = value[0].toInt()
                TAG_BITS_PER_SAMPLE -> bitsPerSample = value[0].toInt()
                TAG_SAMPLES_PER_PIXEL -> samplesPerPixel = value[0].toInt()
                TAG_SAMPLE_FORMAT -> sampleFormat = value[0].toInt()
                TAG_COMPRESSION -> compression = value[0].toInt()
                TAG_ROWS_PER_STRIP -> rowsPerStrip = value[0].toInt()
                TAG_STRIP_OFFSETS -> stripOffsets = value.toMutableList()
                TAG_STRIP_BYTE_COUNTS -> stripByteCounts = value.toMutableList()
            }

            buf.position(valueOffset + 4)
        }

        if (compression != 1) {
            throw IllegalArgumentException("Only uncompressed TIFF is supported (compression=$compression)")
        }

        val channels = minOf(samplesPerPixel, 3)
        val bytesPerSample = bitsPerSample / 8
        val pixelCount = width * height * channels

        // Read pixel data from strips
        val floatData = FloatArray(pixelCount)
        var pixelIdx = 0

        for (stripIdx in stripOffsets.indices) {
            val offset = stripOffsets[stripIdx].toInt()
            val byteCount = if (stripIdx < stripByteCounts.size) {
                stripByteCounts[stripIdx].toInt()
            } else {
                pixelCount * bytesPerSample - pixelIdx * bytesPerSample
            }

            val stripBuf = ByteBuffer.wrap(allBytes, offset, byteCount).order(order)
            val samplesInStrip = byteCount / bytesPerSample

            for (s in 0 until samplesInStrip) {
                if (pixelIdx >= pixelCount) break
                floatData[pixelIdx] = when {
                    sampleFormat == 3 && bitsPerSample == 32 -> stripBuf.float
                    sampleFormat == 3 && bitsPerSample == 64 -> stripBuf.double.toFloat()
                    bitsPerSample == 16 -> (stripBuf.short.toInt() and 0xFFFF).toFloat()
                    bitsPerSample == 32 && sampleFormat == 1 -> {
                        (stripBuf.int.toLong() and 0xFFFFFFFFL).toFloat()
                    }
                    bitsPerSample == 32 -> stripBuf.int.toFloat()
                    bitsPerSample == 8 -> (stripBuf.get().toInt() and 0xFF).toFloat()
                    else -> stripBuf.float
                }
                pixelIdx++
            }
        }

        // Normalize
        var min = Float.MAX_VALUE
        var max = Float.MIN_VALUE
        for (v in floatData) {
            if (v.isFinite()) {
                if (v < min) min = v
                if (v > max) max = v
            }
        }
        val range = if (max > min) max - min else 1f

        // TIFF stores interleaved (RGBRGB...) by default
        val normalized = FloatArray(pixelCount) { i ->
            ((floatData[i] - min) / range).coerceIn(0f, 1f)
        }

        return AstroImage(
            width = width,
            height = height,
            channels = channels,
            data = normalized,
            bitDepth = bitsPerSample,
            format = "TIFF"
        )
    }

    private fun readTagValue(
        buf: ByteBuffer,
        allBytes: ByteArray,
        type: Int,
        count: Int,
        order: ByteOrder
    ): List<Long> {
        val typeSize = when (type) {
            1, 6 -> 1   // BYTE, SBYTE
            3, 8 -> 2   // SHORT, SSHORT
            4, 9 -> 4   // LONG, SLONG
            5, 10 -> 8  // RATIONAL
            11 -> 4     // FLOAT
            12 -> 8     // DOUBLE
            else -> 1
        }

        val totalSize = count * typeSize
        val values = mutableListOf<Long>()

        val dataBuf: ByteBuffer = if (totalSize <= 4) {
            // Value is inline
            val pos = buf.position()
            ByteBuffer.wrap(allBytes, pos, 4).order(order)
        } else {
            // Value is at offset
            val offset = buf.int
            buf.position(buf.position() - 4)
            ByteBuffer.wrap(allBytes, offset, totalSize).order(order)
        }

        for (i in 0 until count) {
            val v = when (type) {
                1 -> (dataBuf.get().toInt() and 0xFF).toLong()
                3 -> (dataBuf.short.toInt() and 0xFFFF).toLong()
                4 -> (dataBuf.int.toLong() and 0xFFFFFFFFL)
                9 -> dataBuf.int.toLong()
                11 -> dataBuf.float.toLong()
                else -> (dataBuf.get().toInt() and 0xFF).toLong()
            }
            values.add(v)
        }

        return values
    }
}