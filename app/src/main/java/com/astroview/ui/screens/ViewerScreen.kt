package com.astroview.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astroview.model.ViewerState
import com.astroview.ui.components.BottomToolbar
import com.astroview.ui.components.ImageViewer
import com.astroview.ui.theme.*

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
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Info bar
            if (state.astroImage != null) {
                InfoBar(state)
            }

            // Main content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = NebulaPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading…",
                                color = StarDim,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    state.errorMessage != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "⚠️",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.errorMessage,
                                color = CosmicRed,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    state.displayBitmap != null -> {
                        ImageViewer(bitmap = state.displayBitmap)
                    }

                    else -> {
                        EmptyState(onOpenFile)
                    }
                }
            }

            // Bottom toolbar
            BottomToolbar(
                autoStretchEnabled = state.autoStretchEnabled,
                hasImage = state.astroImage != null,
                onOpenFile = onOpenFile,
                onToggleStretch = onToggleStretch,
                onRotateLeft = onRotateLeft,
                onRotateRight = onRotateRight,
                onFlipH = onFlipH,
                onFlipV = onFlipV,
                onReset = onReset
            )
        }
    }
}

@Composable
private fun InfoBar(state: ViewerState) {
    val image = state.astroImage ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceSurface)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = state.fileName ?: "Unknown",
            style = MaterialTheme.typography.labelSmall,
            color = NebulaPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        val colorMode = if (image.channels == 1) "Mono" else "RGB"
        val info = "${image.width}×${image.height} · ${image.bitDepth}bit · $colorMode · ${image.format}"

        Text(
            text = info,
            style = MaterialTheme.typography.labelSmall,
            color = StarDim
        )

        if (state.autoStretchEnabled) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = CosmicGreen.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "STF",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmicGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onOpenFile: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(48.dp)
    ) {
        Text(
            text = "✦",
            style = MaterialTheme.typography.displayLarge,
            color = NebulaPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AstroView",
            style = MaterialTheme.typography.headlineMedium,
            color = StarWhite
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "FITS · XISF · TIFF Viewer",
            style = MaterialTheme.typography.bodyLarge,
            color = StarDim,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onOpenFile,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = NebulaPrimary
            )
        ) {
            Text("Open File")
        }
    }
}