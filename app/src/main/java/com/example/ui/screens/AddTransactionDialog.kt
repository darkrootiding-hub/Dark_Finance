package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TransactionType
import com.example.ui.theme.*
import com.example.util.EsewaTransactionParser
import com.example.util.ParsedTransaction
import com.example.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    viewModel: FinanceViewModel,
    sharedText: String? = null,
    defaultType: TransactionType = TransactionType.EXPENSE
) {
    val context = LocalContext.current

    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf(sharedText ?: "") }
    var selectedType by remember { mutableStateOf(defaultType) }
    var selectedAccount by remember { mutableStateOf("Main Account") }
    var merchant by remember { mutableStateOf("") }
    var hasReceipt by remember { mutableStateOf(false) }
    var showScanSuccess by remember { mutableStateOf(false) }
    var parsedInfo by remember { mutableStateOf<ParsedTransaction?>(null) }
    var attachedFileName by remember { mutableStateOf<String?>(null) }

    // PDF / File Picker Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val parsed = EsewaTransactionParser.parseWithContext(context, null, uri.toString())
            parsedInfo = parsed
            attachedFileName = uri.lastPathSegment ?: "PDF_Statement.pdf"

            if (parsed.amount != null) {
                amount = if (parsed.amount % 1.0 == 0.0) parsed.amount.toInt().toString() else parsed.amount.toString()
            }
            selectedType = parsed.type
            if (parsed.category.isNotBlank()) category = parsed.category
            if (parsed.account.isNotBlank()) selectedAccount = parsed.account
            if (parsed.merchant.isNotBlank()) merchant = parsed.merchant
            if (parsed.note.isNotBlank()) description = parsed.note
            hasReceipt = true
            showScanSuccess = true
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Automatically parse shared transaction text (from eSewa, Khalti, Bank SMS, etc.)
    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            val parsed = EsewaTransactionParser.parseWithContext(context, sharedText, null)
            parsedInfo = parsed
            if (parsed.amount != null) {
                amount = if (parsed.amount % 1.0 == 0.0) parsed.amount.toInt().toString() else parsed.amount.toString()
            }
            selectedType = parsed.type
            if (parsed.category.isNotBlank()) category = parsed.category
            if (parsed.account.isNotBlank()) selectedAccount = parsed.account
            if (parsed.merchant.isNotBlank()) merchant = parsed.merchant
            description = parsed.note
        }
    }

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts = listOf("eSewa Wallet", "Khalti Wallet", "FonePay Wallet", "Main Account", "Savings Wallet", "Bank Account", "Cash")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White.copy(alpha = 0.94f),
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(FintechTextTertiary.copy(alpha = 0.5f))
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "New Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = FintechTextPrimary
                    )
                    Text(
                        text = "Record income, expense, or import statement",
                        fontSize = 12.sp,
                        color = FintechTextSecondary
                    )
                }

                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = FintechBackground,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = FintechTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // PDF Statement / Receipt Import Button
            Surface(
                onClick = { pdfPickerLauncher.launch("*/*") },
                shape = RoundedCornerShape(20.dp),
                color = FintechPrimary.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, FintechPrimary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = FintechPrimary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Pick PDF",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = attachedFileName ?: "Import eSewa / Fonepay PDF",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = FintechTextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = if (attachedFileName != null) "Statement data auto-filled" else "Tap to pick PDF statement or receipt image",
                                fontSize = 11.sp,
                                color = FintechPrimary
                            )
                        }
                    }

                    Button(
                        onClick = { pdfPickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FintechPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Auto-imported eSewa / Payment Receipt Banner
            if (parsedInfo != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFE8F5E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF60BB46).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF60BB46),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-filled from ${parsedInfo?.sourceApp ?: "Shared Receipt"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "Merchant: ${parsedInfo?.merchant?.ifBlank { "Detected" }} | Ref: ${parsedInfo?.transactionId}",
                                fontSize = 11.sp,
                                color = Color(0xFF388E3C)
                            )
                        }
                    }
                }
            }

            // Transaction Type Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(FintechBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val types = listOf(
                    TransactionType.EXPENSE to "Expense",
                    TransactionType.INCOME to "Income",
                    TransactionType.SAVINGS to "Savings",
                    TransactionType.TRANSFER to "Transfer"
                )

                types.forEach { (type, label) ->
                    val isSelected = selectedType == type
                    val typeColor = when (type) {
                        TransactionType.EXPENSE -> FintechError
                        TransactionType.INCOME -> FintechSuccess
                        TransactionType.SAVINGS -> PremiumGold
                        TransactionType.TRANSFER -> FintechSecondary
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) typeColor else Color.Transparent)
                            .clickable { selectedType = type },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else FintechTextSecondary
                        )
                    }
                }
            }

            // Amount Input Display
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White, RoundedCornerShape(24.dp))
                    .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = FintechPrimary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.8f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Enter Amount", fontSize = 12.sp, color = FintechTextSecondary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Rs. ",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = FintechPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            placeholder = { Text("0.00", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = FintechTextTertiary) },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = FintechTextPrimary,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true,
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }

            // Merchant Field
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant / Receiver Name") },
                placeholder = { Text("e.g. PRISA GIFT CENTER") },
                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = FintechPrimary) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Quick Category Selector Chips
            Column {
                Text("Category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FintechTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val defaultCatNames = listOf("Food & Dining", "Shopping", "Bills & Utilities", "Salary", "Transport", "Health", "Entertainment")
                    val allCats = (categories.map { it.name } + defaultCatNames).distinct()

                    items(allCats) { cat ->
                        val isSelected = category.equals(cat, ignoreCase = true)
                        Surface(
                            onClick = { category = cat },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) FintechPrimary else FintechBackground,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, FintechTextTertiary.copy(alpha = 0.2f)) else null
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else FintechTextPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Payment Account Selector
            Column {
                Text("Account / Payment Method", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = FintechTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts) { acc ->
                        val isSelected = selectedAccount.equals(acc, ignoreCase = true)
                        Surface(
                            onClick = { selectedAccount = acc },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) FintechSecondary.copy(alpha = 0.15f) else FintechBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) FintechSecondary else Color.Transparent
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (acc.contains("Card")) Icons.Default.CreditCard else Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (isSelected) FintechSecondary else FintechTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    acc,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) FintechSecondary else FintechTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Description / Notes
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Note / Description") },
                placeholder = { Text("Note or remark...") },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = FintechBackground,
                    unfocusedContainerColor = FintechBackground,
                    focusedBorderColor = FintechPrimary,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Save Action Button
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    val finalCategory = if (category.isNotBlank()) category else "General"
                    if (amt > 0) {
                        viewModel.addTransaction(
                            selectedType,
                            amt,
                            finalCategory,
                            selectedAccount,
                            if (merchant.isNotBlank()) "$merchant - $description" else description
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = FintechPrimary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FintechPrimary,
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
