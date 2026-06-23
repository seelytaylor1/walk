package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.steptracker.StepSource
import com.wanderingledger.core.steptracker.StepTrackerService
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

/**
 * Acceptance criteria tests for Milestone 2: Trading, Events, and Camping.
 * Each test maps to a specific AC item in GitHub issues #11, #13, or #15.
 *
 * SeedWorld facts (from SeedWorld.kt):
 * - Hearthwick (townId=1): Apples (goodId=1, sellPrice=5, buyPrice=3), Iron (goodId=2, sellPrice=27, buyPrice=17)
 * - Stoneford (townId=2): Iron (goodId=2, sellPrice=12, buyPrice=7), Silk (goodId=3, sellPrice=45, buyPrice=29)
 * - Mistfall (townId=3): Silk (goodId=3, sellPrice=21, buyPrice=13), Apples (goodId=1, sellPrice=12, buyPrice=7)
 * - Player starts at Hearthwick with 50 gold.
 *
 * Note: "sellPrice" = what the town charges the player (player buys at this price).
 *       "buyPrice"  = what the town pays the player (player sells at this price).
 * Note: Issue #15 (CampStateDetector) tests live in feature:journey/CampStateTest.kt
 *       because CampStateDetector is not a dependency of core:data.
 */
@RunWith(RobolectricTestRunner::class)
class Milestone2AcceptanceCriteriaTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var companionRepository: CompanionRepository
    private lateinit var rumorRepository: RumorRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var marketRepository: MarketRepository
    private lateinit var stepBankRepository: RoomStepBankRepository
    private lateinit var stepTrackerService: StepTrackerService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository)
        marketRepository = MarketRepository(database)
        stepBankRepository = RoomStepBankRepository(database)
        stepTrackerService = StepTrackerService(stepBankRepository)
        runTest { gameRepository.initializeNewGame(seed = 1L) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Issue #11: Town trading ───────────────────────────────────────────────

    /**
     * AC-11-1: Buying a Good deducts gold and adds to inventory.
     * Apples at Hearthwick: player buys at town's sellPrice=5.
     */
    @Test
    fun `AC-11-1 buying a good deducts gold and adds to inventory`() = runTest {
        val before = gameRepository.observePlayerState().first()

        // Hearthwick: Apples (goodId=1), player buys at town's sellPrice=5
        val result = marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        assertTrue("Expected BuyResult.Success, got $result", result is BuyResult.Success)

        val after = gameRepository.observePlayerState().first()
        assertEquals(before.gold - 5L, after.gold)

        val inventory = database.inventoryDao().listInventory(playerId = 1L).first()
        val apples = inventory.firstOrNull { it.goodId == 1L }
        assertTrue("Player should have Apples in inventory", apples != null && apples.quantity >= 1)
    }

    /**
     * AC-11-2: Selling a Good adds gold and removes from inventory.
     * Buys 2 Apples then sells 1 back. After the buy the price updates, so
     * we read the actual buyPrice from the market rather than hardcoding it.
     */
    @Test
    fun `AC-11-2 selling a good adds gold and removes from inventory`() = runTest {
        // Buy 2 Apples first so we have inventory to sell
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 2)

        val beforeSell = gameRepository.observePlayerState().first()

        // Read the current buyPrice (what the town pays us) before selling
        val market = marketRepository.observeMarket(townId = 1L).first()
        val applesRow = market.rows.firstOrNull { it.good.goodId == 1L }
        requireNotNull(applesRow) { "Apples should be in Hearthwick market" }
        val expectedEarned = applesRow.townPrice.buyPrice

        val result = marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
        assertTrue("Expected SellResult.Success, got $result", result is SellResult.Success)

        val after = gameRepository.observePlayerState().first()
        assertEquals(beforeSell.gold + expectedEarned, after.gold)

        val inventory = database.inventoryDao().listInventory(playerId = 1L).first()
        val apples = inventory.firstOrNull { it.goodId == 1L }
        assertEquals(1, apples?.quantity ?: 0) // had 2, sold 1
    }

    /**
     * AC-11-3: A profitable trade route exists (buy low in one town, sell high in another).
     * Iron at Stoneford (sellPrice=12 player pays) vs Iron at Hearthwick (buyPrice=17 player earns).
     * Stoneford's sellPrice < Hearthwick's buyPrice confirms the spread is profitable.
     */
    @Test
    fun `AC-11-3 profitable trade route earns gold`() = runTest {
        val stonefordMarket = marketRepository.observeMarket(townId = 2L).first()
        val ironAtStoneford = stonefordMarket.rows.firstOrNull { it.good.goodId == 2L }
        requireNotNull(ironAtStoneford) { "Iron should be available at Stoneford" }

        val hearthwickMarket = marketRepository.observeMarket(townId = 1L).first()
        val ironAtHearthwick = hearthwickMarket.rows.firstOrNull { it.good.goodId == 2L }
        requireNotNull(ironAtHearthwick) { "Iron should be available at Hearthwick" }

        // Buying Iron at Stoneford (player pays sellPrice) and selling at Hearthwick
        // (player earns buyPrice) is profitable when Stoneford sellPrice < Hearthwick buyPrice.
        assertTrue(
            "Iron should be cheaper to buy at Stoneford (sellPrice=${ironAtStoneford.townPrice.sellPrice}) " +
                "than to sell at Hearthwick (buyPrice=${ironAtHearthwick.townPrice.buyPrice})",
            ironAtStoneford.townPrice.sellPrice < ironAtHearthwick.townPrice.buyPrice,
        )
    }

    /**
     * AC-11-4: completedTradesCount increments after each successful buy or sell.
     */
    @Test
    fun `AC-11-4 completedTradesCount increments on each successful trade`() = runTest {
        val before = gameRepository.observePlayerState().first()
        assertEquals(0, before.completedTradesCount)

        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        val afterBuy = gameRepository.observePlayerState().first()
        assertEquals(1, afterBuy.completedTradesCount)

        marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
        val afterSell = gameRepository.observePlayerState().first()
        assertEquals(2, afterSell.completedTradesCount)
    }

    /**
     * AC-11-5: recruitCompanion fails with NotEnoughTrades until 3 trades are completed.
     */
    @Test
    fun `AC-11-5 companion recruitment blocked until 3 trades completed`() = runTest {
        // 0 trades: should fail
        val result0 = companionRepository.recruitCompanion(companionId = 1L)
        assertTrue(
            "Expected NotEnoughTrades before any trades, got $result0",
            result0 is RecruitmentResult.NotEnoughTrades,
        )

        // 1 trade: still blocked
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        val result1 = companionRepository.recruitCompanion(companionId = 1L)
        assertTrue(
            "Expected NotEnoughTrades after 1 trade, got $result1",
            result1 is RecruitmentResult.NotEnoughTrades,
        )

        // 2 trades: still blocked
        marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
        val result2 = companionRepository.recruitCompanion(companionId = 1L)
        assertTrue(
            "Expected NotEnoughTrades after 2 trades, got $result2",
            result2 is RecruitmentResult.NotEnoughTrades,
        )

        // 3rd trade: buy again
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        val result3 = companionRepository.recruitCompanion(companionId = 1L)
        assertTrue(
            "Expected Success after 3 trades, got $result3",
            result3 is RecruitmentResult.Success,
        )
    }

    // ── Issue #13: Ledger event log ───────────────────────────────────────────

    /**
     * AC-13-1: Travel events are logged to the EventLog.
     * After a successful travel, the event_logs table contains an "arrival" entry.
     */
    @Test
    fun `AC-13-1 travel appends an arrival event to the event log`() = runTest {
        stepTrackerService.recordSensorDelta(count = 1000, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val shortRoad = roads.first { it.toTownId == 2L }
        val result = gameRepository.travel(shortRoad.segmentId)

        assertTrue("Expected TravelResult.Arrived, got $result", result is TravelResult.Arrived)

        val events = database.eventLogDao().listRecentEvents(10).first()
        assertTrue("Expected at least one event log entry after travel", events.isNotEmpty())
        val arrivalEvent = events.firstOrNull { it.type == "arrival" }
        assertTrue("Expected an 'arrival' event in the log", arrivalEvent != null)
    }

    /**
     * AC-13-2: The arrival event log entry includes the destination town ID in its metadata.
     */
    @Test
    fun `AC-13-2 arrival event log meta includes destination townId`() = runTest {
        stepTrackerService.recordSensorDelta(count = 1000, source = StepSource.Simulation)

        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val shortRoad = roads.first { it.toTownId == 2L }
        gameRepository.travel(shortRoad.segmentId)

        val events = database.eventLogDao().listRecentEvents(10).first()
        val arrivalEvent = events.firstOrNull { it.type == "arrival" }
        requireNotNull(arrivalEvent) { "Expected an arrival event" }
        assertTrue(
            "Arrival event meta should reference toTownId=2, got: ${arrivalEvent.meta}",
            arrivalEvent.meta.contains("\"toTownId\":2"),
        )
    }

    /**
     * AC-13-3: Event log persists across DB reads (simulating a restart).
     * A fresh Flow read after travel still shows the arrival event.
     */
    @Test
    fun `AC-13-3 event log persists after travel (survives restart simulation)`() = runTest {
        stepTrackerService.recordSensorDelta(count = 1000, source = StepSource.Simulation)
        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        gameRepository.travel(roads.first { it.toTownId == 2L }.segmentId)

        // Re-query directly from the DAO (same DB, simulating cold-start read)
        val events = database.eventLogDao().listRecentEvents(10).first()
        assertTrue(
            "Arrival event should persist in the event log",
            events.any { it.type == "arrival" },
        )
    }

    /**
     * AC-13-4: Failed travel (NotEnoughSteps) does NOT add an event log entry.
     */
    @Test
    fun `AC-13-4 failed travel does not log an arrival event`() = runTest {
        // 0 steps banked — travel must fail
        val roads = gameRepository.observeRoadsFromCurrentTown().first()
        val result = gameRepository.travel(roads.first { it.toTownId == 2L }.segmentId)

        assertTrue("Expected NotEnoughSteps, got $result", result is TravelResult.NotEnoughSteps)

        val events = database.eventLogDao().listRecentEvents(10).first()
        val arrivalEvent = events.firstOrNull { it.type == "arrival" }
        assertTrue("Failed travel should not produce an arrival event", arrivalEvent == null)
    }
}
