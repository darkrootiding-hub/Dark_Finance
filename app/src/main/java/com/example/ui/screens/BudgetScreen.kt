package com.example.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BudgetScreen(
    viewModel: FinanceViewModel,
    navController: NavController? = null
) {
    val totalExpense by viewModel.totalExpenseThisMonth.collectAsStateWithLifecycle()
    val monthlySalary by viewModel.monthlySalary.collectAsStateWithLifecycle()
    val expensesByCategory by viewModel.expenseByCategoryThisMonth.collectAsStateWithLifecycle()
    val format = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    var selectedMonthIndex by remember { mutableStateOf(0) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    val months = remember { listOf("This Month", "Last Month", "3 Months Ago") }

    val budgetRemaining = remember(monthlySalary, totalExpense) { (monthlySalary - totalExpense).coerceAtLeast(0.0) }
    val overallProgress = remember(monthlySalary, totalExpense) { if (monthlySalary > 0) (totalExpense / monthlySalary).toFloat().coerceIn(0f, 1f) else 0f }
    val sortedCategoryList = remember(expensesByCategory) { expensesByCategory.toList() }

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
                            text = "Budget & Expenses",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = FintechTextPrimary
                        )
                        Text(
                            text = "Track limits & monthly category spend",
                            fontSize = 12.sp,
                            color = FintechTextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassIconButton(onClick = { showEditBudgetDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Budget", tint = FintechPrimary, modifier = Modifier.size(20.dp))
                        }
                        GlassIconButton(onClick = { navController?.navigate("reports") }) {
                            Icon(Icons.Default.PieChart, contentDescription = "Reports", tint = FintechPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Month Selector Strip
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(months.indices.toList(), key = { it }) { idx ->
                        val isSelected = selectedMonthIndex == idx
                        Surface(
                            onClick = { selectedMonthIndex = idx },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) FintechPrimary else Color.White.copy(alpha = 0.75f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                        ) {
                            Text(
                                text = months[idx],
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else FintechTextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Glass Overall Budget Card
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    backgroundColor = Color.White.copy(alpha = 0.82f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Monthly Budget", fontSize = 12.sp, color = FintechTextSecondary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = FintechPrimary,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { showEditBudgetDialog = true }
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                format.format(monthlySalary),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FintechTextPrimary,
                                modifier = Modifier.clickable { showEditBudgetDialog = true }
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showEditBudgetDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FintechPrimary.copy(alpha = 0.12f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = FintechPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set Limit", fontSize = 11.sp, color = FintechPrimary, fontWeight = FontWeight.Bold)
                            }

                            Surface(
                                shape = CircleShape,
                                color = FintechPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${(overallProgress * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FintechPrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Animated Progress Bar
                    val animProgress by animateFloatAsState(
                        targetValue = overallProgress,
                        animationSpec = tween(durationMillis = 600),
                        label = "progress"
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape)
                    ) {
                        drawRoundRect(
                            color = FintechBackground,
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                        drawRoundRect(
                            color = if (animProgress > 0.85f) FintechError else FintechPrimary,
                            size = size.copy(width = size.width * animProgress),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Spent: ${format.format(totalExpense)}", fontSize = 11.sp, color = FintechError, fontWeight = FontWeight.Bold)
                        Text("Remaining: ${format.format(budgetRemaining)}", fontSize = 11.sp, color = FintechSuccess, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Expense Category Breakdown Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FintechTextPrimary
                    )
                    Text(
                        text = "${sortedCategoryList.size} Active",
                        fontSize = 12.sp,
                        color = FintechTextSecondary
                    )
                }
            }

            // Category Items
            if (sortedCategoryList.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No expenses logged for this period.", fontSize = 13.sp, color = FintechTextSecondary)
                        }
                    }
                }
            } else {
                items(sortedCategoryList, key = { it.first }) { (category, amount) ->
                    val limit = (monthlySalary / sortedCategoryList.size.coerceAtLeast(1)).coerceAtLeast(200.0)
                    val catProgress = (amount / limit).toFloat().coerceIn(0f, 1f)

                    GlassCategoryProgressItem(
                        category = category,
                        spent = amount,
                        limit = limit,
                        progress = catProgress,
                        format = format
                    )
                }
            }
        }
    }

    if (showEditBudgetDialog) {
        EditBudgetDialog(
            currentBudget = monthlySalary,
            onDismiss = { showEditBudgetDialog = false },
            onSave = { newSalary ->
                viewModel.updateMonthlySalary(newSalary)
            }
        )
    }
}

@Composable
fun EditBudgetDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var budgetInput by remember { mutableStateOf(if (currentBudget > 0) currentBudget.toInt().toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Monthly Budget",
                fontWeight = FontWeight.Bold,
                color = FintechTextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Update your monthly budget spending limit anytime.",
                    fontSize = 12.sp,
                    color = FintechTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("Monthly Budget Amount") },
                    placeholder = { Text("e.g. 50000") },
                    leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = FintechPrimary) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = budgetInput.toDoubleOrNull() ?: 0.0
                    if (parsed >= 0) {
                        onSave(parsed)
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FintechPrimary)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FintechTextSecondary)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}

@Composable
fun GlassCategoryProgressItem(
    category: String,
    spent: Double,
    limit: Double,
    progress: Float,
    format: NumberFormat
) {
    val progressColor = when {
        progress > 0.85f -> FintechError
        progress > 0.65f -> PremiumGold
        else -> FintechSuccess
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        elevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = progressColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = progressColor, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(category, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FintechTextPrimary)
                        Text("Limit: ${format.format(limit)}", fontSize = 11.sp, color = FintechTextSecondary)
                    }
                }

                Text(
                    text = format.format(spent),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = FintechTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated Bar
            val animBar by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(800),
                label = "bar"
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            ) {
                drawRoundRect(
                    color = FintechBackground,
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                drawRoundRect(
                    color = progressColor,
                    size = size.copy(width = size.width * animBar),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }
    }
}
