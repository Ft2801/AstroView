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

/**
 * ViewModel for the image viewer screen.
 *
 * Manages the lifecycle of image loading, auto-stretch computation, and geometric transforms.
 * Exposes a single [state] StateFlow consumed by the Compose UI.
 *
 * Bitmap caching strategy:
 *   - [originalBitmap]  : Bitmap rendered from the raw normalized pixel data.
 *   - [stretchedBitmap] : Bitmap rendered from the MTF auto-stretched pixel data.
 *   Both are computed once after loading and cached to allow instant toggling between views.
 *
 * Threading:
 *   - File I/O and decoding run on Dispatchers.IO.
 *   - Auto-stretch and bitmap creation run on Dispatchers.Default.
 *   - All StateFlow updates are performed on the main thread via viewModelScope.
 */
class ViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ViewerState())
    val state: StateFlow<ViewerState> = _state.asStateFlow()

    /** Bitmap produced from the raw normalized image data, without stretch. */
    private var originalBitmap: Bitmap? = null

    /** Bitmap produced after applying the MTF auto-stretch. */
    private var stretchedBitmap: Bitmap? = null

    // -------------------------------------------------------------------------
    // File loading
    // -------------------------------------------------------------------------

    /**
     * Loads and decodes the file at [uri].
     *
     * On success, both the original and stretched bitmaps are pre-computed and cached.
     * On failure, the error message is surfaced in the UI state.
     */
    fun loadFile(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading    = true,
                errorMessage = null
            )

            try {
                val context  = getApplication<Application>()
                val fileName = resolveFileName(context, uri)

                // Decode the image on the I/O dispatcher.
                val image: AstroImage = withContext(Dispatchers.IO) {
                    ImageDecoder.decode(context, uri)
                }

                // Compute both bitmaps on the default (CPU) dispatcher.
                val (origBmp, stretchBmp) = withContext(Dispatchers.Default) {
                    val original      = ImageProcessor.toBitmap(image)
                    val stretchData   = AutoStretch.apply(image)
                    val stretched     = ImageProcessor.toBitmap(image, stretchData)
                    Pair(original, stretched)
                }

                originalBitmap  = origBmp
                stretchedBitmap = stretchBmp

                _state.value = ViewerState(
                    astroImage         = image,
                    displayBitmap      = origBmp,
                    autoStretchEnabled = false,
                    fileName           = fileName
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading    = false,
                    errorMessage = e.message ?: "An unknown error occurred while loading the file."
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Stretch toggle
    // -------------------------------------------------------------------------

    /**
     * Toggles the MTF auto-stretch on or off.
     * Switches between [originalBitmap] and [stretchedBitmap] and re-applies current transforms.
     */
    fun toggleAutoStretch() {
        val current = _state.value
        if (current.astroImage == null) return

        val newEnabled = !current.autoStretchEnabled
        val base       = if (newEnabled) stretchedBitmap else originalBitmap
        base ?: return

        val display = TransformEngine.applyTransform(
            base,
            current.rotationDegrees,
            current.flipHorizontal,
            current.flipVertical
        )

        _state.value = current.copy(
            autoStretchEnabled = newEnabled,
            displayBitmap      = display
        )
    }

    // -------------------------------------------------------------------------
    // Geometric transforms
    // -------------------------------------------------------------------------

    /** Rotates the current display 90 degrees counter-clockwise. */
    fun rotateLeft() = applyRotation(-90)

    /** Rotates the current display 90 degrees clockwise. */
    fun rotateRight() = applyRotation(90)

    /** Toggles a horizontal flip on the current display. */
    fun flipHorizontal() {
        val current = _state.value
        if (current.astroImage == null) return
        applyAndUpdateTransform(current.copy(flipHorizontal = !current.flipHorizontal))
    }

    /** Toggles a vertical flip on the current display. */
    fun flipVertical() {
        val current = _state.value
        if (current.astroImage == null) return
        applyAndUpdateTransform(current.copy(flipVertical = !current.flipVertical))
    }

    /**
     * Resets all geometric transforms (rotation and flips) to their default values
     * and restores the base bitmap without any matrix transformation.
     */
    fun resetTransforms() {
        val current = _state.value
        if (current.astroImage == null) return

        val base = if (current.autoStretchEnabled) stretchedBitmap else originalBitmap
        base ?: return

        _state.value = current.copy(
            rotationDegrees = 0,
            flipHorizontal  = false,
            flipVertical    = false,
            displayBitmap   = base
        )
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Adds [degrees] to the current rotation, wraps the result to [0, 360),
     * and triggers a transform update.
     */
    private fun applyRotation(degrees: Int) {
        val current = _state.value
        if (current.astroImage == null) return
        val newRotation = (current.rotationDegrees + degrees + 360) % 360
        applyAndUpdateTransform(current.copy(rotationDegrees = newRotation))
    }

    /**
     * Applies the geometric transforms described in [newState] to the appropriate base bitmap
     * on Dispatchers.Default, then updates the StateFlow with the resulting display bitmap.
     */
    private fun applyAndUpdateTransform(newState: ViewerState) {
        viewModelScope.launch {
            val base = if (newState.autoStretchEnabled) stretchedBitmap else originalBitmap
            base ?: return@launch

            val transformed = withContext(Dispatchers.Default) {
                TransformEngine.applyTransform(
                    base,
                    newState.rotationDegrees,
                    newState.flipHorizontal,
                    newState.flipVertical
                )
            }
            _state.value = newState.copy(displayBitmap = transformed)
        }
    }

    /**
     * Resolves the display file name from the content URI.
     * Falls back to the last path segment if the display name is unavailable.
     */
    private fun resolveFileName(context: android.content.Context, uri: Uri): String {
        var name = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(index) ?: ""
            }
        }
        if (name.isEmpty()) name = uri.lastPathSegment ?: "file"
        return name
    }

    /**
     * Recycles cached Bitmaps when the ViewModel is cleared to avoid memory leaks.
     */
    override fun onCleared() {
        super.onCleared()
        originalBitmap?.recycle()
        stretchedBitmap?.recycle()
        originalBitmap  = null
        stretchedBitmap = null
    }
}