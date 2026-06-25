package com.wanderingledger.app

import android.content.Context
import com.wanderingledger.core.data.CompanionCommentaryEngine
import com.wanderingledger.core.data.CompanionRepository
import com.wanderingledger.core.data.GameRepository
import com.wanderingledger.core.data.InventoryRepository
import com.wanderingledger.core.data.MarketRepository
import com.wanderingledger.core.data.OrderRepository
import com.wanderingledger.core.data.RoomStepBankRepository
import com.wanderingledger.core.data.RumorRepository
import com.wanderingledger.core.audio.AudioPreferences
import com.wanderingledger.core.database.WanderingLedgerDatabase
import com.wanderingledger.core.designsystem.accessibility.AccessibilityPreferences
import com.wanderingledger.core.steptracker.StepTrackerService

/**
 * Application-scoped dependency holder.
 *
 * The database and repositories live for the lifetime of the process, not the
 * Activity. This is what makes Jetpack [androidx.lifecycle.ViewModel]s safe:
 * a ViewModel retained across a configuration change keeps referencing these
 * same instances instead of an Activity-scoped database that was closed in
 * `onDestroy`.
 */
class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    val database: WanderingLedgerDatabase = WanderingLedgerDatabase.create(appContext)

    val rumorRepository = RumorRepository(database)
    val companionRepository = CompanionRepository(database)
    val companionNarrator = CompanionNarrator(companionRepository, CompanionCommentaryEngine())
    val orderRepository = OrderRepository(database)
    val gameRepository = GameRepository(database, rumorRepository, companionRepository, orderRepository)
    val marketRepository = MarketRepository(database)
    val inventoryRepository = InventoryRepository(database)
    val stepTrackerService = StepTrackerService(RoomStepBankRepository(database))

    val accessibilityPreferences = AccessibilityPreferences(appContext)
    val audioPreferences = AudioPreferences(appContext)
}
