package com.astroview.model

import android.graphics.Bitmap

/**
 * Represents the complete UI state of the image viewer screen.
 *
 * This data class is emitted by ViewerViewModel as a StateFlow and consumed
 * by ViewerScreen to drive the Compose UI.
 *
 * @property isLoading          True while a file is being decoded in the background.
 * @property errorMessage       Non-null when an error occurred during loading.
 * @property astroImage         The decoded image model, or null if no file has been loaded.
 * @property displayBitmap      The Bitmap currently rendered in the viewer, with any active
 *                              transforms applied.
 * @property autoStretchEnabled Whether the MTF auto-stretch is currently active.
 * @property rotationDegrees    Current cumulative clockwise rotation (0, 90, 180, or 270).
 * @property flipHorizontal     Whether a horizontal flip is currently applied.
 * @property flipVertical       Whether a vertical flip is currently applied.
 * @property fileName           Display name of the currently loaded file.
 */
data class ViewerState(
    val isLoading:          Boolean = false,
    val errorMessage:       String? = null,
    val astroImage:         AstroImage? = null,
    val displayBitmap:      Bitmap? = null,
    val autoStretchEnabled: Boolean = false,
    val rotationDegrees:    Int     = 0,
    val flipHorizontal:     Boolean = false,
    val flipVertical:       Boolean = false,
    val fileName:           String? = null
)