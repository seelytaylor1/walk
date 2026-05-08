package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.testing.TestDatabaseFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameRepositoryTest {

    private lateinit var database: WanderingLedgerDatabase
    private lateinit var rumorRepository: RumorRepository
    private lateinit var gameRepository: GameRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        rumorRepository = RumorRepository(database)
        gameRepository = GameRepository(database, rumorRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun initializeNewGameSeedsDatabase() = runTest {
        gameRepository.initializeNewGame()
        
        val player = gameRepository.observePlayerState().first()
        assertEquals("Ledger Keeper", player.name)
        assertEquals(1L, player.currentTownId)
    }

    @Test
    fun travelUpdatesPlayerLocation() = runTest {
        gameRepository.initializeNewGame()
        val player = database.playerDao().getPlayerSnapshot()!!
        database.playerDao().updatePlayer(player.copy(bankedSteps = 200L))
        
        // Road 1: Hearthwick(1) -> Stoneford(2)
        gameRepository.travel(1L)
        
        val updatedPlayer = gameRepository.observePlayerState().first()
        assertEquals(2L, updatedPlayer.currentTownId)
    }
}
