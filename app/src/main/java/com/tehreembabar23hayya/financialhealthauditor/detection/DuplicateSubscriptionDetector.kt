package com.tehreembabar23hayya.financialhealthauditor.detection

import com.tehreembabar23hayya.financialhealthauditor.data.Transaction
import com.tehreembabar23hayya.financialhealthauditor.data.TransactionDao
import kotlin.math.abs

/**
 * Detects duplicate/overlapping recurring subscriptions.
 *
 * Split into a pure function (easy to unit test, no DB needed) and a thin
 * DB-wiring wrapper — see NOTE below on adjusting DAO method names.
 */
import kotlinx.coroutines.flow.first

object DuplicateSubscriptionDetector {


    private const val AMOUNT_TOLERANCE = 0.05 // 5%
    private const val INTERVAL_DAYS_TARGET = 30L
    private const val INTERVAL_TOLERANCE_DAYS = 3L
    private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

    /**
     * Pure logic: given all transactions, returns the ones that should be
     * flagged as duplicates (copies with isFlagged=true, issueType set).
     * No side effects, no DB access — easy to unit test directly.
     */
    fun findDuplicateFlags(transactions: List<Transaction>): List<Transaction> {
        val byMerchant = transactions.groupBy { normalizeMerchant(it.merchant) }

        val recurringClusters = byMerchant.values.mapNotNull { txns ->
            val cluster = findRecurringCluster(txns)
            if (cluster.size >= 2) cluster else null
        }

        // 2+ distinct recurring merchants active at once = likely duplicate/forgotten subscription
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

    /**
     * DB-wiring wrapper — call this once after synthetic data generation
     * (or after any batch of new transactions).
     *
     * NOTE: adjust `getAllTransactions()` and `update()` below if your
     * actual TransactionDao (built in Step 1) uses different method names
     * — I don't have visibility into exactly what Antigravity generated
     * for that file. If assembleDebug fails with "Unresolved reference"
     * on either call, open TransactionDao.kt, find the real method names,
     * and swap them in here.
     */
    suspend fun detectAndFlag(transactionDao: TransactionDao) {
        val all = transactionDao.getAllTransactions().first()
        val toFlag = findDuplicateFlags(all)
        toFlag.forEach { tx ->
            val byMerchant = all.groupBy { normalizeMerchant(it.merchant) }
            val cluster = byMerchant[normalizeMerchant(tx.merchant)]?.let { findRecurringCluster(it) } ?: emptyList()
            val context = android.app.ActivityThread.currentApplication()
            val explanation = ExplanationService.explainDuplicate(context, tx, cluster)
            val draftText = ResolutionService.generateDraft(context, tx, explanation)
            transactionDao.insertTransaction(tx.copy(explanation = explanation, draftText = draftText))
        }
    }
}