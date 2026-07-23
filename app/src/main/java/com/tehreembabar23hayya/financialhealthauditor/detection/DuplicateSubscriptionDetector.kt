package com.tehreembabar23hayya.financialhealthauditor.detection

import android.content.Context
import com.tehreembabar23hayya.financialhealthauditor.data.Transaction
import com.tehreembabar23hayya.financialhealthauditor.data.TransactionDao
import kotlinx.coroutines.flow.first
import kotlin.math.abs

object DuplicateSubscriptionDetector {

    private const val AMOUNT_TOLERANCE = 0.05
    private const val INTERVAL_DAYS_TARGET = 30L
    private const val INTERVAL_TOLERANCE_DAYS = 3L
    private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

    fun findDuplicateFlags(transactions: List<Transaction>): List<Transaction> {
        val byMerchant = transactions.groupBy { normalizeMerchant(it.merchant) }

        val recurringClusters = byMerchant.values.mapNotNull { txns ->
            val cluster = findRecurringCluster(txns)
            if (cluster.size >= 2) cluster else null
        }

        if (recurringClusters.size < 2) return emptyList()

        return recurringClusters.mapNotNull { cluster ->
            cluster.maxByOrNull { it.date }?.copy(
                isFlagged = true,
                issueType = "duplicate"
            )
        }
    }

    private fun normalizeMerchant(name: String): String =
        name.trim().lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun findRecurringCluster(transactions: List<Transaction>): List<Transaction> {
        val sorted = transactions.sortedBy { it.date }
        if (sorted.size < 2) return emptyList()

        val cluster = mutableListOf(sorted.first())
        for (i in 1 until sorted.size) {
            val prev = cluster.last()
            val current = sorted[i]
            val daysBetween = (current.date - prev.date) / MILLIS_PER_DAY
            val amountDiff = abs(current.amount - prev.amount) / prev.amount

            val isRegularInterval = abs(daysBetween - INTERVAL_DAYS_TARGET) <= INTERVAL_TOLERANCE_DAYS
            val isSimilarAmount = amountDiff <= AMOUNT_TOLERANCE

            if (isRegularInterval && isSimilarAmount) {
                cluster.add(current)
            }
        }
        return cluster
    }

    suspend fun detectAndFlag(transactionDao: TransactionDao, context: Context) {
        val all = transactionDao.getAllTransactions().first()
        val toFlag = findDuplicateFlags(all)

        toFlag.forEach { tx ->
            val byMerchant = all.groupBy { normalizeMerchant(it.merchant) }
            val cluster = byMerchant[normalizeMerchant(tx.merchant)]
                ?.let { findRecurringCluster(it) } ?: emptyList()

            val explanation = ExplanationService.explainDuplicate(context, tx, cluster)
            val draftText = ResolutionService.generateDraft(context, tx, explanation)

            transactionDao.insertTransaction(
                tx.copy(explanation = explanation, draftText = draftText)
            )
        }
    }
}