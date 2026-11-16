package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// -----------------------------------------------------------------------------
// Palette Tokens (Light & Dark)
// -----------------------------------------------------------------------------

private val LightBg = Color(0xFFF2F5F6)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSunk = Color(0xFFE7ECEF)
private val LightBar = Color(0xFFCBD6DB)
private val LightInk = Color(0xFF0D1B2A)
private val LightInk2 = Color(0xFF4A5A6C)
private val LightInk3 = Color(0xFF66768A)
private val LightLine = Color(0xFFE0E6EA)
private val LightAccent = Color(0xFF0A7C6B)
private val LightAccentInk = Color(0xFF075C50)
private val LightAccentSoft = Color(0xFFD8F0EA)
private val LightOnAccent = Color(0xFFFFFFFF)
private val LightHero = Color(0xFF0B5E52)
private val LightHeroFg = Color(0xFFFFFFFF)

private val LightOk = Color(0xFF17753F)
private val LightOkSoft = Color(0xFFDCF2E3)
private val LightWarn = Color(0xFF9A5B00)
private val LightWarnSoft = Color(0xFFFCEED0)
private val LightBad = Color(0xFFB83225)
private val LightBadSoft = Color(0xFFFADFDA)
private val LightOnBad = Color(0xFFFFFFFF)

private val LightCat1 = Color(0xFFE4702F)
private val LightCat2 = Color(0xFF3B6EE0)
private val LightCat3 = Color(0xFF2E9E6F)
private val LightCat4 = Color(0xFFD19A12)
private val LightCat5 = Color(0xFF8758D6)
private val LightCat6 = Color(0xFFD2457C)
private val LightCat7 = Color(0xFF12869B)
private val LightCat8 = Color(0xFF9A6B43)
private val LightCat9 = Color(0xFF5B6B80)

private val DarkBg = Color(0xFF0B131B)
private val DarkSurface = Color(0xFF14202B)
private val DarkSunk = Color(0xFF0F1A23)
private val DarkBar = Color(0xFF2A3E4D)
private val DarkInk = Color(0xFFEAF1F5)
private val DarkInk2 = Color(0xFFA3B2C0)
private val DarkInk3 = Color(0xFF8496A8)
private val DarkLine = Color(0xFF223240)
private val DarkAccent = Color(0xFF2CC3A7)
private val DarkAccentInk = Color(0xFF5FE0C6)
private val DarkAccentSoft = Color(0xFF12372F)
private val DarkOnAccent = Color(0xFF04231D)
private val DarkHero = Color(0xFF0F4A42)
private val DarkHeroFg = Color(0xFFFFFFFF)

private val DarkOk = Color(0xFF4CCB84)
private val DarkOkSoft = Color(0xFF133522)
private val DarkWarn = Color(0xFFF0B04A)
private val DarkWarnSoft = Color(0xFF3A2B0E)
private val DarkBad = Color(0xFFFF8A7A)
private val DarkBadSoft = Color(0xFF3E1B17)
private val DarkOnBad = Color(0xFF0B131B)

private val DarkCat1 = Color(0xFFF08A4F)
private val DarkCat2 = Color(0xFF6B96FF)
private val DarkCat3 = Color(0xFF4CC48D)
private val DarkCat4 = Color(0xFFEBBB45)
private val DarkCat5 = Color(0xFFA98BF0)
private val DarkCat6 = Color(0xFFEE6B9C)
private val DarkCat7 = Color(0xFF35B5CB)
private val DarkCat8 = Color(0xFFC48F66)
private val DarkCat9 = Color(0xFF8DA0B5)

// -----------------------------------------------------------------------------
// Material 3 ColorSchemes
// -----------------------------------------------------------------------------

val RecapSpendsLightColorScheme: ColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = LightAccentSoft,
    onPrimaryContainer = LightAccentInk,
    secondary = LightHero,
    onSecondary = LightHeroFg,
    secondaryContainer = LightSunk,
    onSecondaryContainer = LightInk,
    tertiary = LightCat2,
    onTertiary = LightOnAccent,
    tertiaryContainer = LightAccentSoft,
    onTertiaryContainer = LightAccentInk,
    background = LightBg,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSunk,
    onSurfaceVariant = LightInk2,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightBg,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSunk,
    surfaceContainerHighest = LightBar,
    outline = LightInk3,
    outlineVariant = LightLine,
    error = LightBad,
    onError = LightOnBad,
    errorContainer = LightBadSoft,
    onErrorContainer = LightBad
)

val RecapSpendsDarkColorScheme: ColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    primaryContainer = DarkAccentSoft,
    onPrimaryContainer = DarkAccentInk,
    secondary = DarkHero,
    onSecondary = DarkHeroFg,
    secondaryContainer = DarkSunk,
    onSecondaryContainer = DarkInk,
    tertiary = DarkCat2,
    onTertiary = DarkBg,
    tertiaryContainer = DarkAccentSoft,
    onTertiaryContainer = DarkAccentInk,
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSunk,
    onSurfaceVariant = DarkInk2,
    surfaceContainerLowest = DarkBg,
    surfaceContainerLow = DarkSunk,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkLine,
    surfaceContainerHighest = DarkBar,
    outline = DarkInk3,
    outlineVariant = DarkLine,
    error = DarkBad,
    onError = DarkOnBad,
    errorContainer = DarkBadSoft,
    onErrorContainer = DarkBad
)

// -----------------------------------------------------------------------------
// Extended Semantic Tokens (Success, Warning, Category Palette)
// -----------------------------------------------------------------------------

@Immutable
data class SelavuExtendedColors(
    val ok: Color,
    val okSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val categoryColors: List<Color>,
    val isDark: Boolean
) {
    fun categoryColor(index: Int): Color {
        val safeIdx = (index - 1).coerceIn(0, categoryColors.lastIndex)
        return categoryColors[safeIdx]
    }
}

private val LightExtendedColors = SelavuExtendedColors(
    ok = LightOk,
    okSoft = LightOkSoft,
    warn = LightWarn,
    warnSoft = LightWarnSoft,
    categoryColors = listOf(
        LightCat1, LightCat2, LightCat3, LightCat4, LightCat5,
        LightCat6, LightCat7, LightCat8, LightCat9
    ),
    isDark = false
)

private val DarkExtendedColors = SelavuExtendedColors(
    ok = DarkOk,
    okSoft = DarkOkSoft,
    warn = DarkWarn,
    warnSoft = DarkWarnSoft,
    categoryColors = listOf(
        DarkCat1, DarkCat2, DarkCat3, DarkCat4, DarkCat5,
        DarkCat6, DarkCat7, DarkCat8, DarkCat9
    ),
    isDark = true
)

val LocalSelavuExtendedColors = staticCompositionLocalOf { LightExtendedColors }

// -----------------------------------------------------------------------------
// Typography (Bundled Manrope & IBM Plex Mono)
// -----------------------------------------------------------------------------

// Static TTF files downloaded via font-util; declaring Normal lets Android synthesize
// crisp Bold / SemiBold weights consistently instead of flattening heavier weights to Regular.
val ManropeFontFamily = FontFamily(
    Font(R.font.manrope, FontWeight.Normal)
)

val IbmPlexMonoFontFamily = FontFamily(
    Font(R.font.ibm_plex_mono, FontWeight.Normal)
)

val MonoCodeStyle = TextStyle(
    fontFamily = IbmPlexMonoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.2.sp,
    fontFeatureSettings = "tnum"
)

@Immutable
data class SelavuTextStyles(
    val monoCode: TextStyle = MonoCodeStyle,
    val numericTitle: TextStyle = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontFeatureSettings = "tnum"
    ),
    val numericBody: TextStyle = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        fontFeatureSettings = "tnum"
    ),
    val numericCaption: TextStyle = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    ),
    val sectionTitle: TextStyle = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    val badgeMicro: TextStyle = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.25.sp
    )
)

val LocalSelavuTextStyles = staticCompositionLocalOf { SelavuTextStyles() }

val SelavuTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp,
        fontFeatureSettings = "tnum"
    ),
    displayMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum"
    ),
    displaySmall = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    titleLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.25.sp
    )
)

// -----------------------------------------------------------------------------
// Shapes
// -----------------------------------------------------------------------------

val SelavuShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// -----------------------------------------------------------------------------
// Centralized Theme Composable & Accessor
// -----------------------------------------------------------------------------

@Composable
fun SelavuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val colorScheme = if (darkTheme) RecapSpendsDarkColorScheme else RecapSpendsLightColorScheme
    val textStyles = SelavuTextStyles()

    CompositionLocalProvider(
        LocalSelavuExtendedColors provides extendedColors,
        LocalSelavuTextStyles provides textStyles
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SelavuTypography,
            shapes = SelavuShapes
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.bodyMedium) {
                content()
            }
        }
    }
}

object SelavuTheme {
    val extendedColors: SelavuExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSelavuExtendedColors.current

    val textStyles: SelavuTextStyles
        @Composable
        @ReadOnlyComposable
        get() = LocalSelavuTextStyles.current
}
