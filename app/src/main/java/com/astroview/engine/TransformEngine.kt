package com.astroview.engine

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * Applies geometric transforms (rotation and flip) to an Android Bitmap.
 *
 * Transforms are composed into a single Matrix and applied in one pass:
 *   1. Scale (flip) around the bitmap center.
 *   2. Rotate around the bitmap center.
 *
 * If no transform is requested, the source Bitmap is returned unchanged
 * without allocating a new object.
 */
object TransformEngine {

    /**
     * Applies the requested transforms to [source] and returns the resulting Bitmap.
     *
     * @param source          The source Bitmap. Not recycled by this method.
     * @param rotationDegrees Clockwise rotation in degrees. Must be a multiple of 90.
     * @param flipH           Whether to flip horizontally (mirror left-right).
     * @param flipV           Whether to flip vertically (mirror top-bottom).
     * @return                A new Bitmap with the transforms applied, or [source] if no
     *                        transform is needed.
     */
    fun applyTransform(
        source: Bitmap,
        rotationDegrees: Int,
        flipH: Boolean,
        flipV: Boolean
    ): Bitmap {
        if (rotationDegrees == 0 && !flipH && !flipV) return source

        val matrix = Matrix()
        val cx = source.width  / 2f
        val cy = source.height / 2f

        // Apply flip transforms as a scale around the center of the image.
        val scaleX = if (flipH) -1f else 1f
        val scaleY = if (flipV) -1f else 1f
        matrix.postScale(scaleX, scaleY, cx, cy)

        // Apply rotation around the center of the image.
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees.toFloat(), cx, cy)
        }

        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}