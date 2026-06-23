package com.wanderingledger.app

import android.app.Application

/**
 * Owns the process-scoped [AppContainer] so the database and repositories
 * outlive any single Activity instance.
 */
class WanderingLedgerApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
