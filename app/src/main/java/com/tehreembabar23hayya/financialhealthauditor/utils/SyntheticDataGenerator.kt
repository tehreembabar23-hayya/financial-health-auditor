package com.tehreembabar23hayya.financialhealthauditor.utils

import com.tehreembabar23hayya.financialhealthauditor.data.Transaction
import com.tehreembabar23hayya.financialhealthauditor.data.TransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Generates ~4 weeks of realistic synthetic transactions and bulk-inserts them via
 * [TransactionDao]. All rows are tagged source="synthetic".
 *
 * Deliberately includes at least 2 pairs of duplicate/overlapping subscriptions so
 * the Duplicate Subscriptions module (Step 7) has real signals to detect.
 */
object SyntheticDataGenerator {

    private val NOW = System.currentTimeMillis()

    /** One day in millis */
    private fun daysAgo(n: Int): Long = NOW - TimeUnit.DAYS.toMillis(n.toLong())

    /** Scatter a timestamp within the same calendar day */
    private fun atTime(dayMillis: Long, hourOfDay: Int, minuteJitter: Int = 0): Long =
        dayMillis + TimeUnit.HOURS.toMillis(hourOfDay.toLong()) +
                TimeUnit.MINUTES.toMillis(minuteJitter.toLong())

    // -------------------------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------------------------

    private fun tx(
        merchant: String,
        amount: Double,
        date: Long,
        category: String,
        isIncome: Boolean = false,
        isRecurring: Boolean = false,
        isFlagged: Boolean = false,
        issueType: String? = null,
        rawText: String? = null
    ) = Transaction(
        merchant = merchant,
        amount = amount,
        date = date,
        category = category,
        rawText = rawText,
        isIncome = isIncome,
        source = "synthetic",
        isRecurring = isRecurring,
        isFlagged = isFlagged,
        issueType = issueType,
        reviewStatus = "pending"
    )

    // -------------------------------------------------------------------------
    // Public entry point
    // -------------------------------------------------------------------------

    /**
     * Inserts all synthetic transactions into [dao] on the IO dispatcher.
     * Safe to call from a coroutine scope (e.g., ViewModel.viewModelScope).
     */
    suspend fun generate(dao: TransactionDao) = withContext(Dispatchers.IO) {
        val transactions = buildList<Transaction> {

            // -----------------------------------------------------------------
            // WEEK 4 (28–22 days ago) – groceries, fuel, utilities
            // -----------------------------------------------------------------
            add(tx("Imtiaz Supermarket",  4250.0,  atTime(daysAgo(28), 11, 15), "Groceries"))
            add(tx("Shell Petrol Station", 3800.0, atTime(daysAgo(27),  8, 30), "Fuel"))
            add(tx("K-Electric",           2100.0, atTime(daysAgo(26), 14,  0), "Utilities", isRecurring = true))
            add(tx("Daraz.pk",             1349.0, atTime(daysAgo(25), 20, 45), "Shopping"))
            add(tx("KFC Pakistan",          850.0, atTime(daysAgo(24), 13, 20), "Dining"))
            add(tx("Careem",                450.0, atTime(daysAgo(23), 18,  5), "Transport"))
            add(tx("Salary Credit",       85000.0, atTime(daysAgo(23),  9,  0), "Income", isIncome = true))

            // -----------------------------------------------------------------
            // WEEK 3 (21–15 days ago) – dining, pharma, entertainment
            // -----------------------------------------------------------------
            add(tx("Imtiaz Supermarket",   3975.0, atTime(daysAgo(21), 10, 30), "Groceries"))
            add(tx("Gourmet Foods",          620.0, atTime(daysAgo(20), 13,  0), "Dining"))
            add(tx("Medlife Pharmacy",       980.0, atTime(daysAgo(19), 16, 20), "Health"))
            add(tx("Careem",                 390.0, atTime(daysAgo(19),  9, 10), "Transport"))
            add(tx("McDonald's Pakistan",    760.0, atTime(daysAgo(18), 12, 45), "Dining"))
            add(tx("Shell Petrol Station", 3500.0, atTime(daysAgo(17),  7, 50), "Fuel"))
            add(tx("Daraz.pk",             2100.0, atTime(daysAgo(16), 21, 15), "Shopping"))

            // -----------------------------------------------------------------
            // WEEK 2 (14–8 days ago) – subscriptions cluster + anomalies
            // -----------------------------------------------------------------
            add(tx("Imtiaz Supermarket",   4100.0, atTime(daysAgo(14), 11,  0), "Groceries"))
            add(tx("Uber Pakistan",          530.0, atTime(daysAgo(13), 17, 35), "Transport"))
            add(tx("Burger King",            680.0, atTime(daysAgo(12), 13,  5), "Dining"))
            add(tx("Utility Stores Corp",  1850.0, atTime(daysAgo(11),  9, 40), "Groceries"))
            add(tx("Jazz Cash Top-up",      500.0, atTime(daysAgo(10), 15, 20), "Telecom"))
            add(tx("Careem",                610.0, atTime(daysAgo( 9), 22,  0), "Transport"))

            // -----------------------------------------------------------------
            // DUPLICATE SUBSCRIPTION PAIR 1: Netflix billed twice in 5 days
            //   (once on day-13, again on day-8 — overlapping monthly cycle)
            // -----------------------------------------------------------------
            add(tx("Netflix",  1100.0, atTime(daysAgo(13), 10,  0), "Entertainment",
                isRecurring = true,
                rawText = "Netflix monthly subscription charge PKR 1100"))
            add(tx("Netflix",  1100.0, atTime(daysAgo( 8), 10, 30), "Entertainment",
                isRecurring = true,
                isFlagged  = false, // detector should flag this
                rawText    = "Netflix subscription renewal PKR 1100"))

            // -----------------------------------------------------------------
            // DUPLICATE SUBSCRIPTION PAIR 2: Two different Spotify accounts
            //   billed within the same month (household + personal)
            // -----------------------------------------------------------------
            add(tx("Spotify Premium",  500.0, atTime(daysAgo(12),  8,  0), "Entertainment",
                isRecurring = true,
                rawText = "Spotify Premium monthly Rs 500 debited"))
            add(tx("Spotify Premium",  500.0, atTime(daysAgo( 7),  8, 15), "Entertainment",
                isRecurring = true,
                rawText = "Spotify Premium subscription Rs 500"))

            // -----------------------------------------------------------------
            // DUPLICATE SUBSCRIPTION PAIR 3: Amazon Prime (PKR equivalent)
            //   billed from two linked cards on the same day
            // -----------------------------------------------------------------
            add(tx("Amazon Prime",  1200.0, atTime(daysAgo(10),  9,  0), "Entertainment",
                isRecurring = true,
                rawText = "Amazon Prime Video annual charge PKR 1200"))
            add(tx("Amazon Prime",  1200.0, atTime(daysAgo(10),  9, 55), "Entertainment",
                isRecurring = true,
                rawText = "Amazon Prime renewal PKR 1200 processed"))

            // -----------------------------------------------------------------
            // WEEK 1 (7–1 days ago) – mix of normal + a couple of suspicious
            // -----------------------------------------------------------------
            add(tx("Imtiaz Supermarket",   4550.0, atTime(daysAgo(7), 11, 30), "Groceries"))
            add(tx("Shell Petrol Station", 4000.0, atTime(daysAgo(6),  8,  0), "Fuel"))
            add(tx("Gourmet Foods",          550.0, atTime(daysAgo(5), 12, 50), "Dining"))
            add(tx("Medlife Pharmacy",       450.0, atTime(daysAgo(4), 15,  0), "Health"))
            add(tx("K-Electric",           2100.0, atTime(daysAgo(3), 14,  0), "Utilities", isRecurring = true))

            // Large odd-hour transfer — potential fraud signal
            add(tx("XQZW Holdings",       95000.0, atTime(daysAgo(3),  3, 14), "Unknown",
                isFlagged = false,
                rawText   = "Alert: PKR 95,000 debited at 03:14 AM sent to XQZW Holdings"))

            add(tx("Careem",                380.0, atTime(daysAgo(2), 19, 10), "Transport"))
            add(tx("Jazz Cash Top-up",      500.0, atTime(daysAgo(1), 10, 30), "Telecom"))

            // Salary freelance credit
            add(tx("Upwork Freelance",    15000.0, atTime(daysAgo(2),  9,  0), "Income", isIncome = true))
        }

        transactions.forEach { dao.insertTransaction(it) }
    }
}
