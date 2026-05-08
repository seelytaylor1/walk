package com.wanderingledger.feature.journey

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanderingledger.core.designsystem.theme.WLTheme
import com.wanderingledger.core.model.Biome
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class RoutePathData(
    val segmentId: Long,
    val destinationName: String,
    val stepCost: Int,
    val narrativeDistance: String,
    val eventPool: List<String>,
    val isAffordable: Boolean,
    val progress: Float = 0f,
)

private data class SplinePoint(
    val x: Float,
    val y: Float,
)

private fun calculateSplineControlPoints(
    start: SplinePoint,
    end: SplinePoint,
    offsetMagnitude: Float,
): Pair<SplinePoint, SplinePoint> {
    val midX = (start.x + end.x) / 2
    val midY = (start.y + end.y) / 2
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = kotlin.math.sqrt(dx * dx + dy * dy)
    if (length < 1f) {
        return Pair(
            SplinePoint(midX - offsetMagnitude * 0.3f, midY),
            SplinePoint(midX + offsetMagnitude * 0.3f, midY),
        )
    }
    val perpX = -dy / length * offsetMagnitude
    val perpY = dx / length * offsetMagnitude
    val cp1 = SplinePoint(midX + perpX * 0.5f, midY + perpY * 0.5f)
    val cp2 = SplinePoint(midX - perpX * 0.5f, midY - perpY * 0.5f)
    return Pair(cp1, cp2)
}

private fun createRoutePath(
    start: SplinePoint,
    end: SplinePoint,
    controlOffset: Float,
): Path {
    val path = Path()
    path.moveTo(start.x, start.y)
    val (cp1, cp2) = calculateSplineControlPoints(start, end, controlOffset)
    path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, end.x, end.y)
    return path
}

private fun getPositionOnSpline(
    start: SplinePoint,
    end: SplinePoint,
    controlOffset: Float,
    t: Float,
): Offset {
    val invT = 1f - t
    val (cp1, cp2) = calculateSplineControlPoints(start, end, controlOffset)
    val x = invT * invT * invT * start.x +
            3f * invT * invT * t * cp1.x +
            3f * invT * t * t * cp2.x +
            t * t * t * end.x
    val y = invT * invT * invT * start.y +
            3f * invT * invT * t * cp1.y +
            3f * invT * t * t * cp2.y +
            t * t * t * end.y
    return Offset(x, y)
}

private fun getTangentOnSpline(
    start: SplinePoint,
    end: SplinePoint,
    controlOffset: Float,
    t: Float,
): Float {
    val (cp1, cp2) = calculateSplineControlPoints(start, end, controlOffset)
    val dx = 3f * (1f - t) * (1f - t) * (cp1.x - start.x) +
            6f * (1f - t) * t * (cp2.x - cp1.x) +
            3f * t * t * (end.x - cp2.x)
    val dy = 3f * (1f - t) * (1f - t) * (cp1.y - start.y) +
            6f * (1f - t) * t * (cp2.y - cp1.y) +
            3f * t * t * (end.y - cp2.y)
    return atan2(dy, dx) * 180f / PI.toFloat()
}

private fun getEncounterEmoji(encounterType: String): String = when (encounterType) {
    "merchant-cart" -> "\uD83D\uDE9A"
    "fog-bank" -> "\uD83C\uDF2B\uFE0F"
    "old-road" -> "\uD83D\uDE97"
    "bandit-ambush" -> "\uD83D\uDEB0"
    else -> "\u26D3\uFE0F"
}

@Composable
fun RouteSplineOverlay(
    routes: List<RoutePathData>,
    biome: Biome,
    currentTravelingRouteId: Long? = null,
    modifier: Modifier = Modifier
) {
    val routeColors = remember(biome) {
        getBiomeRouteColors(biome)
    }
    val maxStepCost = routes.maxOfOrNull { it.stepCost } ?: 1

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth * 0.5f
            val centerY = canvasHeight * 0.5f
            val baseRadius = min(canvasWidth, canvasHeight) * 0.35f

            routes.forEachIndexed { index, route ->
                val baseAngle = (index * 45f - 22.5f) * PI.toFloat() / 180f
                val routeLength = 0.3f + 0.7f * (route.stepCost.toFloat() / maxStepCost)
                val controlOffset = 80f + index * 20f

                val startX = centerX + cos(baseAngle) * baseRadius * 0.2f
                val startY = centerY + sin(baseAngle) * baseRadius * 0.2f
                val endX = centerX + cos(baseAngle) * baseRadius * routeLength
                val endY = centerY + sin(baseAngle) * baseRadius * routeLength

                val start = SplinePoint(startX, startY)
                val end = SplinePoint(endX, endY)

                val routeColor = if (route.isAffordable) {
                    routeColors.affordable
                } else {
                    routeColors.unaffordable
                }

                val routePath = createRoutePath(start, end, controlOffset)
                drawPath(
                    path = routePath,
                    color = routeColor,
                    style = Stroke(
                        width = 8f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                    )
                )
                drawPath(
                    path = routePath,
                    color = routeColor.copy(alpha = 0.6f),
                    style = Stroke(
                        width = 16f,
                        cap = StrokeCap.Round
                    )
                )

                if (currentTravelingRouteId == route.segmentId && route.progress > 0f) {
                    val markerPosition = getPositionOnSpline(start, end, controlOffset, route.progress)
                    val markerAngle = getTangentOnSpline(start, end, controlOffset, route.progress)

                    rotate(markerAngle, markerPosition) {
                        val markerPath = Path().apply {
                            moveTo(markerPosition.x, markerPosition.y - 24f)
                            lineTo(markerPosition.x - 16f, markerPosition.y + 12f)
                            lineTo(markerPosition.x, markerPosition.y + 4f)
                            lineTo(markerPosition.x + 16f, markerPosition.y + 12f)
                            close()
                        }
                        drawPath(
                            path = markerPath,
                            color = routeColors.marker,
                        )
                    }

                    val progressDotPosition = getPositionOnSpline(start, end, controlOffset, route.progress)
                    drawCircle(
                        color = routeColors.marker,
                        radius = 16f,
                        center = progressDotPosition
                    )
                }
            }
        }

        routes.forEachIndexed { index, route ->
            if (route.eventPool.isNotEmpty()) {
                val encounterPositions = listOf(0.3f, 0.6f, 0.85f)
                route.eventPool.take(3).forEachIndexed { eventIndex, encounterType ->
                    val encounterT = encounterPositions.getOrElse(eventIndex) { 0.5f }
                    val (x, y) = calculateSplineScreenPosition(
                        routeIndex = index,
                        t = encounterT,
                        maxStepCost = maxStepCost,
                        stepCost = route.stepCost
                    )
                    Text(
                        text = getEncounterEmoji(encounterType),
                        fontSize = 24.sp,
                        modifier = Modifier
                            .offset(
                                x = (x - 12).dp,
                                y = (y - 12).dp
                            )
                    )
                }
            }
        }
    }
}

private fun calculateSplineScreenPosition(
    routeIndex: Int,
    t: Float,
    maxStepCost: Int,
    stepCost: Int,
): Pair<Float, Float> {
    val baseAngle = (routeIndex * 45f - 22.5f) * PI.toFloat() / 180f
    val routeLength = 0.3f + 0.7f * (stepCost.toFloat() / maxStepCost)
    val controlOffset = 80f + routeIndex * 20f

    val centerX = 0.5f
    val centerY = 0.5f
    val baseRadius = 0.35f

    val startX = centerX + cos(baseAngle) * baseRadius * 0.2f
    val startY = centerY + sin(baseAngle) * baseRadius * 0.2f
    val endX = centerX + cos(baseAngle) * baseRadius * routeLength
    val endY = centerY + sin(baseAngle) * baseRadius * routeLength

    val start = SplinePoint(startX, startY)
    val end = SplinePoint(endX, endY)

    val position = getPositionOnSpline(start, end, controlOffset, t)
    return Pair(position.x, position.y)
}

@Composable
fun AnimatedTravelMarker(
    isTraveling: Boolean,
    routeSegmentId: Long,
    routePathData: RoutePathData,
    progress: Float,
    biome: Biome,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (isTraveling) progress else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "travel_progress"
    )

    val routeColor = remember(biome) {
        getBiomeRouteColors(biome).marker
    }

    if (animatedProgress > 0f) {
        Box(modifier = modifier.fillMaxWidth()) {
            Text(
                text = "\uD83D\uDEB6\u200D\u2642\uFE0F",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private data class BiomeRouteColors(
    val affordable: Color,
    val unaffordable: Color,
    val marker: Color,
    val waypoint: Color,
)

private fun getBiomeRouteColors(biome: Biome): BiomeRouteColors = when (biome) {
    Biome.Forest -> BiomeRouteColors(
        affordable = Color(0xFF2E7D32),
        unaffordable = Color(0xFF9E9E9E),
        marker = Color(0xFF1B5E20),
        waypoint = Color(0xFF795548),
    )
    Biome.Mountain -> BiomeRouteColors(
        affordable = Color(0xFF455A64),
        unaffordable = Color(0xFF9E9E9E),
        marker = Color(0xFF263238),
        waypoint = Color(0xFF5D4037),
    )
    Biome.Swamp -> BiomeRouteColors(
        affordable = Color(0xFF00695C),
        unaffordable = Color(0xFF9E9E9E),
        marker = Color(0xFF004D40),
        waypoint = Color(0xFF3E2723),
    )
    Biome.Coast -> BiomeRouteColors(
        affordable = Color(0xFF0277BD),
        unaffordable = Color(0xFF9E9E9E),
        marker = Color(0xFF01579B),
        waypoint = Color(0xFFFF8F00),
    )
}