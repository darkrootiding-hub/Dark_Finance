package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AiChatMessage
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatSheet(
    onDismiss: () -> Unit,
    viewModel: FinanceViewModel
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val quickSuggestions = remember {
        listOf(
            "Spent Rs 450 on lunch at Bakery",
            "Added 50000 salary income",
            "Delete my last transaction",
            "Delete transaction #1",
            "How much did I spend this month?"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White.copy(alpha = 0.95f),
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(FintechTextTertiary.copy(alpha = 0.4f))
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(6.dp, CircleShape, spotColor = FintechPrimary.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(FintechPrimary, FintechSecondary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Assistant",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Groq AI Assistant",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FintechTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(FintechSuccess)
                            )
                        }
                        Text(
                            text = "Llama 3.3 70B • Add & Delete Transactions",
                            fontSize = 11.sp,
                            color = FintechTextSecondary
                        )
                    }
                }

                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = FintechBackground,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = FintechTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Quick Prompt Suggestions Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                items(quickSuggestions) { prompt ->
                    Surface(
                        onClick = {
                            viewModel.sendAiChatMessage(prompt)
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = FintechPrimary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FintechPrimary.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = prompt,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FintechPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Divider(color = FintechTextTertiary.copy(alpha = 0.15f), thickness = 1.dp)

            // Chat Messages Stream List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubbleItem(message = msg)
                }
            }

            // Bottom Input Bar
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = FintechBackground,
                border = androidx.compose.foundation.BorderStroke(1.2.dp, FintechPrimary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 4.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = FintechPrimary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask or type e.g. 'Spent 350 at Cafe'...", fontSize = 13.sp, color = FintechTextTertiary) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = FintechTextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    val query = inputText
                                    inputText = ""
                                    viewModel.sendAiChatMessage(query)
                                }
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val query = inputText
                                inputText = ""
                                viewModel.sendAiChatMessage(query)
                            }
                        },
                        shape = CircleShape,
                        color = if (inputText.isNotBlank()) FintechPrimary else FintechTextTertiary.copy(alpha = 0.3f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(message: AiChatMessage) {
    val isUser = message.sender == "USER"
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.US) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = FintechPrimary.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Top)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "AI",
                        tint = FintechPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (isUser) 20.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 20.dp
                ),
                color = if (isUser) FintechPrimary else Color.White,
                border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, FintechTextTertiary.copy(alpha = 0.15f)) else null,
                modifier = Modifier.shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (message.isPending) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = FintechPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI is thinking & processing...",
                                fontSize = 12.sp,
                                color = FintechTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            text = message.text,
                            fontSize = 13.sp,
                            color = if (isUser) Color.White else FintechTextPrimary,
                            lineHeight = 18.sp
                        )

                        // Action Result Pill Badge (Added / Deleted Transaction Confirmation)
                        if (!message.actionSummary.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val isDelete = message.actionType == "DELETE"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDelete) FintechError.copy(alpha = 0.12f) else FintechSuccess.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDelete) FintechError.copy(alpha = 0.4f) else FintechSuccess.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isDelete) Icons.Default.DeleteOutline else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isDelete) FintechError else FintechSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = message.actionSummary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDelete) FintechError else Color(0xFF1B5E20)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formattedTime,
                fontSize = 10.sp,
                color = FintechTextTertiary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
