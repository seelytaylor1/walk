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
class CompanionRepositoryTest {

    private lateinit var database: WanderingLedgerDatabase
    private lateinit var rumorRepository: RumorRepository
    private lateinit var companionRepository: CompanionRepository
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        rumorRepository = RumorRepository(database)
        companionRepository = CompanionRepository(database)
        gameRepository = GameRepository(database, rumorRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeRecruitableCompanions() = runTest {
        gameRepository.initializeNewGame(seed = 1L)
        
        // Mira is at town 1
        val recruitable = companionRepository.observeRecruitableCompanionsAtTown(1L).first()
        assertEquals(1, recruitable.size)
        assertEquals("Mira", recruitable.first().name)
    }

    @Test
    fun recruitCompanionUpdatesState() = runTest {
        gameRepository.initializeNewGame(seed = 1L)
        
        companionRepository.recruitCompanion(1L) // Recruit Mira
        
        val active = companionRepository.observeActiveCompanions().first()
        assertEquals(1, active.size)
        assertEquals("Mira", active.first().name)
        assertTrue(active.first().isActive)
        
        val recruitable = companionRepository.observeRecruitableCompanionsAtTown(1L).first()
        assertTrue(recruitable.isEmpty())
    }

    @Test
    fun updateBondIncreasesLevel() = runTest {
        gameRepository.initializeNewGame(seed = 1L)
        companionRepository.recruitCompanion(1L)
        
        companionRepository.updateBond(1L, 10)
        
        val active = companionRepository.observeActiveCompanions().first()
        assertEquals(10, active.first().bondLevel)
    }
}
