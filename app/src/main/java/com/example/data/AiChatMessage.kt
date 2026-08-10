package com.example.data

import java.util.UUID

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionSummary: String? = null,
    val isPending: Boolean = false,
    val actionType: String? = null // "ADD", "DELETE", "NONE"
)
