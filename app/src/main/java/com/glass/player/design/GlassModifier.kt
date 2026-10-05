package com.glass.player.design

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glass.player.design.shaders.rememberGlassRuntimeShader
import kotlinx.coroutines.launch

/**
 * Lightweight, crash-proof native Compose glass blur state
 */
class HazeState

@Composable
fun rememberHazeState(): HazeState = remember { HazeState() }

fun Modifier.hazeSource(state: HazeState): Modifier = this

enum class GlassQualityTier {
    TierA,
    TierB,
    TierC
}

val LocalGlassQualityTier = compositionLocalOf<GlassQualityTier?> { null }

fun detectGlassQualityTier(context: Context): GlassQualityTier {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    if (powerManager?.isPowerSaveMode == true) {
        return GlassQualityTier.TierC
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlassQualityTier.TierB
    } else {
        GlassQualityTier.TierC
    }
}

@Composable
fun rememberAutoGlassQualityTier(overrideTier: GlassQualityTier? = null): GlassQualityTier {
    if (overrideTier != null) return overrideTier
    val context = LocalContext.current
    return remember(context) {
        detectGlassQualityTier(context)
    }
}

fun Modifier.glass(
    hazeState: HazeState? = null,
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
    val activeTier = qualityTier 
        ?: LocalGlassQualityTier.current 
        ?: rememberAutoGlassQualityTier()

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val sheenAlpha = remember { Animatable(if (activeTier == GlassQualityTier.TierC) 0.08f else 0.05f) }
    var touchPos by remember { mutableStateOf(Offset.Unspecified) }

    val shaderInstance = rememberGlassRuntimeShader()

    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activeTier != GlassQualityTier.TierC) {
        Modifier.graphicsLayer {
            val radiusPx = with(density) { blurRadius.toPx() }
            if (radiusPx > 0f) {
                try {
                    val androidEffect = android.graphics.RenderEffect.createBlurEffect(
                        radiusPx,
                        radiusPx,
                        android.graphics.Shader.TileMode.CLAMP
                    )
                    renderEffect = androidEffect.asComposeRenderEffect()
                } catch (t: Throwable) {
                    // Graceful fallback to flat glass
                }
            }
            this.shape = shape
            this.clip = true
        }
    } else {
        Modifier
    }

    val pointerModifier = if (interactiveSheen) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                touchPos = down.position
                coroutineScope.launch {
                    sheenAlpha.animateTo(
                        targetValue = 0.28f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.70f)
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
                        targetValue = 0.05f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.85f)
                    )
                }
            }
        }
    } else {
        Modifier
    }

    val shaderModifier = Modifier.graphicsLayer {
        if (activeTier == GlassQualityTier.TierA && shaderInstance != null) {
            if (size.width > 1f && size.height > 1f) {
                try {
                    val touchRadiusPx = with(density) { 72.dp.toPx() }
                    val cornerRadiusPx = with(density) { 22.dp.toPx() }
                    shaderInstance.updateUniforms(
                        resolution = size,
                        touchPos = touchPos,
                        touchRadius = touchRadiusPx,
                        refractionIndex = refractionIndex,
                        chromaticDispersion = 0.018f,
                        cornerRadius = cornerRadiusPx,
                        sheenAlpha = sheenAlpha.value,
                        saturation = saturation
                    )
                    renderEffect = try {
                        shaderInstance.toComposeRenderEffect()
                    } catch (t: Throwable) {
                        null
                    }
                } catch (t: Throwable) {
                    renderEffect = null
                }
            }
        }
        this.shape = shape
        this.clip = true
    }

    val resolvedTint = tint ?: GlassTheme.colors.glassTint
    val tintModifier = Modifier.background(color = resolvedTint, shape = shape)

    val resolvedBorder = borderBrush ?: GlassTheme.colors.glassBorder
    val borderModifier = Modifier.border(width = borderWidth, brush = resolvedBorder, shape = shape)

    val overlayModifier = Modifier
        .clip(shape)
        .drawWithContent {
            drawContent()

            val innerShadowBrush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0x18000000)),
                startY = size.height * 0.72f,
                endY = size.height
            )
            drawRect(brush = innerShadowBrush)

            if (activeTier != GlassQualityTier.TierA && touchPos.isSpecified && sheenAlpha.value > 0.01f) {
                val sheenRadius = (size.minDimension * 0.7f).coerceIn(40f, 260f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (sheenAlpha.value * 0.75f).coerceIn(0f, 0.32f)),
                            Color.Transparent
                        ),
                        center = touchPos,
                        radius = sheenRadius
                    ),
                    center = touchPos,
                    radius = sheenRadius
                )
            }

            val fauxAmbientBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.07f),
                    Color.Transparent,
                    Color.Transparent
                ),
                start = Offset.Zero,
                end = Offset(size.width * 0.6f, size.height * 0.6f)
            )
            drawRect(brush = fauxAmbientBrush)
        }

    this.then(pointerModifier)
        .then(blurModifier)
        .then(shaderModifier)
        .then(tintModifier)
        .then(borderModifier)
        .then(overlayModifier)
}
