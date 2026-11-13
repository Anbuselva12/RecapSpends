package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class SelavuExtendedColors(
    val bg: Color,
    val surface: Color,
    val sunk: Color,
    val bar: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val line: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val hero: Color,
    val heroFg: Color,
    val ok: Color,
    val okSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val bad: Color,
    val badSoft: Color,
    val categoryColors: List<Color>,
    val isDark: Boolean
) {
    fun categoryColor(index: Int): Color {
        val safeIdx = ((index - 1).coerceIn(0, categoryColors.lastIndex))
        return categoryColors[safeIdx]
    }
}

val LocalSelavuColors = staticCompositionLocalOf {
    SelavuExtendedColors(
        bg = LightBg,
        surface = LightSurface,
        sunk = LightSunk,
        bar = LightBar,
        ink = LightInk,
        ink2 = LightInk2,
        ink3 = LightInk3,
        line = LightLine,
        accent = LightAccent,
        accentInk = LightAccentInk,
        accentSoft = LightAccentSoft,
        onAccent = LightOnAccent,
        hero = LightHero,
        heroFg = LightHeroFg,
        ok = LightOk,
        okSoft = LightOkSoft,
        warn = LightWarn,
        warnSoft = LightWarnSoft,
        bad = LightBad,
        badSoft = LightBadSoft,
        categoryColors = listOf(
            LightCat1, LightCat2, LightCat3, LightCat4, LightCat5,
            LightCat6, LightCat7, LightCat8, LightCat9
        ),
        isDark = false
    )
}

private val LightExtended = SelavuExtendedColors(
    bg = LightBg,
    surface = LightSurface,
    sunk = LightSunk,
    bar = LightBar,
    ink = LightInk,
    ink2 = LightInk2,
    ink3 = LightInk3,
    line = LightLine,
    accent = LightAccent,
    accentInk = LightAccentInk,
    accentSoft = LightAccentSoft,
    onAccent = LightOnAccent,
    hero = LightHero,
    heroFg = LightHeroFg,
    ok = LightOk,
    okSoft = LightOkSoft,
    warn = LightWarn,
    warnSoft = LightWarnSoft,
    bad = LightBad,
    badSoft = LightBadSoft,
    categoryColors = listOf(
        LightCat1, LightCat2, LightCat3, LightCat4, LightCat5,
        LightCat6, LightCat7, LightCat8, LightCat9
    ),
    isDark = false
)

private val DarkExtended = SelavuExtendedColors(
    bg = DarkBg,
    surface = DarkSurface,
    sunk = DarkSunk,
    bar = DarkBar,
    ink = DarkInk,
    ink2 = DarkInk2,
    ink3 = DarkInk3,
    line = DarkLine,
    accent = DarkAccent,
    accentInk = DarkAccentInk,
    accentSoft = DarkAccentSoft,
    onAccent = DarkOnAccent,
    hero = DarkHero,
    heroFg = DarkHeroFg,
    ok = DarkOk,
    okSoft = DarkOkSoft,
    warn = DarkWarn,
    warnSoft = DarkWarnSoft,
    bad = DarkBad,
    badSoft = DarkBadSoft,
    categoryColors = listOf(
        DarkCat1, DarkCat2, DarkCat3, DarkCat4, DarkCat5,
        DarkCat6, DarkCat7, DarkCat8, DarkCat9
    ),
    isDark = true
)

private val LightM3Scheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = LightAccentSoft,
    onPrimaryContainer = LightAccentInk,
    secondary = LightHero,
    onSecondary = LightHeroFg,
    background = LightBg,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSunk,
    onSurfaceVariant = LightInk2,
    outline = LightLine,
    error = LightBad,
    errorContainer = LightBadSoft,
    onErrorContainer = LightBad
)

private val DarkM3Scheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    primaryContainer = DarkAccentSoft,
    onPrimaryContainer = DarkAccentInk,
    secondary = DarkHero,
    onSecondary = DarkHeroFg,
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSunk,
    onSurfaceVariant = DarkInk2,
    outline = DarkLine,
    error = DarkBad,
    errorContainer = DarkBadSoft,
    onErrorContainer = DarkBad
)

val SelavuShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun SelavuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val extended = if (darkTheme) DarkExtended else LightExtended
    val colorScheme = if (darkTheme) DarkM3Scheme else LightM3Scheme

    CompositionLocalProvider(LocalSelavuColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SelavuTypography,
            shapes = SelavuShapes,
            content = content
        )
    }
}

object SelavuTheme {
    val colors: SelavuExtendedColors
        @Composable get() = LocalSelavuColors.current
}
