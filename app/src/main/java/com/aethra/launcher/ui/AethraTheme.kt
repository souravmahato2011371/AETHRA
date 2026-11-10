package com.aethra.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Kept for the legacy launcher preview that remains in the source tree.
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

enum class ThemeMode { LIGHT, DARK }

enum class VisualStyle(val title: String, val subtitle: String) {
    LIQUID_GLASS("Liquid Glass", "Translucent depth, cool light and fluid surfaces"),
    NEO_CONSTRUCTIVIST_POP("Neo-Constructivist Pop", "Bold geometry, expressive type and vivid color"),
    SIGNATURE_PREMIUM("Signature Premium", "Quiet luxury, warm materials and refined detail")
}

data class AethraColors(
    val background: Color,
    val surface: Color,
    val raised: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accentSecondary: Color,
    val outline: Color,
    val glass: Boolean
)

val LocalAethraColors = staticCompositionLocalOf {
    AethraColors(
        background = Color(0xFF101211),
        surface = Color(0xFF202622),
        raised = Color(0xFF303A32),
        text = Color(0xFFF4F0E6),
        muted = Color(0xFFA5ADA2),
        accent = Color(0xFFC5F36A),
        accentSecondary = Color(0xFFFF5938),
        outline = Color(0xFF455047),
        glass = false
    )
}

@Composable
fun AethraTheme(
    style: VisualStyle = VisualStyle.NEO_CONSTRUCTIVIST_POP,
    mode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val colors = when (style) {
        VisualStyle.LIQUID_GLASS -> if (mode == ThemeMode.DARK) {
            AethraColors(Color(0xFF0A1020), Color(0xCC1A2844), Color(0xE6243858), Color(0xFFF5F8FF), Color(0xFFB5C2D8), Color(0xFFB9C9FF), Color(0xFF70E2E2), Color(0x66FFFFFF), true)
        } else {
            AethraColors(Color(0xFFE9EFF8), Color(0xDDFBFDFF), Color(0xEAF3F7FF), Color(0xFF172235), Color(0xFF62718A), Color(0xFF526FBE), Color(0xFF168F9C), Color(0xAAFFFFFF), true)
        }

        VisualStyle.NEO_CONSTRUCTIVIST_POP -> if (mode == ThemeMode.DARK) {
            AethraColors(Color(0xFF101211), Color(0xFF202622), Color(0xFF303A32), Color(0xFFF4F0E6), Color(0xFFA5ADA2), Color(0xFFC5F36A), Color(0xFFFF5938), Color(0xFF4A554C), false)
        } else {
            AethraColors(Color(0xFFF4F0E6), Color(0xFFFFFFFF), Color(0xFFE9E2D4), Color(0xFF151714), Color(0xFF666C63), Color(0xFFB9E84F), Color(0xFFEE5332), Color(0xFFCCC4B5), false)
        }

        VisualStyle.SIGNATURE_PREMIUM -> if (mode == ThemeMode.DARK) {
            AethraColors(Color(0xFF090B0E), Color(0xFF171B20), Color(0xFF22282F), Color(0xFFF5F1E8), Color(0xFFAAAFAF), Color(0xFFD8BD83), Color(0xFF9CAEC0), Color(0xFF454A50), false)
        } else {
            AethraColors(Color(0xFFF5F2EB), Color(0xFFFFFEFB), Color(0xFFEDE7DC), Color(0xFF191B1D), Color(0xFF77766F), Color(0xFF9E7B3F), Color(0xFF586B7B), Color(0xFFD8D0C4), false)
        }
    }

    // The three art directions use deliberately different typographic voices.
    // Native system families guarantee a legible fallback when offline.
    val displayFamily = when (style) {
        VisualStyle.LIQUID_GLASS -> FontFamily.SansSerif
        VisualStyle.NEO_CONSTRUCTIVIST_POP -> FontFamily.SansSerif
        VisualStyle.SIGNATURE_PREMIUM -> FontFamily.Serif
    }
    val baseTypography = Typography()
    val typography = baseTypography.copy(
        displayLarge = baseTypography.displayLarge.copy(fontFamily = displayFamily, fontWeight = FontWeight.Bold),
        displayMedium = baseTypography.displayMedium.copy(fontFamily = displayFamily, fontWeight = FontWeight.Bold),
        headlineLarge = baseTypography.headlineLarge.copy(fontFamily = displayFamily, fontWeight = FontWeight.Bold),
        headlineMedium = baseTypography.headlineMedium.copy(fontFamily = displayFamily, fontWeight = FontWeight.SemiBold),
        titleLarge = baseTypography.titleLarge.copy(fontFamily = displayFamily, fontWeight = FontWeight.SemiBold),
        labelSmall = baseTypography.labelSmall.copy(
            fontFamily = if (style == VisualStyle.NEO_CONSTRUCTIVIST_POP) FontFamily.Monospace else FontFamily.SansSerif,
            fontWeight = FontWeight.Bold
        )
    )

    val scheme = if (mode == ThemeMode.DARK) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.background,
            secondary = colors.accentSecondary,
            onSecondary = colors.background,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.raised,
            onSurfaceVariant = colors.muted,
            outline = colors.outline
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = if (style == VisualStyle.NEO_CONSTRUCTIVIST_POP) Color(0xFF151714) else Color.White,
            secondary = colors.accentSecondary,
            onSecondary = colors.background,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.raised,
            onSurfaceVariant = colors.muted,
            outline = colors.outline
        )
    }

    CompositionLocalProvider(LocalAethraColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
