package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
        val MONTHLY_SALARY = doublePreferencesKey("monthly_salary")
        val INITIAL_BALANCE = doublePreferencesKey("initial_balance")
        val CARD_NUMBER = androidx.datastore.preferences.core.stringPreferencesKey("card_number")
        val USER_NAME = androidx.datastore.preferences.core.stringPreferencesKey("user_name")
    }

    val isSetupComplete: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_SETUP_COMPLETE] ?: false
        }

    val userName: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME] ?: "David"
        }

    val monthlySalary: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[MONTHLY_SALARY] ?: 0.0
        }
        
    val initialBalance: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[INITIAL_BALANCE] ?: 0.0
        }

    val cardNumber: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[CARD_NUMBER] ?: ""
        }

    suspend fun saveCardNumber(number: String) {
        context.dataStore.edit { preferences ->
            preferences[CARD_NUMBER] = number
        }
    }

    suspend fun updateMonthlySalary(salary: Double) {
        context.dataStore.edit { preferences ->
            preferences[MONTHLY_SALARY] = salary
        }
    }

    suspend fun saveSetupComplete(salary: Double, balance: Double, name: String = "David") {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
            preferences[MONTHLY_SALARY] = salary
            preferences[INITIAL_BALANCE] = balance
            preferences[IS_SETUP_COMPLETE] = true
        }
    }
}
