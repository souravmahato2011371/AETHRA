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
            VisualStyle.LIQUID_GLASS -> GlassAtmosphere()
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
private fun GlassAtmosphere() {
    val transition = rememberInfiniteTransition(label = "aethra-glass-atmosphere")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000), repeatMode = RepeatMode.Reverse),
        label = "glass-phase"
    )
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val first = Offset(w * (0.08f + phase * 0.24f), h * (0.16f + phase * 0.08f))
        val second = Offset(w * (0.90f - phase * 0.22f), h * (0.64f - phase * 0.10f))
        val third = Offset(w * (0.52f + phase * 0.10f), h * 0.98f)
        drawRect(Color(0xFF071326))
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF9FC5FF).copy(alpha = 0.42f), Color(0xFF5479C7).copy(alpha = 0.16f), Color.Transparent),
                center = first,
                radius = w * 0.70f
            ), w * 0.70f, first
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF65E2E0).copy(alpha = 0.29f), Color(0xFF3F75AA).copy(alpha = 0.10f), Color.Transparent),
                center = second,
                radius = w * 0.58f
            ), w * 0.58f, second
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF9C8BFF).copy(alpha = 0.16f), Color.Transparent),
                center = third,
                radius = w * 0.48f
            ), w * 0.48f, third
        )
        drawRect(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.035f), Color.Transparent, Color.Black.copy(alpha = 0.18f))
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
    val glassMotion = rememberInfiniteTransition(label = "aethra-glass-surface-motion")
    val specularPosition by glassMotion.animateFloat(
        initialValue = -0.65f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(tween(7200), repeatMode = RepeatMode.Restart),
        label = "glass-specular-position"
    )
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
    val fill = if (colors.glass) {
        Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.25f),
            0.22f to colors.surface.copy(alpha = 0.42f),
            0.58f to colors.raised.copy(alpha = 0.27f),
            1f to Color(0xFF9FCBFF).copy(alpha = 0.10f)
        )
    } else Brush.linearGradient(listOf(colors.surface, colors.surface))

    Column(
        modifier.then(hardShadow)
            .then(if (colors.glass) Modifier.shadow(22.dp, shape, clip = false) else Modifier)
            .clip(shape).background(fill)
            .then(if (colors.glass) Modifier.drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.68f), Color.White.copy(alpha = 0.10f),
                            Color(0xFF9CCBFF).copy(alpha = 0.30f), Color.White.copy(alpha = 0.24f))
                    ),
                    style = Stroke(stroke),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(25.dp.toPx())
                )
                drawLine(
                    Color.White.copy(alpha = 0.52f),
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
                        listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.12f, 0f),
                        radius = size.width * 0.62f
                    ),
                    radius = size.width * 0.62f,
                    center = Offset(size.width * 0.12f, 0f)
                )
                val glintX = size.width * specularPosition
                drawRect(
                    Brush.linearGradient(
                        0f to Color.Transparent,
                        0.42f to Color.White.copy(alpha = 0.015f),
                        0.5f to Color.White.copy(alpha = 0.12f),
                        0.58f to Color.White.copy(alpha = 0.015f),
                        1f to Color.Transparent,
                        start = Offset(glintX - size.width * 0.34f, 0f),
                        end = Offset(glintX + size.width * 0.34f, size.height)
                    )
                )
                drawRoundRect(
                    Color.White.copy(alpha = 0.16f),
                    topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                    size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
                    style = Stroke(0.7.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(23.dp.toPx())
                )
            } else Modifier)
            .border(if (colors.sharp) 2.dp else 1.dp,
                if (colors.glass) Color.White.copy(alpha = 0.34f) else colors.outline.copy(alpha = 0.88f), shape),
        content = content
    )
}
