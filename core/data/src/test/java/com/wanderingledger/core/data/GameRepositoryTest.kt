package com.wanderingledger.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.testing.TestDatabaseFactory
import com.wanderingledger.core.steptracker.StepSource
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

@RunWith(RobolectricTestRunner::class)
class GameRepositoryTest {
    private lateinit var database: WanderingLedgerDatabase
    private lateinit var gameRepository: GameRepository
    private lateinit var stepBankRepository: RoomStepBankRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = TestDatabaseFactory.createInMemoryDatabase(context)
        gameRepository = GameRepository(database)
        stepBankRepository = RoomStepBankRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seedWorldHasExpectedTownsAndRoads() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        val towns = database.townDao().listTowns().first()
        val roadsFromHearthwick = database.roadSegmentDao().listRoadsFrom(1L).first()
        val roadsFromStoneford = database.roadSegmentDao().listRoadsFrom(2L).first()
        val roadsFromMistfall = database.roadSegmentDao().listRoadsFrom(3L).first()

        assertEquals(listOf("Hearthwick", "Stoneford", "Mistfall"), towns.map { it.name })
        assertEquals(listOf(2L, 3L), roadsFromHearthwick.map { it.toTownId })
        assertEquals(listOf(1L, 3L), roadsFromStoneford.map { it.toTownId })
        assertEquals(listOf(2L, 1L), roadsFromMistfall.map { it.toTownId })
    }

    @Test
    fun recordedStepsIncreaseBankAndLifetimeSteps() = runTest {
        gameRepository.initializeNewGame(seed = 1L)

        stepBankRepository.recordDetectedSteps(
            count = 75,
            source = StepSource.Simulation,
            recordedAt = 1_000L,
        )

        val player = gameRepository.observePlayerState().first()
        assertEquals(75L, player.bankedSteps)
        assertEquals(75L, player.lifetimeSteps)
        assertEquals(75L, stepBankRepository.observeStepBank().first())
    }

    @Test
    fun insufficientStepsBlocksTravelWithoutMutatingPlayer() = runTest {
        gameRepository.initializeNewGame(seed = 1L)
        stepBankRepository.recordDetectedSteps(
            count = 119,
            source = StepSource.Simulation,
            recordedAt = 1_000L,
        )

        val result = gameRepository.travel(segmentId = 1L)

        assertEquals(TravelResult.NotEnoughSteps(required = 120L, available = 119L), result)
        val player = gameRepository.observePlayerState().first()
        assertEquals(1L, player.currentTownId)
        assertEquals(119L, player.bankedSteps)
    }

    @Test
    fun successfulTravelSpendsRoadCostAndUpdatesTown() = runTest {
        gameRepository.initializeNewGame(seed = 1L)
        stepBankRepository.recordDetectedSteps(
            count = 150,
            source = StepSource.Simulation,
            recordedAt = 1_000L,
        )

        val result = gameRepository.travel(segmentId = 1L)

        assertEquals(TravelResult.Arrived(townId = 2L, remainingSteps = 30L), result)
        val player = gameRepository.observePlayerState().first()
        val stoneford = database.townDao().getTownSnapshot(2L)
        assertEquals(2L, player.currentTownId)
        assertEquals(30L, player.bankedSteps)
        assertEquals(150L, player.lifetimeSteps)
        assertEquals("visited", stoneford?.storyState)
        assertTrue((stoneford?.lastVisitedAt ?: 0L) > 0L)
    }

    @Test
    fun roadStepCostControlsTravelShortfallAndSpend() = runTest {
        gameRepository.initializeNewGame(seed = 1L)
        val hearthwickRoads = database.roadSegmentDao().listRoadsFrom(1L).first()
        val stonefordRoad = hearthwickRoads.first { it.toTownId == 2L }
        val mistfallRoad = hearthwickRoads.first { it.toTownId == 3L }

        assertEquals(120, stonefordRoad.stepCost)
        assertEquals(240, mistfallRoad.stepCost)

        stepBankRepository.recordDetectedSteps(
            count = 200,
            source = StepSource.Simulation,
            recordedAt = 1_000L,
        )

        val blockedResult = gameRepository.travel(mistfallRoad.segmentId)
        assertEquals(TravelResult.NotEnoughSteps(required = 240L, available = 200L), blockedResult)
        assertFalse(blockedResult is TravelResult.Arrived)

        val arrivedResult = gameRepository.travel(stonefordRoad.segmentId)
        assertEquals(TravelResult.Arrived(townId = 2L, remainingSteps = 80L), arrivedResult)
    }
}
