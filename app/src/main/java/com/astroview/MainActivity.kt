package com.astroview

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.astroview.ui.screens.ViewerScreen
import com.astroview.ui.theme.AstroViewTheme
import com.astroview.viewmodel.ViewerViewModel

/**
 * Single-activity entry point for AstroView.
 *
 * Responsibilities:
 * - Configure edge-to-edge display.
 * - Register the file picker launcher.
 * - Handle incoming VIEW intents from external file managers.
 * - Delegate all state management to ViewerViewModel.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: ViewerViewModel by viewModels()

    // Registers a launcher that opens the system document picker.
    // The selected URI is forwarded to the ViewModel for decoding.
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Attempt to persist read permission across restarts.
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            viewModel.loadFile(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow the app to draw behind system bars.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AstroViewTheme {
                val state by viewModel.state.collectAsState()
                ViewerScreen(
                    state = state,
                    onOpenFile = { openFilePicker() },
                    onToggleStretch = { viewModel.toggleAutoStretch() },
                    onRotateLeft = { viewModel.rotateLeft() },
                    onRotateRight = { viewModel.rotateRight() },
                    onFlipH = { viewModel.flipHorizontal() },
                    onFlipV = { viewModel.flipVertical() },
                    onReset = { viewModel.resetTransforms() }
                )
            }
        }

        // Handle a file opened directly from an external application.
        intent?.data?.let { uri ->
            viewModel.loadFile(uri)
        }
    }

    /**
     * Launches the system document picker.
     * All MIME types are accepted; format validation is performed in ImageDecoder.
     */
    private fun openFilePicker() {
        filePickerLauncher.launch(arrayOf("*/*"))
    }
}