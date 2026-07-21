package com.tehreembabar23hayya.financialhealthauditor.utils

/**
 * Holds parsed fields extracted from raw transaction text.
 * All fields are nullable — a null value means the field could not be determined.
 *
 * @param amount    Numeric amount as a Double (e.g. 2450.0)
 * @param merchant  Merchant / recipient name as found in the text
 * @param isDebit   true = money went out, false = money came in, null = unknown
 */
data class ParsedTransaction(
    val amount: Double?,
    val merchant: String?,
    val isDebit: Boolean?
)

object TextParser {

    // -------------------------------------------------------------------
    // Amount patterns
    // Matches: "Rs 2,450", "Rs.500.00", "PKR 5,000", "PKR5000", "Rs2500"
    // -------------------------------------------------------------------
    private val AMOUNT_REGEX = Regex(
        """(?i)(?:PKR|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // -------------------------------------------------------------------
    // Merchant patterns
    // Captures the word(s) following "sent to", "to", or "at"
    // Stops at a sentence boundary, digit, or common trailing keywords.
    // -------------------------------------------------------------------
    private val MERCHANT_REGEX = Regex(
        """(?i)(?:sent\s+to|paid\s+to|transferred\s+to|payment\s+to|at|to)\s+([A-Za-z0-9&'.\-\s]{2,40?)""",
        RegexOption.IGNORE_CASE
    )

    // -------------------------------------------------------------------
    // Debit / Credit detection keyword sets
    // -------------------------------------------------------------------
    private val DEBIT_KEYWORDS = setOf(
        "sent", "debited", "paid", "payment", "transferred", "withdrawal", "deducted", "charged"
    )
    private val CREDIT_KEYWORDS = setOf(
        "received", "credited", "added", "deposited", "refunded", "incoming"
    )

    /**
     * Parses [rawText] and extracts [ParsedTransaction] fields via regex heuristics.
     */
    fun parse(rawText: String): ParsedTransaction {
        val amount   = extractAmount(rawText)
        val merchant = extractMerchant(rawText)
        val isDebit  = extractIsDebit(rawText)
        return ParsedTransaction(amount = amount, merchant = merchant, isDebit = isDebit)
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private fun extractAmount(text: String): Double? {
        val match = AMOUNT_REGEX.find(text) ?: return null
        // Strip commas, then parse
        val raw = match.groupValues[1].replace(",", "")
        return raw.toDoubleOrNull()
    }

    private fun extractMerchant(text: String): String? {
        val match = MERCHANT_REGEX.find(text) ?: return null
        // Trim trailing punctuation / noise and collapse inner whitespace
        val raw = match.groupValues[1]
            .trim()
            .trimEnd('.', ',', '!', '?', ';', ':')
            .replace(Regex("""\s{2,}"""), " ")
        // Discard if too short or looks like a number
        if (raw.length < 2 || raw.all { it.isDigit() || it == ',' || it == '.' }) return null
        return raw.ifBlank { null }
    }

    private fun extractIsDebit(text: String): Boolean? {
        val lower = text.lowercase()
        val words = lower.split(Regex("""\W+"""))
        val hasDebit  = words.any { it in DEBIT_KEYWORDS }
        val hasCredit = words.any { it in CREDIT_KEYWORDS }
        return when {
            hasDebit && !hasCredit  -> true
            hasCredit && !hasDebit  -> false
            hasDebit && hasCredit   -> true   // "sent" takes priority if both appear
            else                    -> null
        }
    }
}
