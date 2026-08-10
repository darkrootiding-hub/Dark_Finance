package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TransactionType
import com.example.ui.components.ConfettiEffect
import com.example.ui.components.GlassCard
import com.example.ui.components.SuccessCheckmarkAnimation
import com.example.ui.theme.*
import com.example.util.EsewaTransactionParser
import com.example.util.ParsedTransaction
import com.example.viewmodel.FinanceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportTransactionSheet(
    onDismiss: () -> Unit,
    viewModel: FinanceViewModel,
    sharedText: String? = null,
    fileUriStr: String? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var activeUriStr by remember { mutableStateOf(fileUriStr) }
    var activeText by remember { mutableStateOf(sharedText) }

    var initialParsed by remember(activeText, activeUriStr) {
        mutableStateOf(EsewaTransactionParser.parseWithContext(context, activeText, activeUriStr))
    }

    // PDF / File Picker Launcher for manual file selection
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            activeUriStr = uri.toString()
            initialParsed = EsewaTransactionParser.parseWithContext(context, activeText, uri.toString())
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Editable State Fields
    var amount by remember(initialParsed) {
        mutableStateOf(
            if (initialParsed.amount != null) {
                if (initialParsed.amount!! % 1.0 == 0.0) initialParsed.amount!!.toInt().toString()
                else initialParsed.amount!!.toString()
            } else ""
        )
    }
    var merchant by remember(initialParsed) { mutableStateOf(initialParsed.merchant) }
    var selectedCategory by remember(initialParsed) { mutableStateOf(initialParsed.category) }
    var selectedType by remember(initialParsed) { mutableStateOf(initialParsed.type) }
    var selectedAccount by remember(initialParsed) { mutableStateOf(initialParsed.account) }
    var transactionId by remember(initialParsed) { mutableStateOf(initialParsed.transactionId) }
    var status by remember(initialParsed) { mutableStateOf(initialParsed.status) }
    var dateStr by remember(initialParsed) { mutableStateOf(initialParsed.dateStr) }
    var timeStr by remember(initialParsed) { mutableStateOf(initialParsed.timeStr) }
    var notes by remember(initialParsed) { mutableStateOf(initialParsed.note) }

    var showIncompleteError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var showRawTextAccordion by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Main Glassmorphism Sheet Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = FintechPrimary.copy(alpha = 0.35f)
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.4f))
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.94f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Drag Indicator Accent Bar
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Header Row with Source App Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF60BB46).copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (initialParsed.isImageOrPdf) Icons.Default.PictureAsPdf else Icons.Default.Share,
                                        contentDescription = null,
                                        tint = Color(0xFF60BB46),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Import Transaction",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FintechTextPrimary
                                )
                                Text(
                                    text = "Source: ${initialParsed.sourceApp}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF60BB46)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = FintechTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // PDF / Document Shared Badge & Pick PDF button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = FintechPrimary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FintechPrimary.copy(alpha = 0.18f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = FintechPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = initialParsed.fileName ?: if (initialParsed.isImageOrPdf) "PDF Statement / File" else "Shared Clipboard Data",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FintechTextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (initialParsed.rawText.length > 30) "PDF Text Extracted (${initialParsed.rawText.length} chars)" else "Ready to parse statement",
                                        fontSize = 11.sp,
                                        color = FintechTextSecondary
                                    )
                                }
                            }

                            // Pick PDF File Button
                            OutlinedButton(
                                onClick = { pdfPickerLauncher.launch("*/*") },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Incomplete Field Warning Banner
                    AnimatedVisibility(
                        visible = showIncompleteError,
                        enter = fadeIn() + expandVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFFEBEE),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Please complete Amount & Merchant Name before saving.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }

                    // Glass Amount Display / Field
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, FintechPrimary.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = FintechPrimary.copy(alpha = 0.05f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TRANSACTION AMOUNT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FintechTextSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Rs. ",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FintechPrimary
                                )
                                OutlinedTextField(
                                    value = amount,
                                    onValueChange = {
                                        amount = it
                                        showIncompleteError = false
                                    },
                                    placeholder = { Text("0.00", fontSize = 28.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FintechTextPrimary
                                    ),
                                    modifier = Modifier.width(180.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Transaction Type Selector Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TransactionType.values().forEach { type ->
                            val isSelected = selectedType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) FintechPrimary else Color.Transparent)
                                    .clickable {
                                        selectedType = type
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type.name.lowercase().capitalize(),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else FintechTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detected Details Grid Cards
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = {
                            merchant = it
                            showIncompleteError = false
                        },
                        label = { Text("Merchant / Receiver") },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = FintechPrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Selector
                    Text(
                        text = "Category (Auto-Suggested)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FintechTextSecondary,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 4.dp, bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val availableCategories = listOf(
                            "Food & Dining", "Bills & Utilities", "Shopping", "Fuel & Transport",
                            "Health", "Education", "Entertainment", "Income", "General"
                        )
                        availableCategories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategory = cat
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                label = { Text(cat, fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FintechPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = FintechPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Account & Transaction ID Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedAccount,
                            onValueChange = { selectedAccount = it },
                            label = { Text("Account / Wallet") },
                            leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = FintechPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = transactionId,
                            onValueChange = { transactionId = it },
                            label = { Text("Txn ID / Ref") },
                            leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = FintechPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date, Time & Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = dateStr,
                            onValueChange = { dateStr = it },
                            label = { Text("Date") },
                            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = FintechPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = status,
                            onValueChange = { status = it },
                            label = { Text("Status") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (status.contains("SUCCESS", ignoreCase = true)) FintechSuccess else FintechSecondary
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Raw Extract") },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = FintechPrimary) },
                        maxLines = 2,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Expandable Raw PDF Extracted Text Accordion
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRawTextAccordion = !showRawTextAccordion }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (showRawTextAccordion) "Hide Extracted PDF Text" else "View Extracted PDF Text",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FintechPrimary
                        )
                        Icon(
                            imageVector = if (showRawTextAccordion) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = FintechPrimary
                        )
                    }

                    AnimatedVisibility(
                        visible = showRawTextAccordion,
                        enter = fadeIn() + expandVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.04f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = initialParsed.rawText.ifBlank { "No raw text detected in document stream." },
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = FintechTextSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons Row (Scan Again, Cancel, Save)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Scan Again Button
                        OutlinedButton(
                            onClick = {
                                initialParsed = EsewaTransactionParser.parseWithContext(context, activeText, activeUriStr)
                                amount = if (initialParsed.amount != null) initialParsed.amount.toString() else ""
                                merchant = initialParsed.merchant
                                selectedCategory = initialParsed.category
                                selectedAccount = initialParsed.account
                                transactionId = initialParsed.transactionId
                                notes = initialParsed.note
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Re-Scan", fontSize = 12.sp)
                        }

                        // Cancel Button
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FintechError),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }

                        // Prominent Floating Save Button
                        Button(
                            onClick = {
                                val amt = amount.toDoubleOrNull()
                                if (amt == null || amt <= 0 || merchant.isBlank()) {
                                    showIncompleteError = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    return@Button
                                }

                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isSaving = true
                                showSuccessAnimation = true
                                showConfetti = true

                                coroutineScope.launch {
                                    // Save into existing transaction Room database
                                    viewModel.addTransaction(
                                        type = selectedType,
                                        amount = amt,
                                        category = selectedCategory,
                                        account = selectedAccount,
                                        description = if (notes.isNotBlank()) notes else "Imported from ${initialParsed.sourceApp}"
                                    )

                                    delay(1500)
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(),
                            modifier = Modifier
                                .weight(1.5f)
                                .shadow(10.dp, RoundedCornerShape(18.dp), spotColor = FintechPrimary.copy(alpha = 0.4f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(listOf(FintechPrimary, FintechSecondary)),
                                        RoundedCornerShape(18.dp)
                                    )
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Confirm & Save", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Overlay Confetti & Success Checkmark Animation
            ConfettiEffect(isTriggered = showConfetti)

            if (showSuccessAnimation) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    SuccessCheckmarkAnimation(visible = true)
                }
            }
        }
    }
}
