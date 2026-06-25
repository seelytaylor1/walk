package com.wanderingledger.core.data

import com.wanderingledger.core.database.OrderEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import kotlin.random.Random

data class OrderCompletion(
    val orderId: Long,
    val issuingTownId: Long,
    val reputationReward: Int,
    val goodName: String,
)

class OrderRepository(private val database: WanderingLedgerDatabase) {

    companion object {
        const val ORDER_CAP_PER_TOWN = 3
        const val DELIVERY_DEADLINE = 3
        const val ROUTE_DEADLINE = 4
        const val DELIVERY_REP = 5
        const val ROUTE_REP = 8
        const val ORDER_QTY_MIN = 1
        const val ORDER_QTY_MAX = 3
    }

    suspend fun generateOrdersForTown(townId: Long, seed: Long) {
        val currentCount = database.orderDao().countActiveOrdersForTown(townId)
        if (currentCount >= ORDER_CAP_PER_TOWN) return

        val rng = Random(seed)
        val prices = database.townPriceDao().listPricesSnapshotForTown(townId)
        val scarce = prices.filter { it.supplyLevel == "Scarce" }
        val abundant = prices.filter { it.supplyLevel == "Abundant" }

        var toGenerate = ORDER_CAP_PER_TOWN - currentCount
        var attempts = 0
        val maxAttempts = toGenerate * 5

        while (toGenerate > 0 && attempts < maxAttempts) {
            attempts++
            val tryRoute = rng.nextBoolean()

            if (tryRoute && abundant.isNotEmpty()) {
                val priceEntry = abundant.random(rng)
                // Exclude contraband goods from Route orders
                val good = database.goodDao().getGoodSnapshot(priceEntry.goodId)
                if (good?.isContraband == true) continue
                val destinations = database.townDao().getTownsDemanding(priceEntry.goodId, excludeTownId = townId)
                if (destinations.isEmpty()) continue
                val destination = destinations.random(rng)
                val qty = rng.nextInt(ORDER_QTY_MIN, ORDER_QTY_MAX + 1)
                database.orderDao().insertOrder(
                    OrderEntity(
                        issuingTownId = townId,
                        destinationTownId = destination.townId,
                        goodId = priceEntry.goodId,
                        quantity = qty,
                        type = "Route",
                        reputationReward = ROUTE_REP,
                        deadlineVisitsLeft = ROUTE_DEADLINE,
                    ),
                )
                toGenerate--
            } else if (scarce.isNotEmpty()) {
                val priceEntry = scarce.random(rng)
                val qty = rng.nextInt(ORDER_QTY_MIN, ORDER_QTY_MAX + 1)
                database.orderDao().insertOrder(
                    OrderEntity(
                        issuingTownId = townId,
                        destinationTownId = townId,
                        goodId = priceEntry.goodId,
                        quantity = qty,
                        type = "Delivery",
                        reputationReward = DELIVERY_REP,
                        deadlineVisitsLeft = DELIVERY_DEADLINE,
                    ),
                )
                toGenerate--
            }
            // If both abundant and scarce are empty, no orders can be generated
            if (abundant.isEmpty() && scarce.isEmpty()) break
        }
    }

    suspend fun checkAndCompleteOrders(arrivedTownId: Long, playerId: Long): List<OrderCompletion> {
        val completions = mutableListOf<OrderCompletion>()
        val activeOrders = database.orderDao().getActiveOrdersSnapshot()

        for (order in activeOrders) {
            val completionTownId = when (order.type) {
                "Delivery" -> order.issuingTownId
                "Route" -> order.destinationTownId
                else -> continue
            }
            if (completionTownId != arrivedTownId) continue

            val item = database.inventoryDao().getItemSnapshot(playerId, order.goodId)
            if (item == null || item.quantity < order.quantity) continue

            // Deduct inventory
            val newQty = item.quantity - order.quantity
            if (newQty <= 0) {
                database.inventoryDao().removeItem(item.id)
            } else {
                database.inventoryDao().updateItem(item.copy(quantity = newQty))
            }

            // Award reputation to issuing town
            database.townDao().addReputation(order.issuingTownId, order.reputationReward)

            // Deactivate order
            database.orderDao().deactivateOrder(order.orderId)

            val goodName = database.goodDao().getGoodSnapshot(order.goodId)?.name ?: "goods"
            completions.add(
                OrderCompletion(
                    orderId = order.orderId,
                    issuingTownId = order.issuingTownId,
                    reputationReward = order.reputationReward,
                    goodName = goodName,
                ),
            )
        }
        return completions
    }

    suspend fun tickDeadlines() {
        database.orderDao().decrementAllActiveDeadlines()
        database.orderDao().expireOverdueOrders()
    }
}
