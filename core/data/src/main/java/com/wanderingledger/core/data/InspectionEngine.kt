package com.wanderingledger.core.data

import com.wanderingledger.core.model.Companion
import kotlin.random.Random

object InspectionEngine {
    const val BASE_INSPECTION_CHANCE = 0.40
    const val ROGUE_BASE_INSPECTION_CHANCE = 0.20
    const val ROGUE_BOND_REDUCTION_PER_LEVEL = 0.03
    const val MIN_INSPECTION_CHANCE = 0.05

    fun inspectionChance(activeRogue: Companion?): Double =
        if (activeRogue == null) {
            BASE_INSPECTION_CHANCE
        } else {
            (ROGUE_BASE_INSPECTION_CHANCE - activeRogue.bondLevel * ROGUE_BOND_REDUCTION_PER_LEVEL)
                .coerceAtLeast(MIN_INSPECTION_CHANCE)
        }

    fun rollInspection(chance: Double, seed: Long): Boolean =
        Random(seed).nextDouble() < chance
}
