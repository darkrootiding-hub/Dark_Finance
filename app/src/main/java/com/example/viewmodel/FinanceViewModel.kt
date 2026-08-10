package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.data.TransactionType
import com.example.data.UserPreferences
import com.example.data.api.FinanceInsightService
import com.example.data.Category
import com.example.data.CategoryDao
import com.example.data.AiChatMessage
import com.example.data.api.AiChatService
import com.example.data.api.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MonthlySummary(
    val monthYear: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val sortKey: Long
)

data class MonthlyStats(
    val income: Double,
    val expense: Double,
    val savings: Double
)

data class MonthExpense(
    val month: String,
    val amount: Double
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TransactionRepository
    private val categoryDao: CategoryDao
    private val userPreferences: UserPreferences
    private val insightService = FinanceInsightService()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        val transactionDao = database.transactionDao()
        categoryDao = database.categoryDao()
        repository = TransactionRepository(transactionDao)
        userPreferences = UserPreferences(application)

        viewModelScope.launch {
            delay(500)
            _isLoading.value = false
        }
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }

    val categories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCategory(name: String, iconName: String) {
        viewModelScope.launch {
            categoryDao.insertCategory(Category(name, iconName))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.deleteCategory(category)
        }
    }
    
    val userName: StateFlow<String> = userPreferences.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "David")
        
    val isSetupComplete: StateFlow<Boolean> = userPreferences.isSetupComplete
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val monthlySalary: StateFlow<Double> = userPreferences.monthlySalary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
        
    val initialBalance: StateFlow<Double> = userPreferences.initialBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cardNumber: StateFlow<String> = userPreferences.cardNumber
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val monthlyStats: StateFlow<MonthlyStats> = run {
        val range = getCurrentMonthRange()
        val database = AppDatabase.getDatabase(getApplication())
        val dao = database.transactionDao()

        kotlinx.coroutines.flow.combine(
            dao.getTotalByTypeAndMonth(TransactionType.INCOME, range.first, range.second),
            dao.getTotalByTypeAndMonth(TransactionType.EXPENSE, range.first, range.second),
            dao.getTotalByTypeAndMonth(TransactionType.SAVINGS, range.first, range.second)
        ) { income, expense, savings ->
            MonthlyStats(income ?: 0.0, expense ?: 0.0, savings ?: 0.0)
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyStats(0.0, 0.0, 0.0))
    }

    fun updateCardNumber(number: String) {
        viewModelScope.launch {
            userPreferences.saveCardNumber(number)
        }
    }

    fun updateMonthlySalary(salary: Double) {
        viewModelScope.launch {
            userPreferences.updateMonthlySalary(salary)
        }
    }

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTimeBalance: StateFlow<Double> = kotlinx.coroutines.flow.combine(
        transactions,
        initialBalance
    ) { txs, initial ->
        val income = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        initial + income - expense
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allTimeIncome: StateFlow<Double> = transactions.map { txs ->
        txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allTimeExpense: StateFlow<Double> = transactions.map { txs ->
        txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allTimeSavings: StateFlow<Double> = transactions.map { txs ->
        txs.filter { it.type == TransactionType.SAVINGS }.sumOf { it.amount }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)


        
    val expenseByCategoryThisMonth: StateFlow<Map<String, Double>> = transactions.map { txs ->
        val range = getCurrentMonthRange()
        txs.filter {
            it.type == TransactionType.EXPENSE && it.timestamp in range.first..range.second
        }.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    val totalExpenseThisMonth: StateFlow<Double> = monthlyStats.map { it.expense }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalSavingsThisMonth: StateFlow<Double> = monthlyStats.map { it.savings }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncomeThisMonth: StateFlow<Double> = monthlyStats.map { it.income }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlySummaries: StateFlow<List<MonthlySummary>> = transactions.map { txs ->
        val format = SimpleDateFormat("MMMM yyyy", Locale.US)
        txs.groupBy {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            val time = cal.apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            Pair(format.format(cal.time), time)
        }.map { (keyPair, monthTxs) ->
            MonthlySummary(
                monthYear = keyPair.first,
                totalIncome = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                totalExpense = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                sortKey = keyPair.second
            )
        }.sortedByDescending { it.sortKey }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pastSixMonthsExpenses: StateFlow<List<MonthExpense>> = transactions.map { txs ->
        val result = mutableListOf<MonthExpense>()
        val format = SimpleDateFormat("MMM", Locale.US)
        val now = Calendar.getInstance()

        for (i in 5 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            val month = cal.get(Calendar.MONTH)
            val year = cal.get(Calendar.YEAR)
            val monthName = format.format(cal.time)

            val total = txs.filter {
                val txCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                it.type == TransactionType.EXPENSE &&
                txCal.get(Calendar.MONTH) == month &&
                txCal.get(Calendar.YEAR) == year
            }.sumOf { it.amount }

            result.add(MonthExpense(monthName, total))
        }
        result
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _insight = MutableStateFlow<String?>(null)
    val insight: StateFlow<String?> = _insight

    fun completeSetup(salary: Double, balance: Double, name: String = "David") {
        viewModelScope.launch {
            userPreferences.saveSetupComplete(salary, balance, name)
        }
    }
    
    fun fetchInsights() {
        viewModelScope.launch {
            _insight.value = "Loading insights..."
            val txs = transactions.value
            if (txs.isEmpty()) {
                _insight.value = "No transactions yet. Add some to get insights!"
            } else {
                _insight.value = insightService.getFinancialInsights(txs)
            }
        }
    }

    fun addTransaction(type: TransactionType, amount: Double, category: String, account: String, description: String) {
        viewModelScope.launch {
            repository.insert(
                Transaction(
                    type = type,
                    amount = amount,
                    category = category,
                    account = account,
                    description = description
                )
            )
        }
    }

    private val aiChatService = AiChatService()

    private val _chatMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                sender = "AI",
                text = "Namaste! I am your AI Finance Assistant powered by Groq. You can chat with me to record income or expenses (e.g. 'Spent Rs 450 on lunch at Bakery'), delete specific wrong transactions (e.g. 'Delete transaction #12' or 'Remove my last transaction'), or ask any spending questions!"
            )
        )
    )
    val chatMessages: StateFlow<List<AiChatMessage>> = _chatMessages.asStateFlow()

    fun sendAiChatMessage(userText: String) {
        if (userText.isBlank()) return

        val userMessage = AiChatMessage(sender = "USER", text = userText.trim())
        val pendingAiMessage = AiChatMessage(sender = "AI", text = "Thinking...", isPending = true)

        _chatMessages.value = _chatMessages.value + userMessage + pendingAiMessage

        viewModelScope.launch {
            val history = _chatMessages.value
                .filter { !it.isPending }
                .takeLast(6)
                .map { Message(role = if (it.sender == "USER") "user" else "assistant", content = it.text) }

            val recentTxs = transactions.value
            val response = withContext(Dispatchers.IO) {
                aiChatService.processUserMessage(userText, history, recentTxs)
            }

            var actionSummary: String? = null
            var actionType: String? = null

            // Execute actions if returned by AI
            if (response.action == "ADD_TRANSACTION" && response.transactionData != null) {
                val data = response.transactionData
                val type = when (data.type?.uppercase()) {
                    "INCOME" -> TransactionType.INCOME
                    "SAVINGS" -> TransactionType.SAVINGS
                    "TRANSFER" -> TransactionType.TRANSFER
                    else -> TransactionType.EXPENSE
                }
                val amt = data.amount ?: 0.0
                val cat = if (!data.category.isNullOrBlank()) data.category else "General"
                val acc = if (!data.account.isNullOrBlank()) data.account else "Main Account"
                val desc = data.description ?: ""

                if (amt > 0) {
                    addTransaction(type, amt, cat, acc, desc)
                    actionSummary = "✓ Recorded ${type.name}: Rs. ${if (amt % 1.0 == 0.0) amt.toInt() else amt} ($cat)"
                    actionType = "ADD"
                }
            } else if (response.action == "DELETE_TRANSACTION" && response.deleteTransactionId != null) {
                val delId = response.deleteTransactionId
                val targetTx = recentTxs.find { it.id == delId }
                repository.deleteById(delId)

                val targetInfo = if (targetTx != null) {
                    "${targetTx.category} Rs. ${targetTx.amount}"
                } else "Transaction #$delId"

                actionSummary = "✓ Deleted $targetInfo"
                actionType = "DELETE"
            }

            val finalAiMessage = AiChatMessage(
                sender = "AI",
                text = response.reply,
                actionSummary = actionSummary,
                actionType = actionType,
                isPending = false
            )

            // Replace pending message with final response
            _chatMessages.value = _chatMessages.value.filter { !it.isPending } + finalAiMessage
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }
}
