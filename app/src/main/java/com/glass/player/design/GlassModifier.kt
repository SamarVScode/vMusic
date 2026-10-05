package com.glass.player.design

import android.content.Context
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState as CoreHazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

typealias HazeState = CoreHazeState

@Composable
fun rememberHazeState(): CoreHazeState = remember { CoreHazeState() }

fun Modifier.hazeSource(state: CoreHazeState?): Modifier =
    if (state != null) this.hazeSource(state) else this

enum class GlassQualityTier {
    TierA,
    TierB,
    TierC
}

fun detectGlassQualityTier(context: Context): GlassQualityTier = GlassQualityTier.TierB

@Composable
fun rememberAutoGlassQualityTier(overrideTier: GlassQualityTier? = null): GlassQualityTier =
    overrideTier ?: GlassQualityTier.TierB

/**
 * Authentic iOS 26 Liquid Glass Modifier:
 * 1. Hardware real-time backdrop blur (dev.chrisbanes.haze with RenderEffect fallback)
 * 2. Specular bevel hairline border (diagonal specular reflection)
 * 3. Top-rim curved bevel gleam (simulates ambient light on curved glass edge)
 * 4. Ambient glass drop shadow for physical separation
 * 5. Interactive dynamic touch sheen responding to finger gestures
 */
fun Modifier.glass(
    hazeState: CoreHazeState? = null,
    shape: Shape = RoundedCornerShape(22.dp),
    tint: Color? = null,
    borderBrush: Brush? = null,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 24.dp,
    saturation: Float = 1.6f,
    refractionIndex: Float = 0.045f,
    interactiveSheen: Boolean = true,
    qualityTier: GlassQualityTier? = null
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    val sheenAlpha = remember { Animatable(0.04f) }
    var touchPos by remember { mutableStateOf(Offset.Unspecified) }

    val resolvedTint = tint ?: GlassTheme.colors.glassTint
    val resolvedBorder = borderBrush ?: GlassTheme.colors.glassBorder

    // Interactive finger touch sheen
    val pointerModifier = if (interactiveSheen) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                touchPos = down.position
                coroutineScope.launch {
                    sheenAlpha.animateTo(
                        targetValue = 0.24f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.65f)
                    )
                }

                do {
                    val event = awaitPointerEvent()
                    val move = event.changes.firstOrNull()
                    if (move != null && move.pressed) {
                        touchPos = move.position
                    }
                } while (event.changes.any { it.pressed })

                coroutineScope.launch {
                    sheenAlpha.animateTo(
                        targetValue = 0.04f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.80f)
                    )
                }
            }
        }
    } else {
        Modifier
    }

    // 1. Physical drop shadow elevation
    val shadowModifier = Modifier.shadow(
        elevation = 8.dp,
        shape = shape,
        clip = false,
        ambientColor = Color(0x60000000),
        spotColor = Color(0x90000000)
    )

    // 2. Real-time Haze backdrop blur or RenderEffect fallback
    val blurModifier = if (hazeState != null) {
        Modifier.hazeEffect(state = hazeState) {
            this.blurRadius = blurRadius
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(blurRadius)
    } else {
        Modifier
    }

    // 3. Translucent frosted backing plate
    val backingModifier = Modifier
        .clip(shape)
        .background(color = resolvedTint, shape = shape)

    // 4. Specular border hairline
    val borderModifier = Modifier.border(
        width = borderWidth,
        brush = resolvedBorder,
        shape = shape
    )

    // 5. Optical glass overlays (Top-rim light shine, inner bottom shadow, and dynamic touch sheen)
    val opticalOverlay = Modifier.drawBehind {
        val topRimBrush = Brush.verticalGradient(
            colors = listOf(Color(0x30FFFFFF), Color(0x08FFFFFF), Color.Transparent),
            startY = 0f,
            endY = (size.height * 0.40f).coerceAtMost(50f)
        )
        drawRect(brush = topRimBrush)

        val innerShadowBrush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0x20000000)),
            startY = size.height * 0.70f,
            endY = size.height
        )
        drawRect(brush = innerShadowBrush)

        if (touchPos.isSpecified && sheenAlpha.value > 0.01f) {
            val sheenRadius = (size.minDimension * 0.75f).coerceIn(40f, 280f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = sheenAlpha.value.coerceIn(0f, 0.30f)),
                        Color.Transparent
                    ),
                    center = touchPos,
                    radius = sheenRadius
                ),
                center = touchPos,
                radius = sheenRadius
            )
        }
    }

    this
        .then(pointerModifier)
        .then(shadowModifier)
        .then(blurModifier)
        .then(backingModifier)
        .then(borderModifier)
        .then(opticalOverlay)
}
