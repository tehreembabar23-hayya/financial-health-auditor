package com.tehreembabar23hayya.financialhealthauditor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val amount: Double,
    val date: Long,
    val category: String,
    val rawText: String? = null,
    val isIncome: Boolean = false,
    val source: String, // "manual" or "ocr"
    val isRecurring: Boolean = false,
    val isFlagged: Boolean = false,
    val issueType: String? = null,
    val reviewStatus: String? = "pending"
)
