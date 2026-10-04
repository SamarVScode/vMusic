package com.glass.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.glass.player.design.GlassText
import com.glass.player.design.GlassTheme
import com.glass.player.design.motion.pressableScale

enum class GlassButtonVariant { Primary, Secondary }

private val Accent = Color(0xFFC0111F)

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GlassButtonVariant = GlassButtonVariant.Primary,
) {
    val shape = RoundedCornerShape(20.dp)
    val bg = if (variant == GlassButtonVariant.Primary) Accent else Color.Transparent
    val fg = if (variant == GlassButtonVariant.Primary) Color.White else Accent

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .then(if (variant == GlassButtonVariant.Secondary) Modifier.border(1.dp, Accent, shape) else Modifier)
            .background(bg, shape)
            .pressableScale(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        GlassText(text = text, style = GlassTheme.typography.subhead, color = fg)
    }
}
