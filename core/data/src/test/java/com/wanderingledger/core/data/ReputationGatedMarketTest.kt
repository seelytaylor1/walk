package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.testing.TestDatabaseFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies that [MarketRepository.observeMarket] filters out rep-gated goods
 * when the town's reputation with the player is below the required threshold.
 *
 * Rare goods (Medicines/Dyes/Charts) have minReputation = 60.
 * Hearthwick starts at reputation = 50 (below threshold).
 */
@RunWith(RobolectricTestRunner::class)
class ReputationGatedMarketTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var marketRepository: MarketRepository
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        val companionRepository = CompanionRepository(database)
        val rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository)
        marketRepository = MarketRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun playerWithLowRepDoesNotSeeRareGoods() =
        runTest {
            // Seed world; Hearthwick (townId=1) starts at reputation=50
            gameRepository.initializeNewGame(seed = 1L)

            // Hearthwick has Dyes (goodId=5) with minReputation=60 — should be hidden
            val market = marketRepository.observeMarket(townId = 1L).first()
            val goodIds = market.rows.map { it.good.goodId }

            assertFalse(
                "Dyes (goodId=5, minRep=60) should NOT appear for rep=50",
                goodIds.contains(5L),
            )
        }

    @Test
    fun playerWithHighRepSeesRareGoods() =
        runTest {
            // Seed world; Hearthwick (townId=1) starts at reputation=50
            gameRepository.initializeNewGame(seed = 1L)

            // Boost Hearthwick reputation to 60
            val town = database.townDao().getTownSnapshot(1L)!!
            database.townDao().updateTown(town.copy(reputation = 60))

            // Now Dyes (goodId=5, minReputation=60) should appear
            val market = marketRepository.observeMarket(townId = 1L).first()
            val goodIds = market.rows.map { it.good.goodId }

            assertTrue(
                "Dyes (goodId=5, minRep=60) SHOULD appear for rep=60",
                goodIds.contains(5L),
            )
        }

    @Test
    fun commonGoodsAlwaysVisibleRegardlessOfRep() =
        runTest {
            // Seed world; Hearthwick (townId=1) at reputation=50
            gameRepository.initializeNewGame(seed = 1L)

            val market = marketRepository.observeMarket(townId = 1L).first()
            val goodIds = market.rows.map { it.good.goodId }

            // Apples (goodId=1, minRep=0) should always be visible
            assertTrue(
                "Apples (goodId=1, minRep=0) should always appear",
                goodIds.contains(1L),
            )
        }
}
