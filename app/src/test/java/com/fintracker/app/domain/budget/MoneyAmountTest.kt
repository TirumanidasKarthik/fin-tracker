package com.fintracker.app.domain.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyAmountTest {
    @Test
    fun parsesGroupedRupeeAmounts() {
        assertEquals(123_400L, MoneyAmount.parseRupeesToPaise("₹1,234"))
    }

    @Test
    fun parsesPaiseWithoutFloatingPointRounding() {
        assertEquals(2_550L, MoneyAmount.parseRupeesToPaise("25.50"))
    }

    @Test
    fun rejectsNegativeAndMalformedMoney() {
        assertNull(MoneyAmount.parseRupeesToPaise("-12"))
        assertNull(MoneyAmount.parseRupeesToPaise("1.234"))
    }

    @Test
    fun parsesPercentIntoBasisPoints() {
        assertEquals(4_025, MoneyAmount.parsePercentToBasisPoints("40.25%"))
        assertEquals("40.5%", MoneyAmount.formatPercent(4_050))
        assertEquals("5%", MoneyAmount.formatPercent(500))
    }
}
