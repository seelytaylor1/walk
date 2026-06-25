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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContrabandSeedTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var companionRepository: CompanionRepository
    private lateinit var rumorRepository: RumorRepository
    private lateinit var orderRepository: OrderRepository
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        rumorRepository = RumorRepository(database)
        orderRepository = OrderRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository, orderRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun caelExistsAsNonActiveRogueCompanionAtMistfall() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Query recruitableCompanions (non-active companions) using Flow.first()
        val recruitableCompanions = database.companionDao().listRecruitableCompanions().first()
        val cael = recruitableCompanions.find { it.companionId == 3L }

        assertTrue("Cael should exist in recruitable companions", cael != null)
        assertEquals("Cael should be named 'Cael'", "Cael", cael?.name)
        assertEquals("Cael should be a Rogue", "Rogue", cael?.role)
        assertEquals("Cael should have combatPower of 2", 2, cael?.combatPower)
        assertEquals("Cael should have bondLevel of 0", 0, cael?.bondLevel)
        assertEquals("Cael should be at Mistfall (townId=3)", 3L, cael?.locationTownId)
        assertFalse("Cael should not be active (isActive=false)", cael?.isActive == true)
    }

    @Test
    fun smuggledSpiritsHasContrabandFlag() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        val smuggledSpirits = database.goodDao().getGoodSnapshot(7L)

        assertFalse("Smuggled Spirits should exist", smuggledSpirits == null)
        assertEquals("Smuggled Spirits should have correct name", "Smuggled Spirits", smuggledSpirits?.name)
        assertTrue("Smuggled Spirits should be contraband", smuggledSpirits?.isContraband == true)
    }

    @Test
    fun stolenRelicsHasContrabandFlag() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        val stolenRelics = database.goodDao().getGoodSnapshot(8L)

        assertFalse("Stolen Relics should exist", stolenRelics == null)
        assertEquals("Stolen Relics should have correct name", "Stolen Relics", stolenRelics?.name)
        assertTrue("Stolen Relics should be contraband", stolenRelics?.isContraband == true)
    }

    @Test
    fun listContrabandItemsSnapshotReturnsEmptyWhenPlayerHasNoContraband() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        val contrabandItems = database.inventoryDao().listContrabandItemsSnapshot(playerId = 1L)

        assertTrue("Should return empty list when player has no contraband", contrabandItems.isEmpty())
    }

    @Test
    fun listContrabandItemsSnapshotReturnsContrabandWhenPlayerHasIt() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Add Smuggled Spirits and Stolen Relics to player inventory
        database.inventoryDao().addItem(
            InventoryItemEntity(playerId = 1L, goodId = 7L, quantity = 2)
        )
        database.inventoryDao().addItem(
            InventoryItemEntity(playerId = 1L, goodId = 8L, quantity = 1)
        )

        val contrabandItems = database.inventoryDao().listContrabandItemsSnapshot(playerId = 1L)

        assertEquals("Should return 2 contraband items", 2, contrabandItems.size)
        assertTrue("Should include Smuggled Spirits", contrabandItems.any { it.goodId == 7L })
        assertTrue("Should include Stolen Relics", contrabandItems.any { it.goodId == 8L })

        // Verify quantities
        val smuggledSpirits = contrabandItems.find { it.goodId == 7L }
        assertEquals("Smuggled Spirits quantity should be 2", 2, smuggledSpirits?.quantity)

        val stolenRelics = contrabandItems.find { it.goodId == 8L }
        assertEquals("Stolen Relics quantity should be 1", 1, stolenRelics?.quantity)
    }

    @Test
    fun listContrabandItemsSnapshotIgnoresNonContrabandGoods() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Add both contraband and regular goods to player inventory
        database.inventoryDao().addItem(
            InventoryItemEntity(playerId = 1L, goodId = 1L, quantity = 5)  // Apples (not contraband)
        )
        database.inventoryDao().addItem(
            InventoryItemEntity(playerId = 1L, goodId = 7L, quantity = 2)  // Smuggled Spirits (contraband)
        )
        database.inventoryDao().addItem(
            InventoryItemEntity(playerId = 1L, goodId = 2L, quantity = 3)  // Iron (not contraband)
        )

        val contrabandItems = database.inventoryDao().listContrabandItemsSnapshot(playerId = 1L)

        assertEquals("Should return only 1 contraband item", 1, contrabandItems.size)
        assertEquals("Should only include Smuggled Spirits", 7L, contrabandItems[0].goodId)
    }
}
