package com.astroview.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astroview.model.ViewerState
import com.astroview.ui.components.BottomToolbar
import com.astroview.ui.components.ImageViewer
import com.astroview.ui.theme.CosmicGreen
import com.astroview.ui.theme.CosmicRed
import com.astroview.ui.theme.NebulaPrimary
import com.astroview.ui.theme.SpaceDark
import com.astroview.ui.theme.SpaceSurface
import com.astroview.ui.theme.StarDim
import com.astroview.ui.theme.StarWhite

/**
 * Root composable for the image viewer screen.
 *
 * Layout (top to bottom):
 *   - Info bar   : shown only when an image is loaded.
 *   - Content area: loading indicator, error message, image viewer, or empty state.
 *   - Bottom toolbar: file open and image transform controls.
 *
 * @param state            Current UI state from ViewerViewModel.
 * @param onOpenFile       Invoked when the user taps Open.
 * @param onToggleStretch  Invoked when the user toggles the STF button.
 * @param onRotateLeft     Invoked when the user taps Rot L.
 * @param onRotateRight    Invoked when the user taps Rot R.
 * @param onFlipH          Invoked when the user taps Flip H.
 * @param onFlipV          Invoked when the user taps Flip V.
 * @param onReset          Invoked when the user taps Reset.
 */
@Composable
fun ViewerScreen(
    state: ViewerState,
    onOpenFile: () -> Unit,
    onToggleStretch: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Info bar is displayed whenever an image is loaded.
            if (state.astroImage != null) {
                InfoBar(state = state)
            }

            // Main content area fills all available vertical space.
            Box(
                modifier          = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment  = Alignment.Center
            ) {
                when {
                    state.isLoading -> LoadingIndicator()

                    state.errorMessage != null -> ErrorMessage(message = state.errorMessage)

                    state.displayBitmap != null -> ImageViewer(bitmap = state.displayBitmap)

                    else -> EmptyState(onOpenFile = onOpenFile)
                }
            }

            // Bottom toolbar is always visible.
            BottomToolbar(
                autoStretchEnabled = state.autoStretchEnabled,
                hasImage           = state.astroImage != null,
                onOpenFile         = onOpenFile,
                onToggleStretch    = onToggleStretch,
                onRotateLeft       = onRotateLeft,
                onRotateRight      = onRotateRight,
                onFlipH            = onFlipH,
                onFlipV            = onFlipV,
                onReset            = onReset
            )
        }
    }
}

/**
 * Horizontal bar at the top of the screen displaying metadata for the loaded image.
 * Shows file name, dimensions, bit depth, color mode, format, and STF status badge.
 */
@Composable
private fun InfoBar(state: ViewerState) {
    val image = state.astroImage ?: return
    val colorMode = if (image.channels == 1) "Mono" else "RGB"
    val info = "${image.width}x${image.height}  ${image.bitDepth}bit  $colorMode  ${image.format}"

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(SpaceSurface)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        // File name truncated to one line.
        Text(
            text     = state.fileName ?: "Unknown",
            style    = MaterialTheme.typography.labelSmall,
            color    = NebulaPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Image metadata.
        Text(
            text  = info,
            style = MaterialTheme.typography.labelSmall,
            color = StarDim
        )

        // STF active badge.
        if (state.autoStretchEnabled) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = CosmicGreen.copy(alpha = 0.2f)
            ) {
                Text(
                    text     = "STF",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = CosmicGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Centered loading spinner shown while a file is being decoded.
 */
@Composable
private fun LoadingIndicator() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            color       = NebulaPrimary,
            strokeWidth = 2.dp,
            modifier    = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text  = "Loading...",
            color = StarDim,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Centered error message shown when decoding fails.
 * Displays the error string from the ViewModel state.
 */
@Composable
private fun ErrorMessage(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.padding(32.dp)
    ) {
        Text(
            text  = message,
            color = CosmicRed,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Placeholder shown when no file has been loaded yet.
 * Displays the application name and an Open File button.
 */
@Composable
private fun EmptyState(onOpenFile: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.padding(48.dp)
    ) {
        Text(
            text  = "AstroView",
            style = MaterialTheme.typography.headlineMedium,
            color = StarWhite
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text      = "FITS and XISF Viewer",
            style     = MaterialTheme.typography.bodyLarge,
            color     = StarDim,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text      = "Maximum 80 MB per file, 15 megapixels per image.",
            style     = MaterialTheme.typography.labelSmall,
            color     = StarDim,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onOpenFile,
            colors  = ButtonDefaults.outlinedButtonColors(contentColor = NebulaPrimary)
        ) {
            Text("Open File")
        }
    }
}