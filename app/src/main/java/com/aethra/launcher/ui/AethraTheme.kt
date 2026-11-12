package com.aethra.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font as LocalFont
import androidx.compose.ui.text.googlefonts.Font as GoogleFontResource
import androidx.compose.ui.text.googlefonts.GoogleFont

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
    LIQUID_GLASS("Liquid Glass", "Optical depth, backdrop blur, refraction and moving light"),
    NEO_CONSTRUCTIVIST_POP("Neo-Constructivist Pop", "Swiss structure, warm paper, vivid geometry and tactile edges"),
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
    val glass: Boolean,
    val sharp: Boolean = false
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


private val liquidFontFamily = FontFamily(
    GoogleFontResource(googleFont = GoogleFont("Inter"), weight = FontWeight.Normal),
    GoogleFontResource(googleFont = GoogleFont("Inter"), weight = FontWeight.Medium),
    GoogleFontResource(googleFont = GoogleFont("Inter"), weight = FontWeight.Bold),
    LocalFont(DeviceFontFamilyName("sans-serif"))
)

private val neoDisplayFontFamily = FontFamily(
    GoogleFontResource(googleFont = GoogleFont("Space Grotesk"), weight = FontWeight.Normal),
    GoogleFontResource(googleFont = GoogleFont("Space Grotesk"), weight = FontWeight.Medium),
    GoogleFontResource(googleFont = GoogleFont("Space Grotesk"), weight = FontWeight.Bold),
    LocalFont(DeviceFontFamilyName("sans-serif"))
)

private val neoBodyFontFamily = FontFamily(
    GoogleFontResource(googleFont = GoogleFont("Manrope"), weight = FontWeight.Normal),
    GoogleFontResource(googleFont = GoogleFont("Manrope"), weight = FontWeight.Medium),
    GoogleFontResource(googleFont = GoogleFont("Manrope"), weight = FontWeight.Bold),
    LocalFont(DeviceFontFamilyName("sans-serif"))
)

private val neoLabelFontFamily = FontFamily(
    GoogleFontResource(googleFont = GoogleFont("DM Mono"), weight = FontWeight.Normal),
    GoogleFontResource(googleFont = GoogleFont("DM Mono"), weight = FontWeight.Medium),
    LocalFont(DeviceFontFamilyName("monospace"))
)

private val premiumBodyFontFamily = FontFamily(
    GoogleFontResource(googleFont = GoogleFont("DM Sans"), weight = FontWeight.Normal),
    GoogleFontResource(googleFont = GoogleFont("DM Sans"), weight = FontWeight.Medium),
    GoogleFontResource(googleFont = GoogleFont("DM Sans"), weight = FontWeight.Bold),
    LocalFont(DeviceFontFamilyName("sans-serif"))
)

private val premiumDisplayFontFamily = FontFamily(
    GoogleFontResource(googleFont = GoogleFont("Cormorant Garamond"), weight = FontWeight.Medium),
    GoogleFontResource(googleFont = GoogleFont("Cormorant Garamond"), weight = FontWeight.SemiBold),
    LocalFont(DeviceFontFamilyName("serif"))
)

@Composable
fun AethraTheme(
    style: VisualStyle = VisualStyle.NEO_CONSTRUCTIVIST_POP,
    mode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val colors = when (style) {
        VisualStyle.LIQUID_GLASS -> if (mode == ThemeMode.DARK) {
            AethraColors(
                Color(0xFF071321), Color(0xA9274266), Color(0xB9496F9D),
                Color(0xFFF5F9FF), Color(0xFFC3D2E8), Color(0xFFE4F2FF),
                Color(0xFF8DECE7), Color(0x88FFFFFF), true
            )
        } else {
            AethraColors(
                Color(0xFFEAF2FC), Color(0xD9FFFFFF), Color(0xC7E3F8),
                Color(0xFF182A42), Color(0xFF5A6E86), Color(0xFF315D9C),
                Color(0xFF078C9B), Color(0x997E9BBE), true
            )
        }

        VisualStyle.NEO_CONSTRUCTIVIST_POP -> if (mode == ThemeMode.DARK) {
            AethraColors(
                Color(0xFF151518), Color(0xFF242427), Color(0xFFFFBD2E),
                Color(0xFFF4EFDF), Color(0xFFC3BBAE), Color(0xFFFF523B),
                Color(0xFF5372FF), Color(0xFFF4EFDF), false, sharp = true
            )
        } else {
            AethraColors(
                Color(0xFFF4EFDF), Color(0xFFFFFAF0), Color(0xFFFFBD2E),
                Color(0xFF171717), Color(0xFF4D4A43), Color(0xFFF0442E),
                Color(0xFF234BE7), Color(0xFF171717), false, sharp = true
            )
        }

        VisualStyle.SIGNATURE_PREMIUM -> if (mode == ThemeMode.DARK) {
            AethraColors(Color(0xFF090B0E), Color(0xFF171B20), Color(0xFF22282F), Color(0xFFF5F1E8), Color(0xFFAAAFAF), Color(0xFFD8BD83), Color(0xFF9CAEC0), Color(0xFF454A50), false)
        } else {
            AethraColors(Color(0xFFF5F2EB), Color(0xFFFFFEFB), Color(0xFFEDE7DC), Color(0xFF191B1D), Color(0xFF77766F), Color(0xFF9E7B3F), Color(0xFF586B7B), Color(0xFFD8D0C4), false)
        }
    }

    val displayFamily = when (style) {
        VisualStyle.LIQUID_GLASS -> liquidFontFamily
        VisualStyle.NEO_CONSTRUCTIVIST_POP -> neoDisplayFontFamily
        VisualStyle.SIGNATURE_PREMIUM -> premiumDisplayFontFamily
    }
    val bodyFamily = when (style) {
        VisualStyle.LIQUID_GLASS -> liquidFontFamily
        VisualStyle.NEO_CONSTRUCTIVIST_POP -> neoBodyFontFamily
        VisualStyle.SIGNATURE_PREMIUM -> premiumBodyFontFamily
    }
    val labelFamily = when (style) {
        VisualStyle.NEO_CONSTRUCTIVIST_POP -> neoLabelFontFamily
        VisualStyle.LIQUID_GLASS -> liquidFontFamily
        VisualStyle.SIGNATURE_PREMIUM -> premiumBodyFontFamily
    }

    val baseTypography = Typography()
    val typography = baseTypography.copy(
        displayLarge = baseTypography.displayLarge.copy(fontFamily = displayFamily, fontWeight = FontWeight.Bold),
        displayMedium = baseTypography.displayMedium.copy(fontFamily = displayFamily, fontWeight = FontWeight.Bold),
        headlineLarge = baseTypography.headlineLarge.copy(fontFamily = displayFamily, fontWeight = FontWeight.Bold),
        headlineMedium = baseTypography.headlineMedium.copy(fontFamily = displayFamily, fontWeight = FontWeight.SemiBold),
        titleLarge = baseTypography.titleLarge.copy(fontFamily = displayFamily, fontWeight = FontWeight.SemiBold),
        bodyLarge = baseTypography.bodyLarge.copy(fontFamily = bodyFamily),
        bodyMedium = baseTypography.bodyMedium.copy(fontFamily = bodyFamily),
        bodySmall = baseTypography.bodySmall.copy(fontFamily = bodyFamily),
        labelSmall = baseTypography.labelSmall.copy(fontFamily = labelFamily, fontWeight = FontWeight.Bold)
    )

    val scheme = if (mode == ThemeMode.DARK) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = if (style == VisualStyle.NEO_CONSTRUCTIVIST_POP) Color(0xFF171717) else colors.background,
            secondary = colors.accentSecondary,
            onSecondary = colors.background,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = if (colors.sharp) Color(0xFF302E2B) else colors.raised,
            onSurfaceVariant = colors.muted,
            outline = colors.outline
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = if (style == VisualStyle.NEO_CONSTRUCTIVIST_POP) Color(0xFF171717) else Color.White,
            secondary = colors.accentSecondary,
            onSecondary = colors.background,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = if (colors.sharp) Color(0xFFE8E0CE) else colors.raised,
            onSurfaceVariant = colors.muted,
            outline = colors.outline
        )
    }

    CompositionLocalProvider(LocalAethraColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
