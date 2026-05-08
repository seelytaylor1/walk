package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
class RumorRepositoryTest {

    private lateinit var database: WanderingLedgerDatabase
    private lateinit var companionRepository: CompanionRepository
    private lateinit var encounterRepository: EncounterRepository
    private lateinit var rumorRepository: RumorRepository
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        encounterRepository = EncounterRepository(database, companionRepository)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, encounterRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun generateRumorOnTownVisit() = runTest {
        gameRepository.initializeNewGame(seed = 1L)
        
        // Initial state has visited Hearthwick (1), current town is 1.
        // We visit Stoneford (2)
        rumorRepository.generateRumorForTownVisit(2L)
        
        val active = rumorRepository.observeActiveRumors().first()
        assertEquals(1, active.size)
        // Rumor should be about Hearthwick (1) or Mistfall (3)
        assertTrue(active.first().text.contains("Hearthwick") || active.first().text.contains("Mistfall"))
    }

    @Test
    fun generateRumorOnRoadEvent() = runTest {
        gameRepository.initializeNewGame(seed = 1L)
        
        // Road 1: Hearthwick -> Stoneford, event pool ["merchant-cart"]
        rumorRepository.generateRumorFromRoadEvent(1L)
        
        val active = rumorRepository.observeActiveRumors().first()
        assertEquals(1, active.size)
        assertTrue(active.first().text.contains("merchant cart") || active.first().text.contains("shortcut"))
    }

    @Test
    fun travelGeneratesTwoRumors() = runTest {
        gameRepository.initializeNewGame(seed = 1L)
        val player = database.playerDao().getPlayerSnapshot()!!
        database.playerDao().updatePlayer(player.copy(bankedSteps = 1000L))
        
        // Travel 1 -> 2
        gameRepository.travel(1L)
        
        val active = rumorRepository.observeActiveRumors().first()
        // One from road event, one from town visit
        assertEquals(2, active.size)
    }

    @Test
    fun addAndObserveRumors() = runTest {
        rumorRepository.addRumor("Test Rumor", expiryVisits = 3)
        
        val active = rumorRepository.observeActiveRumors().first()
        assertEquals(1, active.size)
        assertEquals("Test Rumor", active.first().text)
        assertEquals(3, active.first().expiryVisitsLeft)
    }

    @Test
    fun rumorExpiresAfterTravel() = runTest {
        gameRepository.initializeNewGame(seed = 1L)
        rumorRepository.addRumor("Expiring soon", expiryVisits = 1)
        
        // Traveling once decrements expiry for all active rumors
        val player = database.playerDao().getPlayerSnapshot()!!
        database.playerDao().updatePlayer(player.copy(bankedSteps = 1000L))
        
        gameRepository.travel(segmentId = 1L) // 1 -> 2
        
        val active = rumorRepository.observeActiveRumors().first()
        val oldRumor = active.find { it.text == "Expiring soon" }
        assertTrue("Old rumor should have expired", oldRumor == null)
        
        // Should have 2 new rumors from the travel
        assertEquals(2, active.size)
    }

    @Test
    fun rumorPersistenceAndDecrement() = runTest {
        gameRepository.initializeNewGame(seed = 1L)
        rumorRepository.addRumor("Long rumor", expiryVisits = 5)
        
        val player = database.playerDao().getPlayerSnapshot()!!
        database.playerDao().updatePlayer(player.copy(bankedSteps = 1000L))
        
        gameRepository.travel(segmentId = 1L) // 1 -> 2
        
        val active = rumorRepository.observeActiveRumors().first()
        val oldRumor = active.find { it.text == "Long rumor" }
        assertTrue("Old rumor should still exist", oldRumor != null)
        assertEquals(4, oldRumor!!.expiryVisitsLeft)
    }

    @Test
    fun falseRumorTagging() = runTest {
        rumorRepository.addRumor("True rumor", isFalse = false)
        rumorRepository.addRumor("False rumor", isFalse = true)
        
        val active = rumorRepository.observeActiveRumors().first()
        val trueRumor = active.first { it.text == "True rumor" }
        val falseRumor = active.first { it.text == "False rumor" }
        
        assertTrue("True rumor should not be marked false", !trueRumor.isFalse)
        assertTrue("False rumor should be marked false", falseRumor.isFalse)
    }
}
