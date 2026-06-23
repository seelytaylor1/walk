package com.wanderingledger.core.telemetry

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object TelemetryService {
    private val _events = MutableSharedFlow<TelemetryEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<TelemetryEvent> = _events.asSharedFlow()

    suspend fun record(event: TelemetryEvent) {
        _events.emit(event)
    }

    fun tryRecord(event: TelemetryEvent) {
        _events.tryEmit(event)
    }
}

sealed class TelemetryEvent {
    abstract val timestamp: Long

    data class StepRecorded(
        override val timestamp: Long,
        val count: Int,
        val source: String,
        val bankedStepsAfter: Long,
    ) : TelemetryEvent()

    data class StepAnomaly(
        override val timestamp: Long,
        val anomalyType: StepAnomalyType,
        val count: Int,
        val details: String,
    ) : TelemetryEvent()

    data class TravelStarted(
        override val timestamp: Long,
        val segmentId: Long,
        val fromTownId: Long,
        val toTownId: Long,
        val requiredSteps: Int,
        val availableSteps: Long,
    ) : TelemetryEvent()

    data class TravelCompleted(
        override val timestamp: Long,
        val segmentId: Long,
        val latencyMs: Long,
        val success: Boolean,
    ) : TelemetryEvent()

    data class MarketTransaction(
        override val timestamp: Long,
        val townId: Long,
        val goodId: String,
        val transactionType: String,
        val quantity: Int,
        val pricePerUnit: Long,
    ) : TelemetryEvent()

    data class MarketAnomaly(
        override val timestamp: Long,
        val anomalyType: MarketAnomalyType,
        val townId: Long,
        val goodId: String,
        val value: Long,
        val threshold: Long,
    ) : TelemetryEvent()
}

enum class StepAnomalyType {
    NegativeDelta,
    ExcessiveBurst,
    ZeroSteps,
    DuplicateTimestamp,
}

enum class MarketAnomalyType {
    PriceSpike,
    PriceCrash,
    UnusualVolume,
    SupplyDepleted,
}
