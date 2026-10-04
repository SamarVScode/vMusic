package com.glass.player.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@Immutable
data class GlassColors(
    val background: Color,
    val labelPrimary: Color,
    val labelSecondary: Color,
    val labelTertiary: Color,
    val separator: Color,
    val fillSubtle: Color,
    val glassTint: Color,
    val glassBorderStart: Color,
    val glassBorderEnd: Color,
    val accent: Color,
    val accentPressed: Color,
    val accentText: Color,
    val accentGlow: Color,
    val isDark: Boolean
) {
    val glassBorder: Brush
        get() = Brush.linearGradient(
            colors = listOf(glassBorderStart, glassBorderEnd),
            start = Offset.Zero,
            end = Offset.Infinite
        )
}

val GlassDarkColors = GlassColors(
    background = Color(0xFF000000),             // OLED Pure Black
    labelPrimary = Color(0xFFFFFFFF),           // 100% White
    labelSecondary = Color(0x99FFFFFF),         // 60% White
    labelTertiary = Color(0x4DFFFFFF),          // 30% White
    separator = Color(0x26FFFFFF),              // 15% White
    fillSubtle = Color(0x1EFFFFFF),             // 12% White
    glassTint = Color(0x1AFFFFFF),              // 10% White
    glassBorderStart = Color(0x38FFFFFF),       // 22% White
    glassBorderEnd = Color(0x0AFFFFFF),         // 4% White
    accent = Color(0xFFC0111F),                 // Blood Red
    accentPressed = Color(0xFF8E0B16),          // Deep Pressed Red
    accentText = Color(0xFFE5303E),             // High-contrast Red for small labels on black
    accentGlow = Color(0x4DFF2A3A),             // #FF2A3A at 30% alpha
    isDark = true
)

val GlassLightColors = GlassColors(
    background = Color(0xFFFFFFFF),             // Pure White
    labelPrimary = Color(0xFF000000),           // 100% Black
    labelSecondary = Color(0x99000000),         // 60% Black
    labelTertiary = Color(0x4D000000),          // 30% Black
    separator = Color(0x1F000000),              // 12% Black
    fillSubtle = Color(0x14000000),             // 8% Black
    glassTint = Color(0x99FFFFFF),              // 60% White
    glassBorderStart = Color(0xCCFFFFFF),       // 80% White
    glassBorderEnd = Color(0x0F000000),         // 6% Black
    accent = Color(0xFFC0111F),                 // Blood Red
    accentPressed = Color(0xFF8E0B16),          // Deep Pressed Red
    accentText = Color(0xFFC0111F),             // Blood Red
    accentGlow = Color(0x2EFF2A3A),             // #FF2A3A at 18% alpha
    isDark = false
)

@Immutable
data class GlassTypography(
    val largeTitle: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.4).sp
    ),
    val title: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    val headline: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.4).sp
    ),
    val body: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.4).sp
    ),
    val subhead: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    val tabularDigits: TextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum"
    )
)

val LocalGlassColors = staticCompositionLocalOf { GlassDarkColors }
val LocalGlassTypography = staticCompositionLocalOf { GlassTypography() }

object GlassTheme {
    val colors: GlassColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current

    val typography: GlassTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassTypography.current
}

@Composable
fun GlassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    typography: GlassTypography = GlassTypography(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) GlassDarkColors else GlassLightColors
    CompositionLocalProvider(
        LocalGlassColors provides colors,
        LocalGlassTypography provides typography,
        content = content
    )
}

@Composable
fun GlassText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = GlassTheme.colors.labelPrimary,
    style: TextStyle = GlassTheme.typography.body,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign = TextAlign.Start
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = textAlign),
        maxLines = maxLines,
        overflow = overflow
    )
}
