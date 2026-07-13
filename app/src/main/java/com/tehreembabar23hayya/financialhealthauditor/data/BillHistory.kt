package com.tehreembabar23hayya.financialhealthauditor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bill_histories")
data class BillHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerName: String,
    val billDate: Long,
    val amount: Double,
    val previousReading: Double,
    val currentReading: Double,
    val averageAmount: Double? = null,
    val expectedAmount: Double? = null
)
