package com.astroview.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astroview.decoder.ImageDecoder
import com.astroview.engine.AutoStretch
import com.astroview.engine.ImageProcessor
import com.astroview.engine.TransformEngine
import com.astroview.model.AstroImage
import com.astroview.model.ViewerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ViewerState())
    val state: StateFlow<ViewerState> = _state.asStateFlow()

    // Cached bitmaps to avoid recomputation
    private var originalBitmap: Bitmap? = null
    private var stretchedBitmap: Bitmap? = null

    fun loadFile(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val context = getApplication<Application>()
                val fileName = getFileName(context, uri)

                val image = withContext(Dispatchers.IO) {
                    ImageDecoder.decode(context, uri)
                }

                // Pre-compute both bitmaps on background thread
                val (origBmp, stretchBmp) = withContext(Dispatchers.Default) {
                    val orig = ImageProcessor.toBitmap(image)
                    val stretchData = AutoStretch.apply(image)
                    val stretch = ImageProcessor.toBitmap(image, stretchData)
                    Pair(orig, stretch)
                }

                originalBitmap = origBmp
                stretchedBitmap = stretchBmp

                _state.value = ViewerState(
                    astroImage = image,
                    displayBitmap = origBmp,
                    autoStretchEnabled = false,
                    fileName = fileName
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Unknown error loading file"
                )
            }
        }
    }

    fun toggleAutoStretch() {
        val current = _state.value
        if (current.astroImage == null) return

        val newEnabled = !current.autoStretchEnabled
        val baseBitmap = if (newEnabled) stretchedBitmap else originalBitmap
        baseBitmap ?: return

        val display = TransformEngine.applyTransform(
            baseBitmap,
            current.rotationDegrees,
            current.flipHorizontal,
            current.flipVertical
        )

        _state.value = current.copy(
            autoStretchEnabled = newEnabled,
            displayBitmap = display
        )
    }

    fun rotateLeft() = applyRotation(-90)
    fun rotateRight() = applyRotation(90)

    fun flipHorizontal() {
        val current = _state.value
        if (current.astroImage == null) return
        updateTransform(
            current.copy(flipHorizontal = !current.flipHorizontal)
        )
    }

    fun flipVertical() {
        val current = _state.value
        if (current.astroImage == null) return
        updateTransform(
            current.copy(flipVertical = !current.flipVertical)
        )
    }

    fun resetTransforms() {
        val current = _state.value
        if (current.astroImage == null) return

        val baseBitmap = if (current.autoStretchEnabled) stretchedBitmap else originalBitmap
        baseBitmap ?: return

        _state.value = current.copy(
            rotationDegrees = 0,
            flipHorizontal = false,
            flipVertical = false,
            displayBitmap = baseBitmap
        )
    }

    private fun applyRotation(degrees: Int) {
        val current = _state.value
        if (current.astroImage == null) return
        val newRotation = (current.rotationDegrees + degrees + 360) % 360
        updateTransform(current.copy(rotationDegrees = newRotation))
    }

    private fun updateTransform(newState: ViewerState) {
        viewModelScope.launch {
            val baseBitmap = if (newState.autoStretchEnabled) stretchedBitmap else originalBitmap
            baseBitmap ?: return@launch

            val transformed = withContext(Dispatchers.Default) {
                TransformEngine.applyTransform(
                    baseBitmap,
                    newState.rotationDegrees,
                    newState.flipHorizontal,
                    newState.flipVertical
                )
            }

            _state.value = newState.copy(displayBitmap = transformed)
        }
    }

    private fun getFileName(context: android.content.Context, uri: Uri): String {
        var name = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(idx) ?: ""
            }
        }
        if (name.isEmpty()) name = uri.lastPathSegment ?: "file"
        return name
    }

    override fun onCleared() {
        super.onCleared()
        originalBitmap?.recycle()
        stretchedBitmap?.recycle()
    }
}