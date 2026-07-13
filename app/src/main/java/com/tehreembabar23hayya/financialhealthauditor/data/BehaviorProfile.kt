package com.tehreembabar23hayya.financialhealthauditor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "behavior_profiles")
data class BehaviorProfile(
    @PrimaryKey val merchant: String,
    val averageAmount: Double,
    val transactionCount: Int,
    val lastTransactionDate: Long
)
