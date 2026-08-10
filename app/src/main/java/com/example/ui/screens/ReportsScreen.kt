package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.TransactionType
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.util.*

@Composable
fun ReportsScreen(
    viewModel: FinanceViewModel,
    navController: NavController? = null
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val expensesByCategory by viewModel.expenseByCategoryThisMonth.collectAsStateWithLifecycle()
    val format = NumberFormat.getCurrencyInstance(Locale.US)

    var selectedTimeframe by remember { mutableStateOf("Monthly") }
    val timeframes = listOf("Weekly", "Monthly", "Yearly")

    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalSavings = transactions.filter { it.type == TransactionType.SAVINGS }.sumOf { it.amount }
    val savingsRate = if (totalIncome > 0) (totalSavings / totalIncome) * 100 else 0.0

    val chartColors = listOf(
        FintechPrimary,
        FintechSecondary,
        FintechAccent,
        FintechSuccess,
        PremiumGold,
        FintechError
    )

    GlassBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Financial Reports",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = FintechTextPrimary
                        )
                        Text(
                            text = "Analytics breakdown & spending donut",
                            fontSize = 12.sp,
                            color = FintechTextSecondary
                        )
                    }

                    GlassIconButton(onClick = { navController?.navigate("home") }) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = FintechPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Timeframe Selector Strip
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(timeframes) { tf ->
                        val isSelected = selectedTimeframe == tf
                        Surface(
                            onClick = { selectedTimeframe = tf },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) FintechPrimary else Color.White.copy(alpha = 0.75f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                        ) {
                            Text(
                                text = tf,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else FintechTextPrimary,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Donut Pie Chart Card
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    backgroundColor = Color.White.copy(alpha = 0.85f)
                ) {
                    Text("Expense Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FintechTextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (expensesByCategory.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No expense transactions to display chart", fontSize = 12.sp, color = FintechTextSecondary)
                        }
                    } else {
                        val totalCategorySpend = expensesByCategory.values.sum().coerceAtLeast(1.0)
                        val sortedCategories = expensesByCategory.toList().sortedByDescending { it.second }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Donut Chart Canvas
                            val animAngle by animateFloatAsState(
                                targetValue = 360f,
                                animationSpec = tween(durationMillis = 1000),
                                label = "donut"
                            )

                            Box(contentAlignment = Alignment.Center) {
                                Canvas(modifier = Modifier.size(130.dp)) {
                                    var startAngle = -90f
                                    sortedCategories.forEachIndexed { index, (_, amount) ->
                                        val sweep = ((amount / totalCategorySpend) * animAngle).toFloat()
                                        val color = chartColors[index % chartColors.size]

                                        drawArc(
                                            color = color,
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = 30.dp.toPx())
                                        )
                                        startAngle += sweep
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total", fontSize = 10.sp, color = FintechTextSecondary)
                                    Text(format.format(totalCategorySpend), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FintechTextPrimary)
                                }
                            }

                            // Donut Legend
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                sortedCategories.take(4).forEachIndexed { index, (cat, amt) ->
                                    val color = chartColors[index % chartColors.size]
                                    val percent = (amt / totalCategorySpend) * 100

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = FintechTextPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${percent.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Key Metrics Glass Grid
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text("Total Income", fontSize = 11.sp, color = FintechTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(format.format(totalIncome), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FintechSuccess)
                    }

                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text("Total Expense", fontSize = 11.sp, color = FintechTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(format.format(totalExpense), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FintechError)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text("Savings Rate", fontSize = 11.sp, color = FintechTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${String.format(Locale.US, "%.1f", savingsRate)}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PremiumGold)
                    }

                    GlassCard(modifier = Modifier.weight(1f)) {
                        Text("Top Expense", fontSize = 11.sp, color = FintechTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        val topCat = expensesByCategory.maxByOrNull { it.value }?.key ?: "None"
                        Text(topCat, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FintechPrimary, maxLines = 1)
                    }
                }
            }
        }
    }
}
