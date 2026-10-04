package com.glass.player.design.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.glass.player.design.GlassTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

object LiquidMotionDefaults {
    val LiquidSpring: AnimationSpec<Float> = spring(
        dampingRatio = 0.70f,
        stiffness = 340f
    )

    val LeadingEdgeSpring: AnimationSpec<Float> = spring(
        dampingRatio = 0.64f,
        stiffness = 420f
    )

    val TrailingEdgeSpring: AnimationSpec<Float> = spring(
        dampingRatio = 0.76f,
        stiffness = 260f
    )

    val AlbumArtSpring: AnimationSpec<Float> = spring(
        dampingRatio = 0.72f,
        stiffness = 380f
    )

    const val PAUSED_ALBUM_ART_SCALE: Float = 0.88f
    const val PLAYING_ALBUM_ART_SCALE: Float = 1.00f
}

data class LiquidBlobBounds(
    val left: Float,
    val right: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
}

@Composable
fun rememberLiquidBlobBounds(
    selectedTabIndex: Int,
    tabBounds: List<Rect>
): State<LiquidBlobBounds> {
    val currentBoundsState = remember { mutableStateOf(LiquidBlobBounds(0f, 0f)) }
    var previousIndex by remember { mutableStateOf(selectedTabIndex) }

    val leftAnim = remember { Animatable(0f) }
    val rightAnim = remember { Animatable(0f) }

    LaunchedEffect(selectedTabIndex, tabBounds) {
        if (tabBounds.isEmpty() || selectedTabIndex !in tabBounds.indices) return@LaunchedEffect

        val targetRect = tabBounds[selectedTabIndex]
        val targetLeft = targetRect.left
        val targetRight = targetRect.right

        if (leftAnim.value == 0f && rightAnim.value == 0f) {
            leftAnim.snapTo(targetLeft)
            rightAnim.snapTo(targetRight)
            currentBoundsState.value = LiquidBlobBounds(targetLeft, targetRight)
            previousIndex = selectedTabIndex
            return@LaunchedEffect
        }

        val movingForward = selectedTabIndex >= previousIndex
        previousIndex = selectedTabIndex

        if (movingForward) {
            launch {
                rightAnim.animateTo(targetRight, LiquidMotionDefaults.LeadingEdgeSpring)
            }
            launch {
                leftAnim.animateTo(targetLeft, LiquidMotionDefaults.TrailingEdgeSpring)
            }
        } else {
            launch {
                leftAnim.animateTo(targetLeft, LiquidMotionDefaults.LeadingEdgeSpring)
            }
            launch {
                rightAnim.animateTo(targetRight, LiquidMotionDefaults.TrailingEdgeSpring)
            }
        }
    }

    LaunchedEffect(leftAnim.value, rightAnim.value) {
        currentBoundsState.value = LiquidBlobBounds(
            left = leftAnim.value,
            right = rightAnim.value
        )
    }

    return currentBoundsState
}

@Composable
fun LiquidBlobIndicator(
    selectedTabIndex: Int,
    tabBounds: List<Rect>,
    modifier: Modifier = Modifier,
    blobColor: Color = GlassTheme.colors.glassTint,
    borderBrush: Brush = GlassTheme.colors.glassBorder,
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 16.dp
) {
    val density = LocalDensity.current
    val bounds by rememberLiquidBlobBounds(
        selectedTabIndex = selectedTabIndex,
        tabBounds = tabBounds
    )

    if (bounds.width > 0f) {
        val shape = RoundedCornerShape(cornerRadius)
        val widthDp = with(density) { bounds.width.toDp() }
        val heightDp = with(density) {
            val h = if (selectedTabIndex in tabBounds.indices) tabBounds[selectedTabIndex].height else 0f
            if (h > 0f) h.toDp() else 36.dp
        }

        Box(
            modifier = modifier
                .offset { IntOffset(x = bounds.left.roundToInt(), y = 0) }
                .size(width = widthDp, height = heightDp)
                .clip(shape)
                .background(blobColor)
                .border(width = borderWidth, brush = borderBrush, shape = shape)
        )
    }
}

@Composable
fun rememberAlbumArtScale(isPlaying: Boolean): State<Float> {
    val targetScale = if (isPlaying) {
        LiquidMotionDefaults.PLAYING_ALBUM_ART_SCALE
    } else {
        LiquidMotionDefaults.PAUSED_ALBUM_ART_SCALE
    }

    return animateFloatAsState(
        targetValue = targetScale,
        animationSpec = LiquidMotionDefaults.AlbumArtSpring,
        label = "AlbumArtSpringScale"
    )
}

fun Modifier.albumArtScale(isPlaying: Boolean): Modifier = composed {
    val scale by rememberAlbumArtScale(isPlaying = isPlaying)
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
