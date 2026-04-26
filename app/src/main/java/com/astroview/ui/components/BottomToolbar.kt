package com.astroview.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.astroview.ui.theme.CosmicGreen
import com.astroview.ui.theme.CosmicRed
import com.astroview.ui.theme.NebulaPrimary
import com.astroview.ui.theme.SpaceCard
import com.astroview.ui.theme.StarDim
import com.astroview.ui.theme.StarWhite
import com.astroview.ui.theme.ToolbarBg

/**
 * Bottom toolbar providing file open, stretch toggle, rotation, flip, and reset controls.
 *
 * Image-specific controls (STF, rotation, flip, reset) are only shown when [hasImage] is true.
 *
 * @param autoStretchEnabled Whether the MTF auto-stretch is currently active.
 * @param hasImage           Whether an image is currently loaded.
 * @param onOpenFile         Callback to open the file picker.
 * @param onToggleStretch    Callback to toggle the auto-stretch.
 * @param onRotateLeft       Callback to rotate 90 degrees counter-clockwise.
 * @param onRotateRight      Callback to rotate 90 degrees clockwise.
 * @param onFlipH            Callback to toggle horizontal flip.
 * @param onFlipV            Callback to toggle vertical flip.
 * @param onReset            Callback to reset all transforms.
 */
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
        Divider(
            color     = SpaceCard,
            thickness = 0.5.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier                = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement   = Arrangement.SpaceEvenly,
            verticalAlignment       = Alignment.CenterVertically
        ) {
            // Open file button is always visible.
            ToolButton(
                icon    = Icons.Rounded.FolderOpen,
                label   = "Open",
                onClick = onOpenFile,
                tint    = NebulaPrimary
            )

            if (hasImage) {
                // The STF button animates its color to indicate active state.
                val stretchColor by animateColorAsState(
                    targetValue = if (autoStretchEnabled) CosmicGreen else StarDim,
                    label       = "stretchColor"
                )
                ToolButton(
                    icon    = Icons.Rounded.AutoFixHigh,
                    label   = if (autoStretchEnabled) "STF On" else "STF Off",
                    onClick = onToggleStretch,
                    tint    = stretchColor
                )
                ToolButton(
                    icon    = Icons.Rounded.RotateLeft,
                    label   = "Rot L",
                    onClick = onRotateLeft,
                    tint    = StarWhite
                )
                ToolButton(
                    icon    = Icons.Rounded.RotateRight,
                    label   = "Rot R",
                    onClick = onRotateRight,
                    tint    = StarWhite
                )
                ToolButton(
                    icon    = Icons.Rounded.Flip,
                    label   = "Flip H",
                    onClick = onFlipH,
                    tint    = StarWhite
                )
                ToolButton(
                    icon    = Icons.Rounded.FlipToBack,
                    label   = "Flip V",
                    onClick = onFlipV,
                    tint    = StarWhite
                )
                ToolButton(
                    icon    = Icons.Rounded.RestartAlt,
                    label   = "Reset",
                    onClick = onReset,
                    tint    = CosmicRed
                )
            }
        }
    }
}

/**
 * A single icon button with a text label beneath it, used inside [BottomToolbar].
 *
 * @param icon     The icon to display.
 * @param label    Short text label displayed below the icon.
 * @param onClick  Click callback.
 * @param tint     Icon and label color.
 */
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
        modifier            = modifier.clip(RoundedCornerShape(8.dp))
    ) {
        IconButton(
            onClick  = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = tint,
                modifier           = Modifier.size(22.dp)
            )
        }
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}