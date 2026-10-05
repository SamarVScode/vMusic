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

    const val OvershootStiffness = 450f
    const val OvershootDamping = 0.60f

    // Authentic iOS bouncy spring with visible overshoot
    const val BouncyStiffness = 380f
    const val BouncyDamping = 0.52f

    // Elastic rubber-band spring for tabs and sheets
    const val ElasticStiffness = 320f
    const val ElasticDamping = 0.58f

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

    fun <T> bouncy(): SpringSpec<T> = spring(
        dampingRatio = BouncyDamping,
        stiffness = BouncyStiffness
    )

    fun <T> elastic(): SpringSpec<T> = spring(
        dampingRatio = ElasticDamping,
        stiffness = ElasticStiffness
    )
}
