package com.astroview.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.astroview.ui.theme.*

@Composable
fun BottomToolbar(
    autoStretchEnabled: Boolean,
    hasImage: Boolean,
    onOpenFile: () -> Unit,
    onToggleStretch: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ToolbarBg)
            .padding(bottom = 8.dp)
    ) {
        // Divider
        Divider(
            color = SpaceCard,
            thickness = 0.5.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Open file button
            ToolButton(
                icon = Icons.Rounded.FolderOpen,
                label = "Open",
                onClick = onOpenFile,
                tint = NebulaPrimary
            )

            if (hasImage) {
                // Auto Stretch toggle
                val stretchColor by animateColorAsState(
                    targetValue = if (autoStretchEnabled) CosmicGreen else StarDim,
                    label = "stretchColor"
                )
                ToolButton(
                    icon = Icons.Rounded.AutoFixHigh,
                    label = if (autoStretchEnabled) "STF On" else "STF Off",
                    onClick = onToggleStretch,
                    tint = stretchColor
                )

                ToolButton(
                    icon = Icons.Rounded.RotateLeft,
                    label = "Rot L",
                    onClick = onRotateLeft,
                    tint = StarWhite
                )

                ToolButton(
                    icon = Icons.Rounded.RotateRight,
                    label = "Rot R",
                    onClick = onRotateRight,
                    tint = StarWhite
                )

                ToolButton(
                    icon = Icons.Rounded.Flip,
                    label = "Flip H",
                    onClick = onFlipH,
                    tint = StarWhite
                )

                ToolButton(
                    icon = Icons.Rounded.FlipToBack,
                    label = "Flip V",
                    onClick = onFlipV,
                    tint = StarWhite
                )

                ToolButton(
                    icon = Icons.Rounded.RestartAlt,
                    label = "Reset",
                    onClick = onReset,
                    tint = CosmicRed
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}