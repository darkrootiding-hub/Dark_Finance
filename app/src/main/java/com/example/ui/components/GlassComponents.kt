package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Root container providing a subtle ambient background with animated soft color blobs
 * giving the true glassmorphism frosted depth effect.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FintechBackground)
    ) {
        // Decorative ambient gradient background (static canvas for high performance scrolling)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Blob 1 (Top-Left - Primary Violet)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(FintechPrimary.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.12f),
                    radius = size.width * 0.7f
                ),
                center = Offset(size.width * 0.15f, size.height * 0.12f),
                radius = size.width * 0.7f
            )
            
            // Blob 2 (Top-Right - Secondary Blue)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(FintechSecondary.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.35f),
                    radius = size.width * 0.65f
                ),
                center = Offset(size.width * 0.85f, size.height * 0.35f),
                radius = size.width * 0.65f
            )

            // Blob 3 (Bottom-Center - Accent Orange)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(FintechAccent.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.8f),
                    radius = size.width * 0.8f
                ),
                center = Offset(size.width * 0.5f, size.height * 0.8f),
                radius = size.width * 0.8f
            )
        }

        content()
    }
}

/**
 * Frosted Glass Card with subtle white border, semi-transparent white backdrop fill,
 * rounded corners (24–30dp), and soft Fintech shadow.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.72f),
    borderColor: Color = Color.White.copy(alpha = 0.85f),
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    val borderBrush = remember(borderColor) {
        Brush.linearGradient(
            colors = listOf(borderColor, borderColor.copy(alpha = 0.3f))
        )
    }

    Box(
        modifier = modifier
            // Default shadow colors keep this on Compose's fast (hardware) shadow path.
            // Custom ambientColor/spotColor here forced every card onto the slow
            // offscreen-compositing path, which is what was causing scroll jank.
            .shadow(
                elevation = elevation,
                shape = shape
            )
            .background(color = backgroundColor, shape = shape)
            .border(
                width = 1.2.dp,
                brush = borderBrush,
                shape = shape
            )
            .clip(shape)
            .then(clickModifier),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

/**
 * Tappable glass icon container.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = Color.White.copy(alpha = 0.8f),
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.9f),
                shape = CircleShape
            )
            .shadow(4.dp, CircleShape),
        shape = CircleShape,
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
