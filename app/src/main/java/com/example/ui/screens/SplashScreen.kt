package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.FintechPrimary
import com.example.ui.theme.FintechSecondary
import com.example.ui.theme.PremiumGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val fullText = "DarkRoot"
    var displayedLength by remember { mutableStateOf(0) }
    var showCursor by remember { mutableStateOf(true) }
    var showSubtitle by remember { mutableStateOf(false) }

    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "splashGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Typewriter effect coroutine
    LaunchedEffect(Unit) {
        delay(300) // Initial delay before typing starts
        for (i in 1..fullText.length) {
            displayedLength = i
            delay(130) // Delay between characters (D -> DA -> DAR...)
        }
        showSubtitle = true
        delay(900) // Hold briefly after typewriter finishes
        onSplashFinished()
    }

    // Cursor blink loop
    LaunchedEffect(Unit) {
        while (isActive) {
            showCursor = true
            delay(400)
            showCursor = false
            delay(400)
        }
    }

    // Dark canvas matching app icon (#090A0F)
    val appIconDarkBg = Color(0xFF090A0F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appIconDarkBg),
        contentAlignment = Alignment.Center
    ) {
        // Background Glowing Gradient Orbs matching app icon palette
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(glowScale)
                .alpha(glowAlpha)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            FintechPrimary.copy(alpha = 0.45f),
                            FintechSecondary.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated Icon Container with glowing ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                // Outer Pulse Ring
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .scale(glowScale)
                        .alpha(0.6f)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(FintechPrimary, FintechSecondary, PremiumGold, FintechPrimary)
                            ),
                            shape = CircleShape
                        )
                )

                // App Icon Container
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(24.dp, CircleShape, spotColor = FintechPrimary),
                    shape = CircleShape,
                    color = Color(0xFF131520),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon_1784967493551),
                        contentDescription = "DarkRoot Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // "DarkRoot" Typewriter Animated Text
            val currentTypedString = fullText.take(displayedLength)
            
            // Build dual-colored annotated string ("Dark" in White, "Root" in Glowing Purple)
            val annotatedText = buildAnnotatedString {
                if (currentTypedString.length <= 4) {
                    // Typing "Dark"
                    withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                        append(currentTypedString)
                    }
                } else {
                    // Typed "Dark" + typing "Root"
                    withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                        append("Dark")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = FintechPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    ) {
                        append(currentTypedString.substring(4))
                    }
                }
            }

            Text(
                text = annotatedText,
                fontSize = 38.sp,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Subtitle Tagline with Smooth Fade-In
            AnimatedVisibility(
                visible = showSubtitle,
                enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.9f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = "SMART WEALTH & FINANCE ENGINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Minimal animated loading bar
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        val progressAnim by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(1200, easing = LinearEasing),
                            label = "splashProgress"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressAnim)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(FintechPrimary, FintechSecondary, PremiumGold)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
