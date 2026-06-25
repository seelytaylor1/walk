package com.wanderingledger.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "towns",
    indices = [Index("name"), Index("region"), Index("lastVisitedAt")],
)
data class TownEntity(
    @PrimaryKey val townId: Long,
    val name: String,
    val region: String,
    @ColumnInfo(defaultValue = "Forest")
    val biome: String,
    val reputation: Int,
    val storyState: String,
    val lastVisitedAt: Long,
)

@Entity(tableName = "goods")
data class GoodEntity(
    @PrimaryKey val goodId: Long,
    val name: String,
    val baseValue: Long,
    val isContraband: Boolean = false,
)

@Entity(
    tableName = "town_produces",
    primaryKeys = ["townId", "goodId"],
    foreignKeys = [
        ForeignKey(TownEntity::class, ["townId"], ["townId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(GoodEntity::class, ["goodId"], ["goodId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("goodId")],
)
data class TownProducesEntity(
    val townId: Long,
    val goodId: Long,
)

@Entity(
    tableName = "town_demands",
    primaryKeys = ["townId", "goodId"],
    foreignKeys = [
        ForeignKey(TownEntity::class, ["townId"], ["townId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(GoodEntity::class, ["goodId"], ["goodId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("goodId")],
)
data class TownDemandsEntity(
    val townId: Long,
    val goodId: Long,
)

@Entity(
    tableName = "town_prices",
    foreignKeys = [
        ForeignKey(TownEntity::class, ["townId"], ["townId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(GoodEntity::class, ["goodId"], ["goodId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("townId"), Index("goodId"), Index(value = ["townId", "goodId"], unique = true)],
)
data class TownPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val townId: Long,
    val goodId: Long,
    val buyPrice: Long,
    val sellPrice: Long,
    val supplyLevel: String,
    val lastUpdatedAt: Long,
    @ColumnInfo(defaultValue = "0")
    val minReputation: Int = 0,
)

@Entity(
    tableName = "player_states",
    foreignKeys = [ForeignKey(TownEntity::class, ["townId"], ["currentTownId"])],
    indices = [Index("currentTownId")],
)
data class PlayerStateEntity(
    @PrimaryKey val playerId: Long = 1,
    val name: String,
    val playerClass: String,
    val gold: Long,
    val currentTownId: Long,
    val inventorySlots: Int,
    val bankedSteps: Long,
    val lifetimeSteps: Long,
    val lastSyncAt: Long,
    @ColumnInfo(defaultValue = "0")
    val completedTradesCount: Int = 0,
)

@Entity(
    tableName = "inventory_items",
    foreignKeys = [
        ForeignKey(PlayerStateEntity::class, ["playerId"], ["playerId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(GoodEntity::class, ["goodId"], ["goodId"]),
    ],
    indices = [Index("playerId"), Index("goodId")],
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playerId: Long,
    val goodId: Long,
    val quantity: Int,
    val isSealed: Boolean = false,
)

@Entity(
    tableName = "companions",
    foreignKeys = [ForeignKey(TownEntity::class, ["townId"], ["locationTownId"])],
    indices = [Index("locationTownId"), Index("isActive")],
)
data class CompanionEntity(
    @PrimaryKey val companionId: Long,
    val name: String,
    val role: String,
    val combatPower: Int,
    val bondLevel: Int,
    val questState: String,
    val locationTownId: Long,
    val isActive: Boolean,
)

@Entity(
    tableName = "road_segments",
    foreignKeys = [
        ForeignKey(TownEntity::class, ["townId"], ["fromTownId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TownEntity::class, ["townId"], ["toTownId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("fromTownId"), Index("toTownId")],
)
data class RoadSegmentEntity(
    @PrimaryKey val segmentId: Long,
    val fromTownId: Long,
    val toTownId: Long,
    val stepCost: Int,
    val narrativeDistance: String,
    val eventPool: String,
)

@Entity(
    tableName = "rumors",
    foreignKeys = [
        ForeignKey(GoodEntity::class, ["goodId"], ["targetGoodId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(TownEntity::class, ["townId"], ["sourceTownId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("targetGoodId"), Index("sourceTownId"), Index("createdAt")],
)
data class RumorEntity(
    @PrimaryKey(autoGenerate = true) val rumorId: Long = 0,
    val text: String,
    val targetGoodId: Long?,
    val sourceTownId: Long?,
    val createdAt: Long,
    val expiryVisitsLeft: Int,
    val isFalse: Boolean,
)

@Entity(tableName = "step_records", indices = [Index(value = ["dateEpoch", "source"])])
data class StepRecordEntity(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0,
    val dateEpoch: Long,
    val steps: Int,
    val source: String,
)

@Entity(tableName = "event_logs", indices = [Index("createdAt")])
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val eventId: Long = 0,
    val type: String,
    val meta: String,
    val result: String,
    val createdAt: Long,
)

/**
 * Stores a price snapshot for a good in a town at a point in time.
 * Used to render price-trend sparklines in the market UI.
 * Trimmed to the most recent [MAX_HISTORY_PER_GOOD_TOWN] entries per (townId, goodId) pair.
 */
@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(TownEntity::class, ["townId"], ["townId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(GoodEntity::class, ["goodId"], ["goodId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("townId"), Index("goodId"), Index(value = ["townId", "goodId", "recordedAt"])],
)
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val townId: Long,
    val goodId: Long,
    /** The buy price (what the town sells at) at this snapshot. */
    val buyPrice: Long,
    /** The sell price (what the town pays the player) at this snapshot. */
    val sellPrice: Long,
    val supplyLevel: String,
    val recordedAt: Long,
) {
    companion object {
        const val MAX_HISTORY_PER_GOOD_TOWN = 10
    }
}
