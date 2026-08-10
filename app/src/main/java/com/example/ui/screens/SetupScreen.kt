package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(viewModel: FinanceViewModel) {
    var name by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var initialBalance by remember { mutableStateOf("") }

    val fullText = "DarkRoot"
    var displayedLength by remember { mutableStateOf(0) }
    var showCursor by remember { mutableStateOf(true) }

    // Typewriter effect coroutine
    LaunchedEffect(Unit) {
        delay(200)
        for (i in 1..fullText.length) {
            displayedLength = i
            delay(120)
        }
    }

    // Cursor blinking loop
    LaunchedEffect(Unit) {
        while (isActive) {
            showCursor = true
            delay(400)
            showCursor = false
            delay(400)
        }
    }

    // Glowing pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "setupGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val appIconDarkBg = Color(0xFF090A0F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appIconDarkBg)
    ) {
        // Background Glowing Gradient Orbs
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-40).dp)
                .size(360.dp)
                .scale(glowScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            FintechPrimary.copy(alpha = 0.35f),
                            FintechSecondary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Logo & Branding
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(84.dp)
                        .shadow(20.dp, CircleShape, spotColor = FintechPrimary),
                    shape = CircleShape,
                    color = Color(0xFF131520),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon_1784967493551),
                        contentDescription = "DarkRoot",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Typewriter Brand Title ("DarkRoot")
            val currentTypedString = fullText.take(displayedLength)
            val annotatedText = buildAnnotatedString {
                if (currentTypedString.length <= 4) {
                    withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                        append(currentTypedString)
                    }
                } else {
                    withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                        append("Dark")
                    }
                    withStyle(style = SpanStyle(color = FintechPrimary, fontWeight = FontWeight.ExtraBold)) {
                        append(currentTypedString.substring(4))
                    }
                }
            }

            Text(
                text = annotatedText,
                fontSize = 32.sp,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Set up your profile & initial targets",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 28.dp)
            )

            // Setup Form Glass Card Container
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.White.copy(alpha = 0.12f),
                borderColor = Color.White.copy(alpha = 0.25f)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    // Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Name", color = Color.White.copy(alpha = 0.8f)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = FintechPrimary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FintechPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Monthly Salary Field
                    OutlinedTextField(
                        value = salary,
                        onValueChange = { salary = it },
                        label = { Text("Monthly Budget / Income ($)", color = Color.White.copy(alpha = 0.8f)) },
                        leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = FintechSuccess) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FintechPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Initial Balance Field
                    OutlinedTextField(
                        value = initialBalance,
                        onValueChange = { initialBalance = it },
                        label = { Text("Current Initial Balance ($)", color = Color.White.copy(alpha = 0.8f)) },
                        leadingIcon = { Icon(Icons.Default.Savings, contentDescription = null, tint = PremiumGold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FintechPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Button with Gradient & Shadow
            Surface(
                onClick = {
                    val parsedSalary = salary.toDoubleOrNull() ?: 2500.0
                    val parsedBalance = initialBalance.toDoubleOrNull() ?: 1000.0
                    viewModel.completeSetup(parsedSalary, parsedBalance, name.ifBlank { "David" })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = FintechPrimary),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(FintechPrimary, FintechSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Get Started",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { viewModel.completeSetup(2500.0, 1000.0, "David") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Skip for now (Use Demo Defaults)",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
