package com.glass.player.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.glass.player.design.GlassTheme

/**
 * 100% Material-free custom Canvas-based icons styled for Liquid Glass.
 */

@Composable
fun ToneFallbackIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = GlassTheme.colors.labelSecondary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.10f

        // Draw an elegant musical eighth note
        val stemX = w * 0.65f
        val noteRadius = w * 0.18f
        val noteCenter = Offset(w * 0.40f, h * 0.72f)

        // Note head
        drawCircle(
            color = tint,
            radius = noteRadius,
            center = noteCenter
        )

        // Stem
        drawLine(
            color = tint,
            start = Offset(stemX, noteCenter.y),
            end = Offset(stemX, h * 0.22f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Flag / beam
        val flagPath = Path().apply {
            moveTo(stemX, h * 0.22f)
            cubicTo(
                stemX + w * 0.22f, h * 0.25f,
                stemX + w * 0.26f, h * 0.45f,
                stemX + w * 0.05f, h * 0.52f
            )
        }
        drawPath(
            path = flagPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun PlayIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.30f, h * 0.20f)
            lineTo(w * 0.82f, h * 0.50f)
            lineTo(w * 0.30f, h * 0.80f)
            close()
        }
        drawPath(path = path, color = tint)
    }
}

@Composable
fun PauseIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barWidth = w * 0.22f
        val barHeight = h * 0.60f
        val corner = CornerRadius(barWidth / 2f, barWidth / 2f)

        // Left bar
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.24f, (h - barHeight) / 2f),
            size = Size(barWidth, barHeight),
            cornerRadius = corner
        )

        // Right bar
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.54f, (h - barHeight) / 2f),
            size = Size(barWidth, barHeight),
            cornerRadius = corner
        )
    }
}

@Composable
fun SkipPreviousIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = GlassTheme.colors.labelPrimary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Left bar
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.20f, h * 0.22f),
            size = Size(w * 0.12f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )

        // Left triangle
        val triangle = Path().apply {
            moveTo(w * 0.80f, h * 0.22f)
            lineTo(w * 0.38f, h * 0.50f)
            lineTo(w * 0.80f, h * 0.78f)
            close()
        }
        drawPath(path = triangle, color = tint)
    }
}

@Composable
fun SkipNextIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = GlassTheme.colors.labelPrimary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Right triangle
        val triangle = Path().apply {
            moveTo(w * 0.20f, h * 0.22f)
            lineTo(w * 0.62f, h * 0.50f)
            lineTo(w * 0.20f, h * 0.78f)
            close()
        }
        drawPath(path = triangle, color = tint)

        // Right bar
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.68f, h * 0.22f),
            size = Size(w * 0.12f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
    }
}

@Composable
fun ShuffleIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = GlassTheme.colors.labelPrimary,
    isActive: Boolean = false
) {
    val activeTint = if (isActive) GlassTheme.colors.accentText else tint

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f

        // Top line crossing to bottom
        val path1 = Path().apply {
            moveTo(w * 0.18f, h * 0.32f)
            lineTo(w * 0.40f, h * 0.32f)
            cubicTo(w * 0.52f, h * 0.32f, w * 0.60f, h * 0.68f, w * 0.72f, h * 0.68f)
            lineTo(w * 0.82f, h * 0.68f)
        }
        drawPath(path1, color = activeTint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        // Arrow head bottom
        val arrow1 = Path().apply {
            moveTo(w * 0.74f, h * 0.58f)
            lineTo(w * 0.85f, h * 0.68f)
            lineTo(w * 0.74f, h * 0.78f)
        }
        drawPath(arrow1, color = activeTint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Bottom line crossing to top
        val path2 = Path().apply {
            moveTo(w * 0.18f, h * 0.68f)
            lineTo(w * 0.35f, h * 0.68f)
            cubicTo(w * 0.44f, h * 0.68f, w * 0.48f, h * 0.56f, w * 0.52f, h * 0.50f)
        }
        drawPath(path2, color = activeTint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        val path3 = Path().apply {
            moveTo(w * 0.60f, h * 0.42f)
            cubicTo(w * 0.64f, h * 0.36f, w * 0.68f, h * 0.32f, w * 0.72f, h * 0.32f)
            lineTo(w * 0.82f, h * 0.32f)
        }
        drawPath(path3, color = activeTint, style = Stroke(width = stroke, cap = StrokeCap.Round))

        // Arrow head top
        val arrow2 = Path().apply {
            moveTo(w * 0.74f, h * 0.22f)
            lineTo(w * 0.85f, h * 0.32f)
            lineTo(w * 0.74f, h * 0.42f)
        }
        drawPath(arrow2, color = activeTint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun RepeatIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = GlassTheme.colors.labelPrimary,
    isActive: Boolean = false
) {
    val activeTint = if (isActive) GlassTheme.colors.accentText else tint

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f

        // Loop top right to bottom left
        val path = Path().apply {
            moveTo(w * 0.28f, h * 0.35f)
            lineTo(w * 0.72f, h * 0.35f)
            cubicTo(w * 0.82f, h * 0.35f, w * 0.82f, h * 0.65f, w * 0.72f, h * 0.65f)
            lineTo(w * 0.28f, h * 0.65f)
            cubicTo(w * 0.18f, h * 0.65f, w * 0.18f, h * 0.35f, w * 0.28f, h * 0.35f)
        }
        drawPath(path, color = activeTint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Top arrow
        val arrowTop = Path().apply {
            moveTo(w * 0.64f, h * 0.25f)
            lineTo(w * 0.75f, h * 0.35f)
            lineTo(w * 0.64f, h * 0.45f)
        }
        drawPath(arrowTop, color = activeTint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SearchIcon(
    modifier: Modifier = Modifier.size(20.dp),
    tint: Color = GlassTheme.colors.labelSecondary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.10f
        val r = w * 0.30f
        val center = Offset(w * 0.42f, h * 0.42f)

        drawCircle(
            color = tint,
            radius = r,
            center = center,
            style = Stroke(width = stroke)
        )

        val handleStart = Offset(center.x + r * 0.707f, center.y + r * 0.707f)
        val handleEnd = Offset(w * 0.88f, h * 0.88f)
        drawLine(
            color = tint,
            start = handleStart,
            end = handleEnd,
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun CloseIcon(
    modifier: Modifier = Modifier.size(20.dp),
    tint: Color = GlassTheme.colors.labelSecondary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.10f

        drawLine(
            color = tint,
            start = Offset(w * 0.26f, h * 0.26f),
            end = Offset(w * 0.74f, h * 0.74f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.74f, h * 0.26f),
            end = Offset(w * 0.26f, h * 0.74f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun ChevronRightIcon(
    modifier: Modifier = Modifier.size(16.dp),
    tint: Color = GlassTheme.colors.labelTertiary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.12f

        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.20f)
            lineTo(w * 0.65f, h * 0.50f)
            lineTo(w * 0.35f, h * 0.80f)
        }
        drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun LibraryTabIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = GlassTheme.colors.labelPrimary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.08f

        // Staggered vinyl / audio cards
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.20f, h * 0.20f),
            size = Size(w * 0.60f, h * 0.60f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
            style = Stroke(width = stroke)
        )

        // Center vinyl circle dot
        drawCircle(
            color = tint,
            radius = w * 0.10f,
            center = Offset(w * 0.50f, h * 0.50f)
        )
    }
}

@Composable
fun SettingsTabIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = GlassTheme.colors.labelPrimary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.08f

        // Modern equalizer sliders representation
        // Track 1
        drawLine(
            color = tint,
            start = Offset(w * 0.30f, h * 0.20f),
            end = Offset(w * 0.30f, h * 0.80f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(color = tint, radius = w * 0.10f, center = Offset(w * 0.30f, h * 0.40f))

        // Track 2
        drawLine(
            color = tint,
            start = Offset(w * 0.70f, h * 0.20f),
            end = Offset(w * 0.70f, h * 0.80f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(color = tint, radius = w * 0.10f, center = Offset(w * 0.70f, h * 0.62f))
    }
}

@Composable
fun EqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier.size(18.dp),
    tint: Color = GlassTheme.colors.accent
) {
    val transition = rememberInfiniteTransition(label = "equalizer_bars")

    val bar1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_bar_1"
    )

    val bar2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_bar_2"
    )

    val bar3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_bar_3"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = w * 0.20f
        val corner = CornerRadius(barW / 2f, barW / 2f)

        val h1 = if (isPlaying) h * bar1 else h * 0.30f
        val h2 = if (isPlaying) h * bar2 else h * 0.60f
        val h3 = if (isPlaying) h * bar3 else h * 0.40f

        // Bar 1
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.10f, h - h1),
            size = Size(barW, h1),
            cornerRadius = corner
        )

        // Bar 2
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.40f, h - h2),
            size = Size(barW, h2),
            cornerRadius = corner
        )

        // Bar 3
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.70f, h - h3),
            size = Size(barW, h3),
            cornerRadius = corner
        )
    }
}
