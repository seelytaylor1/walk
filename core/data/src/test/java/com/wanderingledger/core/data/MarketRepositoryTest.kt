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
 */
@RunWith(RobolectricTestRunner::class)
class MarketRepositoryTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var companionRepository: CompanionRepository
    private lateinit var rumorRepository: RumorRepository
    private lateinit var marketRepository: MarketRepository
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository, OrderRepository(database))
        marketRepository = MarketRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── Buy ──────────────────────────────────────────────────────────────────

    @Test
    fun buyGoodDeductsGoldAndAddsToInventory() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            // Hearthwick (townId=1) sells Apples (goodId=1) at Abundant supply
            // Seed sell price = 5, player starts with 50 gold
            val result = marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 2)

            assertTrue("Expected BuyResult.Success but got $result", result is BuyResult.Success)
            val success = result as BuyResult.Success
            assertEquals(1L, success.goodId)
            assertEquals(2, success.quantity)

            val player = database.playerDao().getPlayerSnapshot()!!
            assertEquals(40L, player.gold) // 50 - (5 * 2) = 40

            val inventory = database.inventoryDao().listInventory(1L).first()
            val apples = inventory.firstOrNull { it.goodId == 1L }
            assertEquals(2, apples?.quantity)
        }

    @Test
    fun buyGoodDecreasesSupplyAndRaisesPrice() =
        runTest {
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
            val (expectedSell, expectedBuy) =
                MarketEngine.computePrices(
                    good.baseValue,
                    com.wanderingledger.core.model.SupplyLevel.Normal,
                )
            assertEquals(expectedSell, priceAfter.sellPrice)
            assertEquals(expectedBuy, priceAfter.buyPrice)
        }

    @Test
    fun buyGoodRecordsPriceHistory() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)

            val history = database.priceHistoryDao().listHistorySnapshot(1L, 1L)
            assertEquals(1, history.size)
            assertEquals("Normal", history.first().supplyLevel) // after buying from Abundant
        }

    @Test
    fun buyGoodFailsWhenNotEnoughGold() =
        runTest {
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
    fun buyGoodFailsWhenInventoryFull() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            // Give the player enough gold to afford the purchase
            val player = database.playerDao().getPlayerSnapshot()!!
            database.playerDao().updatePlayer(player.copy(gold = 10_000L))

            // Player has 12 inventory slots; try to buy 13 apples at once
            val result = marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 13)

            assertTrue("Expected InventoryFull but got $result", result is BuyResult.InventoryFull)
        }

    @Test
    fun buyGoodFailsForUnknownGood() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            val result = marketRepository.buyGood(townId = 1L, goodId = 999L, quantity = 1)

            assertTrue("Expected GoodNotAvailable but got $result", result is BuyResult.GoodNotAvailable)
        }

    @Test
    fun buyGoodFailsForInvalidQuantity() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            val result = marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 0)

            assertTrue("Expected InvalidQuantity but got $result", result is BuyResult.InvalidQuantity)
        }

    // ── Sell ─────────────────────────────────────────────────────────────────

    @Test
    fun sellGoodAddsGoldAndRemovesFromInventory() =
        runTest {
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
    fun sellGoodRemovesStackWhenQuantityReachesZero() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 2)
            marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 2)

            val inventory = database.inventoryDao().listInventory(1L).first()
            val apples = inventory.firstOrNull { it.goodId == 1L }
            assertEquals(null, apples) // stack should be removed
        }

    @Test
    fun sellGoodIncreasesSupplyAndLowersPrice() =
        runTest {
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
            val (expectedSell, expectedBuy) =
                MarketEngine.computePrices(
                    good.baseValue,
                    com.wanderingledger.core.model.SupplyLevel.Normal,
                )
            assertEquals(expectedSell, priceAfter.sellPrice)
            assertEquals(expectedBuy, priceAfter.buyPrice)
        }

    @Test
    fun sellGoodRecordsPriceHistory() =
        runTest {
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
    fun sellGoodFailsWhenNotEnoughInventory() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            val result = marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)

            assertTrue("Expected NotEnoughInventory but got $result", result is SellResult.NotEnoughInventory)
            val notEnough = result as SellResult.NotEnoughInventory
            assertEquals(1, notEnough.required)
            assertEquals(0, notEnough.available)
        }

    @Test
    fun sellGoodFailsForUnknownGood() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            val result = marketRepository.sellGood(townId = 1L, goodId = 999L, quantity = 1)

            assertTrue("Expected GoodNotAvailable but got $result", result is SellResult.GoodNotAvailable)
        }

    @Test
    fun sellGoodFailsForInvalidQuantity() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            val result = marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = -1)

            assertTrue("Expected InvalidQuantity but got $result", result is SellResult.InvalidQuantity)
        }

    // ── Price history trimming ────────────────────────────────────────────────

    @Test
    fun priceHistoryIsTrimmedToMaxEntries() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            // Perform more than MAX_HISTORY_PER_GOOD_TOWN buy/sell cycles to generate history
            repeat(12) { i ->
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

            val history = database.priceHistoryDao().listHistorySnapshot(1L, 1L)
            val maxHistory = com.wanderingledger.core.database.PriceHistoryEntity.MAX_HISTORY_PER_GOOD_TOWN
            assertTrue(
                "History should be trimmed to max $maxHistory but was ${history.size}",
                history.size <= maxHistory,
            )
        }

    // ── Supply cycle invariant ───────────────────────────────────────────────

    @Test
    fun buyingToScarceAndSellingBackToAbundantRestoresOriginalPrices() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            val originalPrice = database.townPriceDao().getPrice(1L, 1L).first()!!
            assertEquals("Abundant", originalPrice.supplyLevel)
            val originalSellPrice = originalPrice.sellPrice
            val originalBuyPrice = originalPrice.buyPrice

            val player = database.playerDao().getPlayerSnapshot()!!
            database.playerDao().updatePlayer(player.copy(gold = 10_000L))

            marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
            marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)
            assertEquals(
                "Scarce",
                database
                    .townPriceDao()
                    .getPrice(1L, 1L)
                    .first()!!
                    .supplyLevel,
            )

            marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
            marketRepository.sellGood(townId = 1L, goodId = 1L, quantity = 1)
            val restoredPrice = database.townPriceDao().getPrice(1L, 1L).first()!!
            assertEquals("Abundant", restoredPrice.supplyLevel)

            assertEquals(originalSellPrice, restoredPrice.sellPrice)
            assertEquals(originalBuyPrice, restoredPrice.buyPrice)
        }

    @Test
    fun tradeRouteIntegrationTest() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)
            val startingGold = 50L

            marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 5)

            val player = database.playerDao().getPlayerSnapshot()!!
            database.playerDao().updatePlayer(player.copy(bankedSteps = 1000L))

            gameRepository.travel(segmentId = 5L)

            val sellResult = marketRepository.sellGood(townId = 3L, goodId = 1L, quantity = 5)
            assertTrue(sellResult is SellResult.Success)
            assertEquals(60L, (sellResult as SellResult.Success).remainingGold)
        }
}
