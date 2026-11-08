package com.fintracker.app.domain.budget

import org.junit.Assert.assertEquals
import org.junit.Test

class BankAllocatorTest {
    @Test
    fun groupsPlannedAmountsByBankAndBucketAndKeepsUnassignedAmountsVisible() {
        val buckets = listOf(
            BudgetBucket(
                id = "needs",
                name = "Needs",
                shareBasisPoints = 5_000,
                lineItems = listOf(
                    BudgetLineItem("rent", "Rent", "hdfc", 12_000),
                    BudgetLineItem("food", "Food", "kotak", 8_000),
                ),
            ),
            BudgetBucket(
                id = "goals",
                name = "Goals",
                shareBasisPoints = 5_000,
                lineItems = listOf(
                    BudgetLineItem("savings", "Savings", "hdfc", 20_000),
                    BudgetLineItem("other", "Other", null, 1_500),
                ),
            ),
        )

        val result = BankAllocator.summarize(
            banks = listOf(Bank("hdfc", "HDFC"), Bank("kotak", "KOTAK"), Bank("empty", "Empty")),
            buckets = buckets,
        )

        assertEquals(listOf("HDFC", "KOTAK", "Unassigned"), result.map { it.bankName })
        assertEquals(listOf(32_000L, 8_000L, 1_500L), result.map { it.totalInPaise })
        assertEquals(listOf("Needs", "Goals"), result.first().bucketAmounts.map { it.bucketName })
        assertEquals(41_500L, result.sumOf { it.totalInPaise })
    }

    @Test
    fun omitsBanksWithNoPlannedAmount() {
        val result = BankAllocator.summarize(
            banks = listOf(Bank("hdfc", "HDFC")),
            buckets = emptyList(),
        )

        assertEquals(emptyList<BankBucketAllocation>(), result)
    }
}
