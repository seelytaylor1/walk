package com.wanderingledger.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.data.CompanionCommentaryEngine
import com.wanderingledger.core.data.CompanionRepository
import com.wanderingledger.core.data.GameRepository
import com.wanderingledger.core.data.OrderRepository
import com.wanderingledger.core.data.RumorRepository
import com.wanderingledger.core.database.CompanionEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.designsystem.accessibility.AccessibilityPreferences
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [CompanionsViewModel].
 *
 * The seeded world provides a player at Hearthwick (townId=1). Tests insert
 * companion entities directly and operate through the ViewModel's public API.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CompanionsViewModelTest {

    private lateinit var database: WanderingLedgerDatabase
    private lateinit var companionRepository: CompanionRepository
    private lateinit var gameRepository: GameRepository
    private lateinit var accessibilityPreferences: AccessibilityPreferences
    private lateinit var narrator: CompanionNarrator
    private lateinit var viewModel: CompanionsViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        val rumorRepository = RumorRepository(database)
        companionRepository = CompanionRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository, OrderRepository(database))
        accessibilityPreferences = AccessibilityPreferences(context)
        narrator = CompanionNarrator(
            companionRepository = companionRepository,
            engine = CompanionCommentaryEngine(cooldownMs = 0L), // no cooldown for most tests
        )
        viewModel = CompanionsViewModel(
            companionRepository = companionRepository,
            gameRepository = gameRepository,
            narrator = narrator,
            accessibilityPreferences = accessibilityPreferences,
            ioDispatcher = testDispatcher,
        )
        // Seed the world so GameRepository calls (observePlayerState, observeTown) succeed
        runBlocking { gameRepository.initializeNewGame(seed = 1L) }
        viewModel.activate(townId = 1L)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    /** Insert an active companion at town 1 and return its ID. */
    private suspend fun insertActiveCompanion(
        id: Long = 100L,
        name: String = "Mira",
        role: String = "Healer",
    ): Long {
        database.companionDao().upsertCompanion(
            CompanionEntity(
                companionId = id,
                name = name,
                role = role,
                combatPower = 2,
                bondLevel = 0,
                questState = "recruited",
                locationTownId = 1L,
                isActive = true,
            ),
        )
        return id
    }

    // ── Test 1: interact() with Spoken result clears message and emits InteractSuccess ──

    @Test
    fun `interact with Spoken result emits InteractSuccess and clears message`() =
        runTest(testDispatcher) {
            val companionId = insertActiveCompanion()

            val effects = mutableListOf<CompanionsEffect>()
            val effectJob = launch {
                viewModel.effects.collect { effects.add(it) }
            }

            viewModel.interact(companionId = companionId, townId = 1L).join()

            assertTrue(
                "Expected InteractSuccess effect, got: $effects",
                effects.contains(CompanionsEffect.InteractSuccess),
            )
            assertNull(
                "Message should be null after Spoken result",
                viewModel.state.value?.message,
            )
            effectJob.cancel()
        }

    // ── Test 2: interact() with OnCooldown sets message and emits CooldownActive ──

    @Test
    fun `interact with OnCooldown sets message and emits CooldownActive`() =
        runTest(testDispatcher) {
            // Use a narrator with a long cooldown so the second call hits OnCooldown
            val cooldownNarrator = CompanionNarrator(
                companionRepository = companionRepository,
                engine = CompanionCommentaryEngine(cooldownMs = Long.MAX_VALUE),
            )
            val vm = CompanionsViewModel(
                companionRepository = companionRepository,
                gameRepository = gameRepository,
                narrator = cooldownNarrator,
                accessibilityPreferences = accessibilityPreferences,
                ioDispatcher = testDispatcher,
            )
            vm.activate(townId = 1L)

            val companionId = insertActiveCompanion()

            val effects = mutableListOf<CompanionsEffect>()
            val effectJob = launch {
                vm.effects.collect { effects.add(it) }
            }

            // First interact: Spoken (starts the cooldown)
            vm.interact(companionId = companionId, townId = 1L).join()
            // Second interact: OnCooldown
            vm.interact(companionId = companionId, townId = 1L).join()

            assertTrue(
                "Expected CooldownActive effect, got: $effects",
                effects.contains(CompanionsEffect.CooldownActive),
            )
            val message = vm.state.value?.message
            assertTrue(
                "Expected cooldown message, got: $message",
                message?.contains("still considering") == true,
            )
            effectJob.cancel()
        }

    // ── Test 3: interact() with NotActive sets message and does NOT emit InteractSuccess ──

    @Test
    fun `interact with NotActive companion sets message and does not emit InteractSuccess`() =
        runTest(testDispatcher) {
            // Companion ID 999 is not inserted, so it won't be found in active companions
            val missingCompanionId = 999L

            val effects = mutableListOf<CompanionsEffect>()
            val effectJob = launch {
                viewModel.effects.collect { effects.add(it) }
            }

            viewModel.interact(companionId = missingCompanionId, townId = 1L).join()

            val message = viewModel.state.value?.message
            assertTrue(
                "Expected NotActive message, got: $message",
                message?.contains("Only active") == true,
            )
            assertTrue(
                "InteractSuccess must NOT be emitted when companion is not active",
                !effects.contains(CompanionsEffect.InteractSuccess),
            )
            effectJob.cancel()
        }

    // ── Test 4: stale message cleared after Spoken (regression for fix) ────────
    //   Steps:
    //   1. recruit() to set a message ("A new voice joins the road.")
    //   2. call interact() that returns Spoken
    //   3. verify state.message is null

    @Test
    fun `stale recruit message is cleared when interact returns Spoken`() =
        runTest(testDispatcher) {
            // Insert the companion to recruit (must be inactive/recruitable)
            val recruitableId = 200L
            database.companionDao().upsertCompanion(
                CompanionEntity(
                    companionId = recruitableId,
                    name = "Tomas",
                    role = "Fighter",
                    combatPower = 3,
                    bondLevel = 0,
                    questState = "available",
                    locationTownId = 1L,
                    isActive = false,
                ),
            )

            // Set completedTradesCount = 3 so recruitment gate passes
            val player = database.playerDao().getPlayerSnapshot()!!
            database.playerDao().updatePlayer(player.copy(completedTradesCount = 3))

            // Insert an active companion so interact() can succeed with Spoken
            val activeId = insertActiveCompanion()

            // recruit() sets a message on the state
            viewModel.recruit(companionId = recruitableId).join()
            assertEquals(
                "A new voice joins the road.",
                viewModel.state.value?.message,
            )

            // interact() returning Spoken should clear the stale message
            viewModel.interact(companionId = activeId, townId = 1L).join()

            assertNull(
                "Stale recruit message should be null after Spoken interact",
                viewModel.state.value?.message,
            )
        }
}
