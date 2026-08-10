package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.Category
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: FinanceViewModel, navController: NavController) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val cardNumber by viewModel.cardNumber.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val salary by viewModel.monthlySalary.collectAsStateWithLifecycle()
    val initialBalance by viewModel.initialBalance.collectAsStateWithLifecycle()

    var showCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var showCardDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassIconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FintechTextPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Settings & Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = FintechTextPrimary)
                    }

                    GlassIconButton(onClick = {
                        categoryToEdit = null
                        showCategoryDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category", tint = FintechPrimary)
                    }
                }
            }

            // User Profile Glass Card
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    onClick = { showProfileDialog = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = FintechPrimary,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(userName.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(userName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FintechTextPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = FintechPrimary, modifier = Modifier.size(14.dp))
                            }
                            Text("Monthly Salary: \$${salary.toInt()}", fontSize = 12.sp, color = FintechTextSecondary)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = FintechSuccess.copy(alpha = 0.15f)
                        ) {
                            Text("PRO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FintechSuccess, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }

            // Payment Card Config
            item {
                Text("Account & Budget", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FintechTextSecondary, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp))
                
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    onClick = { showProfileDialog = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = FintechPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Monthly Budget Limit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FintechTextPrimary)
                                Text("Rs. ${salary.toInt()}", fontSize = 12.sp, color = FintechTextSecondary)
                            }
                        }
                        Icon(Icons.Default.Edit, contentDescription = null, tint = FintechPrimary, modifier = Modifier.size(18.dp))
                    }
                }

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    onClick = { showCardDialog = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = FintechPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Linked Card Number", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FintechTextPrimary)
                                Text("•••• ${cardNumber.takeLast(4)}", fontSize = 12.sp, color = FintechTextSecondary)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = FintechTextTertiary)
                    }
                }
            }

            // Category Manager
            item {
                Text("Categories Manager", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FintechTextSecondary, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp))
            }

            items(categories, key = { it.name }) { category ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    elevation = 2.dp,
                    onClick = {
                        categoryToEdit = category
                        showCategoryDialog = true
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = FintechSecondary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = FintechSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(category.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FintechTextPrimary, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.deleteCategory(category) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = FintechError)
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        ProfileDialog(
            currentName = userName,
            currentSalary = salary,
            currentBalance = initialBalance,
            onDismiss = { showProfileDialog = false },
            onSave = { name, salaryVal, balanceVal ->
                viewModel.completeSetup(salaryVal, balanceVal, name)
                showProfileDialog = false
            }
        )
    }

    if (showCardDialog) {
        CardDialog(
            initialNumber = cardNumber,
            onDismiss = { showCardDialog = false },
            onSave = { newNum ->
                viewModel.updateCardNumber(newNum)
                showCardDialog = false
            }
        )
    }

    if (showCategoryDialog) {
        CategoryDialog(
            category = categoryToEdit,
            onDismiss = { showCategoryDialog = false },
            onSave = { name, iconName ->
                if (categoryToEdit != null && categoryToEdit!!.name != name) {
                    viewModel.deleteCategory(categoryToEdit!!)
                }
                viewModel.addCategory(name, iconName)
                showCategoryDialog = false
            }
        )
    }
}

@Composable
fun CardDialog(
    initialNumber: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialNumber) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Card Number", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Card Number (16 digits)") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onSave(text) }, colors = ButtonDefaults.buttonColors(containerColor = FintechPrimary)) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color.White
    )
}

@Composable
fun CategoryDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "New Category" else "Edit Category", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name, "Category") }, colors = ButtonDefaults.buttonColors(containerColor = FintechPrimary)) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color.White
    )
}

@Composable
fun ProfileDialog(
    currentName: String,
    currentSalary: Double,
    currentBalance: Double,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var salaryStr by remember { mutableStateOf(currentSalary.toInt().toString()) }
    var balanceStr by remember { mutableStateOf(currentBalance.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile & Budget", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = salaryStr,
                    onValueChange = { salaryStr = it },
                    label = { Text("Monthly Salary / Budget (\$)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Initial Balance (\$)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val salaryVal = salaryStr.toDoubleOrNull() ?: 0.0
                    val balanceVal = balanceStr.toDoubleOrNull() ?: 0.0
                    onSave(name.ifBlank { "User" }, salaryVal, balanceVal)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FintechPrimary)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color.White
    )
}
