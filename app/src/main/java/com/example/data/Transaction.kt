package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType { INCOME, EXPENSE, SAVINGS, TRANSFER }

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val account: String,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String = ""
)
