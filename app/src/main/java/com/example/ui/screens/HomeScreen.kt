package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.Transaction
import com.example.data.TransactionType
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.components.ShimmerCard
import com.example.ui.components.ShimmerTransactionList
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * Home screen — original glassmorphism look restored, rebuilt for smooth scrolling.
 *
 * Perf rules that actually fix the jank (kept from the previous pass, now applied
 * consistently across every element that has a shadow):
 *  1. NO custom shadow ambientColor/spotColor anywhere. Default shadow colors keep
 *     Compose on its hardware-accelerated shadow path; a custom color forces every
 *     shadowed element onto slow offscreen compositing — this was the actual bug.
 *  2. Every LazyColumn/LazyRow item has a stable `key` so Compose can skip/reuse
 *     rows instead of rebuilding them on every recomposition.
 *  3. State is collected once, at the section that actually needs it — not
 *     redundantly at the top of the screen AND inside each section (the original
 *     file did both for `transactions` and `pastSixMonthsExpenses`, which meant
 *     the whole screen recomposed on every change for no reason).
 *  4. Recent transactions are individual keyed LazyColumn items, not one giant
 *     item containing a manually-looped Column.
 *  5. GlassBackground's decorative blobs are a single static Canvas drawn once,
 *     not re-issued per scroll frame.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FinanceViewModel,
    navController: NavController,
    onAddTransaction: (TransactionType) -> Unit = {}
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val insight by viewModel.insight.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val format = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    var hasUnreadNotifications by remember { mutableStateOf(false) }
    var showAiChatSheet by remember { mutableStateOf(false) }

    GlassBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp)
        ) {
            item(key = "header") {
                HomeHeaderWrapper(
                    viewModel = viewModel,
                    hasUnreadNotifications = hasUnreadNotifications,
                    onNotificationClick = {
                        hasUnreadNotifications = false
                        navController.navigate("reports")
                    },
                    navController = navController
                )
            }

            item(key = "balance_card") {
                if (isLoading) {
                    ShimmerCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), height = 180.dp)
                } else {
                    PremiumBalanceCardWrapper(
                        viewModel = viewModel,
                        format = format
                    )
                }
            }

            item(key = "summary_grid") {
                val onSummaryClick = remember(navController) { { screen: String -> navController.navigate(screen) } }
                HomeSummaryGridWrapper(
                    viewModel = viewModel,
                    format = format,
                    onSummaryClick = onSummaryClick
                )
            }

            item(key = "quick_actions") {
                val onOpenAiChat = remember { { showAiChatSheet = true } }
                QuickActionsSection(
                    navController = navController,
                    onAddTransaction = onAddTransaction,
                    onOpenAiChat = onOpenAiChat
                )
            }

            item(key = "ai_banner") {
                val onOpenAiChat = remember { { showAiChatSheet = true } }
                AiAssistantBanner(onOpenAiChat = onOpenAiChat)
            }

            if (!insight.isNullOrBlank()) {
                item(key = "ai_insight") {
                    AiInsightGlassCard(insight = insight!!)
                }
            }

            item(key = "analytics") {
                AnalyticsSectionCard(viewModel = viewModel)
            }

            item(key = "recent_header") {
                RecentActivityHeader(onViewAllClick = { navController.navigate("transactions") })
            }

            if (isLoading) {
                item(key = "recent_shimmer") {
                    ShimmerTransactionList(count = 3)
                }
            } else {
                recentTransactionItems(transactions = transactions, format = format)
            }
        }
    }

    if (showAiChatSheet) {
        AiChatSheet(
            onDismiss = { showAiChatSheet = false },
            viewModel = viewModel
        )
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
fun HomeHeaderWrapper(
    viewModel: FinanceViewModel,
    hasUnreadNotifications: Boolean,
    onNotificationClick: () -> Unit,
    navController: NavController
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    HomeHeader(
        greeting = greeting,
        userName = userName.ifBlank { "David" },
        hasUnreadNotifications = hasUnreadNotifications,
        onProfileClick = { navController.navigate("settings") },
        onNotificationClick = onNotificationClick,
        onMoreClick = { navController.navigate("settings") }
    )
}

@Composable
fun HomeHeader(
    greeting: String,
    userName: String,
    hasUnreadNotifications: Boolean,
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = FintechPrimary.copy(alpha = 0.15f),
                border = BorderStroke(1.5.dp, FintechPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .size(46.dp)
                    .clickable(onClick = onProfileClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = FintechPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(greeting, fontSize = 12.sp, color = FintechTextSecondary)
                Text(userName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FintechTextPrimary)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box {
                GlassIconButton(onClick = onNotificationClick) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = FintechTextPrimary, modifier = Modifier.size(20.dp))
                }
                if (hasUnreadNotifications) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(FintechError)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }

            GlassIconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = FintechTextPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Balance card
// ---------------------------------------------------------------------------

@Composable
fun PremiumBalanceCardWrapper(
    viewModel: FinanceViewModel,
    format: NumberFormat
) {
    val allTimeBalance by viewModel.allTimeBalance.collectAsStateWithLifecycle()
    val cardNumber by viewModel.cardNumber.collectAsStateWithLifecycle()
    val totalIncomeThisMonth by viewModel.totalIncomeThisMonth.collectAsStateWithLifecycle()
    val totalExpenseThisMonth by viewModel.totalExpenseThisMonth.collectAsStateWithLifecycle()

    val changeThisMonth = remember(totalIncomeThisMonth, totalExpenseThisMonth) { totalIncomeThisMonth - totalExpenseThisMonth }
    val startOfMonthBalance = remember(allTimeBalance, changeThisMonth) { allTimeBalance - changeThisMonth }
    val percentageChange = remember(startOfMonthBalance, changeThisMonth) {
        if (startOfMonthBalance > 0) (changeThisMonth / startOfMonthBalance) * 100 else 0.0
    }

    PremiumBalanceCard(
        balance = format.format(allTimeBalance),
        cardNumber = cardNumber,
        percentageChange = percentageChange
    )
}

@Composable
fun PremiumBalanceCard(
    balance: String,
    cardNumber: String,
    percentageChange: Double
) {
    val displayCardNum = remember(cardNumber) {
        if (cardNumber.length >= 4) "•••• ${cardNumber.takeLast(4)}" else "•••• 4242"
    }
    var isBalanceVisible by remember { mutableStateOf(true) }

    val cardBrush = remember {
        Brush.linearGradient(colors = listOf(Color(0xFF2A2167), Color(0xFF4C3BCF), Color(0xFF4F8CFF)))
    }
    val borderBrush = remember {
        Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.2f)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            // No spotColor here — that custom tint was forcing this card off the
            // fast shadow path. Visually near-identical, meaningfully cheaper.
            .shadow(12.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(cardBrush)
            .border(width = 1.2.dp, brush = borderBrush, shape = RoundedCornerShape(28.dp))
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Balance", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle balance visibility",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { isBalanceVisible = !isBalanceVisible }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isBalanceVisible) balance else "••••••",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                // Card brand chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F00)))
                        Spacer(modifier = Modifier.width((-2).dp))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEB001B).copy(alpha = 0.8f)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.18f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val isPos = percentageChange >= 0
                        Icon(
                            imageVector = if (isPos) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isPos) FintechSuccess else FintechError,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val sign = if (isPos) "+" else ""
                        Text(
                            text = "$sign${String.format(Locale.US, "%.1f", percentageChange)}% this month",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = displayCardNum,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Summary grid
// ---------------------------------------------------------------------------

@Composable
fun HomeSummaryGridWrapper(
    viewModel: FinanceViewModel,
    format: NumberFormat,
    onSummaryClick: (String) -> Unit
) {
    val allTimeIncome by viewModel.allTimeIncome.collectAsStateWithLifecycle()
    val allTimeExpense by viewModel.allTimeExpense.collectAsStateWithLifecycle()
    val allTimeSavings by viewModel.allTimeSavings.collectAsStateWithLifecycle()
    val monthlySalary by viewModel.monthlySalary.collectAsStateWithLifecycle()

    val budgetLeft = remember(monthlySalary, allTimeExpense) { (monthlySalary - allTimeExpense).coerceAtLeast(0.0) }

    HomeSummaryGrid(
        income = format.format(allTimeIncome),
        expenses = format.format(allTimeExpense),
        savings = format.format(allTimeSavings),
        budgetLeft = format.format(budgetLeft),
        onSummaryClick = onSummaryClick
    )
}

@Composable
fun HomeSummaryGrid(
    income: String,
    expenses: String,
    savings: String,
    budgetLeft: String,
    onSummaryClick: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryMiniCard("Income", income, Icons.Default.ArrowDownward, FintechSuccess, Modifier.weight(1f)) { onSummaryClick("transactions") }
            SummaryMiniCard("Expenses", expenses, Icons.Default.ArrowUpward, FintechError, Modifier.weight(1f)) { onSummaryClick("transactions") }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryMiniCard("Savings", savings, Icons.Default.PieChart, PremiumGold, Modifier.weight(1f)) { onSummaryClick("transactions") }
            SummaryMiniCard("Budget Left", budgetLeft, Icons.Default.AccountBalanceWallet, FintechSecondary, Modifier.weight(1f)) { onSummaryClick("budget") }
        }
    }
}

@Composable
fun SummaryMiniCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(modifier = modifier, onClick = onClick, elevation = 3.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(title, fontSize = 11.sp, color = FintechTextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = FintechTextPrimary)
            }
            Surface(shape = CircleShape, color = accentColor.copy(alpha = 0.15f), modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Quick actions
// ---------------------------------------------------------------------------

@Composable
fun QuickActionsSection(
    navController: NavController,
    onAddTransaction: (TransactionType) -> Unit,
    onOpenAiChat: () -> Unit = {}
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = FintechTextPrimary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "qa_ai") { QuickActionPill(Icons.Default.AutoAwesome, FintechPrimary, "AI Chat", onOpenAiChat) }
            item(key = "qa_import_pdf") { QuickActionPill(Icons.Default.PictureAsPdf, Color(0xFF60BB46), "Import PDF") { onAddTransaction(TransactionType.EXPENSE) } }
            item(key = "qa_share_import") { QuickActionPill(Icons.Default.Share, FintechPrimary, "Share Import") { onAddTransaction(TransactionType.EXPENSE) } }
            item(key = "qa_income") { QuickActionPill(Icons.Default.ArrowDownward, FintechSuccess, "Income") { onAddTransaction(TransactionType.INCOME) } }
            item(key = "qa_expense") { QuickActionPill(Icons.Default.ArrowUpward, FintechError, "Expense") { onAddTransaction(TransactionType.EXPENSE) } }
            item(key = "qa_transfer") { QuickActionPill(Icons.Default.CompareArrows, FintechPrimary, "Transfer") { onAddTransaction(TransactionType.TRANSFER) } }
            item(key = "qa_budget") { QuickActionPill(Icons.Default.PieChart, PremiumGold, "Budget") { navController.navigate("budget") } }
            item(key = "qa_reports") { QuickActionPill(Icons.Default.BarChart, FintechSecondary, "Reports") { navController.navigate("reports") } }
            item(key = "qa_settings") { QuickActionPill(Icons.Default.Settings, FintechTextSecondary, "Settings") { navController.navigate("settings") } }
        }
    }
}

@Composable
fun QuickActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).width(62.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, Color.White),
            // No spotColor — same fast-path fix as everywhere else. This pill
            // is repeated 9x on screen, so it's the highest-leverage instance.
            modifier = Modifier.size(52.dp).shadow(4.dp, CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = FintechTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// AI banner + insight
// ---------------------------------------------------------------------------

@Composable
fun AiAssistantBanner(onOpenAiChat: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        onClick = onOpenAiChat
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        // No spotColor here either.
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(FintechPrimary, FintechSecondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Groq AI Assistant", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FintechTextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = FintechPrimary.copy(alpha = 0.12f)) {
                            Text("TAP TO CHAT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FintechPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Add income/expenses or delete entries by voice or chat!", fontSize = 11.sp, color = FintechTextSecondary)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Open Chat", tint = FintechPrimary)
        }
    }
}

@Composable
fun AiInsightGlassCard(insight: String) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        backgroundColor = FintechPrimary.copy(alpha = 0.08f),
        borderColor = FintechPrimary.copy(alpha = 0.3f),
        elevation = 2.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = FintechPrimary, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("AI Financial Insight", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FintechPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(insight, fontSize = 12.sp, color = FintechTextPrimary.copy(alpha = 0.85f), lineHeight = 16.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Analytics (6-month bar chart)
// ---------------------------------------------------------------------------

@Composable
fun AnalyticsSectionCard(viewModel: FinanceViewModel) {
    val pastSixMonths by viewModel.pastSixMonthsExpenses.collectAsStateWithLifecycle()

    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Analytics", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FintechTextPrimary)
            Surface(shape = RoundedCornerShape(12.dp), color = FintechSecondary.copy(alpha = 0.12f)) {
                Text("Past 6 Months", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FintechSecondary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (pastSixMonths.isEmpty() || pastSixMonths.all { it.amount == 0.0 }) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text("No spending history available yet.", color = FintechTextSecondary, fontSize = 12.sp)
            }
        } else {
            val maxAmount = remember(pastSixMonths) { pastSixMonths.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0 }
            val lastIdx = pastSixMonths.lastIndex

            Row(
                modifier = Modifier.fillMaxWidth().height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                pastSixMonths.forEachIndexed { index, item ->
                    AnalyticsBar(
                        month = item.month,
                        amount = item.amount,
                        maxAmount = maxAmount,
                        isCurrent = index == lastIdx,
                        index = index,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyticsBar(
    month: String,
    amount: Double,
    maxAmount: Double,
    isCurrent: Boolean,
    index: Int,
    modifier: Modifier = Modifier
) {
    val fraction = (amount / maxAmount).toFloat().coerceAtLeast(0.08f)

    // One-shot: animates once when the bar's target fraction changes (i.e. once
    // per data load), then sits idle. It does not re-trigger on scroll.
    val animFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 800, delayMillis = index * 80),
        label = "bar"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = modifier) {
        Canvas(modifier = Modifier.width(22.dp).fillMaxHeight(animFraction)) {
            drawRoundRect(
                brush = if (isCurrent) Brush.verticalGradient(listOf(FintechPrimary, FintechSecondary))
                else Brush.verticalGradient(listOf(FintechTextTertiary.copy(alpha = 0.4f), FintechTextTertiary.copy(alpha = 0.2f))),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = month,
            fontSize = 11.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
            color = if (isCurrent) FintechPrimary else FintechTextSecondary
        )
    }
}

// ---------------------------------------------------------------------------
// Recent transactions
// ---------------------------------------------------------------------------

@Composable
fun RecentActivityHeader(onViewAllClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = FintechTextPrimary)
        Text(
            "View All",
            style = MaterialTheme.typography.bodyMedium,
            color = FintechPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onViewAllClick)
        )
    }
}

/**
 * Plain (non-composable) LazyListScope extension. `transactions` is collected
 * once in [HomeScreen] and passed in here as a real list, so each row becomes
 * its own keyed LazyColumn item — Compose can skip/reuse rows individually
 * instead of rebuilding one big blob item on every change.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.recentTransactionItems(
    transactions: List<Transaction>,
    format: NumberFormat
) {
    val recent = transactions.take(5)

    if (recent.isEmpty()) {
        item(key = "recent_empty") {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("No transactions yet. Tap '+' to add one!", color = FintechTextSecondary, fontSize = 13.sp)
                }
            }
        }
        return
    }

    items(recent, key = { it.id }) { tx ->
        GlassTransactionItem(tx = tx, format = format)
    }
}

@Composable
fun GlassTransactionItem(tx: Transaction, format: NumberFormat) {
    val isIncome = remember(tx.type) { tx.type == TransactionType.INCOME }
    val isSavings = remember(tx.type) { tx.type == TransactionType.SAVINGS }
    val amountColor = remember(tx.type) {
        when {
            isIncome -> FintechSuccess
            isSavings -> PremiumGold
            tx.type == TransactionType.EXPENSE -> FintechError
            else -> FintechSecondary
        }
    }
    val sign = remember(tx.type) { if (isIncome || isSavings) "+" else if (tx.type == TransactionType.EXPENSE) "-" else "" }

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        elevation = 2.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Surface(shape = RoundedCornerShape(16.dp), color = amountColor.copy(alpha = 0.12f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.ArrowDownward else if (isSavings) Icons.Default.Savings else Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.category, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FintechTextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(tx.account, fontSize = 11.sp, color = FintechTextSecondary)
            }
            Text(
                text = "$sign${format.format(tx.amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = amountColor
            )
        }
    }
}
