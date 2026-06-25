package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.InventoryItemEntity
import com.wanderingledger.core.database.OrderEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.testing.TestDatabaseFactory
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrderRepositoryTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var orderRepository: OrderRepository

    @Before
    fun setUp() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            database = TestDatabaseFactory.createSeededInMemoryDatabase(context)
            orderRepository = OrderRepository(database)
        }

    @After
    fun tearDown() {
        database.close()
    }

    // 1. generateOrdersForTown generates orders (up to 3 cap)
    @Test
    fun generateOrdersFilledToCapForTown() =
        runTest {
            orderRepository.generateOrdersForTown(townId = 1L, seed = 1L)
            val orders = database.orderDao().getActiveOrdersForTownSnapshot(townId = 1L)
            assertTrue("Should generate up to 3 orders", orders.size <= 3)
            assertTrue("Should generate at least 1 order", orders.isNotEmpty())
            orders.forEach { order ->
                assertTrue(
                    "Order type must be Delivery or Route",
                    order.type == "Delivery" || order.type == "Route",
                )
                assertTrue("Order quantity must be in range 1–3", order.quantity in 1..3)
                assertTrue("Order goodId must be positive", order.goodId > 0)
            }
        }

    // 2. generateOrdersForTown respects the 3-order cap (doesn't insert when at cap)
    @Test
    fun generateOrdersDoesNotExceedCapOnRepeatCall() =
        runTest {
            orderRepository.generateOrdersForTown(townId = 1L, seed = 1L)
            orderRepository.generateOrdersForTown(townId = 1L, seed = 2L) // second visit
            val orders = database.orderDao().getActiveOrdersForTownSnapshot(townId = 1L)
            assertTrue("Orders should never exceed cap of 3", orders.size <= 3)
        }

    // 3. checkAndCompleteOrders completes a fulfilled delivery order and awards rep
    @Test
    fun deliveryOrderCompletesWhenPlayerArrivesWithGoods() =
        runTest {
            // Insert a Delivery order: player must bring goodId=2 (Iron) to Hearthwick (town 1)
            database.orderDao().insertOrder(
                OrderEntity(
                    issuingTownId = 1L,
                    destinationTownId = 1L,
                    goodId = 2L,
                    quantity = 1,
                    type = "Delivery",
                    reputationReward = 5,
                    deadlineVisitsLeft = 3,
                ),
            )
            // Give player 1 unit of Iron
            database.inventoryDao().addItem(
                InventoryItemEntity(playerId = 1L, goodId = 2L, quantity = 1),
            )

            val completions = orderRepository.checkAndCompleteOrders(arrivedTownId = 1L, playerId = 1L)

            assertEquals("One order should complete", 1, completions.size)
            assertEquals("Reputation reward is +5 for Delivery", 5, completions.first().reputationReward)

            val remainingOrders = database.orderDao().getActiveOrdersForTownSnapshot(townId = 1L)
            assertTrue("Completed order should be deactivated", remainingOrders.isEmpty())

            val inventory = database.inventoryDao().getItemSnapshot(playerId = 1L, goodId = 2L)
            assertTrue("Iron should be deducted from inventory", inventory == null || inventory.quantity == 0)
        }

    // 4. checkAndCompleteOrders does NOT complete an unfulfilled order
    @Test
    fun routeOrderCompletesAtDestinationNotAtSource() =
        runTest {
            // Route order: Stoneford (townId=2) issues it, destination is Mistfall (townId=3)
            database.orderDao().insertOrder(
                OrderEntity(
                    issuingTownId = 2L,
                    destinationTownId = 3L,
                    goodId = 2L,
                    quantity = 1,
                    type = "Route",
                    reputationReward = 8,
                    deadlineVisitsLeft = 4,
                ),
            )
            database.inventoryDao().addItem(
                InventoryItemEntity(playerId = 1L, goodId = 2L, quantity = 1),
            )

            // Arriving at source town should NOT complete the order
            val atSource = orderRepository.checkAndCompleteOrders(arrivedTownId = 2L, playerId = 1L)
            assertEquals("Route should not complete at source", 0, atSource.size)

            // Arriving at destination SHOULD complete it
            val atDest = orderRepository.checkAndCompleteOrders(arrivedTownId = 3L, playerId = 1L)
            assertEquals("Route should complete at destination", 1, atDest.size)
            assertEquals("Reputation reward is +8 for Route", 8, atDest.first().reputationReward)
        }

    // 5. tickDeadlines decrements deadlines and expires orders at 0
    @Test
    fun tickDeadlinesDecrementsAndExpires() =
        runTest {
            database.orderDao().insertOrder(
                OrderEntity(
                    issuingTownId = 1L,
                    destinationTownId = 1L,
                    goodId = 1L,
                    quantity = 1,
                    type = "Delivery",
                    reputationReward = 5,
                    deadlineVisitsLeft = 1,
                ),
            )
            orderRepository.tickDeadlines()

            val orders = database.orderDao().getActiveOrdersForTownSnapshot(townId = 1L)
            assertTrue("Order with 1 deadline remaining should expire after tick", orders.isEmpty())
        }
}
