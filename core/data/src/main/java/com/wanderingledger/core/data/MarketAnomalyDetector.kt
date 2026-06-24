package com.wanderingledger.core.data

import com.wanderingledger.core.model.SupplyLevel
import com.wanderingledger.core.telemetry.MarketAnomalyType
import com.wanderingledger.core.telemetry.TelemetryEvent

/** A point-in-time price reading used as input to [MarketAnomalyDetector.detect]. */
data class PriceSnapshot(
    val sellPrice: Long,
    val supplyLevel: SupplyLevel,
)

/**
 * Pure anomaly detection for market price changes.
 *
 * Takes two [PriceSnapshot]s and returns the anomalies that occurred between
 * them, as [TelemetryEvent.MarketAnomaly] values ready to emit. Has no side
 * effects — callers (e.g. [MarketRepository]) emit via [TelemetryService].
 */
object MarketAnomalyDetector {
    fun detect(
        townId: Long,
        goodId: Long,
        baseValue: Long,
        before: PriceSnapshot,
        after: PriceSnapshot,
        now: Long = System.currentTimeMillis(),
    ): List<TelemetryEvent.MarketAnomaly> {
        val events = mutableListOf<TelemetryEvent.MarketAnomaly>()

        val priceChangePct = if (before.sellPrice > 0) {
            ((after.sellPrice - before.sellPrice).toDouble() / before.sellPrice * 100).toLong()
        } else 0L

        if (priceChangePct > 50) {
            events += TelemetryEvent.MarketAnomaly(
                timestamp = now,
                anomalyType = MarketAnomalyType.PriceSpike,
                townId = townId,
                goodId = goodId.toString(),
                value = after.sellPrice,
                threshold = (before.sellPrice * 1.5).toLong(),
            )
        }

        if (priceChangePct < -30) {
            events += TelemetryEvent.MarketAnomaly(
                timestamp = now,
                anomalyType = MarketAnomalyType.PriceCrash,
                townId = townId,
                goodId = goodId.toString(),
                value = after.sellPrice,
                threshold = (before.sellPrice * 0.7).toLong(),
            )
        }

        if (before.supplyLevel != SupplyLevel.Scarce && after.supplyLevel == SupplyLevel.Scarce) {
            events += TelemetryEvent.MarketAnomaly(
                timestamp = now,
                anomalyType = MarketAnomalyType.SupplyDepleted,
                townId = townId,
                goodId = goodId.toString(),
                value = after.supplyLevel.ordinal.toLong(),
                threshold = SupplyLevel.Scarce.ordinal.toLong(),
            )
        }

        val deviationPct = if (baseValue > 0) {
            ((after.sellPrice - baseValue).toDouble() / baseValue * 100).toLong()
        } else 0L
        if (deviationPct > 100) {
            events += TelemetryEvent.MarketAnomaly(
                timestamp = now,
                anomalyType = MarketAnomalyType.UnusualVolume,
                townId = townId,
                goodId = goodId.toString(),
                value = after.sellPrice,
                threshold = baseValue * 2,
            )
        }

        return events
    }
}
