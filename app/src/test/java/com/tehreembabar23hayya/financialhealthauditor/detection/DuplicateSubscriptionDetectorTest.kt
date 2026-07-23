package com.tehreembabar23hayya.financialhealthauditor.detection

import com.tehreembabar23hayya.financialhealthauditor.data.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateSubscriptionDetectorTest {

    // Fixed reference date, ~120 days ago, so all test dates are in the past
    private val day = 24 * 60 * 60 * 1000L
    private val now = System.currentTimeMillis()

    private fun txn(
        id: Long,
        merchant: String,
        amount: Double,
        daysAgo: Long,
        isFlagged: Boolean = false,
        issueType: String? = null
    ) = Transaction(
        id = id,
        merchant = merchant,
        amount = amount,
        date = now - (daysAgo * day),
        category = "subscription",
        rawText = null,
        isIncome = false,
        source = "synthetic",
        isRecurring = false,
        isFlagged = isFlagged,
        issueType = issueType,
        reviewStatus = "pending"
    )

    @Test
    fun `flags most recent transaction when two overlapping subscriptions exist`() {
        val transactions = listOf(
            // Netflix: 3 monthly charges — recurring
            txn(1L, "Netflix", 1200.0, 90),
            txn(2L, "Netflix", 1200.0, 60),
            txn(3L, "Netflix", 1200.0, 30),
            // Spotify: 3 monthly charges — also recurring, overlapping period
            txn(4L, "Spotify", 850.0, 88),
            txn(5L, "Spotify", 850.0, 58),
            txn(6L, "Spotify", 850.0, 28),
            // A one-off grocery purchase — should never be flagged
            txn(7L, "Grocery Mart", 3400.0, 15)
        )

        val flagged = DuplicateSubscriptionDetector.findDuplicateFlags(transactions)

        assertEquals(2, flagged.size)
        assertTrue(flagged.all { it.issueType == "duplicate" && it.isFlagged })
        // Most recent transaction of each cluster should be the one flagged
        assertTrue(flagged.any { it.id == 3 }) // most recent Netflix
        assertTrue(flagged.any { it.id == 6 }) // most recent Spotify
    }

    @Test
    fun `does not flag a single recurring subscription with no overlap`() {
        val transactions = listOf(
            txn(1L, "Netflix", 1200.0, 90),
            txn(2L, "Netflix", 1200.0, 60),
            txn(3L, "Netflix", 1200.0, 30)
        )

        val flagged = DuplicateSubscriptionDetector.findDuplicateFlags(transactions)

        assertEquals(0, flagged.size)
    }

    @Test
    fun `does not flag irregular one-off purchases`() {
        val transactions = listOf(
            txn(1L, "Grocery Mart", 3400.0, 40),
            txn(2L, "Electronics Store", 15000.0, 20),
            txn(3L, "Restaurant", 900.0, 5)
        )

        val flagged = DuplicateSubscriptionDetector.findDuplicateFlags(transactions)

        assertEquals(0, flagged.size)
    }

    @Test
    fun `does not flag when amounts differ too much between occurrences`() {
        val transactions = listOf(
            txn(1L, "Netflix", 1200.0, 90),
            txn(2L, "Netflix", 1400.0, 60), // ~17% jump, exceeds 5% tolerance
            txn(3L, "Spotify", 850.0, 88),
            txn(4L, "Spotify", 950.0, 58)   // ~12% jump, exceeds tolerance
        )

        val flagged = DuplicateSubscriptionDetector.findDuplicateFlags(transactions)

        assertEquals(0, flagged.size)
    }
}