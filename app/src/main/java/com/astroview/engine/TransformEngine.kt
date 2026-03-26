package com.astroview.engine

import android.graphics.Bitmap
import android.graphics.Matrix

object TransformEngine {

    fun applyTransform(
        source: Bitmap,
        rotationDegrees: Int,
        flipH: Boolean,
        flipV: Boolean
    ): Bitmap {
        if (rotationDegrees == 0 && !flipH && !flipV) return source

        val matrix = Matrix()

        // Apply flips
        val sx = if (flipH) -1f else 1f
        val sy = if (flipV) -1f else 1f
        matrix.postScale(sx, sy, source.width / 2f, source.height / 2f)

        // Apply rotation
        if (rotationDegrees != 0) {
            matrix.postRotate(
                rotationDegrees.toFloat(),
                source.width / 2f,
                source.height / 2f
            )
        }

        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}