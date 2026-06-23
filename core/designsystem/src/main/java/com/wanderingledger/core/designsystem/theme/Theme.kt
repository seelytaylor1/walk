package com.wanderingledger.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.wanderingledger.core.model.Biome

private val LightColorScheme =
    lightColorScheme(
        primary = PrimaryLight,
        onPrimary = OnPrimaryLight,
        primaryContainer = PrimaryContainerLight,
        onPrimaryContainer = OnPrimaryContainerLight,
        secondary = SecondaryLight,
        onSecondary = OnSecondaryLight,
        secondaryContainer = SecondaryContainerLight,
        onSecondaryContainer = OnSecondaryContainerLight,
        tertiary = TertiaryLight,
        onTertiary = OnTertiaryLight,
        tertiaryContainer = TertiaryContainerLight,
        onTertiaryContainer = OnTertiaryContainerLight,
        error = ErrorLight,
        onError = OnErrorLight,
        errorContainer = ErrorContainerLight,
        onErrorContainer = OnErrorContainerLight,
        background = BackgroundLight,
        onBackground = OnBackgroundLight,
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceVariantLight,
        onSurfaceVariant = OnSurfaceVariantLight,
        outline = OutlineLight,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = PrimaryDark,
        onPrimary = OnPrimaryDark,
        primaryContainer = PrimaryContainerDark,
        onPrimaryContainer = OnPrimaryContainerDark,
        secondary = SecondaryDark,
        onSecondary = OnSecondaryDark,
        secondaryContainer = SecondaryContainerDark,
        onSecondaryContainer = OnSecondaryContainerDark,
        tertiary = TertiaryDark,
        onTertiary = OnTertiaryDark,
        tertiaryContainer = TertiaryContainerDark,
        onTertiaryContainer = OnTertiaryContainerDark,
        error = ErrorDark,
        onError = OnErrorDark,
        errorContainer = ErrorContainerDark,
        onErrorContainer = OnErrorContainerDark,
        background = BackgroundDark,
        onBackground = OnBackgroundDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark,
    )

data class WanderingLedgerTheme(
    val biome: Biome,
    val isDark: Boolean,
) {
    val biomeColors: BiomeColors =
        when (biome) {
            Biome.Forest -> ForestColors
            Biome.Mountain -> MountainColors
            Biome.Swamp -> SwampColors
            Biome.Coast -> CoastColors
        }

    val colors: ThemeColors
        get() = if (isDark) DarkThemeColors else LightThemeColors
}

data class ThemeColors(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
)

private val LightThemeColors =
    ThemeColors(
        primary = PrimaryLight,
        onPrimary = OnPrimaryLight,
        secondary = SecondaryLight,
        onSecondary = OnSecondaryLight,
        background = BackgroundLight,
        onBackground = OnBackgroundLight,
        surface = SurfaceLight,
        onSurface = OnSurfaceLight,
    )

private val DarkThemeColors =
    ThemeColors(
        primary = PrimaryDark,
        onPrimary = OnPrimaryDark,
        secondary = SecondaryDark,
        onSecondary = OnSecondaryDark,
        background = BackgroundDark,
        onBackground = OnBackgroundDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
    )

val LocalWanderingLedgerTheme =
    staticCompositionLocalOf {
        WanderingLedgerTheme(biome = Biome.Forest, isDark = false)
    }

/**
 * Access the current WanderingLedger theme.
 */
object WLTheme {
    val current: WanderingLedgerTheme
        @Composable
        get() = LocalWanderingLedgerTheme.current
}

/**
 * Wandering Ledger theme.
 *
 * Provides Material Design 3 theming with custom colors, typography, and shapes
 * tailored for a walking and trading game experience.
 *
 * @param darkTheme Whether to use dark theme. Defaults to system setting.
 * @param biome The biome for atmospheric theming. Defaults to Forest.
 * @param dynamicColor Whether to use dynamic color (Android 12+). Currently disabled.
 * @param content The composable content to theme.
 */
@Composable
fun WanderingLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    biome: Biome = Biome.Forest,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    val wlTheme =
        WanderingLedgerTheme(
            biome = biome,
            isDark = darkTheme,
        )

    CompositionLocalProvider(LocalWanderingLedgerTheme provides wlTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WanderingLedgerTypography,
            shapes = WanderingLedgerShapes,
            content = content,
        )
    }
}
