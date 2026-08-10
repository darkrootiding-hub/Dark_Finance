package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.TransactionType
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Transactions : Screen("transactions", "History", Icons.Default.ReceiptLong)
    object Add : Screen("add", "Add", Icons.Default.Add) // Center FAB placeholder
    object Budget : Screen("budget", "Budget", Icons.Default.AccountBalanceWallet)
    object Reports : Screen("reports", "Analytics", Icons.Default.BarChart)
}

val navItems = listOf(
    Screen.Home,
    Screen.Transactions,
    Screen.Add,
    Screen.Budget,
    Screen.Reports
)

@Composable
fun AppNavigation(
    viewModel: FinanceViewModel,
    sharedText: String? = null,
    sharedUriStr: String? = null
) {
    val navController = rememberNavController()
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(sharedText != null || sharedUriStr != null) }
    var addDialogType by remember { mutableStateOf(TransactionType.EXPENSE) }

    LaunchedEffect(sharedText, sharedUriStr) {
        if (sharedText != null || sharedUriStr != null) {
            showImportSheet = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FintechBackground)
    ) {
        // Main NavHost content
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onAddTransaction = { type ->
                        addDialogType = type
                        showAddDialog = true
                    }
                )
            }
            composable(Screen.Transactions.route) { TransactionsScreen(viewModel, navController) }
            composable(Screen.Budget.route) { BudgetScreen(viewModel, navController) }
            composable(Screen.Reports.route) { ReportsScreen(viewModel, navController) }
            composable("settings") { SettingsScreen(viewModel, navController) }
        }

        // Floating Glass Bottom Navigation Bar resting above navigationBars inset
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

        // Render Floating Navigation Bar on main screens
        if (currentRoute != "settings") {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Nav Bar Capsule Background
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = FintechPrimary.copy(alpha = 0.25f)
                        )
                        .border(
                            width = 1.2.dp,
                            color = Color.White.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(32.dp)
                        ),
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White.copy(alpha = 0.90f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEach { screen ->
                            if (screen == Screen.Add) {
                                // Empty placeholder spacer for the floating center FAB
                                Spacer(modifier = Modifier.width(56.dp))
                            } else {
                                val isSelected = currentRoute == screen.route
                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.15f else 1.0f,
                                    animationSpec = spring(stiffness = 300f),
                                    label = "navScale"
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable {
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        tint = if (isSelected) FintechPrimary else FintechTextSecondary,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = screen.title,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) FintechPrimary else FintechTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Center Floating Glowing Action Button (Unclipped)
                Surface(
                    onClick = {
                        addDialogType = TransactionType.EXPENSE
                        showAddDialog = true
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-14).dp)
                        .size(56.dp)
                        .shadow(14.dp, CircleShape, spotColor = FintechPrimary.copy(alpha = 0.5f))
                        .border(3.dp, Color.White, CircleShape),
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(FintechPrimary, FintechSecondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Transaction",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    if (showImportSheet) {
        ImportTransactionSheet(
            onDismiss = { showImportSheet = false },
            viewModel = viewModel,
            sharedText = sharedText,
            fileUriStr = sharedUriStr
        )
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            viewModel = viewModel,
            sharedText = sharedText,
            defaultType = addDialogType
        )
    }
}
