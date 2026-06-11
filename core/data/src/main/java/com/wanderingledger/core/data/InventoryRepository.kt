package com.wanderingledger.core.data

import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.model.Good
import com.wanderingledger.core.model.InventoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

// ── Domain types ─────────────────────────────────────────────────────────────

/**
 * A single inventory row enriched with the good's display name and base value.
 */
data class InventoryRow(
    val item: InventoryItem,
    val good: Good,
)

/**
 * Aggregate summary of the player's inventory state.
 */
data class InventorySummary(
    val totalItemsCarried: Int,
    val inventoryCapacity: Int,
    val gold: Long,
    val rows: List<InventoryRow>,
)

// ── Repository ────────────────────────────────────────────────────────────────

/**
 * Room-backed repository that exposes reactive inventory state.
 *
 * Reads from [InventoryDao] and [GoodDao] to enrich inventory items with
 * good names and base values for display.
 */
class InventoryRepository(
    private val database: WanderingLedgerDatabase,
) {
    /**
     * Stream the player's inventory as a list of enriched [InventoryItem]s.
     * Emits whenever inventory changes.
     */
    fun observeInventory(playerId: Long): Flow<List<InventoryItem>> =
        database.inventoryDao().listInventory(playerId).combine(
            database.goodDao().listGoods(),
        ) { items, _ ->
            items.map { entity ->
                InventoryItem(
                    id = entity.id,
                    playerId = entity.playerId,
                    goodId = entity.goodId,
                    quantity = entity.quantity,
                    isSealed = entity.isSealed,
                )
            }
        }

    /**
     * Stream a full [InventorySummary] for [playerId], including gold, capacity,
     * total items carried, and each row enriched with good metadata.
     * Emits whenever inventory, player gold, or goods change.
     */
    fun observeInventorySummary(playerId: Long): Flow<InventorySummary> =
        combine(
            database.inventoryDao().listInventory(playerId),
            database.playerDao().getPlayer().filterNotNull(),
            database.goodDao().listGoods(),
        ) { items, player, goods ->
            val goodsById = goods.associateBy { it.goodId }

            val rows =
                items.mapNotNull { entity ->
                    val good = goodsById[entity.goodId] ?: return@mapNotNull null
                    InventoryRow(
                        item =
                            InventoryItem(
                                id = entity.id,
                                playerId = entity.playerId,
                                goodId = entity.goodId,
                                quantity = entity.quantity,
                                isSealed = entity.isSealed,
                            ),
                        good =
                            Good(
                                goodId = good.goodId,
                                name = good.name,
                                baseValue = good.baseValue,
                                isContraband = good.isContraband,
                            ),
                    )
                }

            InventorySummary(
                totalItemsCarried = items.sumOf { it.quantity },
                inventoryCapacity = player.inventorySlots,
                gold = player.gold,
                rows = rows,
            )
        }
}
