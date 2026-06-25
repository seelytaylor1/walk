package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.testing.TestDatabaseFactory
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
 * Market filtering behaviour (hiding rep-gated goods from observeMarket) is tested in B2.
 */
@RunWith(RobolectricTestRunner::class)
class ReputationGatedMarketTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        val companionRepository = CompanionRepository(database)
        val rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository)
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
}
