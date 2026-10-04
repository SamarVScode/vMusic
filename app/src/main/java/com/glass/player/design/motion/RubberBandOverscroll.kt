package com.glass.player.design.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

fun calculateRubberBandOffset(
    unconsumedDelta: Float,
    dimension: Float,
    coefficient: Float = 0.55f
): Float {
    if (dimension <= 0f || unconsumedDelta == 0f) return 0f
    val absDelta = abs(unconsumedDelta)
    val s = sign(unconsumedDelta)
    val damped = (1.0f - (1.0f / ((absDelta * coefficient / dimension) + 1.0f))) * dimension
    return damped * s
}

class RubberBandOverscrollState(
    private val scope: CoroutineScope,
    val springSpec: AnimationSpec<Float> = spring(
        dampingRatio = 0.78f,
        stiffness = 380f
    )
) {
    val animatableOffset = Animatable(0f)

    var rawAccumulatedOverscroll by mutableFloatStateOf(0f)
        private set

    var isSpringingBack by mutableStateOf(false)
        private set

    val offset: Float get() = animatableOffset.value

    fun onScroll(delta: Float, dimension: Float): Float {
        if (dimension <= 0f) return 0f

        if ((rawAccumulatedOverscroll > 0f && delta < 0f) || (rawAccumulatedOverscroll < 0f && delta > 0f)) {
            val previousRaw = rawAccumulatedOverscroll
            rawAccumulatedOverscroll += delta

            if ((previousRaw > 0f && rawAccumulatedOverscroll < 0f) || (previousRaw < 0f && rawAccumulatedOverscroll > 0f)) {
                val consumed = -previousRaw
                rawAccumulatedOverscroll = 0f
                scope.launch { animatableOffset.snapTo(0f) }
                return consumed
            }

            val newDamped = calculateRubberBandOffset(rawAccumulatedOverscroll, dimension)
            scope.launch { animatableOffset.snapTo(newDamped) }
            return delta
        }

        rawAccumulatedOverscroll += delta
        val newDamped = calculateRubberBandOffset(rawAccumulatedOverscroll, dimension)
        scope.launch { animatableOffset.snapTo(newDamped) }
        return delta
    }

    fun onRelease() {
        if (rawAccumulatedOverscroll != 0f || animatableOffset.value != 0f) {
            isSpringingBack = true
            scope.launch {
                try {
                    animatableOffset.animateTo(0f, springSpec)
                } finally {
                    rawAccumulatedOverscroll = 0f
                    isSpringingBack = false
                }
            }
        }
    }
}

@Composable
fun rememberRubberBandOverscrollState(
    springSpec: AnimationSpec<Float> = spring(
        dampingRatio = 0.78f,
        stiffness = 380f
    )
): RubberBandOverscrollState {
    val scope = rememberCoroutineScope()
    return remember(scope, springSpec) {
        RubberBandOverscrollState(scope = scope, springSpec = springSpec)
    }
}

fun Modifier.rubberBandOverscroll(
    state: RubberBandOverscrollState? = null,
    orientation: Orientation = Orientation.Vertical,
    enabled: Boolean = true
): Modifier = composed {
    if (!enabled) return@composed this

    val rubberBandState = state ?: rememberRubberBandOverscrollState()
    var containerDimension by remember { mutableFloatStateOf(0f) }

    val connection = remember(rubberBandState, orientation, containerDimension) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = if (orientation == Orientation.Vertical) available.y else available.x

                if ((rubberBandState.rawAccumulatedOverscroll > 0f && delta < 0f) ||
                    (rubberBandState.rawAccumulatedOverscroll < 0f && delta > 0f)
                ) {
                    val consumedDelta = rubberBandState.onScroll(delta, containerDimension)
                    return if (orientation == Orientation.Vertical) Offset(0f, consumedDelta) else Offset(consumedDelta, 0f)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = if (orientation == Orientation.Vertical) available.y else available.x
                if (delta != 0f && source == NestedScrollSource.UserInput) {
                    val consumedDelta = rubberBandState.onScroll(delta, containerDimension)
                    return if (orientation == Orientation.Vertical) Offset(0f, consumedDelta) else Offset(consumedDelta, 0f)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (rubberBandState.rawAccumulatedOverscroll != 0f) {
                    rubberBandState.onRelease()
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                rubberBandState.onRelease()
                return Velocity.Zero
            }
        }
    }

    this.onSizeChanged { size ->
        containerDimension = (if (orientation == Orientation.Vertical) size.height else size.width).toFloat()
    }
    .nestedScroll(connection)
    .graphicsLayer {
        if (orientation == Orientation.Vertical) {
            translationY = rubberBandState.offset
        } else {
            translationX = rubberBandState.offset
        }
    }
}
