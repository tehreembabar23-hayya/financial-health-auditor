package com.tehreembabar23hayya.financialhealthauditor.utils

import com.tehreembabar23hayya.financialhealthauditor.data.BehaviorProfile
import com.tehreembabar23hayya.financialhealthauditor.data.BehaviorProfileDao
import com.tehreembabar23hayya.financialhealthauditor.data.TransactionDao
import kotlinx.coroutines.flow.first

object BehaviorProfileBuilder {

    /**
     * Rebuilds or updates BehaviorProfile records for all merchants
     * present in the Transaction table.
     */
    suspend fun rebuildAllProfiles(
        transactionDao: TransactionDao,
        behaviorProfileDao: BehaviorProfileDao
    ) {
        val allTransactions = transactionDao.getAllTransactions().first()
        val groupedByMerchant = allTransactions.groupBy { it.merchant }

        for ((merchant, transactions) in groupedByMerchant) {
            if (transactions.isEmpty()) continue
            val count = transactions.size
            val avgAmount = transactions.map { it.amount }.average()
            val maxDate = transactions.maxOf { it.date }

            val profile = BehaviorProfile(
                merchant = merchant,
                averageAmount = avgAmount,
                transactionCount = count,
                lastTransactionDate = maxDate
            )
            behaviorProfileDao.insertProfile(profile)
        }
    }

    /**
     * Updates or creates a BehaviorProfile record specifically for [merchant]
     * based on all current transactions for that merchant in the database.
     */
    suspend fun updateProfileForMerchant(
        merchant: String,
        transactionDao: TransactionDao,
        behaviorProfileDao: BehaviorProfileDao
    ) {
        val allTransactions = transactionDao.getAllTransactions().first()
        val merchantTransactions = allTransactions.filter { it.merchant.equals(merchant, ignoreCase = true) }

        if (merchantTransactions.isNotEmpty()) {
            val count = merchantTransactions.size
            val avgAmount = merchantTransactions.map { it.amount }.average()
            val maxDate = merchantTransactions.maxOf { it.date }

            val profile = BehaviorProfile(
                merchant = merchant,
                averageAmount = avgAmount,
                transactionCount = count,
                lastTransactionDate = maxDate
            )
            behaviorProfileDao.insertProfile(profile)
        }
    }
}
