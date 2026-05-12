package com.wanderingledger.feature.worldmap

import androidx.compose.ui.geometry.Offset

data class MapLocation(
    val townId: Long,
    val name: String,
    val offset: Offset,
    val isDiscovered: Boolean = false,
    val isCurrentLocation: Boolean = false
)

data class MapRoute(
    val fromId: Long,
    val toId: Long,
    val fromOffset: Offset,
    val toOffset: Offset,
    val isDiscovered: Boolean = false
)
