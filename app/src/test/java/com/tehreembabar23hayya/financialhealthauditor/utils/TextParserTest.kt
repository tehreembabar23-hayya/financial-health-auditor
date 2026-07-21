package com.tehreembabar23hayya.financialhealthauditor.utils

import org.junit.Assert.*
import org.junit.Test

class TextParserTest {

    private fun parse(text: String) = TextParser.parse(text)

    // ====================================================================
    // NORMAL CASES
    // ====================================================================

    @Test
    fun `Rs with space and comma amount parsed correctly`() {
        val result = parse("Your account has been debited Rs 2,450 sent to Noon Shopping.")
        assertEquals(2450.0, result.amount)
        assertEquals("Noon Shopping", result.merchant)
        assertEquals(true, result.isDebit)
    }

    @Test
    fun `Rs dot format without space`() {
        val result = parse("Rs.500.00 paid to EasyLoad at Jazz outlet.")
        assertEquals(500.0, result.amount)
        assertEquals(true, result.isDebit)
    }

    @Test
    fun `PKR uppercase with comma`() {
        val result = parse("PKR 5,000 received from Ahmad Ali in your account.")
        assertEquals(5000.0, result.amount)
        assertEquals(false, result.isDebit)
    }

    @Test
    fun `PKR no space`() {
        val result = parse("PKR2500 transferred to FoodPanda.")
        assertEquals(2500.0, result.amount)
        assertEquals(true, result.isDebit)
    }

    @Test
    fun `credit keyword sets isDebit false`() {
        val result = parse("Rs 1,200 credited to your Jazz Cash account.")
        assertEquals(1200.0, result.amount)
        assertEquals(false, result.isDebit)
    }

    @Test
    fun `sent to merchant extracted`() {
        val result = parse("Rs 750 sent to Karachi Biryani House on 20-Jul-2026.")
        assertEquals(750.0, result.amount)
        assertEquals("Karachi Biryani House", result.merchant)
        assertEquals(true, result.isDebit)
    }

    @Test
    fun `decimal amount preserved`() {
        val result = parse("PKR 3,299.99 debited for Daraz order. Merchant: Daraz.")
        assertEquals(3299.99, result.amount!!, 0.001)
        assertEquals(true, result.isDebit)
    }

    // ====================================================================
    // MISSING FIELDS
    // ====================================================================

    @Test
    fun `no amount returns null amount`() {
        val result = parse("Transaction successful. Sent to Utility Store.")
        assertNull(result.amount)
        assertEquals(true, result.isDebit)
    }

    @Test
    fun `no merchant returns null merchant`() {
        val result = parse("Rs 1,000 debited from your account.")
        assertEquals(1000.0, result.amount)
        assertNull(result.merchant)
        assertEquals(true, result.isDebit)
    }

    @Test
    fun `no debit or credit keyword returns null isDebit`() {
        val result = parse("Rs 500 processed for account 0300-1234567.")
        assertEquals(500.0, result.amount)
        assertNull(result.isDebit)
    }

    @Test
    fun `completely unrelated text returns all nulls`() {
        val result = parse("Please verify your OTP: 391842. Do not share it with anyone.")
        assertNull(result.amount)
        assertNull(result.merchant)
        assertNull(result.isDebit)
    }

    // ====================================================================
    // FRAUD-LIKE CASES  (large amount, odd timing, unfamiliar merchant)
    // ====================================================================

    @Test
    fun `fraud - very large amount at 3am unfamiliar merchant`() {
        // Odd-hour large transfer to a suspicious-looking entity
        val result = parse(
            "Alert: PKR 95,000 debited at 03:14 AM sent to XQZW Holdings via JazzCash."
        )
        assertEquals(95000.0, result.amount)
        // Merchant fragment captured
        assertNotNull(result.merchant)
        assertEquals(true, result.isDebit)
    }

    @Test
    fun `fraud - round large amount to unknown individual`() {
        val result = parse(
            "Rs 50,000 transferred to Ali Bhai at midnight. Ref# 00293847."
        )
        assertEquals(50000.0, result.amount)
        assertEquals(true, result.isDebit)
        assertNotNull(result.merchant)
    }

    @Test
    fun `fraud - repeated small debit pattern`() {
        // Simulates a string that might appear multiple times in a session; parser picks first match
        val result = parse(
            "Rs 1 sent to TestMerchant001. Rs 1 sent to TestMerchant001. Rs 1 sent to TestMerchant001."
        )
        // Amount should still be 1.0 (first match)
        assertEquals(1.0, result.amount)
        assertEquals(true, result.isDebit)
    }
}
