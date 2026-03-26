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

class MainActivity : ComponentActivity() {

    private val viewModel: ViewerViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Persist read permission
            try {
                contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            viewModel.loadFile(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        // Handle intent (file opened from file manager)
        intent?.data?.let { uri ->
            viewModel.loadFile(uri)
        }
    }

    private fun openFilePicker() {
        filePickerLauncher.launch(
            arrayOf(
                "*/*"  // Accept all, we filter by extension in decoder
            )
        )
    }
}