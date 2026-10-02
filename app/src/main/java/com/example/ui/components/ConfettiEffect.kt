package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class Particle(
    val startX: Float,
    val speedX: Float,
    val speedY: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float,
    val initialRotation: Float
)

@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 70
) {
    val progress = remember { Animatable(0f) }

    val colors = listOf(
        Color(0xFFF59E0B), // Gold
        Color(0xFFEF4444), // Red
        Color(0xFF10B981), // Green
        Color(0xFF3B82F6), // Blue
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFF06B6D4)  // Cyan
    )

    val particles = remember {
        List(particleCount) {
            Particle(
                startX = Random.nextFloat(),
                speedX = (Random.nextFloat() - 0.5f) * 160f,
                speedY = Random.nextFloat() * 800f + 600f,
                size = Random.nextFloat() * 12f + 8f,
                color = colors[Random.nextInt(colors.size)],
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                initialRotation = Random.nextFloat() * 360f
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3500, easing = LinearEasing)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = progress.value

        particles.forEach { p ->
            val x = (p.startX * w) + (p.speedX * t)
            val y = (t * p.speedY) - 50f
            val currentRot = p.initialRotation + (p.rotationSpeed * t)

            if (y in -50f..h + 50f && x in -50f..w + 50f) {
                rotate(degrees = currentRot, pivot = Offset(x, y)) {
                    drawRect(
                        color = p.color.copy(alpha = (1f - (t * 0.4f)).coerceIn(0f, 1f)),
                        topLeft = Offset(x - p.size / 2f, y - p.size / 2f),
                        size = Size(p.size, p.size * 0.6f)
                    )
                }
            }
        }
    }
}
