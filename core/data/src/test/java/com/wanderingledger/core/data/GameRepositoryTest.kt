package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.InventoryItemEntity
import com.wanderingledger.core.database.OrderEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.testing.TestDatabaseFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameRepositoryTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var companionRepository: CompanionRepository
    private lateinit var rumorRepository: RumorRepository
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository, OrderRepository(database))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun initializeNewGameSeedsDatabase() =
        runTest {
            gameRepository.initializeNewGame()

            val player = gameRepository.observePlayerState().first()
            assertEquals("Ledger Keeper", player.name)
            assertEquals(1L, player.currentTownId)
        }

    @Test
    fun travelUpdatesPlayerLocation() =
        runTest {
            gameRepository.initializeNewGame()
            val player = database.playerDao().getPlayerSnapshot()!!
            database.playerDao().updatePlayer(player.copy(bankedSteps = 2000L))

            // Road 1: Hearthwick(1) -> Stoneford(2)
            gameRepository.travel(1L)

            val updatedPlayer = gameRepository.observePlayerState().first()
            assertEquals(2L, updatedPlayer.currentTownId)
        }

    @Test
    fun travelCompletesDeliveryOrderOnArrival() = runBlocking {
        gameRepository.initializeNewGame()
        // Give player enough steps to travel segment 1 (Hearthwick→Stoneford, 1000 steps)
        database.playerDao().updatePlayer(
            database.playerDao().getPlayerSnapshot()!!.copy(bankedSteps = 2000)
        )
        // Insert Delivery order: bring Iron (goodId=2) to Stoneford (townId=2)
        val orderId = database.orderDao().insertOrder(
            OrderEntity(
                issuingTownId = 2L,
                destinationTownId = 2L,
                goodId = 2L,
                quantity = 1,
                type = "Delivery",
                reputationReward = 5,
                deadlineVisitsLeft = 3,
            )
        )
        // Give player 1 Iron
        database.inventoryDao().addItem(
            InventoryItemEntity(playerId = 1L, goodId = 2L, quantity = 1)
        )

        val result = gameRepository.travel(segmentId = 1L, seed = 42L)

        assertTrue("Travel should succeed", result is TravelResult.Arrived)
        // Verify the specific order was deactivated (not isActive)
        // getActiveOrdersSnapshot returns only isActive=1 orders; our order should not appear
        val allActive = database.orderDao().getActiveOrdersSnapshot()
        assertTrue(
            "Completed order (id=$orderId) should be deactivated",
            allActive.none { it.orderId == orderId },
        )
        // Verify Stoneford gained rep
        val stoneford = database.townDao().getTownSnapshot(2L)
        assertEquals("Stoneford reputation should increase by 5", 55, stoneford?.reputation)
    }
}
