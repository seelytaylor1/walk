package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.CompanionEntity
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
class CompanionRepositoryTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var companionRepository: CompanionRepository
    private lateinit var rumorRepository: RumorRepository
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        companionRepository = CompanionRepository(database)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository, companionRepository, OrderRepository(database))
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** Set completedTradesCount to 3 so the recruitment gate is satisfied. */
    private suspend fun satisfyTradeGate() {
        val player = database.playerDao().getPlayerSnapshot() ?: return
        database.playerDao().updatePlayer(player.copy(completedTradesCount = 3))
    }

    @Test
    fun observeRecruitableCompanions() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)

            val recruitable = companionRepository.observeRecruitableCompanionsAtTown(1L).first()
            assertEquals(1, recruitable.size)
            assertEquals("Mira", recruitable.first().name)
        }

    @Test
    fun recruitCompanionUpdatesState() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)
            satisfyTradeGate()

            val result = companionRepository.recruitCompanion(1L)
            assertEquals(RecruitmentResult.Success, result)

            val active = companionRepository.observeActiveCompanions().first()
            assertEquals(1, active.size)
            assertEquals("Mira", active.first().name)
            assertTrue(active.first().isActive)

            val recruitable = companionRepository.observeRecruitableCompanionsAtTown(1L).first()
            assertTrue(recruitable.isEmpty())
        }

    @Test
    fun recruitCompanionInitializesBondToZero() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)
            satisfyTradeGate()

            companionRepository.recruitCompanion(1L)

            val active = companionRepository.observeActiveCompanions().first()
            assertEquals(0, active.first().bondLevel)
        }

    @Test
    fun updateBondClampsAtMaxLevel() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)
            satisfyTradeGate()
            companionRepository.recruitCompanion(1L)

            companionRepository.updateBond(1L, 10)

            val active = companionRepository.observeActiveCompanions().first()
            assertEquals(5, active.first().bondLevel)
        }

    @Test
    fun partyFullPreventsRecruitment() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)
            satisfyTradeGate()

            companionRepository.recruitCompanion(1L) // Mira
            companionRepository.recruitCompanion(2L) // Bram

            // Add third companion (Lina) and recruit
            database.companionDao().upsertCompanion(
                CompanionEntity(3, "Lina", "Healer", 4, 0, "available", 1, false),
            )
            var result = companionRepository.recruitCompanion(3L)
            assertEquals(RecruitmentResult.Success, result)

            // Add fourth companion (Zara) - party should be full
            database.companionDao().upsertCompanion(
                CompanionEntity(4, "Zara", "Mage", 5, 0, "available", 1, false),
            )
            result = companionRepository.recruitCompanion(4L)
            assertEquals(RecruitmentResult.PartyFull, result)
        }

    @Test
    fun dismissCompanionMakesAvailableAgain() =
        runTest {
            gameRepository.initializeNewGame(seed = 1L)
            satisfyTradeGate()

            companionRepository.recruitCompanion(1L)
            companionRepository.dismissCompanion(1L)

            val active = companionRepository.observeActiveCompanions().first()
            assertTrue(active.isEmpty())

            val recruitable = companionRepository.observeRecruitableCompanionsAtTown(1L).first()
            assertEquals(1, recruitable.size)
        }
}
