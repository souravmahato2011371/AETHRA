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
    style: VisualStyle,
    mode: ThemeMode,
    content: @Composable () -> Unit
) {
    val colors = when (style) {
        VisualStyle.LIQUID_GLASS -> if (mode == ThemeMode.DARK) {
            AethraColors(
                background = Color(0xFF0A1020),
                surface = Color(0xCC1A2844),
                raised = Color(0xE6243858),
                text = Color(0xFFF5F8FF),
                muted = Color(0xFFB5C2D8),
                accent = Color(0xFFB9C9FF),
                accentSecondary = Color(0xFF70E2E2),
                outline = Color(0x66FFFFFF),
                glass = true
            )
        } else {
            AethraColors(
                background = Color(0xFFE9EFF8),
                surface = Color(0xDDFBFDFF),
                raised = Color(0xEAF3F7FF),
                text = Color(0xFF172235),
                muted = Color(0xFF62718A),
                accent = Color(0xFF526FBE),
                accentSecondary = Color(0xFF168F9C),
                outline = Color(0xAAFFFFFF),
                glass = true
            )
        }

        VisualStyle.NEO_CONSTRUCTIVIST_POP -> if (mode == ThemeMode.DARK) {
            AethraColors(
                background = Color(0xFF101211),
                surface = Color(0xFF202622),
                raised = Color(0xFF303A32),
                text = Color(0xFFF4F0E6),
                muted = Color(0xFFA5ADA2),
                accent = Color(0xFFC5F36A),
                accentSecondary = Color(0xFFFF5938),
                outline = Color(0xFF4A554C),
                glass = false
            )
        } else {
            AethraColors(
                background = Color(0xFFF4F0E6),
                surface = Color(0xFFFFFFFF),
                raised = Color(0xFFE9E2D4),
                text = Color(0xFF151714),
                muted = Color(0xFF666C63),
                accent = Color(0xFFB9E84F),
                accentSecondary = Color(0xFFEE5332),
                outline = Color(0xFFCCC4B5),
                glass = false
            )
        }

        VisualStyle.SIGNATURE_PREMIUM -> if (mode == ThemeMode.DARK) {
            AethraColors(
                background = Color(0xFF090B0E),
                surface = Color(0xFF171B20),
                raised = Color(0xFF22282F),
                text = Color(0xFFF5F1E8),
                muted = Color(0xFFAAAFAF),
                accent = Color(0xFFD8BD83),
                accentSecondary = Color(0xFF9CAEC0),
                outline = Color(0xFF454A50),
                glass = false
            )
        } else {
            AethraColors(
                background = Color(0xFFF5F2EB),
                surface = Color(0xFFFFFEFB),
                raised = Color(0xFFEDE7DC),
                text = Color(0xFF191B1D),
                muted = Color(0xFF77766F),
                accent = Color(0xFF9E7B3F),
                accentSecondary = Color(0xFF586B7B),
                outline = Color(0xFFD8D0C4),
                glass = false
            )
        }
    }

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
