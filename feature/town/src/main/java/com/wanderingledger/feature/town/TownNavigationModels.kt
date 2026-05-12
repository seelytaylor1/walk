package com.wanderingledger.feature.town

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Types of districts available in a town.
 */
enum class DistrictType {
    Market,
    Tavern,
    Archives,
    Gates,
    Treasury,
    Shrine
}

/**
 * Representation of a town district for navigation.
 */
data class District(
    val type: DistrictType,
    val name: String,
    val description: String,
    val icon: ImageVector? = null,
    val isAvailable: Boolean = true,
    val unlockRequirement: String? = null
)

/**
 * UI state for the Town Navigation screen.
 */
data class TownNavigationState(
    val townName: String,
    val districts: List<District> = emptyList()
)
