package com.example.ui.screens

import androidx.compose.animation.*
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
import com.example.ui.components.ShimmerTransactionList
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    navController: NavController? = null
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val totalIncome by viewModel.allTimeIncome.collectAsStateWithLifecycle()
    val totalExpenses by viewModel.allTimeExpense.collectAsStateWithLifecycle()
    val allTimeBalance by viewModel.allTimeBalance.collectAsStateWithLifecycle()
    val format = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var txToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showAiChatSheet by remember { mutableStateOf(false) }

    val netBalance = remember(totalIncome, totalExpenses) { totalIncome - totalExpenses }

    GlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Transactions",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = FintechTextPrimary
                    )
                    Text(
                        text = "Full statement & search history",
                        fontSize = 12.sp,
                        color = FintechTextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassIconButton(onClick = { showAiChatSheet = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = FintechPrimary, modifier = Modifier.size(20.dp))
                    }
                    GlassIconButton(onClick = { navController?.navigate("reports") }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = FintechPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Summary Stats Row (Glass Cards)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniTxStatCard(
                    label = "Income",
                    value = format.format(totalIncome),
                    color = FintechSuccess,
                    modifier = Modifier.weight(1f)
                )
                MiniTxStatCard(
                    label = "Expenses",
                    value = format.format(totalExpenses),
                    color = FintechError,
                    modifier = Modifier.weight(1f)
                )
                MiniTxStatCard(
                    label = "Net",
                    value = format.format(netBalance),
                    color = FintechPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                placeholder = { Text("Search transactions, category, notes...", fontSize = 13.sp, color = FintechTextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FintechPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = FintechTextSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.85f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.65f),
                    focusedBorderColor = FintechPrimary,
                    unfocusedBorderColor = Color.White
                ),
                singleLine = true
            )

            // Filter Chips Bar
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("All", "Income", "Expenses", "Transfer", "Savings")
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    val activeColor = when (filter) {
                        "Income" -> FintechSuccess
                        "Expenses" -> FintechError
                        "Savings" -> PremiumGold
                        else -> FintechPrimary
                    }

                    Surface(
                        onClick = { selectedFilter = filter },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) activeColor else Color.White.copy(alpha = 0.75f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else FintechTextPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Filtered Transactions
            val filteredTxs = remember(transactions, searchQuery, selectedFilter) {
                transactions.filter { tx ->
                    val matchesSearch = if (searchQuery.isBlank()) true else {
                        tx.category.contains(searchQuery, ignoreCase = true) ||
                                tx.description.contains(searchQuery, ignoreCase = true) ||
                                tx.account.contains(searchQuery, ignoreCase = true)
                    }
                    val matchesFilter = when (selectedFilter) {
                        "All" -> true
                        "Income" -> tx.type == TransactionType.INCOME
                        "Expenses" -> tx.type == TransactionType.EXPENSE
                        "Transfer" -> tx.type == TransactionType.TRANSFER
                        "Savings" -> tx.type == TransactionType.SAVINGS
                        else -> true
                    }
                    matchesSearch && matchesFilter
                }
            }

            val groupedTxs = remember(filteredTxs) {
                val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.US)
                filteredTxs.groupBy {
                    dateFormat.format(Date(it.timestamp))
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    ShimmerTransactionList(count = 6)
                }
            } else if (filteredTxs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = FintechTextTertiary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No matching transactions found", fontSize = 14.sp, color = FintechTextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    groupedTxs.forEach { (dateHeader, txList) ->
                        item {
                            Text(
                                text = dateHeader,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FintechTextSecondary,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }

                        items(txList, key = { it.id }) { tx ->
                            TxRowCard(
                                tx = tx,
                                format = format,
                                onDelete = { txToDelete = tx }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (txToDelete != null) {
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("Delete Transaction", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${txToDelete?.category}' (${format.format(txToDelete?.amount)})?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(txToDelete!!.id)
                        txToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FintechError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) { Text("Cancel") }
            },
            containerColor = Color.White
        )
    }

    if (showAiChatSheet) {
        AiChatSheet(
            onDismiss = { showAiChatSheet = false },
            viewModel = viewModel
        )
    }
}

@Composable
fun MiniTxStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier,
        elevation = 2.dp
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, fontSize = 10.sp, color = FintechTextSecondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun TxRowCard(
    tx: Transaction,
    format: NumberFormat,
    onDelete: () -> Unit
) {
    val isIncome = tx.type == TransactionType.INCOME
    val isSavings = tx.type == TransactionType.SAVINGS
    val amountColor = when {
        isIncome -> FintechSuccess
        isSavings -> PremiumGold
        tx.type == TransactionType.EXPENSE -> FintechError
        else -> FintechSecondary
    }
    val sign = if (isIncome || isSavings) "+" else if (tx.type == TransactionType.EXPENSE) "-" else ""

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        elevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = amountColor.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tx.account, fontSize = 11.sp, color = FintechTextSecondary)
                    if (tx.description.isNotBlank()) {
                        Text(" • ", fontSize = 11.sp, color = FintechTextTertiary)
                        Text(tx.description, fontSize = 11.sp, color = FintechTextSecondary, maxLines = 1)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign${format.format(tx.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = amountColor
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = FintechTextTertiary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
