package com.glass.player.design.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.pressableScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1.0f,
        animationSpec = if (isPressed) {
            GlassSprings.stiff()
        } else {
            GlassSprings.overshoot()
        },
        label = "glass_pressable_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null, // Strictly NO Material ripple
                    enabled = enabled,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
}

fun Modifier.pressableScale(
    onClick: () -> Unit
): Modifier = pressableScale(enabled = true, pressedScale = 0.96f, onClick = onClick)
