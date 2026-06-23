package com.wanderingledger.core.model

data class Town(
    val townId: Long,
    val name: String,
    val region: String,
    val biome: Biome,
    val producedGoodIds: List<Long> = emptyList(),
    val demandedGoodIds: List<Long> = emptyList(),
    val reputation: Int = 50,
    val storyState: String = "new",
    val lastVisitedAt: Long = 0,
)

data class Good(
    val goodId: Long,
    val name: String,
    val baseValue: Long,
    val isContraband: Boolean = false,
)

data class TownPrice(
    val id: Long,
    val townId: Long,
    val goodId: Long,
    val buyPrice: Long,
    val sellPrice: Long,
    val supplyLevel: SupplyLevel,
    val lastUpdatedAt: Long,
)

data class PriceHistory(
    val id: Long,
    val townId: Long,
    val goodId: Long,
    val buyPrice: Long,
    val sellPrice: Long,
    val supplyLevel: SupplyLevel,
    val recordedAt: Long,
)

data class PlayerState(
    val playerId: Long = 1,
    val name: String,
    val playerClass: PlayerClass = PlayerClass.Wanderer,
    val gold: Long,
    val currentTownId: Long,
    val inventorySlots: Int = 12,
    val bankedSteps: Long = 0,
    val lifetimeSteps: Long,
    val lastSyncAt: Long = 0,
    val completedTradesCount: Int = 0,
)

data class InventoryItem(
    val id: Long = 0,
    val playerId: Long,
    val goodId: Long,
    val quantity: Int,
    val isSealed: Boolean = false,
)

data class Companion(
    val companionId: Long,
    val name: String,
    val role: CompanionRole,
    val combatPower: Int,
    val bondLevel: Int,
    val questState: String = "available",
    val locationTownId: Long,
    val isActive: Boolean = false,
)

data class RoadSegment(
    val segmentId: Long,
    val fromTownId: Long,
    val toTownId: Long,
    val stepCost: Int,
    val narrativeDistance: String,
    val eventPool: String = "[]",
)

data class Rumor(
    val rumorId: Long = 0,
    val text: String,
    val targetGoodId: Long?,
    val sourceTownId: Long?,
    val createdAt: Long,
    val expiryVisitsLeft: Int,
    val isFalse: Boolean = false,
)

data class StepRecord(
    val recordId: Long = 0,
    val dateEpoch: Long,
    val steps: Int,
    val source: StepSource,
)

enum class PlayerClass {
    Merchant,
    Scholar,
    Wanderer,
}

enum class SupplyLevel {
    Scarce,
    Normal,
    Abundant,
}

enum class CompanionRole {
    Fighter,
    Scout,
    Healer,
    Rogue,
    Mage,
    ;

    val emoji: String
        get() =
            when (this) {
                Fighter -> "⚔️"
                Scout -> "🔍"
                Healer -> "💚"
                Rogue -> "🗡️"
                Mage -> "✨"
            }
}

enum class StepSource {
    Hardware,
    MotionFallback,
    Simulation,
}

enum class Biome {
    Forest,
    Mountain,
    Swamp,
    Coast,
}
