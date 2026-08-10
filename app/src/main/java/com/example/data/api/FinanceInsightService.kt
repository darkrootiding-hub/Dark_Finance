package com.example.data.api

import com.example.BuildConfig
import com.example.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceInsightService {
    suspend fun getFinancialInsights(transactions: List<Transaction>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GROQ_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GROQ_API_KEY") {
            return@withContext "Please configure your Groq API Key."
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val transactionText = transactions.take(20).joinToString("\n") { tx ->
            "${dateFormat.format(Date(tx.timestamp))} - ${tx.type.name} - ${tx.category}: $${tx.amount}"
        }

        val prompt = """
            Based on the following recent transactions, provide a short, personalized financial insight or spending tip.
            Keep it under 3 sentences and be encouraging but realistic.
            
            Transactions:
            $transactionText
        """.trimIndent()

        val request = ChatCompletionRequest(
            messages = listOf(
                Message(role = "system", content = "You are a helpful and concise financial advisor."),
                Message(role = "user", content = prompt)
            ),
            model = "llama-3.3-70b-versatile"
        )

        try {
            val response = RetrofitClient.service.generateContent("Bearer $apiKey", request)
            response.choices?.firstOrNull()?.message?.content ?: "No insights available right now."
        } catch (e: Exception) {
            "Error generating insights: ${e.message}"
        }
    }
}
