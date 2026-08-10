package com.example.data.api

import com.example.BuildConfig
import com.example.data.Transaction
import com.example.data.TransactionType
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class AiParsedTransactionData(
    val type: String? = "EXPENSE",
    val amount: Double? = 0.0,
    val category: String? = "General",
    val account: String? = "Main Account",
    val description: String? = ""
)

@JsonClass(generateAdapter = true)
data class AiChatActionResponse(
    val reply: String,
    val action: String? = "NONE", // "ADD_TRANSACTION", "DELETE_TRANSACTION", "NONE"
    val transactionData: AiParsedTransactionData? = null,
    val deleteTransactionId: Int? = null
)

class AiChatService {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val actionAdapter = moshi.adapter(AiChatActionResponse::class.java)

    suspend fun processUserMessage(
        userMessage: String,
        conversationHistory: List<Message>,
        recentTransactions: List<Transaction>
    ): AiChatActionResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GROQ_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GROQ_API_KEY") {
            return@withContext AiChatActionResponse(
                reply = "Groq API key is missing. Please set your GROQ_API_KEY in secrets/env to enable AI chat.",
                action = "NONE"
            )
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
        val transactionContext = if (recentTransactions.isEmpty()) {
            "No recent transactions recorded."
        } else {
            recentTransactions.take(30).joinToString("\n") { tx ->
                "ID: ${tx.id} | Date: ${dateFormat.format(Date(tx.timestamp))} | Type: ${tx.type.name} | Amount: Rs. ${tx.amount} | Category: ${tx.category} | Account: ${tx.account} | Description: ${tx.description}"
            }
        }

        val systemPrompt = """
            You are a smart financial AI assistant integrated into a mobile finance manager app in Nepal.
            Your goal is to help users manage their money, add income or expenses, and delete wrong/duplicate transactions upon request.

            CURRENT USER RECENT TRANSACTIONS LIST:
            $transactionContext

            INSTRUCTIONS:
            You MUST ALWAYS return your response as a valid raw JSON object matching this exact structure:
            {
              "reply": "Your friendly, helpful message to the user here.",
              "action": "ADD_TRANSACTION" | "DELETE_TRANSACTION" | "NONE",
              "transactionData": {
                "type": "EXPENSE" | "INCOME" | "SAVINGS" | "TRANSFER",
                "amount": 500.0,
                "category": "Food & Dining",
                "account": "eSewa Wallet",
                "description": "Bakery momo"
              },
              "deleteTransactionId": 12
            }

            RULES FOR ACTIONS:
            1. If user asks to ADD/RECORD income, expense, savings, or transfer (e.g., "I spent Rs 500 on lunch at Bakery", "I earned 45000 salary"):
               - Set "action": "ADD_TRANSACTION"
               - Fill "transactionData" with parsed details (type, amount, category, account, description).
               - Default account to "Main Account" or "eSewa Wallet" if unspecified. Default category to relevant category (e.g., "Food & Dining", "Salary", "Shopping", "Bills & Utilities", "Transport", "Health").
               - Set "deleteTransactionId": null

            2. If user asks to DELETE, REMOVE, or CANCEL a transaction (e.g., "Delete transaction #12", "Remove the 500 expense at Bakery", "Delete my last transaction"):
               - Match the transaction from CURRENT USER RECENT TRANSACTIONS LIST above.
               - Set "action": "DELETE_TRANSACTION"
               - Set "deleteTransactionId": <ID of matched transaction>
               - Set "transactionData": null

            3. For general questions, advice, budget queries, or greetings:
               - Set "action": "NONE"
               - Set "transactionData": null
               - Set "deleteTransactionId": null

            CRITICAL: Return ONLY valid JSON. No markdown formatting outside json, no extra preambles.
        """.trimIndent()

        val messages = mutableListOf<Message>()
        messages.add(Message(role = "system", content = systemPrompt))

        // Append recent chat history (up to last 6 messages for context)
        messages.addAll(conversationHistory.takeLast(6))
        messages.add(Message(role = "user", content = userMessage))

        val request = ChatCompletionRequest(
            messages = messages,
            model = "llama-3.3-70b-versatile"
        )

        try {
            val response = RetrofitClient.service.generateContent("Bearer $apiKey", request)
            val rawContent = response.choices?.firstOrNull()?.message?.content?.trim() ?: ""

            // Clean json if codeblock wrapped
            val jsonString = rawContent
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsed = try {
                actionAdapter.fromJson(jsonString)
            } catch (_: Exception) {
                // Regex fallback if JSON was slightly malformed
                extractFallbackResponse(jsonString, rawContent)
            }

            parsed ?: AiChatActionResponse(
                reply = rawContent.ifBlank { "I've processed your request." },
                action = "NONE"
            )
        } catch (e: Exception) {
            AiChatActionResponse(
                reply = "Sorry, I ran into an issue connecting to AI: ${e.message}",
                action = "NONE"
            )
        }
    }

    private fun extractFallbackResponse(jsonString: String, rawContent: String): AiChatActionResponse? {
        return try {
            val replyMatch = Regex(""""reply"\s*:\s*"([^"]+)"""").find(jsonString)
            val reply = replyMatch?.groupValues?.get(1) ?: rawContent

            val actionMatch = Regex(""""action"\s*:\s*"([^"]+)"""").find(jsonString)
            val action = actionMatch?.groupValues?.get(1) ?: "NONE"

            val deleteIdMatch = Regex(""""deleteTransactionId"\s*:\s*(\d+)""").find(jsonString)
            val deleteId = deleteIdMatch?.groupValues?.get(1)?.toIntOrNull()

            val typeMatch = Regex(""""type"\s*:\s*"([^"]+)"""").find(jsonString)
            val amountMatch = Regex(""""amount"\s*:\s*([0-9.]+)""").find(jsonString)
            val categoryMatch = Regex(""""category"\s*:\s*"([^"]+)"""").find(jsonString)
            val descriptionMatch = Regex(""""description"\s*:\s*"([^"]+)"""").find(jsonString)

            val txData = if (action == "ADD_TRANSACTION") {
                AiParsedTransactionData(
                    type = typeMatch?.groupValues?.get(1) ?: "EXPENSE",
                    amount = amountMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0,
                    category = categoryMatch?.groupValues?.get(1) ?: "General",
                    account = "Main Account",
                    description = descriptionMatch?.groupValues?.get(1) ?: ""
                )
            } else null

            AiChatActionResponse(
                reply = reply,
                action = action,
                transactionData = txData,
                deleteTransactionId = deleteId
            )
        } catch (_: Exception) {
            null
        }
    }
}
