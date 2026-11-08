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

    @Test
    fun formatsCurrentShareOfIncomeToTwoDecimalPlaces() {
        assertEquals("40%", MoneyAmount.formatPercentOf(40_000, 100_000))
        assertEquals("12.35%", MoneyAmount.formatPercentOf(123_456, 1_000_000))
        assertEquals("0%", MoneyAmount.formatPercentOf(1, 0))
    }
}
