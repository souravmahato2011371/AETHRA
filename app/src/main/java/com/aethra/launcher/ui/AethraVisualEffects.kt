package com.aethra.launcher.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun AethraVisualBackdrop(
    style: VisualStyle,
    colors: AethraColors,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize().background(colors.background)) {
        when (style) {
            VisualStyle.LIQUID_GLASS -> GlassAtmosphere(colors)
            VisualStyle.NEO_CONSTRUCTIVIST_POP -> NcpGridBackdrop(colors)
            VisualStyle.SIGNATURE_PREMIUM -> PremiumAtmosphere()
        }
        content()
    }
}

@Composable
private fun NcpGridBackdrop(colors: AethraColors) {
    Canvas(Modifier.fillMaxSize()) {
        val step = 42.dp.toPx()
        val line = colors.text.copy(alpha = 0.055f)
        var x = 0f
        while (x <= size.width) {
            drawLine(line, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            y += step
        }
    }
}

@Composable
private fun PremiumAtmosphere() {
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFFD8BD83).copy(alpha = 0.11f), Color.Transparent),
                center = Offset(size.width * 0.82f, size.height * 0.12f),
                radius = size.width * 0.8f
            ),
            radius = size.width * 0.8f,
            center = Offset(size.width * 0.82f, size.height * 0.12f)
        )
    }
}

/**
 * A low-cost glass environment: animated light fields and layered highlights.
 * It deliberately avoids backdrop-capture and full-screen blur passes so the
 * interface remains predictable on entry-level GPUs.
 */
@Composable
private fun GlassAtmosphere(colors: AethraColors) {
    val isLight = colors.background.luminance() > 0.5f
    // One shared, slow-moving light field keeps the whole screen alive without
    // starting a separate infinite animation for every card in a long list.
    val transition = rememberInfiniteTransition(label = "aethra-glass-atmosphere")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000), repeatMode = RepeatMode.Reverse),
        label = "glass-phase"
    )
    val sweep by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(26000), repeatMode = RepeatMode.Restart),
        label = "glass-light-sweep"
    )
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val first = Offset(w * (0.06f + phase * 0.28f), h * (0.12f + phase * 0.10f))
        val second = Offset(w * (0.93f - phase * 0.26f), h * (0.62f - phase * 0.12f))
        val third = Offset(w * (0.50f + phase * 0.14f), h * 1.02f)
        drawRect(colors.background)
        drawRect(
            Brush.linearGradient(
                colors = if (isLight) listOf(Color(0xFFEAF3FF), Color(0xFFDCEAF8), Color(0xFFF1F6FC))
                    else listOf(Color(0xFF142B4B), Color(0xFF0B1930), Color(0xFF0A1425)),
                start = Offset.Zero,
                end = Offset(w, h)
            )
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFFB8D7FF).copy(alpha = if (isLight) 0.36f else 0.40f), Color(0xFF668DCF).copy(alpha = if (isLight) 0.12f else 0.17f), Color.Transparent),
                center = first,
                radius = w * 0.76f
            ), w * 0.76f, first
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF78E6E2).copy(alpha = if (isLight) 0.23f else 0.25f), Color(0xFF3F75AA).copy(alpha = if (isLight) 0.06f else 0.09f), Color.Transparent),
                center = second,
                radius = w * 0.66f
            ), w * 0.66f, second
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF9C8BFF).copy(alpha = if (isLight) 0.12f else 0.17f), Color(0xFF647EBE).copy(alpha = if (isLight) 0.04f else 0.06f), Color.Transparent),
                center = third,
                radius = w * 0.52f
            ), w * 0.52f, third
        )
        val sweepX = w * sweep
        drawRect(
            Brush.linearGradient(
                0f to Color.Transparent,
                0.46f to Color.White.copy(alpha = if (isLight) 0.045f else 0.012f),
                0.50f to Color.White.copy(alpha = if (isLight) 0.10f else 0.045f),
                0.54f to Color.White.copy(alpha = if (isLight) 0.045f else 0.012f),
                1f to Color.Transparent,
                start = Offset(sweepX - w * 0.38f, 0f),
                end = Offset(sweepX + w * 0.38f, h)
            )
        )
        drawRect(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = if (isLight) 0.16f else 0.045f), Color.Transparent, Color.Black.copy(alpha = if (isLight) 0.045f else 0.22f))
            )
        )
    }
}

@Composable
fun AethraLiquidLens() {
    val transition = rememberInfiniteTransition(label = "aethra-liquid-lens")
    val pulse by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(tween(5200), repeatMode = RepeatMode.Reverse),
        label = "lens-pulse"
    )
    val sweep by transition.animateFloat(
        initialValue = -0.45f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(6400), repeatMode = RepeatMode.Restart),
        label = "lens-sweep"
    )
    Box(
        Modifier.size(112.dp)
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.62f), Color(0xFFB8E9FF).copy(alpha = 0.30f),
                        Color(0xFF79DDEB).copy(alpha = 0.18f), Color(0xFF4564A9).copy(alpha = 0.28f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.78f), CircleShape)
            .drawBehind {
                drawCircle(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.54f), Color.Transparent),
                        start = Offset(size.width * 0.08f, size.height * 0.05f),
                        end = Offset(size.width * 0.92f, size.height * 0.82f)
                    ),
                    radius = size.minDimension * 0.48f,
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                val x = size.width * sweep
                drawOval(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.30f), Color.Transparent),
                        start = Offset(x - size.width * 0.28f, 0f),
                        end = Offset(x + size.width * 0.28f, size.height)
                    ),
                    topLeft = Offset(0f, 0f),
                    size = size
                )
                drawCircle(Color.White.copy(alpha = 0.70f), size.minDimension * 0.09f,
                    Offset(size.width * 0.30f, size.height * 0.24f))
            },
        contentAlignment = Alignment.Center
    ) {}
}

@Composable
fun AethraSurfaceCard(
    modifier: Modifier,
    colors: AethraColors,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(
        when {
            colors.sharp -> 2.dp
            colors.glass -> 25.dp
            else -> 18.dp
        }
    )
    val hardShadow = if (colors.sharp) Modifier.drawBehind {
        val offset = 5.dp.toPx()
        drawRect(
            color = colors.text.copy(alpha = 0.94f),
            topLeft = Offset(offset, offset),
            size = Size((size.width - offset).coerceAtLeast(0f), (size.height - offset).coerceAtLeast(0f))
        )
    } else Modifier
    val isLightGlass = colors.glass && colors.background.luminance() > 0.5f
    val fill = if (colors.glass) {
        Brush.linearGradient(
            0f to Color.White.copy(alpha = if (isLightGlass) 0.62f else 0.25f),
            0.22f to colors.surface.copy(alpha = if (isLightGlass) 0.54f else 0.42f),
            0.58f to colors.raised.copy(alpha = if (isLightGlass) 0.32f else 0.27f),
            1f to Color(0xFF9FCBFF).copy(alpha = if (isLightGlass) 0.19f else 0.10f)
        )
    } else Brush.linearGradient(listOf(colors.surface, colors.surface))

    Column(
        modifier.then(hardShadow)
            .then(if (colors.glass) Modifier.shadow(16.dp, shape, clip = false) else Modifier)
            .clip(shape).background(fill)
            .then(if (colors.glass) Modifier.drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    Brush.linearGradient(
                        listOf(
                            (if (isLightGlass) Color(0xFF6588B0) else Color.White).copy(alpha = if (isLightGlass) 0.34f else 0.68f),
                            Color.White.copy(alpha = 0.10f),
                            Color(0xFF9CCBFF).copy(alpha = if (isLightGlass) 0.22f else 0.30f),
                            Color.White.copy(alpha = 0.24f)
                        )
                    ),
                    style = Stroke(stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(25.dp.toPx())
                )
                drawLine(
                    (if (isLightGlass) Color(0xFF587CA4) else Color.White).copy(alpha = if (isLightGlass) 0.34f else 0.52f),
                    Offset(20.dp.toPx(), stroke),
                    Offset(size.width - 20.dp.toPx(), stroke),
                    stroke
                )
                drawLine(
                    Color.White.copy(alpha = 0.12f),
                    Offset(18.dp.toPx(), size.height - stroke),
                    Offset(size.width - 18.dp.toPx(), size.height - stroke),
                    stroke
                )
                drawCircle(
                    Brush.radialGradient(
                        listOf((if (isLightGlass) Color(0xFF6F97C1) else Color.White).copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.12f, 0f),
                        radius = size.width * 0.62f
                    ),
                    radius = size.width * 0.62f,
                    center = Offset(size.width * 0.12f, 0f)
                )
                // Static specular ribbon; motion is reserved for the hero and player
                // so a long library list remains smooth on mid-range devices.
                drawLine(
                    Color.White.copy(alpha = 0.18f),
                    Offset(size.width * 0.30f, 1.5.dp.toPx()),
                    Offset(size.width * 0.72f, 1.5.dp.toPx()),
                    1.dp.toPx()
                )
                drawRoundRect(
                    (if (isLightGlass) Color(0xFF587CA4) else Color.White).copy(alpha = 0.16f),
                    topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                    size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
                    style = Stroke(0.7.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(23.dp.toPx())
                )
            } else Modifier)
            .border(if (colors.sharp) 2.dp else 1.dp,
                if (colors.glass) (if (isLightGlass) Color(0xFF6D8FB5).copy(alpha = 0.48f) else Color.White.copy(alpha = 0.34f)) else colors.outline.copy(alpha = 0.88f), shape),
        content = content
    )
}
