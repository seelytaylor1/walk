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
class CompanionRecruitmentGateTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var companionRepository: CompanionRepository
    private lateinit var rumorRepository: RumorRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var marketRepository: MarketRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository)
        marketRepository = MarketRepository(database)
        runTest { gameRepository.initializeNewGame(seed = 1L) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** AC-14: Recruitment blocked before 3 trades. */
    @Test
    fun `AC-14 recruitment blocked before 3 completed trades`() = runTest {
        val miraId = 1L // Mira the Scout, located at Town 1 (Hearthwick)

        val result = companionRepository.recruitCompanion(miraId)
        assertTrue(
            "Expected NotEnoughTrades before 3 trades, got $result",
            result is RecruitmentResult.NotEnoughTrades,
        )
    }

    /** AC-14: Recruitment allowed at exactly 3 completed trades. */
    @Test
    fun `AC-14 recruitment allowed after 3 completed trades`() = runTest {
        val miraId = 1L

        // Complete 3 trades (buy 3 Apples from Hearthwick market)
        repeat(3) {
            marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        }

        val result = companionRepository.recruitCompanion(miraId)
        assertEquals(RecruitmentResult.Success, result)
    }

    /** AC-14: completedTradesCount increments on both buy and sell. */
    @Test
    fun `AC-14 trade count increments on buy and sell`() = runTest {
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        val afterBuy = gameRepository.observePlayerState().first()
        assertEquals(1, afterBuy.completedTradesCount)

        marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
        val afterSell = gameRepository.observePlayerState().first()
        assertEquals(2, afterSell.completedTradesCount)
    }
}
