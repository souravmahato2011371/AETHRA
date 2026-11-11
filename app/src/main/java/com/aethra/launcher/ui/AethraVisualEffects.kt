package com.aethra.launcher.ui

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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun AethraVisualBackdrop(
    style: VisualStyle,
    colors: AethraColors,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        when (style) {
            VisualStyle.LIQUID_GLASS -> GlassAtmosphere()
            VisualStyle.NEO_CONSTRUCTIVIST_POP -> NcpGridBackdrop(colors)
            VisualStyle.SIGNATURE_PREMIUM -> Unit
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

/**
 * Glass atmosphere is intentionally shader-free: broad radial gradients create
 * the depth of a lit glass environment without a full-screen blur render pass.
 */
@Composable
private fun GlassAtmosphere() {
    val transition = rememberInfiniteTransition(label = "aethra-glass-atmosphere")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000), repeatMode = RepeatMode.Reverse),
        label = "glass-phase"
    )
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val first = Offset(w * (0.12f + phase * 0.18f), h * 0.20f)
        val second = Offset(w * (0.86f - phase * 0.14f), h * 0.62f)
        drawRect(Color(0xFF0A142A))
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF8CB7FF).copy(alpha = 0.34f), Color(0xFF526FC0).copy(alpha = 0.12f), Color.Transparent),
                center = first,
                radius = w * 0.62f
            ),
            radius = w * 0.62f,
            center = first
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF65DDE0).copy(alpha = 0.25f), Color(0xFF3F75AA).copy(alpha = 0.09f), Color.Transparent),
                center = second,
                radius = w * 0.54f
            ),
            radius = w * 0.54f,
            center = second
        )
    }
}

@Composable
fun AethraLiquidLens() {
    val transition = rememberInfiniteTransition(label = "aethra-liquid-lens")
    val pulse by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(tween(4200), repeatMode = RepeatMode.Reverse),
        label = "lens-pulse"
    )
    Box(
        Modifier
            .size(112.dp)
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = 0.58f),
                        Color(0xFFB8E9FF).copy(alpha = 0.30f),
                        Color(0xFF79DDEB).copy(alpha = 0.18f),
                        Color(0xFF4564A9).copy(alpha = 0.24f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.76f), CircleShape)
            .drawBehind {
                drawCircle(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.48f), Color.Transparent),
                        start = Offset(size.width * 0.12f, size.height * 0.08f),
                        end = Offset(size.width * 0.88f, size.height * 0.72f)
                    ),
                    radius = size.minDimension * 0.48f,
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawCircle(
                    Color.White.copy(alpha = 0.66f),
                    radius = size.minDimension * 0.105f,
                    center = Offset(size.width * 0.31f, size.height * 0.26f)
                )
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
            colors.glass -> 24.dp
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
            listOf(
                Color.White.copy(alpha = 0.22f),
                colors.surface.copy(alpha = 0.36f),
                colors.raised.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    } else Brush.linearGradient(listOf(colors.surface, colors.surface))

    Column(
        modifier
            .then(hardShadow)
            .clip(shape)
            .background(fill)
            .then(
                if (colors.glass) Modifier.drawBehind {
                    drawLine(
                        Color.White.copy(alpha = 0.48f),
                        Offset(18.dp.toPx(), 1.dp.toPx()),
                        Offset(size.width - 18.dp.toPx(), 1.dp.toPx()),
                        1.dp.toPx()
                    )
                } else Modifier
            )
            .border(if (colors.sharp) 2.dp else 1.dp, colors.outline.copy(alpha = 0.88f), shape),
        content = content
    )
}
