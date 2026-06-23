package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.testing.TestDatabaseFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RumorFalseRateTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var rumorRepository: RumorRepository
    private lateinit var companionRepository: CompanionRepository
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository)
        runTest { gameRepository.initializeNewGame(seed = 1L) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** AC-12: ~50% of Rumors are false. Test with 100 seeded calls and check 30%–70% range. */
    @Test
    fun `AC-12 rumor false rate is approximately 50 percent`() = runTest {
        var falseCount = 0
        val total = 100
        for (seed in 1L..total) {
            rumorRepository.generateRumorForTownVisit(visitedTownId = 1L, seed = seed)
        }

        val rumors = rumorRepository.observeActiveRumors().first()
        falseCount = rumors.count { it.isFalse }

        // Allow a wide band (30%–70%) to account for RNG variance
        val falseRate = falseCount.toDouble() / rumors.size
        assertTrue(
            "Expected false rate between 30% and 70%, got ${(falseRate * 100).toInt()}%",
            falseRate in 0.30..0.70,
        )
    }

    /** AC-12: Rumors expire after 2 visits (expiryVisitsLeft = 2 on generation). */
    @Test
    fun `AC-12 town-visit rumors default to 2-visit expiry`() = runTest {
        rumorRepository.generateRumorForTownVisit(visitedTownId = 1L, seed = 42L)

        val rumors = rumorRepository.observeActiveRumors().first()
        assertTrue("Expected at least one rumor", rumors.isNotEmpty())
        val rumor = rumors.last()
        assertTrue(
            "Expected expiryVisitsLeft = 2, got ${rumor.expiryVisitsLeft}",
            rumor.expiryVisitsLeft == 2,
        )
    }
}
