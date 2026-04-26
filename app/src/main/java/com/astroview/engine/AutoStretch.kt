package com.astroview.engine

import com.astroview.model.AstroImage

/**
 * Applies a Midtone Transfer Function (MTF) auto-stretch to a normalized AstroImage.
 *
 * The algorithm is inspired by PixInsight's Screen Transfer Function (STF) and operates
 * independently on each channel:
 *
 * 1. Compute the per-channel median.
 * 2. Compute the Median Absolute Deviation (MAD) and normalize it by 1.4826 to obtain
 *    a robust estimate of the standard deviation.
 * 3. Set the shadow clipping point at: median - 2.8 * normalizedMAD.
 * 4. Compute the midtone balance parameter that maps the normalized median to TARGET_MEDIAN.
 * 5. Apply the MTF transfer function to every pixel value in the channel.
 *
 * MTF formula:
 *   MTF(x, m) = (m - 1) * x / ((2m - 1) * x - m)
 *
 * Input data must be normalized to [0.0, 1.0].
 * Output data is also in [0.0, 1.0].
 */
object AutoStretch {

    /** Target median output value for the MTF midtone mapping. */
    private const val TARGET_MEDIAN = 0.25f

    /**
     * MAD multiplier for shadow clipping.
     * Shadow clip = median + SHADOW_CLIPPING * normalizedMAD
     * The negative value ensures clipping below the median.
     */
    private const val SHADOW_CLIPPING = -2.8f

    /**
     * Applies the MTF auto-stretch to all channels of [image].
     *
     * @param image The source AstroImage with normalized float data.
     * @return      A new FloatArray of the same size with the stretch applied.
     */
    fun apply(image: AstroImage): FloatArray {
        val width     = image.width
        val height    = image.height
        val channels  = image.channels
        val src       = image.data
        val result    = FloatArray(src.size)
        val planeSize = width * height

        for (c in 0 until channels) {

            // Extract all pixel values for this channel.
            val channelValues = FloatArray(planeSize) { i -> src[i * channels + c] }

            // Compute the median by sorting a copy of the channel values.
            val sorted = channelValues.clone()
            sorted.sort()
            val median = sorted[planeSize / 2]

            // Compute MAD and normalize it to approximate the standard deviation.
            val deviations = FloatArray(planeSize) { i ->
                kotlin.math.abs(channelValues[i] - median)
            }
            deviations.sort()
            val normalizedMad = deviations[planeSize / 2] * 1.4826f

            // Determine shadow clipping point (clamped to 0).
            val shadowClip = if (normalizedMad > 0f) {
                maxOf(0f, median + SHADOW_CLIPPING * normalizedMad)
            } else {
                0f
            }

            val highlight = 1f

            // Compute the normalized source median within the [shadowClip, highlight] range.
            val normalizedMedian = if (highlight > shadowClip) {
                ((median - shadowClip) / (highlight - shadowClip)).coerceIn(0f, 1f)
            } else {
                0.5f
            }

            // Compute the midtone balance parameter for the target median.
            val midtone = computeMidtoneBalance(normalizedMedian, TARGET_MEDIAN)

            // Apply the MTF transfer function to each pixel in this channel.
            for (i in 0 until planeSize) {
                val normalized = if (highlight > shadowClip) {
                    ((channelValues[i] - shadowClip) / (highlight - shadowClip))
                        .coerceIn(0f, 1f)
                } else {
                    0f
                }
                result[i * channels + c] = mtf(normalized, midtone)
            }
        }

        return result
    }

    /**
     * Midtone Transfer Function.
     *
     * Maps an input value [x] through a tone curve controlled by the midtone parameter [m].
     * When m = 0.5, the function is linear (identity). Values of m below 0.5 darken the
     * output; values above 0.5 brighten it.
     *
     * @param x Input value in [0.0, 1.0].
     * @param m Midtone balance parameter in (0.0, 1.0).
     * @return  Output value in [0.0, 1.0].
     */
    private fun mtf(x: Float, m: Float): Float {
        if (x <= 0f) return 0f
        if (x >= 1f) return 1f
        if (m <= 0f) return 1f
        if (m >= 1f) return 0f
        if (x == m)  return 0.5f
        return ((m - 1f) * x) / (((2f * m - 1f) * x) - m)
    }

    /**
     * Computes the midtone balance parameter [m] such that MTF([sourceMedian], m) = [targetMedian].
     *
     * This is the algebraic inverse of the MTF equation solved for m.
     *
     * @param sourceMedian The normalized median of the source channel, in (0.0, 1.0).
     * @param targetMedian The desired output median after applying the MTF.
     * @return             The midtone balance parameter m.
     */
    private fun computeMidtoneBalance(sourceMedian: Float, targetMedian: Float): Float {
        if (sourceMedian <= 0f || sourceMedian >= 1f) return 0.5f
        return ((targetMedian - 1f) * sourceMedian) /
                (((2f * targetMedian - 1f) * sourceMedian) - targetMedian)
    }
}