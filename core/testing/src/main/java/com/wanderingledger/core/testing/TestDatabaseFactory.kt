package com.wanderingledger.core.testing

import android.content.Context
import androidx.room.Room
import com.wanderingledger.core.database.SeedWorld
import com.wanderingledger.core.database.WanderingLedgerDatabase

object TestDatabaseFactory {
    fun createInMemoryDatabase(context: Context): WanderingLedgerDatabase =
        Room.inMemoryDatabaseBuilder(context, WanderingLedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    suspend fun createSeededInMemoryDatabase(context: Context): WanderingLedgerDatabase =
        createInMemoryDatabase(context).also { database ->
            SeedWorld.ensureSeeded(database, now = 1L)
        }
}
