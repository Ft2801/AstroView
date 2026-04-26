package com.astroview.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Material 3 dark color scheme using the AstroView space palette.
 */
private val DarkColorScheme = darkColorScheme(
    primary          = NebulaPrimary,
    secondary        = NebulaSecondary,
    background       = SpaceDark,
    surface          = SpaceSurface,
    onPrimary        = SpaceDark,
    onSecondary      = StarWhite,
    onBackground     = StarWhite,
    onSurface        = StarWhite,
    surfaceVariant   = SpaceCard,
    onSurfaceVariant = StarDim
)

/**
 * Top-level theme composable.
 * Applies the dark space color scheme and custom typography to all descendant composables.
 */
@Composable
fun AstroViewTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AstroTypography,
        content     = content
    )
}