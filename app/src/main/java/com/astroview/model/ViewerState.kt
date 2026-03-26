package com.astroview.model

import android.graphics.Bitmap

data class ViewerState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val astroImage: AstroImage? = null,
    val displayBitmap: Bitmap? = null,
    val autoStretchEnabled: Boolean = false,
    val rotationDegrees: Int = 0,        // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val fileName: String? = null
)