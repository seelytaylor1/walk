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

@RunWith(RobolectricTestRunner::class)
class ContrabandMarketTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var marketRepository: MarketRepository
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        marketRepository = MarketRepository(database)
        val companionRepository = CompanionRepository(database)
        val rumorRepository = RumorRepository(database)
        val orderRepository = OrderRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository, orderRepository)
        gameRepository.initializeNewGame(seed = 1L)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun contrabandGoodsHiddenWithoutRogue() = runTest {
        // No Rogue active; Smuggled Spirits (goodId=7) and Stolen Relics (goodId=8) both priced
        // at Hearthwick (townId=1) with minReputation=0 but should be hidden without Rogue
        val market = marketRepository.observeMarket(townId = 1L).first()
        val goodIds = market.rows.map { it.good.goodId }
        assertFalse("Smuggled Spirits should not appear without Rogue", goodIds.contains(7L))
        assertFalse("Stolen Relics should not appear without Rogue", goodIds.contains(8L))
    }

    @Test
    fun contrabandGoodsVisibleWithActiveRogue() = runTest {
        // Activate Cael (Rogue, companionId=3)
        val recruitableCompanions = database.companionDao().listRecruitableCompanions().first()
        val cael = recruitableCompanions.find { it.companionId == 3L }!!
        database.companionDao().upsertCompanion(cael.copy(isActive = true))

        val market = marketRepository.observeMarket(townId = 1L).first()
        val goodIds = market.rows.map { it.good.goodId }
        assertTrue("Smuggled Spirits should appear with active Rogue", goodIds.contains(7L))
    }

    @Test
    fun baseGoodsAlwaysVisibleRegardlessOfRogue() = runTest {
        val market = marketRepository.observeMarket(townId = 1L).first()
        val goodIds = market.rows.map { it.good.goodId }
        // Apples (1) and Iron (2) are at Hearthwick; both should always be visible
        assertTrue("Apples always visible", goodIds.contains(1L))
        assertTrue("Iron always visible", goodIds.contains(2L))
    }
}
