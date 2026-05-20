package com.wanderingledger.feature.worldmap

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanderingledger.core.designsystem.theme.WLTheme
import kotlin.random.Random

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WorldMapRenderer(
    locations: List<MapLocation>,
    routes: List<MapRoute>,
    onLocationClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val infiniteTransition = rememberInfiniteTransition(label = "map_animations")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val parchmentColor = WLTheme.current.colors.background
    val inkColor = WLTheme.current.colors.secondary
    val highlightColor = WLTheme.current.colors.primary

    // Tap-target size for town nodes — meets 48dp minimum
    val tapTargetDp = 48.dp
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .background(parchmentColor)
            .pointerInput(locations) {
                detectTapGestures { offset ->
                    val clickedLocation = locations.find { location ->
                        val screenOffset = Offset(location.offset.x * size.width, location.offset.y * size.height)
                        (offset - screenOffset).getDistance() < 40.dp.toPx()
                    }
                    clickedLocation?.let { onLocationClick(it.townId) }
                }
            }
    ) {
        val boxWidth  = maxWidth
        val boxHeight = maxHeight

        // Decorative canvas — invisible to accessibility tree
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics { invisibleToUser() },
        ) {
            val width  = size.width
            val height = size.height

            drawTerrainDetails(width, height)

            routes.forEach { route ->
                val start = Offset(route.fromOffset.x * width, route.fromOffset.y * height)
                val end   = Offset(route.toOffset.x  * width, route.toOffset.y  * height)

                if (route.isDiscovered) {
                    drawPath(
                        path = Path().apply {
                            moveTo(start.x, start.y)
                            quadraticBezierTo(
                                (start.x + end.x) / 2 + 20f,
                                (start.y + end.y) / 2 - 20f,
                                end.x, end.y
                            )
                        },
                        color = inkColor.copy(alpha = 0.4f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                }
            }

            locations.forEach { location ->
                val pos = Offset(location.offset.x * width, location.offset.y * height)

                if (location.isDiscovered) {
                    val radius = if (location.isCurrentLocation) 12.dp.toPx() * pulse else 8.dp.toPx()
                    val color  = if (location.isCurrentLocation) highlightColor else inkColor

                    drawCircle(color = color, radius = radius, center = pos)
                    drawCircle(
                        color = color.copy(alpha = 0.3f),
                        radius = radius + 8.dp.toPx(),
                        center = pos,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    val textLayoutResult = textMeasurer.measure(
                        text = location.name,
                        style = TextStyle(
                            color = inkColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(pos.x - textLayoutResult.size.width / 2, pos.y + 12.dp.toPx())
                    )
                } else {
                    drawCircle(
                        color = inkColor.copy(alpha = 0.1f),
                        radius = 6.dp.toPx(),
                        center = pos
                    )
                }
            }
        }

        // Accessibility overlays — one invisible 48dp tap target per discovered town
        locations.filter { it.isDiscovered }.forEach { location ->
            val xOffset = with(density) { (location.offset.x * boxWidth.value).dp - tapTargetDp / 2 }
            val yOffset = with(density) { (location.offset.y * boxHeight.value).dp - tapTargetDp / 2 }

            Box(
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
                    .size(tapTargetDp)
                    .semantics {
                        val currentLabel = if (location.isCurrentLocation) " (current location)" else ""
                        contentDescription = "${location.name}$currentLabel. Tap to view routes."
                        role = Role.Button
                    }
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTerrainDetails(width: Float, height: Float) {
    val random = Random(42) // Fixed seed for consistent terrain
    repeat(15) {
        val x = random.nextFloat() * width
        val y = random.nextFloat() * height
        val type = random.nextInt(3)

        when (type) {
            0 -> drawMountain(x, y)
            1 -> drawForestCluster(x, y)
            2 -> drawRiverSpline(x, y)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMountain(x: Float, y: Float) {
    val color = Color(0xFF8D8D8D).copy(alpha = 0.1f)
    val path = Path().apply {
        moveTo(x, y)
        lineTo(x + 20f, y - 30f)
        lineTo(x + 40f, y)
        close()
    }
    drawPath(path, color)
    drawPath(path, color.copy(alpha = 0.2f), style = Stroke(width = 1f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawForestCluster(x: Float, y: Float) {
    val color = Color(0xFF4CAF50).copy(alpha = 0.1f)
    repeat(3) { i ->
        drawCircle(
            color = color,
            radius = 10f,
            center = Offset(x + i * 10f, y + (i % 2) * 5f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRiverSpline(x: Float, y: Float) {
    val color = Color(0xFF2196F3).copy(alpha = 0.05f)
    drawPath(
        path = Path().apply {
            moveTo(x, y)
            cubicTo(x + 50f, y + 20f, x - 50f, y + 80f, x + 20f, y + 100f)
        },
        color = color,
        style = Stroke(width = 4f)
    )
}

private val FontWeight = androidx.compose.ui.text.font.FontWeight
