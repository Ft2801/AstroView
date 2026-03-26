package com.astroview.decoder

import com.astroview.model.AstroImage
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.xml.parsers.DocumentBuilderFactory

object XisfDecoder {

    fun decode(stream: BufferedInputStream): AstroImage {
        // Read signature: "XISF0100" (8 bytes)
        val signature = ByteArray(8)
        readFully(stream, signature)
        val sig = String(signature)
        if (sig != "XISF0100") {
            throw IllegalArgumentException("Invalid XISF signature: $sig")
        }

        // Read header length (4 bytes LE) and reserved (4 bytes)
        val headerMeta = ByteArray(8)
        readFully(stream, headerMeta)
        val headerLen = ByteBuffer.wrap(headerMeta, 0, 4)
            .order(ByteOrder.LITTLE_ENDIAN).int

        // Read reserved bytes to complete the 16-byte prefix block
        // Total header block = 16 bytes signature block + headerLen
        // Actually XISF: first 16 bytes = signature(8) + headerLength(4) + reserved(4)
        // Then XML header of headerLen bytes

        val xmlBytes = ByteArray(headerLen)
        readFully(stream, xmlBytes)

        // Parse XML
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(xmlBytes))

        val imageNode = doc.getElementsByTagName("Image").item(0)
            ?: throw IllegalArgumentException("No Image element in XISF header")

        val attrs = imageNode.attributes
        val geometry = attrs.getNamedItem("geometry")?.nodeValue
            ?: throw IllegalArgumentException("Missing geometry attribute")
        val sampleFormat = attrs.getNamedItem("sampleFormat")?.nodeValue
            ?: throw IllegalArgumentException("Missing sampleFormat attribute")
        val colorSpace = attrs.getNamedItem("colorSpace")?.nodeValue ?: "Gray"

        // Parse geometry: "width:height:channels"
        val geomParts = geometry.split(":")
        val width = geomParts[0].toInt()
        val height = geomParts[1].toInt()
        val channels = if (geomParts.size > 2) geomParts[2].toInt() else 1

        // Determine location of pixel data
        // Look for attachment or embedded data
        val location = attrs.getNamedItem("location")?.nodeValue ?: ""

        val pixelCount = width * height * channels
        val bytesPerSample = when {
            sampleFormat.contains("Float32") -> 4
            sampleFormat.contains("Float64") -> 8
            sampleFormat.contains("UInt32") || sampleFormat.contains("Int32") -> 4
            sampleFormat.contains("UInt16") || sampleFormat.contains("Int16") -> 2
            sampleFormat.contains("UInt8") || sampleFormat.contains("Int8") -> 1
            else -> 4
        }

        val rawBytes: ByteArray

        if (location.startsWith("attachment:")) {
            // "attachment:position:size"
            val locParts = location.split(":")
            val attachPos = locParts[1].toLong()
            val attachSize = locParts[2].toInt()

            // Current position in stream = 16 + headerLen
            val currentPos = 16L + headerLen
            val skipAmount = attachPos - currentPos
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
            // Inline: data follows header directly
            val dataSize = pixelCount * bytesPerSample
            rawBytes = ByteArray(dataSize)
            readFully(stream, rawBytes)
        }

        // XISF is little-endian
        val buffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN)

        val floatData = FloatArray(pixelCount)

        when {
            sampleFormat.contains("Float32") -> {
                for (i in 0 until pixelCount) floatData[i] = buffer.float
            }
            sampleFormat.contains("Float64") -> {
                for (i in 0 until pixelCount) floatData[i] = buffer.double.toFloat()
            }
            sampleFormat.contains("UInt16") -> {
                for (i in 0 until pixelCount) {
                    floatData[i] = (buffer.short.toInt() and 0xFFFF).toFloat()
                }
            }
            sampleFormat.contains("UInt32") -> {
                for (i in 0 until pixelCount) {
                    floatData[i] = (buffer.int.toLong() and 0xFFFFFFFFL).toFloat()
                }
            }
            sampleFormat.contains("UInt8") -> {
                for (i in 0 until pixelCount) {
                    floatData[i] = (buffer.get().toInt() and 0xFF).toFloat()
                }
            }
            else -> {
                for (i in 0 until pixelCount) floatData[i] = buffer.float
            }
        }

        // Normalize - XISF stores planar (channel-sequential)
        val normalized: FloatArray
        var min = Float.MAX_VALUE
        var max = Float.MIN_VALUE
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
                    for (c in 0 until minOf(channels, 3)) {
                        val srcIdx = c * planeSize + y * width + x
                        val dstIdx = (y * width + x) * minOf(channels, 3) + c
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
            sampleFormat.contains("32") -> 32
            sampleFormat.contains("64") -> 64
            sampleFormat.contains("16") -> 16
            sampleFormat.contains("8") -> 8
            else -> 32
        }

        return AstroImage(
            width = width,
            height = height,
            channels = minOf(channels, 3),
            data = normalized,
            bitDepth = bitDepth,
            format = "XISF"
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