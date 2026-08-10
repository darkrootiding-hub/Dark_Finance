package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

data class ConfettiParticle(
    val id: Int,
    var x: Float,
    var y: Float,
    val vx: Float,
    var vy: Float,
    val size: Float,
    val color: Color,
    var rotation: Float,
    val rotationSpeed: Float,
    val isCircle: Boolean
)

@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    isTriggered: Boolean = false,
    onFinished: () -> Unit = {}
) {
    if (!isTriggered) return

    val particles = remember {
        val colors = listOf(
            PremiumGold,
            FintechPrimary,
            FintechSecondary,
            FintechSuccess,
            FintechAccent,
            Color(0xFFE91E63),
            Color(0xFF00BCD4)
        )
        List(70) { index ->
            ConfettiParticle(
                id = index,
                x = Random.nextFloat() * 1000f,
                y = Random.nextFloat() * -300f,
                vx = (Random.nextFloat() - 0.5f) * 12f,
                vy = Random.nextFloat() * 15f + 8f,
                size = Random.nextFloat() * 16f + 8f,
                color = colors[Random.nextInt(colors.size)],
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 18f,
                isCircle = Random.nextBoolean()
            )
        }
    }

    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(isTriggered) {
        val startTime = System.currentTimeMillis()
        val duration = 2400L
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val frac = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            progress = frac

            // Update particle positions
            particles.forEach { p ->
                p.x += p.vx
                p.y += p.vy
                p.vy += 0.35f // Gravity
                p.rotation += p.rotationSpeed
            }

            if (frac >= 1f) {
                onFinished()
                break
            }
            delay(16) // ~60 FPS
        }
    }

    val alpha = (1f - progress).coerceIn(0f, 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        particles.forEach { p ->
            // Scale particle x relative to actual canvas width
            val px = (p.x / 1000f) * canvasWidth
            val py = p.y * (canvasHeight / 1200f)

            rotate(degrees = p.rotation, pivot = Offset(px, py)) {
                if (p.isCircle) {
                    drawCircle(
                        color = p.color.copy(alpha = alpha),
                        radius = p.size / 2f,
                        center = Offset(px, py)
                    )
                } else {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(px - p.size / 2f, py - p.size / 2f),
                        size = Size(p.size, p.size * 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun SuccessCheckmarkAnimation(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    message: String = "Transaction Imported!"
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.5f),
        modifier = modifier
    ) {
        val scaleAnim by animateFloatAsState(
            targetValue = if (visible) 1f else 0.8f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            label = "checkScale"
        )

        Surface(
            shape = CircleShape,
            color = FintechSuccess,
            shadowElevation = 16.dp,
            modifier = Modifier
                .scale(scaleAnim)
                .size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}
