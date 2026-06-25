package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.InventoryItemEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.testing.TestDatabaseFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InspectionIntegrationTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        gameRepository = GameRepository(
            database,
            RumorRepository(database),
            CompanionRepository(database),
            OrderRepository(database),
        )
        gameRepository.initializeNewGame(seed = 1L)
        // Give player enough steps to travel segment 1 (Hearthwick→Stoneford, 1000 steps)
        database.playerDao().updatePlayer(
            database.playerDao().getPlayerSnapshot()!!.copy(bankedSteps = 10_000L)
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun noInspectionWhenPlayerHasNoContraband() = runTest {
        // Player has only Apples (goodId=1, not contraband)
        database.inventoryDao().addItem(
            InventoryItemEntity(playerId = 1L, goodId = 1L, quantity = 3)
        )

        val result = gameRepository.travel(segmentId = 1L, seed = 1L)

        assertTrue("Travel should succeed", result is TravelResult.Arrived)

        val events = database.eventLogDao().listRecentEvents(20).first()
        val inspectionEvent = events.firstOrNull { it.type == "inspection" }
        assertTrue("No inspection event should be logged when no contraband", inspectionEvent == null)
    }

    @Test
    fun contrabandConfiscatedWhenInspectedWithoutRogue() = runTest {
        // Add Smuggled Spirits (goodId=7, contraband) to player inventory
        database.inventoryDao().addItem(
            InventoryItemEntity(playerId = 1L, goodId = 7L, quantity = 2)
        )

        // Find a seed where inspection triggers without Rogue (40% base chance)
        val inspectionSeed = (1L..200L).firstOrNull { s ->
            InspectionEngine.rollInspection(InspectionEngine.BASE_INSPECTION_CHANCE, seed = s + 7919L)
        }
        requireNotNull(inspectionSeed) { "Could not find a seed that triggers 40% inspection in 200 tries" }

        val result = gameRepository.travel(segmentId = 1L, seed = inspectionSeed)

        assertTrue("Travel should succeed", result is TravelResult.Arrived)

        val contrabandRemaining = database.inventoryDao().listContrabandItemsSnapshot(playerId = 1L)
        assertTrue("Contraband should be confiscated on inspection", contrabandRemaining.isEmpty())

        val events = database.eventLogDao().listRecentEvents(20).first()
        val inspectionEvent = events.firstOrNull { it.type == "inspection" }
        assertTrue("Inspection event should be logged", inspectionEvent != null)
        assertTrue(
            "Inspection event result should mention confiscation",
            inspectionEvent!!.result.contains("confiscated", ignoreCase = true),
        )
    }

    @Test
    fun contrabandSurvivesWhenInspectionMisses() = runTest {
        database.inventoryDao().addItem(
            InventoryItemEntity(playerId = 1L, goodId = 7L, quantity = 1)
        )

        // Find a seed where inspection does NOT trigger at 40% chance
        val safeSeed = (1L..200L).firstOrNull { s ->
            !InspectionEngine.rollInspection(InspectionEngine.BASE_INSPECTION_CHANCE, seed = s + 7919L)
        }
        requireNotNull(safeSeed) { "Could not find a seed that misses 40% inspection in 200 tries" }

        val result = gameRepository.travel(segmentId = 1L, seed = safeSeed)

        assertTrue("Travel should succeed", result is TravelResult.Arrived)

        val contrabandRemaining = database.inventoryDao().listContrabandItemsSnapshot(playerId = 1L)
        assertEquals("Contraband should survive when inspection misses", 1, contrabandRemaining.sumOf { it.quantity })

        val events = database.eventLogDao().listRecentEvents(20).first()
        val inspectionEvent = events.firstOrNull { it.type == "inspection" }
        assertTrue("No inspection event should be logged when inspection misses", inspectionEvent == null)
    }
}
