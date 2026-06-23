package com.wanderingledger.app

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.wanderingledger.core.audio.AudioEvent
import com.wanderingledger.core.audio.AudioManager
import com.wanderingledger.core.audio.AudioPreferences
import com.wanderingledger.core.data.BuyResult
import com.wanderingledger.core.data.CompanionCommentaryContext
import com.wanderingledger.core.data.CompanionCommentaryEngine
import com.wanderingledger.core.data.CompanionCommentaryResult
import com.wanderingledger.core.data.CompanionRepository
import com.wanderingledger.core.data.GameRepository
import com.wanderingledger.core.data.InventoryRepository
import com.wanderingledger.core.data.MarketRepository
import com.wanderingledger.core.data.RecruitmentResult
import com.wanderingledger.core.data.RoomStepBankRepository
import com.wanderingledger.core.data.RumorRepository
import com.wanderingledger.core.data.SellResult
import com.wanderingledger.core.data.TravelResult
import com.wanderingledger.core.data.requestCommentary
import com.wanderingledger.core.database.RoadSegmentEntity
import com.wanderingledger.core.database.TownEntity
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.designsystem.accessibility.AccessibilityPreferences
import com.wanderingledger.core.haptics.HapticEffect
import com.wanderingledger.core.haptics.HapticManager
import com.wanderingledger.core.model.Biome
import com.wanderingledger.core.model.RoadSegment
import com.wanderingledger.core.model.Town
import com.wanderingledger.core.steptracker.StepSource
import com.wanderingledger.core.steptracker.StepTrackerService
import com.wanderingledger.core.ui.BottomNavBar
import com.wanderingledger.core.ui.NavigationShell
import com.wanderingledger.feature.companions.CompanionCommentaryUi
import com.wanderingledger.feature.companions.CompanionInteractCallback
import com.wanderingledger.feature.companions.CompanionNavigationCallback
import com.wanderingledger.feature.companions.CompanionRecruitCallback
import com.wanderingledger.feature.companions.CompanionsActions
import com.wanderingledger.feature.companions.CompanionsScreenView
import com.wanderingledger.feature.companions.buildCompanionsScreenState
import com.wanderingledger.feature.journey.JourneyActions
import com.wanderingledger.feature.journey.JourneyScreen
import com.wanderingledger.feature.journey.buildJourneyScreenState
import com.wanderingledger.feature.ledger.ChronicleActions
import com.wanderingledger.feature.ledger.ChronicleNavigationCallback
import com.wanderingledger.feature.ledger.ChronicleScreenView
import com.wanderingledger.feature.ledger.LedgerActions
import com.wanderingledger.feature.ledger.LedgerNavigationCallback
import com.wanderingledger.feature.ledger.LedgerScreenView
import com.wanderingledger.feature.ledger.buildChronicleUiState
import com.wanderingledger.feature.ledger.buildLedgerScreenState
import com.wanderingledger.feature.ledger.model.ChronicleEntry
import com.wanderingledger.feature.ledger.model.ChronicleIcon
import com.wanderingledger.feature.ledger.model.EntryType
import com.wanderingledger.feature.settings.SettingsActions
import com.wanderingledger.feature.settings.SettingsNavigationCallback
import com.wanderingledger.feature.settings.SettingsScreenState
import com.wanderingledger.feature.settings.SettingsScreenView
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
import com.wanderingledger.feature.town.TownChronicleCallback
import com.wanderingledger.feature.town.TownCompanionsCallback
import com.wanderingledger.feature.town.TownInventoryCallback
import com.wanderingledger.feature.town.TownLedgerCallback
import com.wanderingledger.feature.town.TownMarketCallback
import com.wanderingledger.feature.town.TownNavigationCallback
import com.wanderingledger.feature.town.TownScreenView
import com.wanderingledger.feature.town.buildInventoryScreenState
import com.wanderingledger.feature.town.buildMarketScreenState
import com.wanderingledger.feature.town.buildTownScreenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var database: WanderingLedgerDatabase
    private lateinit var gameRepository: GameRepository
    private lateinit var rumorRepository: RumorRepository
    private lateinit var companionRepository: CompanionRepository
    private lateinit var companionCommentaryEngine: CompanionCommentaryEngine
    private lateinit var marketRepository: MarketRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var stepTrackerService: StepTrackerService

    // Audio & haptics
    private lateinit var audioPreferences: AudioPreferences
    private lateinit var audioManager: AudioManager
    private lateinit var hapticManager: HapticManager

    // Accessibility
    private lateinit var accessibilityPreferences: AccessibilityPreferences

    // Sensors
    private var sensorManager: SensorManager? = null
    private var stepCounterSensor: Sensor? = null
    private var lastSensorValue: Float = -1f
    private var stepSensorRegistered: Boolean = false

    private val stepSensorListener =
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

                val sensorValue = event.values[0]
                if (lastSensorValue < 0) {
                    lastSensorValue = sensorValue
                    return
                }

                val delta = (sensorValue - lastSensorValue).toInt()
                if (delta > 0) {
                    lastSensorValue = sensorValue
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            stepTrackerService.recordSensorDelta(delta, StepSource.Hardware)
                        }
                        refreshActiveScreenStepCount()
                    }
                }
            }

            override fun onAccuracyChanged(
                sensor: Sensor?,
                accuracy: Int,
            ) {}
        }

    private lateinit var journeyView: ComposeView
    private lateinit var worldMapView: ComposeView
    private lateinit var townArrivalView: ComposeView
    private lateinit var townView: TownScreenView
    private lateinit var marketView: MarketScreenView
    private lateinit var inventoryView: InventoryScreenView
    private lateinit var ledgerView: LedgerScreenView
    private lateinit var chronicleView: ChronicleScreenView
    private lateinit var companionsView: CompanionsScreenView
    private lateinit var settingsView: SettingsScreenView
    private lateinit var navigationShell: NavigationShell

    private var currentScreenType: NavigationShell.ScreenType = NavigationShell.ScreenType.WORLD_MAP
    private var currentTownId: Long = 1L
    private var latestCompanionCommentary: CompanionCommentaryUi? = null

    private lateinit var journeyViewModel: JourneyViewModel

    private val worldMapScreenState =
        MutableStateFlow(
            com.wanderingledger.feature.worldmap.WorldMapScreenState(
                currentTownName = "",
                currentTownRegion = "",
                bankedSteps = 0,
                lifetimeSteps = 0,
                routes = emptyList(),
                message = null,
            ),
        )

    private val townArrivalScreenState =
        MutableStateFlow(
            com.wanderingledger.feature.town.TownArrivalScreenState(
                townName = "",
                townRegion = "",
                biome = com.wanderingledger.core.model.Biome.Forest,
            ),
        )

    private var marketObserveJob: Job? = null
    private var inventoryObserveJob: Job? = null
    private var ledgerObserveJob: Job? = null
    private var chronicleObserveJob: Job? = null
    private var companionsObserveJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as WanderingLedgerApp).container
        database = container.database
        rumorRepository = container.rumorRepository
        companionRepository = container.companionRepository
        companionCommentaryEngine = container.companionCommentaryEngine
        gameRepository = container.gameRepository
        marketRepository = container.marketRepository
        inventoryRepository = container.inventoryRepository
        stepTrackerService = container.stepTrackerService

        audioPreferences = container.audioPreferences
        audioManager = AudioManager(this, audioPreferences, scope)
        hapticManager = HapticManager(this, audioPreferences, scope)
        accessibilityPreferences = container.accessibilityPreferences

        val journeyViewModelFactory =
            viewModelFactory {
                initializer {
                    JourneyViewModel(
                        gameRepository = gameRepository,
                        companionRepository = companionRepository,
                        companionCommentaryEngine = companionCommentaryEngine,
                        stepTrackerService = stepTrackerService,
                        accessibilityPreferences = accessibilityPreferences,
                    )
                }
            }
        journeyViewModel = ViewModelProvider(this, journeyViewModelFactory)[JourneyViewModel::class.java]

        scope.launch {
            journeyViewModel.effects.collect { effect -> handleJourneyEffect(effect) }
        }

        // Initialize Sensors
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounterSensor == null) {
            Log.w("MainActivity", "Step counter sensor not available on this device.")
        }

        journeyView =
            ComposeView(this).apply {
                setContent {
                    val state = journeyViewModel.state.collectAsState()
                    val actions =
                        remember {
                            com.wanderingledger.feature.journey.JourneyActions(
                                onTravel = { segmentId -> journeyViewModel.onTravel(segmentId) },
                                onSimulateSteps = { journeyViewModel.onSimulateSteps() },
                                onMakeCamp = { journeyViewModel.onMakeCamp() },
                                onWakeFromCamp = { journeyViewModel.onWakeFromCamp() },
                            )
                        }
                    com.wanderingledger.feature.journey.JourneyScreen(
                        state = state.value,
                        actions = actions,
                        modifier =
                            androidx.compose.ui.Modifier
                                .fillMaxSize(),
                    )
                }
            }

        worldMapView =
            ComposeView(this).apply {
                setContent {
                    val state = worldMapScreenState.collectAsState()
                    val actions =
                        remember {
                            com.wanderingledger.feature.worldmap.WorldMapActions(
                                onTravel = { segmentId ->
                                    scope.launch {
                                        audioManager.play(AudioEvent.TravelBegin)
                                        hapticManager.perform(HapticEffect.SOFT_TAP)
                                        val result =
                                            withContext(Dispatchers.IO) {
                                                gameRepository.travel(segmentId)
                                            }
                                        when (result) {
                                            is TravelResult.Arrived -> {
                                                journeyViewModel.notifyTraveled()
                                                audioManager.play(AudioEvent.TownArrival)
                                                hapticManager.perform(HapticEffect.REWARD)
                                                showTownArrival(result.townId, result.remainingSteps)
                                            }
                                            else -> {
                                                if (result is TravelResult.NotEnoughSteps) {
                                                    hapticManager.perform(HapticEffect.ERROR)
                                                }
                                                // If we started traveling, switch to Journey screen
                                                showJourney(result.toMessage())
                                            }
                                        }
                                    }
                                },
                                onSimulateSteps = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            stepTrackerService.recordSensorDelta(75, StepSource.Simulation)
                                        }
                                        refreshWorldMapScreen("Banked 75 simulated steps.")
                                    }
                                },
                            )
                        }
                    com.wanderingledger.feature.worldmap.WorldMapScreen(
                        state = state.value,
                        actions = actions,
                        modifier =
                            androidx.compose.ui.Modifier
                                .fillMaxSize(),
                    )
                }
            }

        townArrivalView =
            ComposeView(this).apply {
                setContent {
                    val state = townArrivalScreenState.collectAsState()
                    com.wanderingledger.feature.town.TownArrivalScreen(
                        state = state.value,
                        onFinish = {
                            scope.launch {
                                showTownView(currentTownId)
                            }
                        },
                    )
                }
            }

        townView = TownScreenView(this)
        marketView = MarketScreenView(this)
        inventoryView = InventoryScreenView(this)
        ledgerView = LedgerScreenView(this)
        chronicleView = ChronicleScreenView(this)
        companionsView = CompanionsScreenView(this)
        settingsView = SettingsScreenView(this)

        navigationShell = NavigationShell(this, worldMapView)

        navigationShell.onNavigateToDestination = { destination ->
            scope.launch {
                when (destination) {
                    BottomNavBar.Destination.WORLD_MAP -> refreshWorldMapScreen()
                    BottomNavBar.Destination.TOWN -> showTownView(currentTownId)
                    BottomNavBar.Destination.LEDGER -> showLedgerView(currentTownId)
                    BottomNavBar.Destination.COMPANIONS -> showCompanionsView(currentTownId)
                }
            }
        }

        navigationShell.onReselectTab = { destination ->
            scope.launch {
                when (destination) {
                    BottomNavBar.Destination.WORLD_MAP -> refreshWorldMapScreen()
                    BottomNavBar.Destination.TOWN -> showTownView(currentTownId)
                    BottomNavBar.Destination.LEDGER -> showLedgerView(currentTownId)
                    BottomNavBar.Destination.COMPANIONS -> showCompanionsView(currentTownId)
                }
            }
        }

        navigationShell.onRestoreScreen = { screenType ->
            scope.launch {
                when (screenType) {
                    NavigationShell.ScreenType.JOURNEY -> showJourney()
                    NavigationShell.ScreenType.WORLD_MAP -> refreshWorldMapScreen()
                    NavigationShell.ScreenType.TOWN -> showTownView(currentTownId)
                    NavigationShell.ScreenType.TOWN_ARRIVAL -> { /* no-op */ }
                    NavigationShell.ScreenType.MARKET -> showMarketView(currentTownId)
                    NavigationShell.ScreenType.INVENTORY -> showInventoryView(currentTownId)
                    NavigationShell.ScreenType.LEDGER -> showLedgerView(currentTownId)
                    NavigationShell.ScreenType.CHRONICLE -> showChronicleView(currentTownId)
                    NavigationShell.ScreenType.COMPANIONS -> showCompanionsView(currentTownId)
                    NavigationShell.ScreenType.SETTINGS -> showSettingsView(currentTownId)
                }
            }
        }

        setContentView(navigationShell)

        scope.launch {
            withContext(Dispatchers.IO) {
                gameRepository.initializeNewGame()
            }
            refreshWorldMapScreen("Welcome to the road.")
        }
    }

    override fun onResume() {
        super.onResume()
        stepCounterSensor?.let {
            sensorManager?.registerListener(stepSensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
            stepSensorRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (stepSensorRegistered) {
            sensorManager?.unregisterListener(stepSensorListener)
            stepSensorRegistered = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
        scope.cancel()
        // The database is owned by AppContainer (process-scoped); do not close it here.
    }

    private suspend fun refreshActiveScreenStepCount() {
        when (currentScreenType) {
            NavigationShell.ScreenType.WORLD_MAP -> refreshWorldMapScreen()
            NavigationShell.ScreenType.JOURNEY -> journeyViewModel.refresh()
            else -> { /* no-op */ }
        }
    }

    private suspend fun refreshWorldMapScreen(message: String? = null) {
        cancelAllObservers()
        currentScreenType = NavigationShell.ScreenType.WORLD_MAP
        val screenState =
            withContext(Dispatchers.IO) {
                val player = gameRepository.observePlayerState().first()
                currentTownId = player.currentTownId
                val town = gameRepository.observeCurrentTown().first()
                val allTowns =
                    database
                        .townDao()
                        .listTowns()
                        .first()
                        .map { it.toMapModel() }
                val allRoads =
                    database
                        .roadSegmentDao()
                        .listRoads()
                        .first()
                        .map { it.toMapModel() }
                val availableRoutes = gameRepository.observeTravelRoutesFromCurrentTown().first()

                com.wanderingledger.feature.worldmap.buildWorldMapScreenState(
                    playerState = player,
                    currentTown = town,
                    allTowns = allTowns,
                    availableRoutes = availableRoutes,
                    allRoads = allRoads,
                    message = message,
                )
            }

        worldMapScreenState.value = screenState
        navigationShell.replaceContent(worldMapView)
        navigationShell.navigateTo(NavigationShell.ScreenType.WORLD_MAP, "World Map", null)
    }

    /**
     * Show the journey screen. State building lives in [JourneyViewModel.refresh];
     * the Activity only swaps the view and updates navigation. The ambient bed is
     * (re)started by the [JourneyEffect.StartAmbient] effect that `refresh` emits.
     */
    private fun showJourney(message: String? = null) {
        cancelAllObservers()
        currentScreenType = NavigationShell.ScreenType.JOURNEY
        scope.launch {
            currentTownId = gameRepository.observePlayerState().first().currentTownId
        }
        journeyViewModel.refresh(message)
        navigationShell.replaceContent(journeyView)
        navigationShell.navigateTo(NavigationShell.ScreenType.JOURNEY, "Journey", null)
    }

    private fun handleJourneyEffect(effect: JourneyEffect) {
        when (effect) {
            JourneyEffect.TravelBegin -> {
                audioManager.play(AudioEvent.TravelBegin)
                hapticManager.perform(HapticEffect.SOFT_TAP)
            }
            JourneyEffect.TravelBlocked -> hapticManager.perform(HapticEffect.ERROR)
            is JourneyEffect.Arrived -> {
                audioManager.play(AudioEvent.TownArrival)
                hapticManager.perform(HapticEffect.REWARD)
                scope.launch { showTownArrival(effect.townId, effect.remainingSteps) }
            }
            is JourneyEffect.StartAmbient -> audioManager.startAmbient(effect.biome)
            is JourneyEffect.CommentaryGenerated -> {
                latestCompanionCommentary = effect.commentary
            }
        }
    }

    private suspend fun showTownArrival(
        townId: Long,
        remainingSteps: Long? = null,
    ) {
        cancelAllObservers()
        currentScreenType = NavigationShell.ScreenType.TOWN_ARRIVAL
        currentTownId = townId
        val town =
            withContext(Dispatchers.IO) {
                gameRepository.observeTown(townId).first() ?: error("Town $townId not found.")
            }
        townArrivalScreenState.value =
            com.wanderingledger.feature.town.TownArrivalScreenState(
                townName = town.name,
                townRegion = town.region,
                biome = town.biome,
            )
        audioManager.startAmbient(town.biome)
        navigationShell.replaceContent(townArrivalView)
        navigationShell.navigateTo(NavigationShell.ScreenType.TOWN_ARRIVAL, "Arrival", null)
    }

    private suspend fun showTownView(
        townId: Long,
        remainingSteps: Long? = null,
    ) {
        cancelAllObservers()
        currentScreenType = NavigationShell.ScreenType.TOWN
        currentTownId = townId
        val screenState =
            withContext(Dispatchers.IO) {
                val player = gameRepository.observePlayerState().first()
                val town =
                    gameRepository.observeTown(townId).first()
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
                onNavigateToWorldMap =
                    TownNavigationCallback {
                        scope.launch { refreshWorldMapScreen() }
                    },
                onOpenMarket =
                    TownMarketCallback {
                        scope.launch { showMarketView(townId) }
                    },
                onOpenInventory =
                    TownInventoryCallback {
                        scope.launch { showInventoryView(townId) }
                    },
                onOpenLedger =
                    TownLedgerCallback {
                        scope.launch { showLedgerView(townId) }
                    },
                onOpenChronicle =
                    TownChronicleCallback {
                        scope.launch { showChronicleView(townId) }
                    },
                onOpenCompanions =
                    TownCompanionsCallback {
                        scope.launch { showCompanionsView(townId) }
                    },
                onOpenSettings =
                    com.wanderingledger.feature.town.TownSettingsCallback {
                        scope.launch { showSettingsView(townId) }
                    },
            ),
        )
    }

    private suspend fun showCompanionsView(
        townId: Long,
        message: String? = null,
    ) {
        cancelAllObservers()
        currentScreenType = NavigationShell.ScreenType.COMPANIONS

        val active =
            withContext(Dispatchers.IO) {
                companionRepository.observeActiveCompanions().first()
            }
        val recruitable =
            withContext(Dispatchers.IO) {
                companionRepository.observeRecruitableCompanionsAtTown(townId).first()
            }
        val reduceMotion = accessibilityPreferences.reduceMotion.first()

        navigationShell.replaceContent(companionsView)
        navigationShell.navigateTo(NavigationShell.ScreenType.COMPANIONS, "Party", null)

        companionsView.render(
            buildCompanionsScreenState(
                active = active,
                recruitable = recruitable,
                message = message,
                recentCommentary = latestCompanionCommentary,
                reducedMotion = reduceMotion,
            ),
            buildCompanionsActions(townId),
        )

        companionsObserveJob =
            scope.launch {
                kotlinx.coroutines.flow
                    .combine(
                        companionRepository.observeActiveCompanions(),
                        companionRepository.observeRecruitableCompanionsAtTown(townId),
                        accessibilityPreferences.reduceMotion,
                    ) { a, r, rm -> Triple(a, r, rm) }
                    .collect { (a, r, rm) ->
                        companionsView.render(
                            buildCompanionsScreenState(
                                a,
                                r,
                                recentCommentary = latestCompanionCommentary,
                                reducedMotion = rm,
                            ),
                            buildCompanionsActions(townId),
                        )
                    }
            }
    }

    private fun buildCompanionsActions(townId: Long): CompanionsActions =
        CompanionsActions(
            onNavigateBack =
                CompanionNavigationCallback {
                    scope.launch { showTownView(townId) }
                },
            onRecruit =
                CompanionRecruitCallback { companionId ->
                    scope.launch {
                        val result =
                            withContext(Dispatchers.IO) {
                                companionRepository.recruitCompanion(companionId)
                            }
                        val message =
                            when (result) {
                                RecruitmentResult.Success -> "A new voice joins the road."
                                RecruitmentResult.AlreadyActive -> "They are already traveling with you."
                                RecruitmentResult.PartyFull -> "The party is full."
                                RecruitmentResult.NotFound -> "That companion is not available here."
                            }
                        renderCompanionsView(townId, message)
                    }
                },
            onInteract =
                CompanionInteractCallback { companionId ->
                    scope.launch {
                        val player =
                            withContext(Dispatchers.IO) {
                                gameRepository.observePlayerState().first()
                            }
                        val town =
                            withContext(Dispatchers.IO) {
                                gameRepository.observeTown(townId).first()
                            }
                        val result =
                            withContext(Dispatchers.IO) {
                                companionRepository.requestCommentary(
                                    companionId = companionId,
                                    context =
                                        if (player.bankedSteps < 80L) {
                                            CompanionCommentaryContext.LowSteps
                                        } else {
                                            CompanionCommentaryContext.Town
                                        },
                                    engine = companionCommentaryEngine,
                                    biome = town?.biome,
                                    bankedSteps = player.bankedSteps,
                                )
                            }
                        val message =
                            when (result) {
                                is CompanionCommentaryResult.Spoken -> {
                                    latestCompanionCommentary = result.commentary.toUi()
                                    withContext(Dispatchers.IO) {
                                        companionRepository.updateBond(companionId, 1)
                                    }
                                    audioManager.play(AudioEvent.BondIncrease)
                                    hapticManager.perform(HapticEffect.REWARD)
                                    null
                                }
                                is CompanionCommentaryResult.OnCooldown ->
                                    "${result.companionName} is still considering the last thing they said."
                                CompanionCommentaryResult.NotActive ->
                                    "Only active companions can answer from the road."
                            }
                        renderCompanionsView(townId, message)
                    }
                },
        )

    private suspend fun renderCompanionsView(
        townId: Long,
        message: String? = null,
    ) {
        val active =
            withContext(Dispatchers.IO) {
                companionRepository.observeActiveCompanions().first()
            }
        val recruitable =
            withContext(Dispatchers.IO) {
                companionRepository.observeRecruitableCompanionsAtTown(townId).first()
            }
        val reduceMotion = accessibilityPreferences.reduceMotion.first()
        companionsView.render(
            buildCompanionsScreenState(
                active = active,
                recruitable = recruitable,
                message = message,
                recentCommentary = latestCompanionCommentary,
                reducedMotion = reduceMotion,
            ),
            buildCompanionsActions(townId),
        )
    }

    private suspend fun showLedgerView(townId: Long) {
        cancelAllObservers()
        currentScreenType = NavigationShell.ScreenType.LEDGER

        audioManager.play(AudioEvent.LedgerOpen)
        hapticManager.perform(HapticEffect.SOFT_TAP)

        val initialRumors =
            withContext(Dispatchers.IO) {
                rumorRepository.observeActiveRumors().first()
            }

        navigationShell.replaceContent(ledgerView)
        navigationShell.navigateTo(NavigationShell.ScreenType.LEDGER, "Ledger", null)

        ledgerView.render(
            buildLedgerScreenState(initialRumors),
            LedgerActions(
                onNavigateBack =
                    LedgerNavigationCallback {
                        scope.launch { showTownView(townId) }
                    },
            ),
        )

        ledgerObserveJob =
            scope.launch {
                rumorRepository.observeActiveRumors().collect { rumors ->
                    ledgerView.render(
                        buildLedgerScreenState(rumors),
                        LedgerActions(
                            onNavigateBack =
                                LedgerNavigationCallback {
                                    scope.launch { showTownView(townId) }
                                },
                        ),
                    )
                }
            }
    }

    private suspend fun showChronicleView(townId: Long) {
        cancelAllObservers()
        currentScreenType = NavigationShell.ScreenType.CHRONICLE

        val initialEntries =
            withContext(Dispatchers.IO) {
                buildChronicleEntries()
            }

        navigationShell.replaceContent(chronicleView)
        navigationShell.navigateTo(NavigationShell.ScreenType.CHRONICLE, "Chronicle", null)

        chronicleView.render(
            buildChronicleUiState(initialEntries),
            ChronicleActions(
                onNavigateBack =
                    ChronicleNavigationCallback {
                        scope.launch { showTownView(townId) }
                    },
            ),
        )

        chronicleObserveJob =
            scope.launch {
                kotlinx.coroutines.flow
                    .combine(
                        database.eventLogDao().listRecentEvents(80),
                        companionRepository.observeActiveCompanions(),
                    ) { _, _ ->
                        withContext(Dispatchers.IO) { buildChronicleEntries() }
                    }.collect { entries ->
                        chronicleView.render(
                            buildChronicleUiState(entries),
                            ChronicleActions(
                                onNavigateBack =
                                    ChronicleNavigationCallback {
                                        scope.launch { showTownView(townId) }
                                    },
                            ),
                        )
                    }
            }
    }

    private suspend fun showMarketView(
        townId: Long,
        message: String? = null,
    ) {
        cancelAllObservers()
        currentScreenType = NavigationShell.ScreenType.MARKET

        val initialMarketState =
            withContext(Dispatchers.IO) {
                marketRepository.observeMarket(townId).first()
            }

        navigationShell.replaceContent(marketView)
        navigationShell.navigateTo(NavigationShell.ScreenType.MARKET, "Market", null)

        marketView.render(
            buildMarketScreenState(initialMarketState, message),
            buildMarketActions(townId),
        )

        marketObserveJob =
            scope.launch {
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
        currentScreenType = NavigationShell.ScreenType.INVENTORY

        val initialSummary =
            withContext(Dispatchers.IO) {
                inventoryRepository.observeInventorySummary(playerId = 1L).first()
            }

        navigationShell.replaceContent(inventoryView)
        navigationShell.navigateTo(NavigationShell.ScreenType.INVENTORY, "Inventory", null)

        inventoryView.render(
            buildInventoryScreenState(initialSummary),
            buildInventoryActions(townId),
        )

        inventoryObserveJob =
            scope.launch {
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
            onNavigateBackToTown =
                InventoryNavigationCallback {
                    scope.launch {
                        inventoryObserveJob?.cancel()
                        showTownView(townId)
                    }
                },
            onSellItem =
                InventorySellCallback { goodId ->
                    scope.launch { showMarketView(townId) }
                },
        )

    private fun buildMarketActions(townId: Long): MarketActions =
        MarketActions(
            onBuy =
                BuyActionCallback { goodId ->
                    scope.launch {
                        val result =
                            withContext(Dispatchers.IO) {
                                marketRepository.buyGood(townId, goodId, quantity = 1)
                            }
                        if (result is BuyResult.Success) {
                            audioManager.play(AudioEvent.MarketBuy)
                            hapticManager.perform(HapticEffect.CONFIRM)
                        } else if (result is BuyResult.NotEnoughGold || result == BuyResult.InventoryFull) {
                            hapticManager.perform(HapticEffect.ERROR)
                        }
                        val msg = result.toBuyMessage()
                        if (msg != null) {
                            val marketState =
                                withContext(Dispatchers.IO) {
                                    marketRepository.observeMarket(townId).first()
                                }
                            marketView.render(
                                buildMarketScreenState(marketState, msg),
                                buildMarketActions(townId),
                            )
                        }
                    }
                },
            onSell =
                SellActionCallback { goodId ->
                    scope.launch {
                        val result =
                            withContext(Dispatchers.IO) {
                                marketRepository.sellGood(townId, goodId, quantity = 1)
                            }
                        if (result is SellResult.Success) {
                            audioManager.play(AudioEvent.MarketSell)
                            hapticManager.perform(HapticEffect.CONFIRM)
                        } else if (result is SellResult.NotEnoughInventory) {
                            hapticManager.perform(HapticEffect.ERROR)
                        }
                        val msg = result.toSellMessage()
                        if (msg != null) {
                            val marketState =
                                withContext(Dispatchers.IO) {
                                    marketRepository.observeMarket(townId).first()
                                }
                            marketView.render(
                                buildMarketScreenState(marketState, msg),
                                buildMarketActions(townId),
                            )
                        }
                    }
                },
            onNavigateBackToTown =
                MarketNavigationCallback {
                    scope.launch {
                        marketObserveJob?.cancel()
                        showTownView(townId)
                    }
                },
        )

    private suspend fun buildChronicleEntries(): List<ChronicleEntry> {
        val towns =
            database
                .townDao()
                .listTowns()
                .first()
                .associateBy { it.townId }
        val roads =
            database
                .roadSegmentDao()
                .listRoads()
                .first()
                .associateBy { it.segmentId }
        val events = database.eventLogDao().listRecentEvents(80).first()
        val companions = companionRepository.observeActiveCompanions().first()

        val eventEntries =
            events.map { event ->
                when (event.type) {
                    "arrival" -> {
                        val meta = JSONObject(event.meta)
                        val segmentId = meta.optLong("segmentId")
                        val toTownId = meta.optLong("toTownId")
                        val road = roads[segmentId]
                        val fromTown = road?.let { towns[it.fromTownId] }
                        val toTown = towns[toTownId]
                        ChronicleEntry(
                            id = event.eventId,
                            type = EntryType.TRAVEL,
                            title = "Arrived in ${toTown?.name ?: "a new town"}",
                            summary = event.result,
                            townName =
                                listOfNotNull(
                                    toTown?.name,
                                    toTown?.region,
                                ).joinToString(", ").ifBlank { "Unknown road" },
                            timestampMs = event.createdAt,
                            routeLabel =
                                if (fromTown != null && toTown != null) {
                                    "${fromTown.name} to ${toTown.name}"
                                } else {
                                    null
                                },
                            icon = ChronicleIcon.MAP,
                        )
                    }
                    "encounter" -> {
                        val meta = JSONObject(event.meta)
                        val goldChange = meta.optLong("goldChange", 0L).takeIf { it != 0L }
                        ChronicleEntry(
                            id = event.eventId,
                            type = EntryType.ENCOUNTER,
                            title = "Road encounter",
                            summary = event.result,
                            townName = "On the road",
                            timestampMs = event.createdAt,
                            goldDelta = goldChange,
                            icon = ChronicleIcon.FIST,
                        )
                    }
                    else ->
                        ChronicleEntry(
                            id = event.eventId,
                            type = EntryType.RUMOR,
                            title = event.type.replaceFirstChar { it.titlecase() },
                            summary = event.result,
                            townName = "Ledger note",
                            timestampMs = event.createdAt,
                            icon = ChronicleIcon.DIALOG,
                        )
                }
            }

        val lastEventTime = events.maxOfOrNull { it.createdAt } ?: System.currentTimeMillis()
        val companionEntries =
            companions.mapIndexed { index, companion ->
                ChronicleEntry(
                    id = Long.MIN_VALUE + companion.companionId,
                    type = EntryType.COMPANION,
                    title = "${companion.name} travels with you",
                    summary = "A companion's presence has become part of the journey's record.",
                    townName = "Party",
                    timestampMs = lastEventTime - index - 1,
                    companionName = companion.name,
                    companionNote = "${companion.role.name.lowercase().replaceFirstChar {
                        it.titlecase()
                    }} bond ${companion.bondLevel}; ${companion.questState}.",
                    icon = ChronicleIcon.DIALOG,
                )
            }

        return eventEntries + companionEntries
    }

    private suspend fun showSettingsView(returnToTownId: Long) {
        cancelAllObservers()
        currentScreenType = NavigationShell.ScreenType.SETTINGS
        navigationShell.replaceContent(settingsView)
        navigationShell.navigateTo(NavigationShell.ScreenType.SETTINGS, "Settings", null)
        renderSettingsView(returnToTownId)
    }

    private fun renderSettingsView(returnToTownId: Long) {
        scope.launch {
            val sfxEnabled = audioPreferences.sfxEnabled.first()
            val ambientEnabled = audioPreferences.ambientEnabled.first()
            val hapticsEnabled = audioPreferences.hapticsEnabled.first()
            val sfxVolume = (audioPreferences.sfxVolume.first() * 100).toInt()
            val ambientVolume = (audioPreferences.ambientVolume.first() * 100).toInt()
            val reduceMotion = accessibilityPreferences.reduceMotion.first()
            val contrastMode = accessibilityPreferences.contrastMode.first()

            settingsView.render(
                com.wanderingledger.feature.settings.SettingsScreenState(
                    sfxEnabled = sfxEnabled,
                    ambientEnabled = ambientEnabled,
                    hapticsEnabled = hapticsEnabled,
                    sfxVolume = sfxVolume,
                    ambientVolume = ambientVolume,
                    reduceMotion = reduceMotion,
                    contrastMode = contrastMode,
                ),
                com.wanderingledger.feature.settings.SettingsActions(
                    onNavigateBack =
                        SettingsNavigationCallback {
                            scope.launch { showTownView(returnToTownId) }
                        },
                    onSfxEnabledChanged = { enabled ->
                        scope.launch { audioPreferences.setSfxEnabled(enabled) }
                    },
                    onAmbientEnabledChanged = { enabled ->
                        scope.launch {
                            audioPreferences.setAmbientEnabled(enabled)
                            if (!enabled) audioManager.stopAmbient()
                        }
                    },
                    onHapticsEnabledChanged = { enabled ->
                        scope.launch { audioPreferences.setHapticsEnabled(enabled) }
                    },
                    onSfxVolumeChanged = { volume ->
                        scope.launch { audioPreferences.setSfxVolume(volume) }
                    },
                    onAmbientVolumeChanged = { volume ->
                        scope.launch {
                            audioPreferences.setAmbientVolume(volume)
                            audioManager.applyAmbientVolume()
                        }
                    },
                    onReduceMotionChanged = { enabled ->
                        scope.launch { accessibilityPreferences.setReduceMotion(enabled) }
                    },
                    onContrastModeChanged = { mode ->
                        scope.launch { accessibilityPreferences.setContrastMode(mode) }
                    },
                ),
            )
        }
    }

    private fun cancelAllObservers() {
        marketObserveJob?.cancel()
        inventoryObserveJob?.cancel()
        ledgerObserveJob?.cancel()
        chronicleObserveJob?.cancel()
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
            is SellResult.NotEnoughInventory -> "You only have $available of that good."
            SellResult.GoodNotAvailable -> "That good is not available here."
            SellResult.InvalidQuantity -> null
        }

    private fun TownEntity.toMapModel(): Town =
        Town(
            townId = townId,
            name = name,
            region = region,
            biome = runCatching { Biome.valueOf(biome) }.getOrDefault(Biome.Forest),
            reputation = reputation,
            storyState = storyState,
            lastVisitedAt = lastVisitedAt,
        )

    private fun RoadSegmentEntity.toMapModel(): RoadSegment =
        RoadSegment(
            segmentId = segmentId,
            fromTownId = fromTownId,
            toTownId = toTownId,
            stepCost = stepCost,
            narrativeDistance = narrativeDistance,
            eventPool = eventPool,
        )
}
