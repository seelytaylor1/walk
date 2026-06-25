package com.wanderingledger.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.data.CompanionRepository
import com.wanderingledger.core.data.GameRepository
import com.wanderingledger.core.data.MarketRepository
import com.wanderingledger.core.data.OrderRepository
import com.wanderingledger.core.data.RumorRepository
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.testing.TestDatabaseFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [MarketViewModel].
 *
 * Uses a seeded in-memory Room database so MarketRepository can function without mocking.
 * The seeded world provides: Hearthwick (townId=1) with Apples (goodId=1, baseValue=10,
 * supply=Abundant, sellPrice=5) and the player starts with 50 gold.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MarketViewModelTest {

    private lateinit var database: WanderingLedgerDatabase
    private lateinit var marketRepository: MarketRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var viewModel: MarketViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        val rumorRepository = RumorRepository(database)
        val companionRepository = CompanionRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository, OrderRepository(database))
        marketRepository = MarketRepository(database)
        // Seed the world: inserts Hearthwick (townId=1), player (gold=50), goods, prices
        runBlocking { gameRepository.initializeNewGame(seed = 1L) }
        viewModel = MarketViewModel(
            marketRepository = marketRepository,
            ioDispatcher = testDispatcher,
        )
        viewModel.activate(townId = 1L)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    // ── Test 1: buy() with BuyResult.Success emits BuySuccess effect and sets message ──

    @Test
    fun `buy with enough gold emits BuySuccess effect and sets success message`() =
        runTest(testDispatcher) {
            val effects = mutableListOf<MarketEffect>()
            val effectJob = launch {
                viewModel.effects.collect { effects.add(it) }
            }

            // Player starts with 50g; Apples sell at 5g each — should succeed
            viewModel.buy(townId = 1L, goodId = 1L).join()

            assertTrue(
                "Expected BuySuccess effect, got: $effects",
                effects.contains(MarketEffect.BuySuccess),
            )
            val message = viewModel.message.value
            assertTrue(
                "Expected 'Bought' in message, got: $message",
                message?.contains("Bought") == true,
            )
            effectJob.cancel()
        }

    // ── Test 2: buy() with insufficient gold emits TransactionError and sets message ──

    @Test
    fun `buy with insufficient gold emits TransactionError and sets error message`() =
        runTest(testDispatcher) {
            // Drain the player's gold first
            val player = database.playerDao().getPlayerSnapshot()!!
            database.playerDao().updatePlayer(player.copy(gold = 0L))

            val effects = mutableListOf<MarketEffect>()
            val effectJob = launch {
                viewModel.effects.collect { effects.add(it) }
            }

            viewModel.buy(townId = 1L, goodId = 1L).join()

            assertTrue(
                "Expected TransactionError effect, got: $effects",
                effects.contains(MarketEffect.TransactionError),
            )
            val message = viewModel.message.value
            assertTrue(
                "Expected 'Not enough gold' in message, got: $message",
                message?.contains("Not enough gold") == true,
            )
            effectJob.cancel()
        }

    // ── Test 3: sell() with owned item emits SellSuccess effect and sets message ──

    @Test
    fun `sell with owned item emits SellSuccess effect and sets success message`() =
        runTest(testDispatcher) {
            // First buy an Apple so we have one to sell
            marketRepository.buyGood(townId = 1L, goodId = 1L, quantity = 1)

            val effects = mutableListOf<MarketEffect>()
            val effectJob = launch {
                viewModel.effects.collect { effects.add(it) }
            }

            viewModel.sell(townId = 1L, goodId = 1L).join()

            assertTrue(
                "Expected SellSuccess effect, got: $effects",
                effects.contains(MarketEffect.SellSuccess),
            )
            val message = viewModel.message.value
            assertTrue(
                "Expected 'Sold' in message, got: $message",
                message?.contains("Sold") == true,
            )
            effectJob.cancel()
        }

    // ── Test 4: sell() with no inventory emits TransactionError ──

    @Test
    fun `sell with no inventory emits TransactionError`() = runTest(testDispatcher) {
        // Player has no Apples in inventory (fresh seeded state)
        val effects = mutableListOf<MarketEffect>()
        val effectJob = launch {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sell(townId = 1L, goodId = 1L).join()

        assertTrue(
            "Expected TransactionError for empty-inventory sell, got: $effects",
            effects.contains(MarketEffect.TransactionError),
        )
        effectJob.cancel()
    }
}
