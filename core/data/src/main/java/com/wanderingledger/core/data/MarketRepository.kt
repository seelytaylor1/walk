package com.wanderingledger.core.data

import androidx.room.withTransaction
import com.wanderingledger.core.database.InventoryItemEntity
import com.wanderingledger.core.database.PriceHistoryEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.model.Good
import com.wanderingledger.core.model.PriceHistory
import com.wanderingledger.core.model.SupplyLevel
import com.wanderingledger.core.model.TownPrice
import com.wanderingledger.core.telemetry.MarketAnomalyType
import com.wanderingledger.core.telemetry.TelemetryEvent
import com.wanderingledger.core.telemetry.TelemetryService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// ── Domain result types ──────────────────────────────────────────────────────

sealed interface BuyResult {
    /** Transaction succeeded. */
    data class Success(
        val goodId: Long,
        val quantity: Int,
        val goldSpent: Long,
        val remainingGold: Long,
    ) : BuyResult

    /** Player does not have enough gold. */
    data class NotEnoughGold(
        val required: Long,
        val available: Long,
    ) : BuyResult

    /** Player's inventory is full. */
    data object InventoryFull : BuyResult

    /** No price record exists for this good at this town. */
    data object GoodNotAvailable : BuyResult

    /** Requested quantity is invalid (≤ 0). */
    data object InvalidQuantity : BuyResult
}

sealed interface SellResult {
    /** Transaction succeeded. */
    data class Success(
        val goodId: Long,
        val quantity: Int,
        val goldEarned: Long,
        val remainingGold: Long,
    ) : SellResult

    /** Player does not own enough of this good. */
    data class NotEnoughInventory(
        val required: Int,
        val available: Int,
    ) : SellResult

    /** No price record exists for this good at this town. */
    data object GoodNotAvailable : SellResult

    /** Requested quantity is invalid (≤ 0). */
    data object InvalidQuantity : SellResult
}

// ── Market screen state ──────────────────────────────────────────────────────

/**
 * A single row in the market listing for a town.
 */
data class MarketRow(
    val good: Good,
    val townPrice: TownPrice,
    /** How many units the player currently owns. */
    val playerQuantity: Int,
    /** True when the player can afford at least 1 unit. */
    val canAfford: Boolean,
    /** True when the player has at least 1 unit to sell. */
    val canSell: Boolean,
)

/**
 * Full market screen state for a town.
 */
data class MarketState(
    val townId: Long,
    val townName: String,
    val playerGold: Long,
    val playerInventoryUsed: Int,
    val playerInventoryCapacity: Int,
    val rows: List<MarketRow>,
)

// ── Repository ───────────────────────────────────────────────────────────────

/**
 * Room-backed implementation of the market contract.
 *
 * Responsibilities:
 * - Expose reactive [MarketState] for a town.
 * - Execute buy/sell transactions atomically, updating gold, inventory, supply level,
 *   computed prices, and price history.
 * - Trim price history to [PriceHistoryEntity.MAX_HISTORY_PER_GOOD_TOWN] entries.
 */
class MarketRepository(
    private val database: WanderingLedgerDatabase,
) {
    // ── Observation ──────────────────────────────────────────────────────────

    /**
     * Stream the full market state for [townId].
     * Emits whenever prices, inventory, or player gold changes.
     */
    fun observeMarket(townId: Long): Flow<MarketState> =
        combine(
            database.townDao().getTown(townId).filterNotNull(),
            database.townPriceDao().listPricesForTown(townId),
            database.playerDao().getPlayer().filterNotNull(),
            database.inventoryDao().listInventory(playerId = 1L),
            database.goodDao().listGoods(),
        ) { town, prices, player, inventory, goods ->
            val goodsById = goods.associateBy { it.goodId }
            val inventoryByGoodId =
                inventory
                    .groupBy { it.goodId }
                    .mapValues { (_, items) -> items.sumOf { it.quantity } }

            val rows =
                prices.mapNotNull { priceEntity ->
                    val good = goodsById[priceEntity.goodId] ?: return@mapNotNull null
                    val supplyLevel = SupplyLevel.valueOf(priceEntity.supplyLevel)
                    val playerQty = inventoryByGoodId[priceEntity.goodId] ?: 0
                    val inventoryUsed = inventory.sumOf { it.quantity }
                    MarketRow(
                        good =
                            Good(
                                goodId = good.goodId,
                                name = good.name,
                                baseValue = good.baseValue,
                                isContraband = good.isContraband,
                            ),
                        townPrice =
                            TownPrice(
                                id = priceEntity.id,
                                townId = priceEntity.townId,
                                goodId = priceEntity.goodId,
                                buyPrice = priceEntity.buyPrice,
                                sellPrice = priceEntity.sellPrice,
                                supplyLevel = supplyLevel,
                                lastUpdatedAt = priceEntity.lastUpdatedAt,
                            ),
                        playerQuantity = playerQty,
                        canAfford = player.gold >= priceEntity.sellPrice,
                        canSell = playerQty > 0,
                    )
                }

            MarketState(
                townId = town.townId,
                townName = town.name,
                playerGold = player.gold,
                playerInventoryUsed = inventory.sumOf { it.quantity },
                playerInventoryCapacity = player.inventorySlots,
                rows = rows,
            )
        }

    /**
     * Stream price history for a specific good at a town, newest first.
     */
    fun observePriceHistory(
        townId: Long,
        goodId: Long,
    ): Flow<List<PriceHistory>> =
        database
            .priceHistoryDao()
            .listHistory(townId, goodId)
            .map { entities -> entities.map { it.toModel() } }

    // ── Transactions ─────────────────────────────────────────────────────────

    /**
     * Buy [quantity] units of [goodId] from [townId].
     *
     * Atomically:
     * 1. Validates quantity, price availability, gold, and inventory space.
     * 2. Deducts gold from player.
     * 3. Adds items to inventory (upserts existing stack).
     * 4. Decreases supply level (buying reduces supply).
     * 5. Recomputes and persists new buy/sell prices.
     * 6. Records a price history snapshot.
     * 7. Trims price history to [PriceHistoryEntity.MAX_HISTORY_PER_GOOD_TOWN].
     */
    suspend fun buyGood(
        townId: Long,
        goodId: Long,
        quantity: Int,
    ): BuyResult {
        if (quantity <= 0) return BuyResult.InvalidQuantity

        return database.withTransaction {
            val player =
                database.playerDao().getPlayerSnapshot()
                    ?: error("Seed world before trading.")
            val priceEntity =
                database.townPriceDao().getPrice(townId, goodId).first()
                    ?: return@withTransaction BuyResult.GoodNotAvailable
            val good =
                database.goodDao().getGood(goodId).first()
                    ?: return@withTransaction BuyResult.GoodNotAvailable

            val totalCost = priceEntity.sellPrice * quantity
            if (player.gold < totalCost) {
                return@withTransaction BuyResult.NotEnoughGold(
                    required = totalCost,
                    available = player.gold,
                )
            }

            val currentInventory = database.inventoryDao().listInventory(player.playerId).first()
            val currentUsed = currentInventory.sumOf { it.quantity }
            if (currentUsed + quantity > player.inventorySlots) {
                return@withTransaction BuyResult.InventoryFull
            }

            // Deduct gold
            database.playerDao().updatePlayer(
                player.copy(gold = player.gold - totalCost),
            )

            // Update inventory — upsert into existing stack if present
            val existingItem = currentInventory.firstOrNull { it.goodId == goodId }
            if (existingItem != null) {
                database.inventoryDao().addItem(
                    existingItem.copy(quantity = existingItem.quantity + quantity),
                )
            } else {
                database.inventoryDao().addItem(
                    InventoryItemEntity(
                        playerId = player.playerId,
                        goodId = goodId,
                        quantity = quantity,
                    ),
                )
            }

            // Update supply and recompute prices
            val currentSupply = SupplyLevel.valueOf(priceEntity.supplyLevel)
            val newSupply = MarketEngine.decreaseSupply(currentSupply)
            val (newSellPrice, newBuyPrice) = MarketEngine.computePrices(good.baseValue, newSupply)
            val now = System.currentTimeMillis()

            database.townPriceDao().updatePrice(
                priceEntity.copy(
                    supplyLevel = newSupply.name,
                    sellPrice = newSellPrice,
                    buyPrice = newBuyPrice,
                    lastUpdatedAt = now,
                ),
            )

            // Record price history snapshot and trim
            recordPriceSnapshot(townId, goodId, newSellPrice, newBuyPrice, newSupply, now)

            // Telemetry: record transaction
            TelemetryService.tryRecord(
                TelemetryEvent.MarketTransaction(
                    timestamp = now,
                    townId = townId,
                    goodId = goodId.toString(),
                    transactionType = "buy",
                    quantity = quantity,
                    pricePerUnit = priceEntity.sellPrice,
                ),
            )

            // Telemetry: detect market anomalies
            detectMarketAnomalies(
                townId,
                goodId,
                good.baseValue,
                priceEntity.sellPrice,
                newSellPrice,
                currentSupply,
                newSupply,
            )

            // Increment trade count on successful buy
            val updatedPlayer = database.playerDao().getPlayerSnapshot()!!
            database.playerDao().updatePlayer(
                updatedPlayer.copy(completedTradesCount = updatedPlayer.completedTradesCount + 1)
            )

            BuyResult.Success(
                goodId = goodId,
                quantity = quantity,
                goldSpent = totalCost,
                remainingGold = player.gold - totalCost,
            )
        }
    }

    /**
     * Sell [quantity] units of [goodId] to [townId].
     *
     * Atomically:
     * 1. Validates quantity, price availability, and inventory ownership.
     * 2. Adds gold to player.
     * 3. Removes items from inventory (removes stack when quantity reaches 0).
     * 4. Increases supply level (selling increases supply).
     * 5. Recomputes and persists new buy/sell prices.
     * 6. Records a price history snapshot.
     * 7. Trims price history to [PriceHistoryEntity.MAX_HISTORY_PER_GOOD_TOWN].
     */
    suspend fun sellGood(
        townId: Long,
        goodId: Long,
        quantity: Int,
    ): SellResult {
        if (quantity <= 0) return SellResult.InvalidQuantity

        return database.withTransaction {
            val player =
                database.playerDao().getPlayerSnapshot()
                    ?: error("Seed world before trading.")
            val priceEntity =
                database.townPriceDao().getPrice(townId, goodId).first()
                    ?: return@withTransaction SellResult.GoodNotAvailable
            val good =
                database.goodDao().getGood(goodId).first()
                    ?: return@withTransaction SellResult.GoodNotAvailable

            val currentInventory = database.inventoryDao().listInventory(player.playerId).first()
            val existingItem = currentInventory.firstOrNull { it.goodId == goodId }
            val ownedQty = existingItem?.quantity ?: 0

            if (ownedQty < quantity) {
                return@withTransaction SellResult.NotEnoughInventory(
                    required = quantity,
                    available = ownedQty,
                )
            }

            val totalEarned = priceEntity.buyPrice * quantity

            // Add gold
            database.playerDao().updatePlayer(
                player.copy(gold = player.gold + totalEarned),
            )

            // Update inventory
            val newQty = ownedQty - quantity
            if (newQty <= 0) {
                database.inventoryDao().removeItem(existingItem!!.id)
            } else {
                database.inventoryDao().addItem(
                    existingItem!!.copy(quantity = newQty),
                )
            }

            // Update supply and recompute prices
            val currentSupply = SupplyLevel.valueOf(priceEntity.supplyLevel)
            val newSupply = MarketEngine.increaseSupply(currentSupply)
            val (newSellPrice, newBuyPrice) = MarketEngine.computePrices(good.baseValue, newSupply)
            val now = System.currentTimeMillis()

            database.townPriceDao().updatePrice(
                priceEntity.copy(
                    supplyLevel = newSupply.name,
                    sellPrice = newSellPrice,
                    buyPrice = newBuyPrice,
                    lastUpdatedAt = now,
                ),
            )

            // Record price history snapshot and trim
            recordPriceSnapshot(townId, goodId, newSellPrice, newBuyPrice, newSupply, now)

            // Telemetry: record transaction
            TelemetryService.tryRecord(
                TelemetryEvent.MarketTransaction(
                    timestamp = now,
                    townId = townId,
                    goodId = goodId.toString(),
                    transactionType = "sell",
                    quantity = quantity,
                    pricePerUnit = priceEntity.buyPrice,
                ),
            )

            // Telemetry: detect market anomalies
            detectMarketAnomalies(
                townId,
                goodId,
                good.baseValue,
                priceEntity.sellPrice,
                newSellPrice,
                currentSupply,
                newSupply,
            )

            // Increment trade count on successful sell
            val updatedPlayer = database.playerDao().getPlayerSnapshot()!!
            database.playerDao().updatePlayer(
                updatedPlayer.copy(completedTradesCount = updatedPlayer.completedTradesCount + 1)
            )

            SellResult.Success(
                goodId = goodId,
                quantity = quantity,
                goldEarned = totalEarned,
                remainingGold = player.gold + totalEarned,
            )
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Insert a price snapshot and trim history to [PriceHistoryEntity.MAX_HISTORY_PER_GOOD_TOWN].
     * Must be called inside a Room transaction.
     */
    private suspend fun recordPriceSnapshot(
        townId: Long,
        goodId: Long,
        sellPrice: Long,
        buyPrice: Long,
        supplyLevel: SupplyLevel,
        recordedAt: Long,
    ) {
        database.priceHistoryDao().insertSnapshot(
            PriceHistoryEntity(
                townId = townId,
                goodId = goodId,
                sellPrice = sellPrice,
                buyPrice = buyPrice,
                supplyLevel = supplyLevel.name,
                recordedAt = recordedAt,
            ),
        )

        val count = database.priceHistoryDao().countHistory(townId, goodId)
        val excess = count - PriceHistoryEntity.MAX_HISTORY_PER_GOOD_TOWN
        if (excess > 0) {
            database.priceHistoryDao().trimOldest(townId, goodId, excess)
        }
    }

    private fun detectMarketAnomalies(
        townId: Long,
        goodId: Long,
        baseValue: Long,
        oldPrice: Long,
        newPrice: Long,
        oldSupply: SupplyLevel,
        newSupply: SupplyLevel,
    ) {
        val now = System.currentTimeMillis()
        val priceChangePercent =
            if (oldPrice > 0) {
                ((newPrice - oldPrice).toDouble() / oldPrice * 100).toLong()
            } else {
                0L
            }

        // Price spike: >50% increase
        if (priceChangePercent > 50) {
            TelemetryService.tryRecord(
                TelemetryEvent.MarketAnomaly(
                    timestamp = now,
                    anomalyType = MarketAnomalyType.PriceSpike,
                    townId = townId,
                    goodId = goodId.toString(),
                    value = newPrice,
                    threshold = (oldPrice * 1.5).toLong(),
                ),
            )
        }

        // Price crash: >30% decrease
        if (priceChangePercent < -30) {
            TelemetryService.tryRecord(
                TelemetryEvent.MarketAnomaly(
                    timestamp = now,
                    anomalyType = MarketAnomalyType.PriceCrash,
                    townId = townId,
                    goodId = goodId.toString(),
                    value = newPrice,
                    threshold = (oldPrice * 0.7).toLong(),
                ),
            )
        }

        // Supply depleted: moved to Scarce
        if (oldSupply != SupplyLevel.Scarce && newSupply == SupplyLevel.Scarce) {
            TelemetryService.tryRecord(
                TelemetryEvent.MarketAnomaly(
                    timestamp = now,
                    anomalyType = MarketAnomalyType.SupplyDepleted,
                    townId = townId,
                    goodId = goodId.toString(),
                    value = newSupply.ordinal.toLong(),
                    threshold = SupplyLevel.Scarce.ordinal.toLong(),
                ),
            )
        }

        // Unusual volume: check if price deviates significantly from base value (>100%)
        val deviationPercent =
            if (baseValue > 0) {
                ((newPrice - baseValue).toDouble() / baseValue * 100).toLong()
            } else {
                0L
            }
        if (deviationPercent > 100) {
            TelemetryService.tryRecord(
                TelemetryEvent.MarketAnomaly(
                    timestamp = now,
                    anomalyType = MarketAnomalyType.UnusualVolume,
                    townId = townId,
                    goodId = goodId.toString(),
                    value = newPrice,
                    threshold = baseValue * 2,
                ),
            )
        }
    }
}

// ── Entity → model mappers ───────────────────────────────────────────────────

private fun PriceHistoryEntity.toModel(): PriceHistory =
    PriceHistory(
        id = id,
        townId = townId,
        goodId = goodId,
        buyPrice = buyPrice,
        sellPrice = sellPrice,
        supplyLevel = SupplyLevel.valueOf(supplyLevel),
        recordedAt = recordedAt,
    )
