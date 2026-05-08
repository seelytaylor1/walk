package com.wanderingledger.app

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.flow.MutableStateFlow
import com.wanderingledger.core.data.BuyResult
import com.wanderingledger.core.data.GameRepository
import com.wanderingledger.core.data.InventoryRepository
import com.wanderingledger.core.data.MarketRepository
import com.wanderingledger.core.data.RoomStepBankRepository
import com.wanderingledger.core.data.RumorRepository
import com.wanderingledger.core.data.CompanionRepository
import com.wanderingledger.core.data.EncounterRepository
import com.wanderingledger.core.data.SellResult
import com.wanderingledger.core.data.TravelResult
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.ui.NavigationShell
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
import com.wanderingledger.feature.town.TownLedgerCallback
import com.wanderingledger.feature.town.TownCompanionsCallback
import com.wanderingledger.feature.town.buildInventoryScreenState
import com.wanderingledger.feature.town.buildMarketScreenState
import com.wanderingledger.feature.town.buildTownScreenState
import com.wanderingledger.feature.journey.JourneyActions
import com.wanderingledger.feature.journey.JourneyScreen
import com.wanderingledger.feature.journey.buildJourneyScreenState
import com.wanderingledger.feature.ledger.LedgerActions
import com.wanderingledger.feature.ledger.LedgerNavigationCallback
import com.wanderingledger.feature.ledger.LedgerScreenView
import com.wanderingledger.feature.ledger.buildLedgerScreenState
import com.wanderingledger.feature.companions.CompanionsActions
import com.wanderingledger.feature.companions.CompanionNavigationCallback
import com.wanderingledger.feature.companions.CompanionRecruitCallback
import com.wanderingledger.feature.companions.CompanionInteractCallback
import com.wanderingledger.feature.companions.CompanionsScreenView
import com.wanderingledger.feature.companions.buildCompanionsScreenState
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
    private lateinit var rumorRepository: RumorRepository
    private lateinit var companionRepository: CompanionRepository
    private lateinit var encounterRepository: EncounterRepository
    private lateinit var marketRepository: MarketRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var stepTrackerService: StepTrackerService

    private lateinit var journeyView: ComposeView
    private lateinit var townView: TownScreenView
    private lateinit var marketView: MarketScreenView
    private lateinit var inventoryView: InventoryScreenView
    private lateinit var ledgerView: LedgerScreenView
    private lateinit var companionsView: CompanionsScreenView
    private lateinit var navigationShell: NavigationShell

    private var currentTownId: Long = 1L

    private val journeyScreenState = MutableStateFlow(
        com.wanderingledger.feature.journey.JourneyScreenState(
            currentTownName = "",
            currentTownRegion = "",
            currentBiome = com.wanderingledger.core.model.Biome.Forest,
            bankedSteps = 0,
            lifetimeSteps = 0,
            routes = emptyList(),
            message = null
        )
    )

    private var marketObserveJob: Job? = null
    private var inventoryObserveJob: Job? = null
    private var ledgerObserveJob: Job? = null
    private var companionsObserveJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = WanderingLedgerDatabase.create(this)
        rumorRepository = RumorRepository(database)
        companionRepository = CompanionRepository(database)
        encounterRepository = EncounterRepository(database, companionRepository)
        gameRepository = GameRepository(database, rumorRepository, encounterRepository)
        marketRepository = MarketRepository(database)
        inventoryRepository = InventoryRepository(database)
        stepTrackerService = StepTrackerService(RoomStepBankRepository(database))

        journeyView = ComposeView(this).apply {
            setContent {
                val state = journeyScreenState.collectAsState()
                val actions = remember {
                    com.wanderingledger.feature.journey.JourneyActions(
                        onTravel = { segmentId ->
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    gameRepository.travel(segmentId)
                                }
                                when (result) {
                                    is TravelResult.Arrived -> showTownView(result.townId, result.remainingSteps)
                                    else -> refreshJourneyScreen(result.toMessage())
                                }
                            }
                        },
                        onSimulateSteps = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    stepTrackerService.recordSensorDelta(75, StepSource.Simulation)
                                }
                                refreshJourneyScreen("Banked 75 simulated steps.")
                            }
                        }
                    )
                }
                com.wanderingledger.feature.journey.JourneyScreen(
                    state = state.value,
                    actions = actions,
                    modifier = androidx.compose.ui.Modifier.fillMaxSize()
                )
            }
        }
        townView = TownScreenView(this)
        marketView = MarketScreenView(this)
        inventoryView = InventoryScreenView(this)
        ledgerView = LedgerScreenView(this)
        companionsView = CompanionsScreenView(this)

        navigationShell = NavigationShell(this, journeyView)
        setContentView(navigationShell)

        scope.launch {
            withContext(Dispatchers.IO) {
                gameRepository.initializeNewGame()
            }
            refreshJourneyScreen("Welcome to the road.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        database.close()
    }

    private suspend fun refreshJourneyScreen(message: String? = null) {
        cancelAllObservers()
        val screenState = withContext(Dispatchers.IO) {
            val player = gameRepository.observePlayerState().first()
            currentTownId = player.currentTownId
            val town = gameRepository.observeCurrentTown().first()
            buildJourneyScreenState(
                currentTownName = town.name,
                currentTownRegion = town.region,
                currentBiome = town.biome,
                bankedSteps = player.bankedSteps,
                lifetimeSteps = player.lifetimeSteps,
                routeDestinations = gameRepository.observeTravelRoutesFromCurrentTown().first().map { route ->
                    Triple(
                        route.segment.segmentId,
                        route.destination.name,
                        Pair(route.segment.stepCost, route.segment.narrativeDistance)
                    )
                },
                message = message,
            )
        }

        journeyScreenState.value = screenState
        navigationShell.replaceContent(journeyView)
        navigationShell.navigateTo(NavigationShell.ScreenType.JOURNEY, "Journey", null)
    }

    private suspend fun showTownView(townId: Long, remainingSteps: Long? = null) {
        cancelAllObservers()
        currentTownId = townId
        val screenState = withContext(Dispatchers.IO) {
            val player = gameRepository.observePlayerState().first()
            val town = gameRepository.observeTown(townId).first()
                ?: error("Town $townId not found.")
            buildTownScreenState(
                town = town,
                bankedSteps = player.bankedSteps,
                gold = player.gold,
                message = remainingSteps?.let { "Arrived with $it steps remaining." },
            )
        }

        navigationShell.replaceContent(townView)
        navigationShell.navigateTo(NavigationShell.ScreenType.TOWN, screenState.townName, screenState.townRegion)

        townView.render(
            screenState,
            TownActions(
                onNavigateToWorldMap = TownNavigationCallback {
                    scope.launch { refreshJourneyScreen() }
                },
                onOpenMarket = TownMarketCallback {
                    scope.launch { showMarketView(townId) }
                },
                onOpenInventory = TownInventoryCallback {
                    scope.launch { showInventoryView(townId) }
                },
                onOpenLedger = TownLedgerCallback {
                    scope.launch { showLedgerView(townId) }
                },
                onOpenCompanions = TownCompanionsCallback {
                    scope.launch { showCompanionsView(townId) }
                },
            ),
        )
    }

    private suspend fun showCompanionsView(townId: Long, message: String? = null) {
        cancelAllObservers()

        val active = withContext(Dispatchers.IO) {
            companionRepository.observeActiveCompanions().first()
        }
        val recruitable = withContext(Dispatchers.IO) {
            companionRepository.observeRecruitableCompanionsAtTown(townId).first()
        }

        navigationShell.replaceContent(companionsView)
        navigationShell.navigateTo(NavigationShell.ScreenType.COMPANIONS, "Party", null)

        companionsView.render(
            buildCompanionsScreenState(active, recruitable, message),
            buildCompanionsActions(townId)
        )

        companionsObserveJob = scope.launch {
            kotlinx.coroutines.flow.combine(
                companionRepository.observeActiveCompanions(),
                companionRepository.observeRecruitableCompanionsAtTown(townId)
            ) { a, r -> a to r }.collect { (a, r) ->
                companionsView.render(
                    buildCompanionsScreenState(a, r),
                    buildCompanionsActions(townId)
                )
            }
        }
    }

    private fun buildCompanionsActions(townId: Long): CompanionsActions =
        CompanionsActions(
            onNavigateBack = CompanionNavigationCallback {
                scope.launch { showTownView(townId) }
            },
            onRecruit = CompanionRecruitCallback { companionId ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        companionRepository.recruitCompanion(companionId)
                    }
                }
            },
            onInteract = CompanionInteractCallback { companionId ->
                scope.launch {
                    showTownView(townId)
                }
            }
        )

    private suspend fun showLedgerView(townId: Long) {
        cancelAllObservers()

        val initialRumors = withContext(Dispatchers.IO) {
            rumorRepository.observeActiveRumors().first()
        }

        navigationShell.replaceContent(ledgerView)
        navigationShell.navigateTo(NavigationShell.ScreenType.LEDGER, "Ledger", null)

        ledgerView.render(
            buildLedgerScreenState(initialRumors),
            LedgerActions(
                onNavigateBack = LedgerNavigationCallback {
                    scope.launch { showTownView(townId) }
                }
            )
        )

        ledgerObserveJob = scope.launch {
            rumorRepository.observeActiveRumors().collect { rumors ->
                ledgerView.render(
                    buildLedgerScreenState(rumors),
                    LedgerActions(
                        onNavigateBack = LedgerNavigationCallback {
                            scope.launch { showTownView(townId) }
                        }
                    )
                )
            }
        }
    }

    private suspend fun showMarketView(townId: Long, message: String? = null) {
        cancelAllObservers()

        val initialMarketState = withContext(Dispatchers.IO) {
            marketRepository.observeMarket(townId).first()
        }

        navigationShell.replaceContent(marketView)
        navigationShell.navigateTo(NavigationShell.ScreenType.MARKET, "Market", null)

        marketView.render(
            buildMarketScreenState(initialMarketState, message),
            buildMarketActions(townId),
        )

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
        cancelAllObservers()

        val initialSummary = withContext(Dispatchers.IO) {
            inventoryRepository.observeInventorySummary(playerId = 1L).first()
        }

        navigationShell.replaceContent(inventoryView)
        navigationShell.navigateTo(NavigationShell.ScreenType.INVENTORY, "Inventory", null)

        inventoryView.render(
            buildInventoryScreenState(initialSummary),
            buildInventoryActions(townId),
        )

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
                    showTownView(townId)
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
                    showTownView(townId)
                }
            },
        )

    private fun cancelAllObservers() {
        marketObserveJob?.cancel()
        inventoryObserveJob?.cancel()
        ledgerObserveJob?.cancel()
        companionsObserveJob?.cancel()
    }

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