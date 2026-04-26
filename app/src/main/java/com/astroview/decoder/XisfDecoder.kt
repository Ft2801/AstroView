package com.astroview.decoder

import com.astroview.model.AstroImage
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Decoder for XISF (Extensible Image Serialization Format) files.
 *
 * Specification reference: https://pixinsight.com/xisf/
 *
 * Supported sample formats: UInt8, UInt16, UInt32, Float32, Float64.
 * Supports monochrome and RGB color spaces.
 * Supports both attachment-based and inline pixel data.
 * Uncompressed data only. Byte order is little-endian per the XISF specification.
 *
 * XISF file structure:
 *   Bytes 0-7   : Signature "XISF0100"
 *   Bytes 8-11  : XML header length (4 bytes, little-endian)
 *   Bytes 12-15 : Reserved (4 bytes)
 *   Bytes 16-N  : XML header of headerLength bytes
 *   Bytes N+1.. : Pixel data (attachment) or further embedded blocks
 *
 * The output float array is normalized to [0.0, 1.0].
 * For multi-channel images the output is channel-interleaved (RGBRGB...).
 */
object XisfDecoder {

    /**
     * Decodes an XISF file from the given buffered stream.
     *
     * @param stream A buffered input stream positioned at the beginning of the file.
     * @return       A normalized AstroImage.
     * @throws IllegalArgumentException on invalid signature, missing XML attributes,
     *                                  or unsupported configurations.
     */
    fun decode(stream: BufferedInputStream): AstroImage {

        // Validate the 8-byte file signature.
        val signature = ByteArray(8)
        readFully(stream, signature)
        val sig = String(signature)
        if (sig != "XISF0100") {
            throw IllegalArgumentException("Invalid XISF signature: $sig")
        }

        // Read the 4-byte XML header length and 4 reserved bytes.
        val headerMeta = ByteArray(8)
        readFully(stream, headerMeta)
        val headerLen = ByteBuffer.wrap(headerMeta, 0, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int

        // Read and parse the XML header.
        val xmlBytes = ByteArray(headerLen)
        readFully(stream, xmlBytes)

        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(xmlBytes))

        val imageNode = doc.getElementsByTagName("Image").item(0)
            ?: throw IllegalArgumentException("No Image element found in XISF header.")

        val attrs = imageNode.attributes

        val geometry = attrs.getNamedItem("geometry")?.nodeValue
            ?: throw IllegalArgumentException("Missing 'geometry' attribute in XISF Image element.")
        val sampleFormat = attrs.getNamedItem("sampleFormat")?.nodeValue
            ?: throw IllegalArgumentException("Missing 'sampleFormat' attribute in XISF Image element.")
        val location = attrs.getNamedItem("location")?.nodeValue ?: ""

        // Parse geometry: "width:height:channels"
        val geomParts = geometry.split(":")
        val width    = geomParts[0].toInt()
        val height   = geomParts[1].toInt()
        val channels = if (geomParts.size > 2) geomParts[2].toInt() else 1

        val pixelCount = width * height * channels

        val bytesPerSample = when {
            sampleFormat.contains("Float64")        -> 8
            sampleFormat.contains("Float32")        -> 4
            sampleFormat.contains("UInt32")         -> 4
            sampleFormat.contains("UInt16")         -> 2
            sampleFormat.contains("UInt8")          -> 1
            else                                    -> 4
        }

        // Read pixel data from an attachment block or inline from the stream.
        val rawBytes: ByteArray

        if (location.startsWith("attachment:")) {
            // Attachment format: "attachment:bytePosition:byteCount"
            val locParts   = location.split(":")
            val attachPos  = locParts[1].toLong()
            val attachSize = locParts[2].toInt()

            // Current stream position is at byte 16 + headerLen.
            val currentPos  = 16L + headerLen
            val skipAmount  = attachPos - currentPos
            if (skipAmount > 0) {
                var skipped = 0L
                while (skipped < skipAmount) {
                    val s = stream.skip(skipAmount - skipped)
                    if (s <= 0) break
                    skipped += s
                }
            }
            rawBytes = ByteArray(attachSize)
            readFully(stream, rawBytes)
        } else {
            // Inline data: pixel bytes follow the XML header directly.
            val dataSize = pixelCount * bytesPerSample
            rawBytes = ByteArray(dataSize)
            readFully(stream, rawBytes)
        }

        // XISF uses little-endian byte order.
        val buffer    = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN)
        val floatData = FloatArray(pixelCount)

        when {
            sampleFormat.contains("Float32") -> {
                for (i in 0 until pixelCount) floatData[i] = buffer.float
            }
            sampleFormat.contains("Float64") -> {
                for (i in 0 until pixelCount) floatData[i] = buffer.double.toFloat()
            }
            sampleFormat.contains("UInt16")  -> {
                for (i in 0 until pixelCount) {
                    floatData[i] = (buffer.short.toInt() and 0xFFFF).toFloat()
                }
            }
            sampleFormat.contains("UInt32")  -> {
                for (i in 0 until pixelCount) {
                    floatData[i] = (buffer.int.toLong() and 0xFFFFFFFFL).toFloat()
                }
            }
            sampleFormat.contains("UInt8")   -> {
                for (i in 0 until pixelCount) {
                    floatData[i] = (buffer.get().toInt() and 0xFF).toFloat()
                }
            }
            else -> {
                for (i in 0 until pixelCount) floatData[i] = buffer.float
            }
        }

        // Normalize to [0.0, 1.0].
        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        for (v in floatData) {
            if (v.isFinite()) {
                if (v < min) min = v
                if (v > max) max = v
            }
        }
        val range = if (max > min) max - min else 1f

        // XISF stores planar data (all R, then all G, then all B).
        // Convert to interleaved layout for uniform downstream processing.
        val normalized: FloatArray
        val outputChannels = minOf(channels, 3)

        if (channels > 1) {
            val planeSize = width * height
            normalized = FloatArray(width * height * outputChannels)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    for (c in 0 until outputChannels) {
                        val srcIdx = c * planeSize + y * width + x
                        val dstIdx = (y * width + x) * outputChannels + c
                        normalized[dstIdx] = ((floatData[srcIdx] - min) / range).coerceIn(0f, 1f)
                    }
                }
            }
        } else {
            normalized = FloatArray(pixelCount) { i ->
                ((floatData[i] - min) / range).coerceIn(0f, 1f)
            }
        }

        val bitDepth = when {
            sampleFormat.contains("64") -> 64
            sampleFormat.contains("32") -> 32
            sampleFormat.contains("16") -> 16
            sampleFormat.contains("8")  -> 8
            else                        -> 32
        }

        return AstroImage(
            width    = width,
            height   = height,
            channels = outputChannels,
            data     = normalized,
            bitDepth = bitDepth,
            format   = "XISF"
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