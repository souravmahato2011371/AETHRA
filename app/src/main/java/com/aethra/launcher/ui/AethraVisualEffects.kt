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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.skydoves.cloudy.LiquidGlassGlow
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.liquidGlass
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky

@Composable
fun AethraVisualBackdrop(
    style: VisualStyle,
    colors: AethraColors,
    content: @Composable () -> Unit
) {
    val sky = if (style == VisualStyle.LIQUID_GLASS) rememberSky() else null
    CompositionLocalProvider(LocalAethraSky provides sky) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .then(if (sky != null) Modifier.sky(sky) else Modifier)
        ) {
            when (style) {
                VisualStyle.LIQUID_GLASS -> GlassAtmosphere()
                VisualStyle.NEO_CONSTRUCTIVIST_POP -> NcpGridBackdrop(colors)
                VisualStyle.SIGNATURE_PREMIUM -> Unit
            }
            content()
        }
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
private fun GlassAtmosphere() {
    val transition = rememberInfiniteTransition(label = "aethra-glass-atmosphere")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10500), repeatMode = RepeatMode.Reverse),
        label = "glass-phase"
    )
    Canvas(Modifier.fillMaxSize().blur(66.dp)) {
        val w = size.width
        val h = size.height
        val first = Offset(w * (0.14f + phase * 0.28f), h * 0.23f)
        val second = Offset(w * (0.88f - phase * 0.2f), h * 0.57f)
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF8CB7FF).copy(alpha = 0.74f), Color(0xFF526FC0).copy(alpha = 0.18f), Color.Transparent),
                center = first,
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = first
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF65DDE0).copy(alpha = 0.62f), Color(0xFF3F75AA).copy(alpha = 0.18f), Color.Transparent),
                center = second,
                radius = w * 0.48f
            ),
            radius = w * 0.48f,
            center = second
        )
        drawCircle(Color(0xFFB5C9FF).copy(alpha = 0.16f), w * 0.16f, Offset(w * 0.68f, h * 0.88f))
    }
}

@Composable
fun AethraLiquidLens() {
    var lensSize by remember { mutableStateOf(Size.Zero) }
    val transition = rememberInfiniteTransition(label = "aethra-liquid-lens")
    val pulse by transition.animateFloat(
        initialValue = 0.965f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(3200), repeatMode = RepeatMode.Reverse),
        label = "lens-pulse"
    )
    Box(
        Modifier
            .size(112.dp)
            .onSizeChanged { lensSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .liquidGlass(
                lensCenter = Offset(lensSize.width / 2f, lensSize.height / 2f),
                lensSize = lensSize,
                cornerRadius = lensSize.minDimension / 2f,
                refraction = 0.34f,
                curve = 0.36f,
                dispersion = 0.06f,
                saturation = 1.18f,
                contrast = 1.08f,
                tint = Color.White.copy(alpha = 0.12f),
                edge = 0.42f,
                glow = LiquidGlassGlow(intensity = 0.88f, sharpness = 8.5f)
            )
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = 0.43f),
                        Color(0xFF79DDEB).copy(alpha = 0.20f),
                        Color(0xFF4564A9).copy(alpha = 0.12f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.78f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.62f), size.minDimension * 0.13f, Offset(size.width * 0.32f, size.height * 0.27f))
            drawCircle(Color.White.copy(alpha = 0.20f), size.minDimension * 0.34f, Offset(size.width * 0.72f, size.height * 0.72f), style = Stroke(1.5.dp.toPx()))
        }
    }
}


@Composable
fun AethraSurfaceCard(
    modifier: Modifier,
    colors: AethraColors,
    content: @Composable ColumnScope.() -> Unit
) {
    val sky = LocalAethraSky.current
    val shape = RoundedCornerShape(
        when {
            colors.sharp -> 2.dp
            colors.glass -> 24.dp
            else -> 18.dp
        }
    )
    val glassModifier = if (colors.glass && sky != null) {
        Modifier.cloudy(
            sky = sky,
            radius = 22,
            tint = colors.surface.copy(alpha = 0.25f),
            shape = shape,
            cpuBlurEnabled = false
        )
    } else Modifier
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
                colors.surface.copy(alpha = 0.58f),
                colors.raised.copy(alpha = 0.34f),
                colors.surface.copy(alpha = 0.70f)
            )
        )
    } else Brush.linearGradient(listOf(colors.surface, colors.surface))

    Column(
        modifier
            .then(hardShadow)
            .then(glassModifier)
            .clip(shape)
            .background(fill)
            .border(if (colors.sharp) 2.dp else 1.dp, colors.outline.copy(alpha = 0.88f), shape),
        content = content
    )
}
