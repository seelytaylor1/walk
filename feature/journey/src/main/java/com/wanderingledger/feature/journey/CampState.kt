package com.wanderingledger.feature.journey

import com.wanderingledger.core.model.Biome
import com.wanderingledger.core.model.Companion as CoreCompanion

enum class JourneyMode {
    ActiveTravel,
    Camping,
    Arriving,
}

enum class CampActivity {
    Sitting,
    Sleeping,
    Cooking,
    KeepingWatch,
    StokingFire,
    Chatting,
}

data class CampState(
    val journeyMode: JourneyMode = JourneyMode.ActiveTravel,
    val campsiteBiome: Biome = Biome.Forest,
    val startTime: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 0,
    val stepsEarnedWhileCamping: Long = 0,
    val currentCompanions: List<CoreCompanion> = emptyList(),
    val campActivities: Map<Long, CampActivity> = emptyMap(),
    val campfireLit: Boolean = true,
    val ambientIntensity: Float = 1f,
) {
    val isCamping: Boolean
        get() = journeyMode == JourneyMode.Camping

    fun withUpdatedDuration(currentTime: Long): CampState {
        val diffMs = currentTime - startTime
        return copy(durationMinutes = (diffMs / (60 * 1000)).toInt())
    }

    companion object {
        fun camping(
            biome: Biome,
            companions: List<CoreCompanion>,
            campfireLit: Boolean = true,
        ): CampState {
            val activities = companions.associate { companion: CoreCompanion ->
                companion.companionId to when {
                    companion.role.name == "Scout" -> CampActivity.KeepingWatch
                    companion.role.name == "Healer" -> CampActivity.Cooking
                    else -> CampActivity.Sitting
                }
            }
            return CampState(
                journeyMode = JourneyMode.Camping,
                campsiteBiome = biome,
                startTime = System.currentTimeMillis(),
                currentCompanions = companions,
                campActivities = activities,
                campfireLit = campfireLit,
            )
        }
    }
}

object CampStateDetector {
    private const val MIN_CAMP_DURATION_MS = 5 * 60 * 1000L
    private const val MIN_IDLE_STEPS = 100L

    fun shouldEnterCamp(
        lastTravelTime: Long,
        currentTime: Long,
        bankedSteps: Long,
    ): Boolean {
        val timeSinceTravel = currentTime - lastTravelTime
        return timeSinceTravel >= MIN_CAMP_DURATION_MS && bankedSteps >= MIN_IDLE_STEPS
    }

    fun determineCampActivity(companion: CoreCompanion): CampActivity {
        return when {
            companion.role.name == "Scout" -> CampActivity.KeepingWatch
            companion.role.name == "Healer" -> CampActivity.Cooking
            companion.bondLevel >= 5 -> CampActivity.Chatting
            else -> CampActivity.Sitting
        }
    }
}