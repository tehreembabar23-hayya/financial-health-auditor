package com.tehreembabar23hayya.financialhealthauditor.utils

import com.tehreembabar23hayya.financialhealthauditor.data.Transaction
import com.tehreembabar23hayya.financialhealthauditor.data.TransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Detects duplicate/overlapping subscription charges in the transaction list.
 *
 * Detection criteria — two transactions are considered duplicate when ALL of:
 *  1. Merchant names are "fuzzy-equal" (normalised names share ≥80 % token overlap,
 *     or one normalised name contains the other).
 *  2. Amounts are within 5 % of each other.
 *  3. The gap between their dates is ≤ [MONTHLY_WINDOW_DAYS] + [INTERVAL_JITTER_DAYS]
 *     (i.e. within a ~monthly cycle with ±3-day tolerance).
 *
 * When 2+ transactions form an overlapping cluster the **most-recent** one is
 * flagged: `isFlagged = true`, `issueType = "duplicate"`.
 *
 * Designed to run on the IO dispatcher; call from a coroutine scope.
 */
object DuplicateSubscriptionDetector {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** Approximate days in one billing month. */
    private const val MONTHLY_WINDOW_DAYS = 31

    /** Allowed jitter either side of the monthly window (±3 days). */
    private const val INTERVAL_JITTER_DAYS = 3

    /**
     * Maximum gap (in days) between two charges that are still considered
     * part of the same monthly cycle overlap.
     */
    private val MAX_GAP_DAYS: Int = MONTHLY_WINDOW_DAYS + INTERVAL_JITTER_DAYS // 34

    /** Amount tolerance: two charges must be within this fraction of each other. */
    private const val AMOUNT_TOLERANCE = 0.05

    /** Minimum Jaccard token-similarity to call two merchant names equivalent. */
    private const val MERCHANT_SIMILARITY_THRESHOLD = 0.8

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Reads all transactions from [dao], detects duplicate subscription clusters,
     * and persists updated flags via [dao] on the IO dispatcher.
     *
     * Returns the list of [Transaction] objects that were newly flagged (useful for
     * testing without a live database).
     */
    suspend fun detect(dao: TransactionDao): List<Transaction> = withContext(Dispatchers.IO) {
        val all = dao.getAllTransactions().first()
        val flagged = detectInMemory(all)
        flagged.forEach { dao.insertTransaction(it) }   // REPLACE strategy persists changes
        flagged
    }

    /**
     * Pure in-memory detection — no database access.
     * Exposed so unit tests can exercise the logic directly.
     *
     * @param transactions List of transactions to analyse.
     * @return Subset of transactions that should be flagged, with
     *         `isFlagged = true` and `issueType = "duplicate"` set.
     */
    fun detectInMemory(transactions: List<Transaction>): List<Transaction> {
        // Only consider expense (non-income) transactions
        val expenses = transactions.filter { !it.isIncome }

        // Track which transaction IDs have already been assigned to a cluster
        val clustered = mutableSetOf<Long>()
        val toFlag = mutableListOf<Transaction>()

        // Compare every pair — O(n²) is acceptable for the ~30-tx synthetic set
        for (i in expenses.indices) {
            val a = expenses[i]
            for (j in i + 1 until expenses.size) {
                val b = expenses[j]

                if (!merchantsMatch(a.merchant, b.merchant)) continue
                if (!amountsMatch(a.amount, b.amount)) continue
                if (!intervalIsMonthly(a.date, b.date)) continue

                // These two form a duplicate pair — build / extend the cluster
                val clusterMembers = mutableListOf(a, b)

                // Look for additional members already found in earlier iterations
                // (simple union: re-scan to collect all reachable via transitive pairs)
                expenses.forEach { tx ->
                    if (tx.id != a.id && tx.id != b.id &&
                        (merchantsMatch(tx.merchant, a.merchant) || merchantsMatch(tx.merchant, b.merchant)) &&
                        amountsMatch(tx.amount, a.amount) &&
                        (intervalIsMonthly(tx.date, a.date) || intervalIsMonthly(tx.date, b.date))
                    ) {
                        if (clusterMembers.none { it.id == tx.id }) clusterMembers.add(tx)
                    }
                }

                if (clusterMembers.size >= 2) {
                    // Flag the most-recent transaction in this cluster
                    val mostRecent = clusterMembers.maxByOrNull { it.date }!!
                    if (!clustered.contains(mostRecent.id)) {
                        clustered.add(mostRecent.id)
                        // Only flag if not already carrying a duplicate flag
                        if (!mostRecent.isFlagged || mostRecent.issueType != "duplicate") {
                            toFlag.add(
                                mostRecent.copy(
                                    isFlagged = true,
                                    issueType = "duplicate"
                                )
                            )
                        }
                    }
                }
            }
        }

        return toFlag
    }

    // -------------------------------------------------------------------------
    // Matching helpers
    // -------------------------------------------------------------------------

    /**
     * Fuzzy merchant name comparison.
     *
     * Strategy (in priority order):
     *  1. Exact match after normalisation.
     *  2. One normalised name is a substring of the other.
     *  3. Jaccard token similarity ≥ [MERCHANT_SIMILARITY_THRESHOLD].
     */
    internal fun merchantsMatch(a: String, b: String): Boolean {
        val na = normaliseMerchant(a)
        val nb = normaliseMerchant(b)
        if (na == nb) return true
        if (na.contains(nb) || nb.contains(na)) return true
        return jaccardSimilarity(na.split(" "), nb.split(" ")) >= MERCHANT_SIMILARITY_THRESHOLD
    }

    /**
     * Returns true when amounts are within [AMOUNT_TOLERANCE] (5 %) of each other.
     * Both amounts must be positive.
     */
    internal fun amountsMatch(a: Double, b: Double): Boolean {
        if (a <= 0 || b <= 0) return false
        val avg = (a + b) / 2.0
        return abs(a - b) / avg <= AMOUNT_TOLERANCE
    }

    /**
     * Returns true when the time difference between [dateA] and [dateB] (epoch ms)
     * is ≤ [MAX_GAP_DAYS] days, meaning both charges fall within one billing cycle
     * (monthly ±3 days).
     *
     * Note: A gap of 0 (same-day charge from two accounts) is also valid.
     */
    internal fun intervalIsMonthly(dateA: Long, dateB: Long): Boolean {
        val diffDays = TimeUnit.MILLISECONDS.toDays(abs(dateA - dateB))
        return diffDays <= MAX_GAP_DAYS
    }

    // -------------------------------------------------------------------------
    // Private utilities
    // -------------------------------------------------------------------------

    /**
     * Strips punctuation and filler words, lowercases, and trims whitespace so
     * name variants like "Netflix", "Netflix Monthly", "NETFLIX" all normalise
     * to the same token set.
     */
    private val FILLER_WORDS = setOf(
        "the", "a", "an", "pk", "pakistan", "monthly", "premium",
        "subscription", "charge", "ltd", "llc", "inc", "pvt", "co"
    )

    private fun normaliseMerchant(name: String): String =
        name.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")  // strip punctuation
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in FILLER_WORDS }
            .joinToString(" ")
            .trim()

    /**
     * Jaccard similarity between two token lists:
     *   |intersection| / |union|
     */
    private fun jaccardSimilarity(tokensA: List<String>, tokensB: List<String>): Double {
        val setA = tokensA.toSet()
        val setB = tokensB.toSet()
        val intersection = setA.intersect(setB).size
        val union = setA.union(setB).size
        return if (union == 0) 1.0 else intersection.toDouble() / union.toDouble()
    }
}
