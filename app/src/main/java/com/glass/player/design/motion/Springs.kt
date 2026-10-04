package com.glass.player.design.motion

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

object GlassSprings {
    const val StiffStiffness = 1100f
    const val StiffDamping = 0.82f

    const val SnappyStiffness = 650f
    const val SnappyDamping = 0.76f

    const val GentleStiffness = 380f
    const val GentleDamping = 0.86f

    const val OvershootStiffness = 520f
    const val OvershootDamping = 0.68f

    fun <T> stiff(): SpringSpec<T> = spring(
        dampingRatio = StiffDamping,
        stiffness = StiffStiffness
    )

    fun <T> snappy(): SpringSpec<T> = spring(
        dampingRatio = SnappyDamping,
        stiffness = SnappyStiffness
    )

    fun <T> gentle(): SpringSpec<T> = spring(
        dampingRatio = GentleDamping,
        stiffness = GentleStiffness
    )

    fun <T> overshoot(): SpringSpec<T> = spring(
        dampingRatio = OvershootDamping,
        stiffness = OvershootStiffness
    )
}
