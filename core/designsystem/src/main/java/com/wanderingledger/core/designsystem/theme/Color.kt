package com.wanderingledger.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Wandering Ledger color palette inspired by medieval trading routes and parchment.
 * Uses warm, earthy tones suitable for a walking and trading game.
 */

// Primary - Warm amber/gold for trading theme
val PrimaryLight = Color(0xFFB8860B) // Dark goldenrod
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFFFE082)
val OnPrimaryContainerLight = Color(0xFF3E2723)

val PrimaryDark = Color(0xFFFFD54F) // Lighter gold for dark mode
val OnPrimaryDark = Color(0xFF3E2723)
val PrimaryContainerDark = Color(0xFFB8860B)
val OnPrimaryContainerDark = Color(0xFFFFFFFF)

// Secondary - Earthy brown for roads and travel
val SecondaryLight = Color(0xFF6D4C41) // Brown
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD7CCC8)
val OnSecondaryContainerLight = Color(0xFF3E2723)

val SecondaryDark = Color(0xFFBCAAA4) // Light brown
val OnSecondaryDark = Color(0xFF3E2723)
val SecondaryContainerDark = Color(0xFF6D4C41)
val OnSecondaryContainerDark = Color(0xFFFFFFFF)

// Tertiary - Sage green for nature/walking
val TertiaryLight = Color(0xFF558B2F) // Green
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFC5E1A5)
val OnTertiaryContainerLight = Color(0xFF1B5E20)

val TertiaryDark = Color(0xFF9CCC65) // Light green
val OnTertiaryDark = Color(0xFF1B5E20)
val TertiaryContainerDark = Color(0xFF558B2F)
val OnTertiaryContainerDark = Color(0xFFFFFFFF)

// Error colors
val ErrorLight = Color(0xFFB00020)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFDEDED)
val OnErrorContainerLight = Color(0xFF410002)

val ErrorDark = Color(0xFFCF6679)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Background and Surface
val BackgroundLight = Color(0xFFFFFBF5) // Warm parchment white
val OnBackgroundLight = Color(0xFF1C1B1F)
val SurfaceLight = Color(0xFFFFFBF5)
val OnSurfaceLight = Color(0xFF1C1B1F)
val SurfaceVariantLight = Color(0xFFE7E0EC)
val OnSurfaceVariantLight = Color(0xFF49454F)

val BackgroundDark = Color(0xFF1C1B1F)
val OnBackgroundDark = Color(0xFFE6E1E5)
val SurfaceDark = Color(0xFF1C1B1F)
val OnSurfaceDark = Color(0xFFE6E1E5)
val SurfaceVariantDark = Color(0xFF49454F)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)

// Outline
val OutlineLight = Color(0xFF79747E)
val OutlineDark = Color(0xFF938F99)

// Semantic colors for game states
val SuccessLight = Color(0xFF2E7D32) // Green for positive actions
val SuccessDark = Color(0xFF66BB6A)

val WarningLight = Color(0xFFF57C00) // Orange for warnings
val WarningDark = Color(0xFFFFB74D)

val InfoLight = Color(0xFF1976D2) // Blue for information
val InfoDark = Color(0xFF64B5F6)

// Game-specific colors
val StepBankColor = Color(0xFF558B2F) // Green for steps
val GoldColor = Color(0xFFFFD700) // Gold for currency
val GoldColorDark = Color(0xFF8B6914) // Darker gold for text contrast
val ContrabandColor = Color(0xFFD32F2F) // Red for contraband goods
val ReputationColor = Color(0xFF1976D2) // Blue for reputation

// Biome color palettes
data class BiomeColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
)

val ForestColors = BiomeColors(
    primary = Color(0xFF2E7D32),     // Deep forest green
    secondary = Color(0xFF795548),    // Earthy brown
    tertiary = Color(0xFF8BC34A),    // Light green
    background = Color(0xFFF1F8E9),  // Pale green tint
    surface = Color(0xFFFFFFFF),
)

val MountainColors = BiomeColors(
    primary = Color(0xFF455A64),    // Stone gray-blue
    secondary = Color(0xFF795548),   // Earthy brown
    tertiary = Color(0xFF90A4AE),    // Light stone
    background = Color(0xFFECEFF1), // Pale gray tint
    surface = Color(0xFFFFFFFF),
)

val SwampColors = BiomeColors(
    primary = Color(0xFF00695C),     // Teal swamp green
    secondary = Color(0xFF5D4037),   // Dark brown
    tertiary = Color(0xFF4DB6AC),    // Light teal
    background = Color(0xFFE0F2F1), // Pale teal tint
    surface = Color(0xFFFFFFFF),
)

val CoastColors = BiomeColors(
    primary = Color(0xFF0277BD),     // Ocean blue
    secondary = Color(0xFFFFB74D),   // Sand/sunset
    tertiary = Color(0xFF4FC3F7),    // Light blue
    background = Color(0xFFE1F5FE), // Pale blue tint
    surface = Color(0xFFFFFFFF),
)
