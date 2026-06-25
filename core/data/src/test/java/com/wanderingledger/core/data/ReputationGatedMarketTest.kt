package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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

/**
 * Verifies that the B1 schema and seed data are correct:
 * - Rare goods (Medicines/Dyes/Charts, goodIds 4-6) are seeded with minReputation = 60
 *   at their source towns.
 * - Common goods (Apples/Iron/Silk, goodIds 1-3) have minReputation = 0.
 * - The minReputation column is present and populated correctly in TownPriceEntity.
 *
 * Also verifies B2 market filtering and discount behaviour in observeMarket.
 */
@RunWith(RobolectricTestRunner::class)
class ReputationGatedMarketTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var gameRepository: GameRepository
    private lateinit var marketRepository: MarketRepository

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
    fun rareGoodsSourcePricesHaveMinReputation60() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            // Source town prices for rare goods carry minReputation = 60
            // Dyes (goodId=5) source is Hearthwick (townId=1)
            val dyesAtHearthwick = database.townPriceDao().listPricesSnapshotForTown(1L)
                .first { it.goodId == 5L }
            assertEquals(
                "Dyes source price at Hearthwick should have minReputation=60",
                60,
                dyesAtHearthwick.minReputation,
            )

            // Medicines (goodId=4) source is Mistfall (townId=3)
            val medicinesAtMistfall = database.townPriceDao().listPricesSnapshotForTown(3L)
                .first { it.goodId == 4L }
            assertEquals(
                "Medicines source price at Mistfall should have minReputation=60",
                60,
                medicinesAtMistfall.minReputation,
            )

            // Charts (goodId=6) source is Stoneford (townId=2)
            val chartsAtStoneford = database.townPriceDao().listPricesSnapshotForTown(2L)
                .first { it.goodId == 6L }
            assertEquals(
                "Charts source price at Stoneford should have minReputation=60",
                60,
                chartsAtStoneford.minReputation,
            )
        }

    @Test
    fun commonGoodPricesHaveMinReputation0() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            // Apples (goodId=1) at Hearthwick (townId=1)
            val applesAtHearthwick = database.townPriceDao().listPricesSnapshotForTown(1L)
                .first { it.goodId == 1L }
            assertEquals(
                "Apples at Hearthwick should have minReputation=0",
                0,
                applesAtHearthwick.minReputation,
            )

            // Iron (goodId=2) at Hearthwick (townId=1)
            val ironAtHearthwick = database.townPriceDao().listPricesSnapshotForTown(1L)
                .first { it.goodId == 2L }
            assertEquals(
                "Iron at Hearthwick should have minReputation=0",
                0,
                ironAtHearthwick.minReputation,
            )
        }

    @Test
    fun rareGoodsDestinationPricesHaveMinReputation0() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            // Dyes (goodId=5) destination is Mistfall (townId=3) — no rep gate at destination
            val dyesAtMistfall = database.townPriceDao().listPricesSnapshotForTown(3L)
                .first { it.goodId == 5L }
            assertEquals(
                "Dyes destination price at Mistfall should have minReputation=0",
                0,
                dyesAtMistfall.minReputation,
            )

            // Medicines (goodId=4) destination is Hearthwick (townId=1)
            val medicinesAtHearthwick = database.townPriceDao().listPricesSnapshotForTown(1L)
                .first { it.goodId == 4L }
            assertEquals(
                "Medicines destination price at Hearthwick should have minReputation=0",
                0,
                medicinesAtHearthwick.minReputation,
            )
        }

    @Test
    fun allSixGoodsAreSeededInDatabase() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            // All 6 goods (common + rare) should be present
            val expectedGoodIds = setOf(1L, 2L, 3L, 4L, 5L, 6L)
            val hearthwickPrices = database.townPriceDao().listPricesSnapshotForTown(1L)
            val stonefordPrices = database.townPriceDao().listPricesSnapshotForTown(2L)
            val mistfallPrices = database.townPriceDao().listPricesSnapshotForTown(3L)
            val allSeededGoodIds = (hearthwickPrices + stonefordPrices + mistfallPrices)
                .map { it.goodId }.toSet()

            assertTrue(
                "All 6 goods (goodIds 1-6) should be seeded across all towns. Found: $allSeededGoodIds",
                allSeededGoodIds.containsAll(expectedGoodIds),
            )
        }

    // ── B2: observeMarket filtering and discount ─────────────────────────────

    @Test
    fun medicinesDoNotAppearAtMistfallWhenRepBelowSixty() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)
            // Mistfall (townId=3) starts at rep=50; Medicines require minReputation=60
            val market = marketRepository.observeMarket(townId = 3L).first()
            val goodIds = market.rows.map { it.good.goodId }
            assertFalse("Medicines (goodId=4) should not appear at rep=50", goodIds.contains(4L))
        }

    @Test
    fun medicinesAppearAtMistfallWhenRepIsExactlySixty() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)
            database.townDao().addReputation(townId = 3L, amount = 10) // 50 + 10 = 60
            val market = marketRepository.observeMarket(townId = 3L).first()
            val medicines = market.rows.find { it.good.goodId == 4L }
            assertTrue("Medicines should appear at rep=60", medicines != null)
            assertEquals(
                "sellPrice should be 14 at rep=60 (no discount yet)",
                14L,
                medicines!!.townPrice.sellPrice,
            )
        }

    @Test
    fun medicinesGetFifteenPercentDiscountAtRepSeventyFive() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)
            database.townDao().addReputation(townId = 3L, amount = 25) // 50 + 25 = 75
            val market = marketRepository.observeMarket(townId = 3L).first()
            val medicines = market.rows.find { it.good.goodId == 4L }
            assertTrue("Medicines should appear at rep=75", medicines != null)
            // sellPrice=14, 15% off → (14 * 0.85).toLong() = 11
            assertEquals("sellPrice should be 11 at rep=75", 11L, medicines!!.townPrice.sellPrice)
        }

    @Test
    fun baseGoodsAreUnaffectedByReputation() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)
            database.townDao().addReputation(townId = 3L, amount = 25) // rep=75
            val market = marketRepository.observeMarket(townId = 3L).first()
            val silk = market.rows.find { it.good.goodId == 3L }
            assertTrue("Silk always visible (minReputation=0)", silk != null)
            assertEquals("Silk sellPrice unchanged at 21", 21L, silk!!.townPrice.sellPrice)
        }
}
