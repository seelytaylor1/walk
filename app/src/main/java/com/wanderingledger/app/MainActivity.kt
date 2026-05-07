package com.wanderingledger.app

import android.app.Activity
import android.os.Bundle
import com.wanderingledger.core.data.BuyResult
import com.wanderingledger.core.data.GameRepository
import com.wanderingledger.core.data.InventoryRepository
import com.wanderingledger.core.data.MarketRepository
import com.wanderingledger.core.data.RoomStepBankRepository
import com.wanderingledger.core.data.SellResult
import com.wanderingledger.core.data.TravelResult
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.steptracker.StepSource
import com.wanderingledger.core.steptracker.StepTrackerService
import com.wanderingledger.feature.town.BuyActionCallback
import com.wanderingledger.feature.town.InventoryActions
import com.wanderingledger.feature.town.InventoryNavigationCallback
import com.wanderingledger.feature.town.InventoryScreenView
import com.wanderingledger.feature.town.InventorySellCallback
import com.wanderingledger.feature.town.MarketActions
import com.wanderingledger.feature.town.MarketNavigationCallback
import com.wanderingledger.feature.town.MarketScreenView
import com.wanderingledger.feature.town.SellActionCallback
import com.wanderingledger.feature.town.TownActions
import com.wanderingledger.feature.town.TownInventoryCallback
import com.wanderingledger.feature.town.TownMarketCallback
import com.wanderingledger.feature.town.TownNavigationCallback
import com.wanderingledger.feature.town.TownScreenView
import com.wanderingledger.feature.town.buildInventoryScreenState
import com.wanderingledger.feature.town.buildMarketScreenState
import com.wanderingledger.feature.town.buildTownScreenState
import com.wanderingledger.feature.worldmap.TravelActionCallback
import com.wanderingledger.feature.worldmap.WorldMapActions
import com.wanderingledger.feature.worldmap.WorldMapScreenView
import com.wanderingledger.feature.worldmap.buildWorldMapScreenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var database: WanderingLedgerDatabase
    private lateinit var gameRepository: GameRepository
    private lateinit var marketRepository: MarketRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var stepTrackerService: StepTrackerService

    private lateinit var worldMapView: WorldMapScreenView
    private lateinit var townView: TownScreenView
    private lateinit var marketView: MarketScreenView
    private lateinit var inventoryView: InventoryScreenView

    /** Tracks the active market observation job so it can be cancelled on navigation away. */
    private var marketObserveJob: Job? = null
    /** Tracks the active inventory observation job so it can be cancelled on navigation away. */
    private var inventoryObserveJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = WanderingLedgerDatabase.create(this)
        gameRepository = GameRepository(database)
        marketRepository = MarketRepository(database)
        inventoryRepository = InventoryRepository(database)
        stepTrackerService = StepTrackerService(RoomStepBankRepository(database))

        worldMapView = WorldMapScreenView(this)
        townView = TownScreenView(this)
        marketView = MarketScreenView(this)
        inventoryView = InventoryScreenView(this)
        setContentView(worldMapView)

        scope.launch {
            withContext(Dispatchers.IO) {
                gameRepository.initializeNewGame()
            }
            refreshWorldMap("Welcome to the road.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        database.close()
    }

    private suspend fun refreshWorldMap(message: String? = null) {
        marketObserveJob?.cancel()
        inventoryObserveJob?.cancel()
        val screenState = withContext(Dispatchers.IO) {
            val player = gameRepository.observePlayerState().first()
            buildWorldMapScreenState(
                playerState = player,
                currentTown = gameRepository.observeCurrentTown().first(),
                routes = gameRepository.observeTravelRoutesFromCurrentTown().first(),
                message = message,
            )
        }

        setContentView(worldMapView)
        worldMapView.render(
            screenState,
            WorldMapActions(
                onTravel = TravelActionCallback { segmentId ->
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            gameRepository.travel(segmentId)
                        }
                        when (result) {
                            is TravelResult.Arrived -> showTownView(result.townId, result.remainingSteps)
                            else -> refreshWorldMap(result.toMessage())
                        }
                    }
                },
                onSimulateSteps = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            stepTrackerService.recordSensorDelta(75, StepSource.Simulation)
                        }
                        refreshWorldMap("Banked 75 simulated steps.")
                    }
                },
            ),
        )
    }

    private suspend fun showTownView(townId: Long, remainingSteps: Long) {
        marketObserveJob?.cancel()
        inventoryObserveJob?.cancel()
        val screenState = withContext(Dispatchers.IO) {
            val player = gameRepository.observePlayerState().first()
            val town = gameRepository.observeTown(townId).first()
                ?: error("Town $townId not found after arrival.")
            buildTownScreenState(
                town = town,
                bankedSteps = player.bankedSteps,
                gold = player.gold,
                message = "Arrived with $remainingSteps steps remaining.",
            )
        }

        setContentView(townView)
        townView.render(
            screenState,
            TownActions(
                onNavigateToWorldMap = TownNavigationCallback {
                    scope.launch { refreshWorldMap() }
                },
                onOpenMarket = TownMarketCallback {
                    scope.launch { showMarketView(townId) }
                },
                onOpenInventory = TownInventoryCallback {
                    scope.launch { showInventoryView(townId) }
                },
            ),
        )
    }

    private suspend fun showMarketView(townId: Long, message: String? = null) {
        marketObserveJob?.cancel()
        inventoryObserveJob?.cancel()

        // Render initial state
        val initialMarketState = withContext(Dispatchers.IO) {
            marketRepository.observeMarket(townId).first()
        }
        setContentView(marketView)
        marketView.render(
            buildMarketScreenState(initialMarketState, message),
            buildMarketActions(townId),
        )

        // Subscribe to reactive updates so prices/inventory changes re-render automatically
        marketObserveJob = scope.launch {
            marketRepository.observeMarket(townId).collect { marketState ->
                marketView.render(
                    buildMarketScreenState(marketState),
                    buildMarketActions(townId),
                )
            }
        }
    }

    private suspend fun showInventoryView(townId: Long) {
        marketObserveJob?.cancel()
        inventoryObserveJob?.cancel()

        // Render initial state
        val initialSummary = withContext(Dispatchers.IO) {
            inventoryRepository.observeInventorySummary(playerId = 1L).first()
        }
        setContentView(inventoryView)
        inventoryView.render(
            buildInventoryScreenState(initialSummary),
            buildInventoryActions(townId),
        )

        // Subscribe to reactive updates so inventory changes re-render automatically
        inventoryObserveJob = scope.launch {
            inventoryRepository.observeInventorySummary(playerId = 1L).collect { summary ->
                inventoryView.render(
                    buildInventoryScreenState(summary),
                    buildInventoryActions(townId),
                )
            }
        }
    }

    private fun buildInventoryActions(townId: Long): InventoryActions =
        InventoryActions(
            onNavigateBackToTown = InventoryNavigationCallback {
                scope.launch {
                    inventoryObserveJob?.cancel()
                    val player = withContext(Dispatchers.IO) {
                        gameRepository.observePlayerState().first()
                    }
                    val town = withContext(Dispatchers.IO) {
                        gameRepository.observeTown(townId).first()
                            ?: error("Town $townId not found.")
                    }
                    val townState = buildTownScreenState(
                        town = town,
                        bankedSteps = player.bankedSteps,
                        gold = player.gold,
                    )
                    setContentView(townView)
                    townView.render(
                        townState,
                        TownActions(
                            onNavigateToWorldMap = TownNavigationCallback {
                                scope.launch { refreshWorldMap() }
                            },
                            onOpenMarket = TownMarketCallback {
                                scope.launch { showMarketView(townId) }
                            },
                            onOpenInventory = TownInventoryCallback {
                                scope.launch { showInventoryView(townId) }
                            },
                        ),
                    )
                }
            },
            onSellItem = InventorySellCallback { goodId ->
                scope.launch { showMarketView(townId) }
            },
        )

    private fun buildMarketActions(townId: Long): MarketActions =
        MarketActions(
            onBuy = BuyActionCallback { goodId ->
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        marketRepository.buyGood(townId, goodId, quantity = 1)
                    }
                    val msg = result.toBuyMessage()
                    // Re-render with feedback message; the reactive flow will also fire
                    // but we want the message to appear immediately.
                    if (msg != null) {
                        val marketState = withContext(Dispatchers.IO) {
                            marketRepository.observeMarket(townId).first()
                        }
                        marketView.render(
                            buildMarketScreenState(marketState, msg),
                            buildMarketActions(townId),
                        )
                    }
                }
            },
            onSell = SellActionCallback { goodId ->
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        marketRepository.sellGood(townId, goodId, quantity = 1)
                    }
                    val msg = result.toSellMessage()
                    if (msg != null) {
                        val marketState = withContext(Dispatchers.IO) {
                            marketRepository.observeMarket(townId).first()
                        }
                        marketView.render(
                            buildMarketScreenState(marketState, msg),
                            buildMarketActions(townId),
                        )
                    }
                }
            },
            onNavigateBackToTown = MarketNavigationCallback {
                scope.launch {
                    marketObserveJob?.cancel()
                    val player = withContext(Dispatchers.IO) {
                        gameRepository.observePlayerState().first()
                    }
                    val town = withContext(Dispatchers.IO) {
                        gameRepository.observeTown(townId).first()
                            ?: error("Town $townId not found.")
                    }
                    val townState = buildTownScreenState(
                        town = town,
                        bankedSteps = player.bankedSteps,
                        gold = player.gold,
                    )
                    setContentView(townView)
                    townView.render(
                        townState,
                        TownActions(
                            onNavigateToWorldMap = TownNavigationCallback {
                                scope.launch { refreshWorldMap() }
                            },
                            onOpenMarket = TownMarketCallback {
                                scope.launch { showMarketView(townId) }
                            },
                            onOpenInventory = TownInventoryCallback {
                                scope.launch { showInventoryView(townId) }
                            },
                        ),
                    )
                }
            },
        )

    private fun TravelResult.toMessage(): String =
        when (this) {
            is TravelResult.Arrived -> "Arrived with $remainingSteps steps left."
            is TravelResult.NotEnoughSteps -> "Need ${required - available} more steps."
            TravelResult.NotAtRoadOrigin -> "That road starts elsewhere."
            TravelResult.RoadNotFound -> "Road not found."
        }

    private fun BuyResult.toBuyMessage(): String? =
        when (this) {
            is BuyResult.Success -> "Bought ${quantity}x for ${goldSpent}g. Gold remaining: ${remainingGold}g."
            is BuyResult.NotEnoughGold -> "Not enough gold. Need ${required}g, have ${available}g."
            BuyResult.InventoryFull -> "Inventory is full."
            BuyResult.GoodNotAvailable -> "That good is not available here."
            BuyResult.InvalidQuantity -> null
        }

    private fun SellResult.toSellMessage(): String? =
        when (this) {
            is SellResult.Success -> "Sold ${quantity}x for ${goldEarned}g. Gold: ${remainingGold}g."
            is SellResult.NotEnoughInventory -> "You only have ${available} of that good."
            SellResult.GoodNotAvailable -> "That good is not available here."
            SellResult.InvalidQuantity -> null
        }
}
