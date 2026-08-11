package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.Transaction
import com.example.data.TransactionType
import com.example.ui.components.ShimmerCard
import com.example.ui.components.ShimmerTransactionList
import com.example.ui.theme.FintechAccent
import com.example.ui.theme.FintechBackground
import com.example.ui.theme.FintechError
import com.example.ui.theme.FintechPrimary
import com.example.ui.theme.FintechSecondary
import com.example.ui.theme.FintechSuccess
import com.example.ui.theme.FintechSurface
import com.example.ui.theme.FintechTextPrimary
import com.example.ui.theme.FintechTextSecondary
import com.example.ui.theme.FintechTextTertiary
import com.example.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Home screen — rebuilt for smooth scrolling.
 *
 * Perf rules followed throughout this file:
 *  1. No custom shadow ambientColor/spotColor anywhere (kills the fast shadow path).
 *  2. Every card uses Material3 `Card`/`CardDefaults` — one hardware-accelerated
 *     shadow layer, not a stacked shadow+border+background+clip chain.
 *  3. The scrollable background is a single flat color. Decoration (the header
 *     gradient) is a fixed-size Box, not a full-screen Canvas, so it never
 *     repaints during scroll.
 *  4. State is collected as low in the tree as possible so a change in one
 *     section (e.g. a new transaction) doesn't recompose the whole screen.
 *  5. Every LazyColumn / LazyRow item has a stable `key` so Compose can skip
 *     and reuse rows instead of rebuilding them.
 */

private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FinanceViewModel,
    navController: NavController,
    onAddTransaction: (TransactionType) -> Unit = {}
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    var showAiChat by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FintechBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "header") {
                HomeHeader(
                    viewModel = viewModel,
                    onNotificationsClick = { navController.navigate("settings") },
                    onAiClick = { showAiChat = true }
                )
            }

            item(key = "balance_card") {
                if (isLoading) {
                    ShimmerCard(modifier = Modifier.padding(horizontal = 20.dp), height = 190.dp)
                } else {
                    BalanceCardSection(viewModel, Modifier.padding(horizontal = 20.dp))
                }
            }

            item(key = "quick_actions") {
                QuickActionsRow(
                    onAddTransaction = onAddTransaction,
                    onTransfer = { navController.navigate("transactions") },
                    onBudget = { navController.navigate("budget") },
                    onReports = { navController.navigate("reports") }
                )
            }

            item(key = "stats_grid") {
                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ShimmerCard(modifier = Modifier.weight(1f), height = 90.dp)
                        ShimmerCard(modifier = Modifier.weight(1f), height = 90.dp)
                    }
                } else {
                    StatsGridSection(viewModel, Modifier.padding(horizontal = 20.dp))
                }
            }

            item(key = "insight_card") {
                InsightCardSection(viewModel, Modifier.padding(horizontal = 20.dp))
            }

            item(key = "analytics_card") {
                AnalyticsSection(viewModel, Modifier.padding(horizontal = 20.dp))
            }

            item(key = "recent_header") {
                RecentActivityHeader(
                    onViewAll = { navController.navigate("transactions") },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            if (isLoading) {
                item(key = "recent_shimmer") {
                    ShimmerTransactionList()
                }
            } else {
                recentTransactionsItems(transactions)
            }
        }
    }

    if (showAiChat) {
        AiChatSheet(onDismiss = { showAiChat = false }, viewModel = viewModel)
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun HomeHeader(
    viewModel: FinanceViewModel,
    onNotificationsClick: () -> Unit,
    onAiClick: () -> Unit
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()

    // Fixed-height decorative gradient behind the header only — cheap, drawn once,
    // scrolls away with the row instead of sitting as a full-screen canvas.
    val headerBrush = remember {
        Brush.linearGradient(
            colors = listOf(FintechPrimary.copy(alpha = 0.10f), Color.Transparent)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBrush)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Welcome back",
                    fontSize = 13.sp,
                    color = FintechTextSecondary
                )
                Text(
                    text = userName.ifBlank { "David" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = FintechTextPrimary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlatIconButton(icon = Icons.Filled.AutoAwesome, tint = FintechPrimary, onClick = onAiClick)
                FlatIconButton(icon = Icons.Filled.Notifications, tint = FintechTextPrimary, onClick = onNotificationsClick)
            }
        }
    }
}

/** Zero-shadow icon button — a tinted circle, no elevation at all. */
@Composable
private fun FlatIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// ---------------------------------------------------------------------------
// Balance card
// ---------------------------------------------------------------------------

@Composable
private fun BalanceCardSection(viewModel: FinanceViewModel, modifier: Modifier = Modifier) {
    val balance by viewModel.allTimeBalance.collectAsStateWithLifecycle()
    val cardNumber by viewModel.cardNumber.collectAsStateWithLifecycle()
    val income by viewModel.totalIncomeThisMonth.collectAsStateWithLifecycle()
    val expense by viewModel.totalExpenseThisMonth.collectAsStateWithLifecycle()

    val cardBrush = remember {
        Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF3730A3), FintechPrimary))
    }
    val maskedCard = remember(cardNumber) {
        val digits = cardNumber.filter { it.isDigit() }
        if (digits.length >= 4) "•••• •••• •••• ${digits.takeLast(4)}" else "•••• •••• •••• 0000"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(cardBrush)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Balance", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                Icon(Icons.Filled.CreditCard, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = currencyFormat.format(balance),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(18.dp))
            Text(maskedCard, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BalancePill(label = "Income", amount = income, icon = Icons.Filled.ArrowDownward, color = FintechSuccess)
                BalancePill(label = "Expense", amount = expense, icon = Icons.Filled.ArrowUpward, color = FintechError)
            }
        }
    }
}

@Composable
private fun BalancePill(label: String, amount: Double, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            Text(currencyFormat.format(amount), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------------------------------------------------------------------------
// Quick actions
// ---------------------------------------------------------------------------

private data class QuickAction(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionsRow(
    onAddTransaction: (TransactionType) -> Unit,
    onTransfer: () -> Unit,
    onBudget: () -> Unit,
    onReports: () -> Unit
) {
    val actions = remember(onAddTransaction, onTransfer, onBudget, onReports) {
        listOf(
            QuickAction("Income", Icons.Filled.ArrowDownward, FintechSuccess) { onAddTransaction(TransactionType.INCOME) },
            QuickAction("Expense", Icons.Filled.ArrowUpward, FintechError) { onAddTransaction(TransactionType.EXPENSE) },
            QuickAction("Savings", Icons.Filled.Savings, FintechSecondary) { onAddTransaction(TransactionType.SAVINGS) },
            QuickAction("Transfer", Icons.Filled.SwapHoriz, FintechAccent, onTransfer),
            QuickAction("Budget", Icons.Filled.PieChart, FintechPrimary, onBudget),
            QuickAction("Reports", Icons.Filled.ReceiptLong, FintechTextSecondary, onReports)
        )
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(actions, key = { it.label }) { action ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(action.color.copy(alpha = 0.12f))
                        .clickable(onClick = action.onClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(action.icon, contentDescription = action.label, tint = action.color, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(action.label, fontSize = 12.sp, color = FintechTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Stats grid
// ---------------------------------------------------------------------------

@Composable
private fun StatsGridSection(viewModel: FinanceViewModel, modifier: Modifier = Modifier) {
    val savings by viewModel.allTimeSavings.collectAsStateWithLifecycle()
    val salary by viewModel.monthlySalary.collectAsStateWithLifecycle()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FlatStatCard(
            modifier = Modifier.weight(1f),
            label = "Total Savings",
            value = currencyFormat.format(savings),
            icon = Icons.Filled.Savings,
            color = FintechSecondary
        )
        FlatStatCard(
            modifier = Modifier.weight(1f),
            label = "Monthly Salary",
            value = currencyFormat.format(salary),
            icon = Icons.Filled.Bolt,
            color = FintechAccent
        )
    }
}

@Composable
private fun FlatStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FintechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FintechTextPrimary)
            Text(label, fontSize = 11.sp, color = FintechTextTertiary)
        }
    }
}

// ---------------------------------------------------------------------------
// AI insight card
// ---------------------------------------------------------------------------

@Composable
private fun InsightCardSection(viewModel: FinanceViewModel, modifier: Modifier = Modifier) {
    val insight by viewModel.insight.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = !insight.isNullOrBlank(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = FintechPrimary.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = FintechPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = insight.orEmpty(),
                    fontSize = 13.sp,
                    color = FintechTextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Analytics (6-month bar chart)
// ---------------------------------------------------------------------------

@Composable
private fun AnalyticsSection(viewModel: FinanceViewModel, modifier: Modifier = Modifier) {
    val pastSixMonths by viewModel.pastSixMonthsExpenses.collectAsStateWithLifecycle()

    if (pastSixMonths.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = FintechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Spending Trend", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = FintechTextPrimary)
            Text("Last 6 months", fontSize = 12.sp, color = FintechTextTertiary)
            Spacer(Modifier.height(16.dp))

            val maxAmount = remember(pastSixMonths) {
                (pastSixMonths.maxOfOrNull { it.amount } ?: 0.0).coerceAtLeast(1.0)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                pastSixMonths.forEach { month ->
                    val fraction = (month.amount / maxAmount).toFloat().coerceIn(0f, 1f)
                    AnimatedBar(fraction = fraction, label = month.month)
                }
            }
        }
    }
}

@Composable
private fun AnimatedBar(fraction: Float, label: String) {
    // One-shot animation driven by an Animatable so it runs once on first
    // composition and then sits idle — it does not re-trigger on scroll.
    val animatedFraction = remember { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(fraction) {
        animatedFraction.animateTo(fraction, animationSpec = tween(500, easing = FastOutSlowInEasing))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((80 * animatedFraction.value).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(FintechPrimary.copy(alpha = 0.85f))
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 10.sp, color = FintechTextTertiary)
    }
}

// ---------------------------------------------------------------------------
// Recent transactions
// ---------------------------------------------------------------------------

@Composable
private fun RecentActivityHeader(onViewAll: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Recent Activity", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FintechTextPrimary)
        Text(
            "View all",
            fontSize = 13.sp,
            color = FintechPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onViewAll)
        )
    }
}

/**
 * Plain (non-composable) LazyListScope extension. State is collected once in
 * [HomeScreen] and the resulting list is passed in here — this function just
 * emits `item`/`items` calls, so it doesn't need to be composable itself.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.recentTransactionsItems(
    transactions: List<Transaction>
) {
    val recent = transactions.take(5)

    if (recent.isEmpty()) {
        item(key = "empty_state") {
            EmptyTransactionsState(modifier = Modifier.padding(horizontal = 20.dp))
        }
        return
    }

    items(recent, key = { it.id }) { transaction ->
        TransactionRow(transaction, modifier = Modifier.padding(horizontal = 20.dp))
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, modifier: Modifier = Modifier) {
    val (icon, color) = remember(transaction.type) {
        when (transaction.type) {
            TransactionType.INCOME -> Icons.Filled.ArrowDownward to FintechSuccess
            TransactionType.EXPENSE -> Icons.Filled.ArrowUpward to FintechError
            TransactionType.SAVINGS -> Icons.Filled.Savings to FintechSecondary
            TransactionType.TRANSFER -> Icons.Filled.SwapHoriz to FintechAccent
        }
    }
    val sign = if (transaction.type == TransactionType.EXPENSE) "-" else "+"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FintechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.category, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FintechTextPrimary)
                Text(transaction.account, fontSize = 12.sp, color = FintechTextTertiary)
            }
            Text(
                text = "$sign${currencyFormat.format(transaction.amount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (transaction.type == TransactionType.EXPENSE) FintechError else FintechSuccess
            )
        }
    }
}

@Composable
private fun EmptyTransactionsState(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FintechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = FintechTextTertiary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text("No transactions yet", fontSize = 13.sp, color = FintechTextSecondary)
        }
    }
}
