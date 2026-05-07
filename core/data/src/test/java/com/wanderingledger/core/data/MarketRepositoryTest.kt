package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.model.SupplyLevel
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
 * Integration tests for [MarketRepository] using an in-memory Room database.
 *
 * Covers:
 * - Buy success: gold deducted, inventory updated, supply decreases, price history recorded.
 * - Buy failure: not enough gold, inventory full, good not available.
 * - Sell success: gold added, inventory updated, supply increases, price history recorded.
 * - Sell failure: not enough inventory, good not available.
 * - Price history trimming to MAX_HISTORY_PER_GOOD_TOWN.
 */
@RunWith(RobolectricTestRunner::class)
class MarketRepositoryTest {

    private lateinit var database: WanderingLedgerDatabase
    private lateinit var marketRepository: MarketRepository
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        marketRepository = MarketRepository(database)
        gameRepository = GameRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Buy ──────────────────────────────────────────────────────────────────

    @Test
    fun buyGoodDeductsGoldAndAddsToInventory() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Hearthwick (townId=1) sells Apples (goodId=1) at Abundant supply
        // Seed sell price = 8, player starts with 50 gold
        val result = marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 2)

        assertTrue("Expected BuyResult.Success but got $result", result is BuyResult.Success)
        val success = result as BuyResult.Success
        assertEquals(1L, success.goodId)
        assertEquals(2, success.quantity)

        val player = database.playerDao().getPlayerSnapshot()!!
        assertTrue("Gold should have decreased", player.gold < 50L)

        val inventory = database.inventoryDao().listInventory(1L).first()
        val apples = inventory.firstOrNull { it.goodId == 1L }
        assertEquals(2, apples?.quantity)
    }

    @Test
    fun buyGoodDecreasesSupplyAndRaisesPrice() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Apples at Hearthwick start Abundant
        val priceBefore = database.townPriceDao().getPrice(1L, 1L).first()!!
        assertEquals("Abundant", priceBefore.supplyLevel)

        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)

        val priceAfter = database.townPriceDao().getPrice(1L, 1L).first()!!
        // Abundant → Normal after buying
        assertEquals("Normal", priceAfter.supplyLevel)

        // Verify the new price matches what the engine computes for Normal supply
        val good = database.goodDao().getGood(1L).first()!!
        val (expectedSell, expectedBuy) = MarketEngine.computePrices(good.baseValue, com.wanderingledger.core.model.SupplyLevel.Normal)
        assertEquals(expectedSell, priceAfter.sellPrice)
        assertEquals(expectedBuy, priceAfter.buyPrice)
    }

    @Test
    fun buyGoodRecordsPriceHistory() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)

        val history = database.priceHistoryDao().listHistorySnapshot(1L, 1L)
        assertEquals(1, history.size)
        assertEquals("Normal", history.first().supplyLevel) // after buying from Abundant
    }

    @Test
    fun buyGoodFailsWhenNotEnoughGold() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Iron at Hearthwick is Scarce — sell price is high (seed: 30)
        // Player has 50 gold; buying 2 at 30 each = 60 > 50
        val result = marketRepository.buyGood(townId = 1L, goodId = 2L, quantity = 2)

        assertTrue("Expected NotEnoughGold but got $result", result is BuyResult.NotEnoughGold)
        val notEnough = result as BuyResult.NotEnoughGold
        assertTrue(notEnough.required > notEnough.available)

        // Gold should be unchanged
        val player = database.playerDao().getPlayerSnapshot()!!
        assertEquals(50L, player.gold)
    }

    @Test
    fun buyGoodFailsWhenInventoryFull() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Give the player enough gold to afford the purchase
        val player = database.playerDao().getPlayerSnapshot()!!
        database.playerDao().updatePlayer(player.copy(gold = 10_000L))

        // Player has 12 inventory slots; try to buy 13 apples at once
        val result = marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 13)

        assertTrue("Expected InventoryFull but got $result", result is BuyResult.InventoryFull)
    }

    @Test
    fun buyGoodFailsForUnknownGood() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        val result = marketRepository.buyGood(townId = 1L, goodId = 999L, quantity = 1)

        assertTrue("Expected GoodNotAvailable but got $result", result is BuyResult.GoodNotAvailable)
    }

    @Test
    fun buyGoodFailsForInvalidQuantity() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        val result = marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 0)

        assertTrue("Expected InvalidQuantity but got $result", result is BuyResult.InvalidQuantity)
    }

    // ── Sell ─────────────────────────────────────────────────────────────────

    @Test
    fun sellGoodAddsGoldAndRemovesFromInventory() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // First buy some apples
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 3)
        val goldAfterBuy = database.playerDao().getPlayerSnapshot()!!.gold

        // Now sell 2 of them back
        val result = marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 2)

        assertTrue("Expected SellResult.Success but got $result", result is SellResult.Success)
        val success = result as SellResult.Success
        assertEquals(1L, success.goodId)
        assertEquals(2, success.quantity)
        assertTrue("Gold should have increased after selling", success.remainingGold > goldAfterBuy)

        val inventory = database.inventoryDao().listInventory(1L).first()
        val apples = inventory.firstOrNull { it.goodId == 1L }
        assertEquals(1, apples?.quantity) // 3 bought - 2 sold = 1 remaining
    }

    @Test
    fun sellGoodRemovesStackWhenQuantityReachesZero() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 2)
        marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 2)

        val inventory = database.inventoryDao().listInventory(1L).first()
        val apples = inventory.firstOrNull { it.goodId == 1L }
        assertEquals(null, apples) // stack should be removed
    }

    @Test
    fun sellGoodIncreasesSupplyAndLowersPrice() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Iron at Hearthwick starts Scarce (seed)
        val priceBefore = database.townPriceDao().getPrice(1L, 2L).first()!!
        assertEquals("Scarce", priceBefore.supplyLevel)

        // Give player some iron to sell (inject directly)
        database.inventoryDao().addItem(
            com.wanderingledger.core.database.InventoryItemEntity(
                playerId = 1L,
                goodId = 2L,
                quantity = 1,
            ),
        )

        marketRepository.sellGood(townId = 1L, goodId = 2L, quantity = 1)

        val priceAfter = database.townPriceDao().getPrice(1L, 2L).first()!!
        // Scarce → Normal after selling
        assertEquals("Normal", priceAfter.supplyLevel)

        // Verify the new price matches what the engine computes for Normal supply
        val good = database.goodDao().getGood(2L).first()!!
        val (expectedSell, expectedBuy) = MarketEngine.computePrices(good.baseValue, com.wanderingledger.core.model.SupplyLevel.Normal)
        assertEquals(expectedSell, priceAfter.sellPrice)
        assertEquals(expectedBuy, priceAfter.buyPrice)
    }

    @Test
    fun sellGoodRecordsPriceHistory() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        database.inventoryDao().addItem(
            com.wanderingledger.core.database.InventoryItemEntity(
                playerId = 1L,
                goodId = 2L,
                quantity = 1,
            ),
        )

        marketRepository.sellGood(townId = 1L, goodId = 2L, quantity = 1)

        val history = database.priceHistoryDao().listHistorySnapshot(1L, 2L)
        assertEquals(1, history.size)
        assertEquals("Normal", history.first().supplyLevel) // Scarce → Normal after selling
    }

    @Test
    fun sellGoodFailsWhenNotEnoughInventory() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        val result = marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)

        assertTrue("Expected NotEnoughInventory but got $result", result is SellResult.NotEnoughInventory)
        val notEnough = result as SellResult.NotEnoughInventory
        assertEquals(1, notEnough.required)
        assertEquals(0, notEnough.available)
    }

    @Test
    fun sellGoodFailsForUnknownGood() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        val result = marketRepository.sellGood(townId = 1L, goodId = 999L, quantity = 1)

        assertTrue("Expected GoodNotAvailable but got $result", result is SellResult.GoodNotAvailable)
    }

    @Test
    fun sellGoodFailsForInvalidQuantity() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        val result = marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = -1)

        assertTrue("Expected InvalidQuantity but got $result", result is SellResult.InvalidQuantity)
    }

    // ── Price history trimming ────────────────────────────────────────────────

    @Test
    fun priceHistoryIsTrimmedToMaxEntries() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Perform more than MAX_HISTORY_PER_GOOD_TOWN buy/sell cycles to generate history
        // We alternate buy and sell to keep supply cycling
        repeat(12) { i ->
            if (i % 2 == 0) {
                // Buy 1 apple (supply decreases)
                marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
            } else {
                // Sell 1 apple back (supply increases)
                val inventory = database.inventoryDao().listInventory(1L).first()
                val apples = inventory.firstOrNull { it.goodId == 1L }
                if ((apples?.quantity ?: 0) > 0) {
                    marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
                }
            }
        }

        val history = database.priceHistoryDao().listHistorySnapshot(1L, 1L)
        assertTrue(
            "History should be trimmed to max ${com.wanderingledger.core.database.PriceHistoryEntity.MAX_HISTORY_PER_GOOD_TOWN} but was ${history.size}",
            history.size <= com.wanderingledger.core.database.PriceHistoryEntity.MAX_HISTORY_PER_GOOD_TOWN,
        )
    }

    // ── Supply cycle invariant ───────────────────────────────────────────────

    @Test
    fun buyingToScarceAndSellingBackToAbundantRestoresOriginalPrices() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Apples at Hearthwick start Abundant (sell=8, buy=5)
        val originalPrice = database.townPriceDao().getPrice(1L, 1L).first()!!
        assertEquals("Abundant", originalPrice.supplyLevel)
        val originalSellPrice = originalPrice.sellPrice
        val originalBuyPrice = originalPrice.buyPrice

        // Give player enough gold and inventory space
        val player = database.playerDao().getPlayerSnapshot()!!
        database.playerDao().updatePlayer(player.copy(gold = 10_000L))

        // Buy twice: Abundant → Normal → Scarce
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        val scarcePrice = database.townPriceDao().getPrice(1L, 1L).first()!!
        assertEquals("Scarce", scarcePrice.supplyLevel)

        // Sell twice: Scarce → Normal → Abundant
        marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
        marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
        val restoredPrice = database.townPriceDao().getPrice(1L, 1L).first()!!
        assertEquals("Abundant", restoredPrice.supplyLevel)

        // Prices should be restored to original values
        assertEquals(
            "Sell price should be restored after full buy/sell cycle",
            originalSellPrice,
            restoredPrice.sellPrice,
        )
        assertEquals(
            "Buy price should be restored after full buy/sell cycle",
            originalBuyPrice,
            restoredPrice.buyPrice,
        )
    }

    @Test
    fun buyingAtScarceClampDoesNotChangePriceFurther() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Iron at Hearthwick starts Scarce (seed)
        val scarcePriceBefore = database.townPriceDao().getPrice(1L, 2L).first()!!
        assertEquals("Scarce", scarcePriceBefore.supplyLevel)

        // Give player enough gold
        val player = database.playerDao().getPlayerSnapshot()!!
        database.playerDao().updatePlayer(player.copy(gold = 10_000L))

        // Buy once — supply is already Scarce, should stay Scarce
        marketRepository.buyGood(townId = 1L, goodId = 2L, quantity = 1)
        val scarcePriceAfter = database.townPriceDao().getPrice(1L, 2L).first()!!
        assertEquals("Scarce", scarcePriceAfter.supplyLevel)

        // Prices should be identical (clamped, no further decrease)
        assertEquals(
            "Sell price should not change when supply is already Scarce",
            scarcePriceBefore.sellPrice,
            scarcePriceAfter.sellPrice,
        )
    }

    // ── Multi-good independence ──────────────────────────────────────────────

    @Test
    fun buyingGoodADoesNotAffectGoodBSupplyOrPrice() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Record Iron (goodId=2) price at Hearthwick before buying Apples (goodId=1)
        val ironPriceBefore = database.townPriceDao().getPrice(1L, 2L).first()!!

        // Buy Apples — this should only affect Apples supply
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)

        // Iron price and supply should be unchanged
        val ironPriceAfter = database.townPriceDao().getPrice(1L, 2L).first()!!
        assertEquals(
            "Iron supply level should be unaffected by buying Apples",
            ironPriceBefore.supplyLevel,
            ironPriceAfter.supplyLevel,
        )
        assertEquals(
            "Iron sell price should be unaffected by buying Apples",
            ironPriceBefore.sellPrice,
            ironPriceAfter.sellPrice,
        )
        assertEquals(
            "Iron buy price should be unaffected by buying Apples",
            ironPriceBefore.buyPrice,
            ironPriceAfter.buyPrice,
        )
    }

    @Test
    fun sellingGoodADoesNotAffectGoodBSupplyOrPrice() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Record Apples (goodId=1) price at Hearthwick before selling Iron (goodId=2)
        val applesPriceBefore = database.townPriceDao().getPrice(1L, 1L).first()!!

        // Inject Iron into inventory and sell it
        database.inventoryDao().addItem(
            com.wanderingledger.core.database.InventoryItemEntity(
                playerId = 1L,
                goodId = 2L,
                quantity = 1,
            ),
        )
        marketRepository.sellGood(townId = 1L, goodId = 2L, quantity = 1)

        // Apples price and supply should be unchanged
        val applesPriceAfter = database.townPriceDao().getPrice(1L, 1L).first()!!
        assertEquals(
            "Apples supply level should be unaffected by selling Iron",
            applesPriceBefore.supplyLevel,
            applesPriceAfter.supplyLevel,
        )
        assertEquals(
            "Apples sell price should be unaffected by selling Iron",
            applesPriceBefore.sellPrice,
            applesPriceAfter.sellPrice,
        )
    }

    // ── Price history ordering ────────────────────────────────────────────────

    @Test
    fun priceHistoryIsReturnedNewestFirst() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Give player enough gold
        val player = database.playerDao().getPlayerSnapshot()!!
        database.playerDao().updatePlayer(player.copy(gold = 10_000L))

        // Perform 3 buy/sell cycles to generate multiple history entries
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)  // Abundant → Normal
        marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1) // Normal → Abundant
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)  // Abundant → Normal

        val history = database.priceHistoryDao().listHistorySnapshot(1L, 1L)
        assertTrue("Should have at least 3 history entries", history.size >= 3)

        // Verify newest-first ordering: each entry's recordedAt >= the next
        for (i in 0 until history.size - 1) {
            assertTrue(
                "History entry $i (recordedAt=${history[i].recordedAt}) should be >= " +
                    "entry ${i + 1} (recordedAt=${history[i + 1].recordedAt})",
                history[i].recordedAt >= history[i + 1].recordedAt,
            )
        }
    }

    @Test
    fun priceHistoryTrimmingRemovesOldestEntries() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Give player enough gold
        val player = database.playerDao().getPlayerSnapshot()!!
        database.playerDao().updatePlayer(player.copy(gold = 10_000L))

        // Generate more than MAX_HISTORY_PER_GOOD_TOWN entries
        val maxHistory = com.wanderingledger.core.database.PriceHistoryEntity.MAX_HISTORY_PER_GOOD_TOWN
        repeat(maxHistory + 3) { i ->
            if (i % 2 == 0) {
                marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
            } else {
                val inventory = database.inventoryDao().listInventory(1L).first()
                val apples = inventory.firstOrNull { it.goodId == 1L }
                if ((apples?.quantity ?: 0) > 0) {
                    marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
                }
            }
        }

        // Count total entries (not limited by the snapshot query's default limit)
        val totalCount = database.priceHistoryDao().countHistory(1L, 1L)
        assertTrue(
            "Total history count $totalCount should be <= max $maxHistory after trimming",
            totalCount <= maxHistory,
        )

        // The returned history should be the most recent entries (newest first)
        val history = database.priceHistoryDao().listHistorySnapshot(1L, 1L)
        assertEquals(
            "Returned history size should equal max history limit",
            maxHistory,
            history.size,
        )
    }

    // ── Concurrent buy/sell consistency ──────────────────────────────────────

    @Test
    fun multipleBuyAndSellCyclesProduceConsistentGoldBalance() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Give player enough gold for many transactions
        val player = database.playerDao().getPlayerSnapshot()!!
        database.playerDao().updatePlayer(player.copy(gold = 10_000L))
        val startingGold = 10_000L

        // Buy 3 apples, sell 3 apples — net gold change should be negative (spread cost)
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
        marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
        marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)

        val finalPlayer = database.playerDao().getPlayerSnapshot()!!
        // After buying and selling the same quantity, gold should be less than start (spread cost)
        assertTrue(
            "Gold after buy/sell cycle should be less than starting gold due to spread",
            finalPlayer.gold < startingGold,
        )

        // Inventory should be empty (bought 3, sold 3)
        val inventory = database.inventoryDao().listInventory(1L).first()
        val apples = inventory.firstOrNull { it.goodId == 1L }
        assertEquals("Inventory should be empty after buying and selling same quantity", null, apples)
    }

    @Test
    fun multipleBuysAccumulateInventoryCorrectly() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Give player enough gold
        val player = database.playerDao().getPlayerSnapshot()!!
        database.playerDao().updatePlayer(player.copy(gold = 10_000L))

        // Buy 1 apple three separate times
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)

        val inventory = database.inventoryDao().listInventory(1L).first()
        val apples = inventory.firstOrNull { it.goodId == 1L }
        assertEquals("Three separate buys should accumulate to 3 apples", 3, apples?.quantity)
    }

    @Test
    fun supplyLevelIsConsistentAfterInterleavedBuysAndSells() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        // Give player enough gold
        val player = database.playerDao().getPlayerSnapshot()!!
        database.playerDao().updatePlayer(player.copy(gold = 10_000L))

        // Apples start Abundant
        // Buy (Abundant→Normal), Buy (Normal→Scarce), Sell (Scarce→Normal), Sell (Normal→Abundant)
        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        val afterFirstBuy = database.townPriceDao().getPrice(1L, 1L).first()!!
        assertEquals("After 1st buy: Normal", "Normal", afterFirstBuy.supplyLevel)

        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
        val afterSecondBuy = database.townPriceDao().getPrice(1L, 1L).first()!!
        assertEquals("After 2nd buy: Scarce", "Scarce", afterSecondBuy.supplyLevel)

        marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
        val afterFirstSell = database.townPriceDao().getPrice(1L, 1L).first()!!
        assertEquals("After 1st sell: Normal", "Normal", afterFirstSell.supplyLevel)

        marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
        val afterSecondSell = database.townPriceDao().getPrice(1L, 1L).first()!!
        assertEquals("After 2nd sell: Abundant", "Abundant", afterSecondSell.supplyLevel)
    }

    // ── observeMarket ─────────────────────────────────────────────────────────

    @Test
    fun observeMarketEmitsCorrectStateForHearthwick() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        val state = marketRepository.observeMarket(townId = 1L).first()

        assertEquals(1L, state.townId)
        assertEquals("Hearthwick", state.townName)
        assertEquals(50L, state.playerGold)
        assertEquals(0, state.playerInventoryUsed)
        assertEquals(12, state.playerInventoryCapacity)
        // Hearthwick has 2 goods seeded: Apples (goodId=1) and Iron (goodId=2)
        assertEquals(2, state.rows.size)
    }

    @Test
    fun observeMarketReflectsAffordabilityCorrectly() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        val state = marketRepository.observeMarket(townId = 1L).first()

        // Apples sell price = 8, player has 50 gold → can afford
        val applesRow = state.rows.first { it.good.goodId == 1L }
        assertTrue(applesRow.canAfford)
        // Player has no apples yet → cannot sell
        assertTrue(!applesRow.canSell)
    }

    @Test
    fun observeMarketUpdatesAfterBuy() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 2)

        val state = marketRepository.observeMarket(townId = 1L).first()
        val applesRow = state.rows.first { it.good.goodId == 1L }

        assertEquals(2, applesRow.playerQuantity)
        assertTrue(applesRow.canSell)
        assertEquals(2, state.playerInventoryUsed)
    }
}
