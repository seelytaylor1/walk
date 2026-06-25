package com.wanderingledger.core.data

import androidx.room.withTransaction
import com.wanderingledger.core.database.GoodEntity
import com.wanderingledger.core.database.InventoryItemEntity
import com.wanderingledger.core.database.PriceHistoryEntity
import com.wanderingledger.core.database.TownPriceEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.model.Good
import com.wanderingledger.core.model.PriceHistory
import com.wanderingledger.core.model.SupplyLevel
import com.wanderingledger.core.model.TownPrice
import com.wanderingledger.core.telemetry.TelemetryEvent
import com.wanderingledger.core.telemetry.TelemetryService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.wanderingledger.core.database.CompanionEntity

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
    companion object {
        const val REP_BETTER_PRICE_THRESHOLD = 75
        const val REP_BETTER_PRICE_DISCOUNT = 0.15
    }

    // ── Observation ──────────────────────────────────────────────────────────

    /**
     * Stream the full market state for [townId].
     * Emits whenever prices, inventory, or player gold changes.
     */
    fun observeMarket(townId: Long): Flow<MarketState> {
        val coreFlow =
            combine(
                database.townDao().getTown(townId).filterNotNull(),
                database.townPriceDao().listPricesForTown(townId),
                database.playerDao().getPlayer().filterNotNull(),
                database.inventoryDao().listInventory(playerId = 1L),
            ) { town, prices, player, inventory ->
                Triple(Pair(town, prices), Pair(player, inventory), Unit)
            }

        return combine(
            coreFlow,
            database.goodDao().listGoods(),
            database.companionDao().listActiveCompanions(),
        ) { (townAndPrices, playerAndInventory, _), goods, companions ->
            val (town, prices) = townAndPrices
            val (player, inventory) = playerAndInventory
            val goodsById = goods.associateBy { it.goodId }
            val inventoryByGoodId =
                inventory
                    .groupBy { it.goodId }
                    .mapValues { (_, items) -> items.sumOf { it.quantity } }
            val rep = town.reputation
            val hasActiveRogue = companions.any { it.role == "Rogue" && it.isActive }

            val rows =
                prices
                    .filter { priceEntity ->
                        val minRep = priceEntity.minReputation
                        if (minRep > rep) return@filter false
                        val good = goodsById[priceEntity.goodId]
                        if (good?.isContraband == true && !hasActiveRogue) return@filter false
                        true
                    }
                    .mapNotNull { priceEntity ->
                        val good = goodsById[priceEntity.goodId] ?: return@mapNotNull null
                        val supplyLevel = SupplyLevel.valueOf(priceEntity.supplyLevel)
                        val playerQty = inventoryByGoodId[priceEntity.goodId] ?: 0
                        val effectiveSellPrice =
                            if (priceEntity.minReputation > 0 && rep >= REP_BETTER_PRICE_THRESHOLD) {
                                (priceEntity.sellPrice * (1.0 - REP_BETTER_PRICE_DISCOUNT)).toLong()
                            } else {
                                priceEntity.sellPrice
                            }
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
                                    sellPrice = effectiveSellPrice,
                                    supplyLevel = supplyLevel,
                                    lastUpdatedAt = priceEntity.lastUpdatedAt,
                                ),
                            playerQuantity = playerQty,
                            canAfford = player.gold >= effectiveSellPrice,
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

            val townRep = database.townDao().getTownSnapshot(townId)?.reputation ?: 0
            val actualSellPrice =
                if (priceEntity.minReputation > 0 && townRep >= REP_BETTER_PRICE_THRESHOLD) {
                    (priceEntity.sellPrice * (1.0 - REP_BETTER_PRICE_DISCOUNT)).toLong()
                } else {
                    priceEntity.sellPrice
                }
            val totalCost = actualSellPrice * quantity
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

            val newSupply = MarketEngine.decreaseSupply(SupplyLevel.valueOf(priceEntity.supplyLevel))
            postTradeUpdate(townId, goodId, good, priceEntity, newSupply, "buy", quantity, actualSellPrice)

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

            val newSupply = MarketEngine.increaseSupply(SupplyLevel.valueOf(priceEntity.supplyLevel))
            postTradeUpdate(townId, goodId, good, priceEntity, newSupply, "sell", quantity, priceEntity.buyPrice)

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
     * Shared post-trade tail: updates supply + prices, records history snapshot,
     * emits telemetry, and increments the trade count. Must be called inside a
     * Room transaction.
     */
    private suspend fun postTradeUpdate(
        townId: Long,
        goodId: Long,
        good: GoodEntity,
        priceEntity: TownPriceEntity,
        newSupply: SupplyLevel,
        transactionType: String,
        quantity: Int,
        pricePerUnit: Long,
    ) {
        val currentSupply = SupplyLevel.valueOf(priceEntity.supplyLevel)
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

        recordPriceSnapshot(townId, goodId, newSellPrice, newBuyPrice, newSupply, now)

        TelemetryService.tryRecord(
            TelemetryEvent.MarketTransaction(
                timestamp = now,
                townId = townId,
                goodId = goodId.toString(),
                transactionType = transactionType,
                quantity = quantity,
                pricePerUnit = pricePerUnit,
            ),
        )

        val before = PriceSnapshot(sellPrice = priceEntity.sellPrice, supplyLevel = currentSupply)
        val after = PriceSnapshot(sellPrice = newSellPrice, supplyLevel = newSupply)
        MarketAnomalyDetector.detect(townId, goodId, good.baseValue, before, after, now).forEach {
            TelemetryService.tryRecord(it)
        }

        val updatedPlayer = database.playerDao().getPlayerSnapshot()!!
        database.playerDao().updatePlayer(
            updatedPlayer.copy(completedTradesCount = updatedPlayer.completedTradesCount + 1),
        )
    }

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
