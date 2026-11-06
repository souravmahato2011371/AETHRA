package com.aethra.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AethraPalette {
    val Ink = Color(0xFF171918)
    val Panel = Color(0xFF232824)
    val PanelRaised = Color(0xFF2D342E)
    val Paper = Color(0xFFF1EFE6)
    val Lime = Color(0xFFB8F05A)
    val Orange = Color(0xFFFF5A36)
    val Muted = Color(0xFF9CA49A)
    val Line = Color(0xFF3B443C)
}

@Composable
fun AethraTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = AethraPalette.Lime,
        onPrimary = AethraPalette.Ink,
        secondary = AethraPalette.Orange,
        background = AethraPalette.Ink,
        onBackground = AethraPalette.Paper,
        surface = AethraPalette.Panel,
        onSurface = AethraPalette.Paper,
        surfaceVariant = AethraPalette.PanelRaised,
        onSurfaceVariant = AethraPalette.Muted
    )

    MaterialTheme(colorScheme = scheme, content = content)
}
