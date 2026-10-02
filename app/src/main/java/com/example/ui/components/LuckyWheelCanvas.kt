package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PrizeEntity
import com.example.ui.theme.WheelPalette
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun LuckyWheelCanvas(
    prizes: List<PrizeEntity>,
    isLocked: Boolean,
    isSpinning: Boolean,
    targetWinningIndex: Int?,
    onSpinFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationAnimatable = remember { Animatable(0f) }

    // Fallback slices if prizes list is empty
    val displayPrizes = if (prizes.isNotEmpty()) prizes else listOf(
        PrizeEntity(campaignId = 0, name = "5% OFF", weight = 10, colorHex = "#10B981"),
        PrizeEntity(campaignId = 0, name = "10% OFF", weight = 10, colorHex = "#3B82F6"),
        PrizeEntity(campaignId = 0, name = "15% OFF", weight = 10, colorHex = "#8B5CF6"),
        PrizeEntity(campaignId = 0, name = "20% OFF", weight = 10, colorHex = "#F59E0B"),
        PrizeEntity(campaignId = 0, name = "Brinde", weight = 10, colorHex = "#EC4899"),
        PrizeEntity(campaignId = 0, name = "50% OFF", weight = 10, colorHex = "#EF4444")
    )

    val sliceCount = displayPrizes.size
    val arcAngle = 360f / sliceCount

    // Handle spin trigger
    LaunchedEffect(isSpinning, targetWinningIndex) {
        if (isSpinning && targetWinningIndex != null && targetWinningIndex in displayPrizes.indices) {
            val currentRot = rotationAnimatable.value
            // Pointer is at the top (270 degrees).
            // Slice i center is at i * arcAngle + arcAngle / 2.
            // We want (center + targetRot) % 360 = 270.
            val sliceCenterAngle = targetWinningIndex * arcAngle + (arcAngle / 2f)
            val desiredRemainder = (270f - sliceCenterAngle).let { (it % 360f + 360f) % 360f }
            val currentRemainder = (currentRot % 360f + 360f) % 360f
            var delta = desiredRemainder - currentRemainder
            if (delta < 0) delta += 360f

            // Full dramatic rotations (6 full spins = 2160 degrees)
            val totalTarget = currentRot + (360f * 6) + delta

            rotationAnimatable.animateTo(
                targetValue = totalTarget,
                animationSpec = tween(
                    durationMillis = 4800,
                    // Decelerate smoothly like real casino friction
                    easing = CubicBezierEasing(0.12f, 0.8f, 0.25f, 1.0f)
                )
            )
            onSpinFinished()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .testTag("lucky_wheel_container")
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val sizePx = min(constraints.maxWidth, constraints.maxHeight).toFloat()
        val currentRotation = rotationAnimatable.value

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = (size.width / 2f) - 8f
            val wheelRadius = outerRadius - 16f

            // 1. Draw outer decorative golden rim
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFDE68A), Color(0xFFD97706), Color(0xFF78350F)),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // Outer rim border
            drawCircle(
                color = Color(0xFF451A03),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 4f)
            )

            // Draw golden studs/lights along rim
            val studCount = 24
            for (i in 0 until studCount) {
                val angleRad = Math.toRadians((i * (360.0 / studCount)).toDouble())
                val studDist = outerRadius - 8f
                val studX = center.x + (studDist * cos(angleRad)).toFloat()
                val studY = center.y + (studDist * sin(angleRad)).toFloat()
                drawCircle(
                    color = Color.White,
                    radius = 3.5f,
                    center = Offset(studX, studY)
                )
                drawCircle(
                    color = Color(0xFFF59E0B),
                    radius = 5.5f,
                    center = Offset(studX, studY),
                    style = Stroke(width = 1.5f)
                )
            }

            // 2. Rotate wheel content
            rotate(degrees = currentRotation, pivot = center) {
                // Slices
                displayPrizes.forEachIndexed { index, prize ->
                    val startAngle = index * arcAngle
                    val color = try {
                        Color(android.graphics.Color.parseColor(prize.colorHex))
                    } catch (e: Exception) {
                        WheelPalette[index % WheelPalette.size]
                    }

                    // Slice Wedge
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = arcAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - wheelRadius, center.y - wheelRadius),
                        size = Size(wheelRadius * 2, wheelRadius * 2)
                    )

                    // Slice Divider Line
                    val divRad = Math.toRadians(startAngle.toDouble())
                    val endX = center.x + (wheelRadius * cos(divRad)).toFloat()
                    val endY = center.y + (wheelRadius * sin(divRad)).toFloat()
                    drawLine(
                        color = Color(0xFF1E1B4B).copy(alpha = 0.6f),
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 3f
                    )

                    // Slice Text Label
                    val textAngle = startAngle + (arcAngle / 2f)
                    drawSliceText(
                        text = prize.name,
                        angleDegrees = textAngle,
                        center = center,
                        radius = wheelRadius
                    )
                }

                // Inner slice border
                drawCircle(
                    color = Color(0xFFFDE68A),
                    radius = wheelRadius,
                    center = center,
                    style = Stroke(width = 3f)
                )
            }

            // 3. Center Hub (Pin/Badge)
            val hubRadius = wheelRadius * 0.22f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFEF3C7), Color(0xFFF59E0B), Color(0xFF78350F)),
                    center = center,
                    radius = hubRadius
                ),
                radius = hubRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFF451A03),
                radius = hubRadius,
                center = center,
                style = Stroke(width = 3.5f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = hubRadius * 0.7f,
                center = center,
                style = Stroke(width = 2f)
            )

            // Center star / icon text
            drawContext.canvas.nativeCanvas.apply {
                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = hubRadius * 0.55f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(160, 0, 0, 0))
                }
                drawText("★", center.x, center.y + (textPaint.textSize * 0.35f), textPaint)
            }

            // 4. Pointer Indicator (Top Arrow)
            drawTopPointer(center = center, outerRadius = outerRadius)
        }

        // Locked State Overlay
        if (isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .testTag("wheel_locked_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFF1E1B4B).copy(alpha = 0.95f),
                    shape = CircleShape,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(110.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Roleta Bloqueada",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws text along a slice radius using native canvas rotation.
 */
private fun DrawScope.drawSliceText(
    text: String,
    angleDegrees: Float,
    center: Offset,
    radius: Float
) {
    val textRadius = radius * 0.68f
    val rad = Math.toRadians(angleDegrees.toDouble())
    val textX = center.x + (textRadius * cos(rad)).toFloat()
    val textY = center.y + (textRadius * sin(rad)).toFloat()

    drawContext.canvas.nativeCanvas.apply {
        save()
        // Rotate canvas around text position so text points outward/inward
        translate(textX, textY)
        rotate(angleDegrees + 90f)

        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = (radius * 0.088f).coerceIn(24f, 40f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(6f, 0f, 2f, android.graphics.Color.argb(220, 0, 0, 0))
        }

        // Truncate if long
        val trimmed = if (text.length > 15) text.take(13) + "…" else text
        drawText(trimmed, 0f, 0f, paint)
        restore()
    }
}

/**
 * Draws the pointer ticker arrow at top center pointing downwards.
 */
private fun DrawScope.drawTopPointer(center: Offset, outerRadius: Float) {
    val pointerTop = center.y - outerRadius - 4f
    val pointerBottom = pointerTop + 36f
    val halfWidth = 16f

    val pointerPath = Path().apply {
        moveTo(center.x, pointerBottom)
        lineTo(center.x - halfWidth, pointerTop)
        lineTo(center.x + halfWidth, pointerTop)
        close()
    }

    // Shadow
    drawPath(
        path = pointerPath,
        color = Color.Black.copy(alpha = 0.5f),
        style = Fill
    )

    // Golden gradient fill
    drawPath(
        path = pointerPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFEF3C7), Color(0xFFF59E0B), Color(0xFFB45309)),
            startY = pointerTop,
            endY = pointerBottom
        ),
        style = Fill
    )

    // Border
    drawPath(
        path = pointerPath,
        color = Color(0xFF451A03),
        style = Stroke(width = 2.5f)
    )
}
