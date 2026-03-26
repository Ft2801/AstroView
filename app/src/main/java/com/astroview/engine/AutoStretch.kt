package com.astroview.engine

import com.astroview.model.AstroImage

/**
 * Implements Midtone Transfer Function (MTF) auto-stretch
 * targeting a median of 0.25, similar to PixInsight's STF.
 */
object AutoStretch {

    private const val TARGET_MEDIAN = 0.25f
    private const val SHADOW_CLIPPING = -2.8f  // MAD multiplier for shadow clipping

    fun apply(image: AstroImage): FloatArray {
        val w = image.width
        val h = image.height
        val ch = image.channels
        val src = image.data
        val result = FloatArray(src.size)
        val planeSize = w * h

        // Process each channel independently
        for (c in 0 until ch) {
            // Extract channel values
            val channelValues = FloatArray(planeSize)
            for (i in 0 until planeSize) {
                channelValues[i] = src[i * ch + c]
            }

            // Calculate median
            val sorted = channelValues.clone()
            sorted.sort()
            val median = sorted[planeSize / 2]

            // Calculate MAD (Median Absolute Deviation)
            val deviations = FloatArray(planeSize) { i ->
                kotlin.math.abs(channelValues[i] - median)
            }
            deviations.sort()
            val mad = deviations[planeSize / 2] * 1.4826f // normalize MAD

            // Shadow clipping point
            val shadowClip = if (mad > 0f) {
                maxOf(0f, median + SHADOW_CLIPPING * mad)
            } else {
                0f
            }

            // Highlight is always 1.0 for normalized data
            val highlight = 1f

            // Calculate midtone balance for target median
            val normalizedMedian = if (highlight > shadowClip) {
                ((median - shadowClip) / (highlight - shadowClip)).coerceIn(0f, 1f)
            } else {
                0.5f
            }

            val midtone = mtfBalance(normalizedMedian, TARGET_MEDIAN)

            // Apply MTF to each pixel
            for (i in 0 until planeSize) {
                val normalized = if (highlight > shadowClip) {
                    ((channelValues[i] - shadowClip) / (highlight - shadowClip))
                        .coerceIn(0f, 1f)
                } else {
                    0f
                }
                result[i * ch + c] = mtf(normalized, midtone)
            }
        }

        return result
    }

    /**
     * Midtone Transfer Function
     * Maps input value x through an S-curve controlled by midtone parameter m.
     */
    private fun mtf(x: Float, m: Float): Float {
        if (x <= 0f) return 0f
        if (x >= 1f) return 1f
        if (m <= 0f) return 1f
        if (m >= 1f) return 0f
        if (x == m) return 0.5f

        return ((m - 1f) * x) / (((2f * m - 1f) * x) - m)
    }

    /**
     * Compute midtone balance parameter that maps sourceMedian to targetMedian.
     */
    private fun mtfBalance(sourceMedian: Float, targetMedian: Float): Float {
        if (sourceMedian <= 0f) return 0.5f
        if (sourceMedian >= 1f) return 0.5f

        // Solve: mtf(sourceMedian, m) = targetMedian
        // m = (targetMedian * (2*sourceMedian - 1)) / (sourceMedian * (2*targetMedian - 1))
        // Wait - we use the inverse formula:
        // The midtone parameter that makes mtf(sourceMedian) = targetMedian is:
        return ((targetMedian - 1f) * sourceMedian) /
                (((2f * targetMedian - 1f) * sourceMedian) - targetMedian)
    }
}