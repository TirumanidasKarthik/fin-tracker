package com.fintracker.app.domain.budget

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetPlanTest {
    @Test
    fun preservesOverBudgetAmountAsNegativeRemainder() {
        val plan = BudgetPlan(
            incomeSources = listOf(
                IncomeSource("primary", "Primary", 60_000),
                IncomeSource("secondary", "Secondary", 20_000),
            ),
            buckets = listOf(
                BudgetBucket(
                    id = "expenses",
                    name = "Expenses",
                    shareBasisPoints = 10_000,
                    lineItems = listOf(
                        BudgetLineItem("housing", "Housing", null, 80_100),
                        BudgetLineItem("utilities", "Utilities", null, 1_200),
                    ),
                ),
            ),
        )

        assertEquals(81_300L, plan.plannedAmountInPaise)
        assertEquals(80_000L, plan.monthlyIncomeInPaise)
        assertEquals(-1_300L, plan.remainingInPaise)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTargetsAboveOneHundredPercent() {
        BudgetPlan(
            incomeSources = listOf(IncomeSource("salary", "Salary", 1_000)),
            buckets = listOf(
                BudgetBucket("a", "A", 6_000),
                BudgetBucket("b", "B", 4_001),
            ),
        )
    }

    @Test
    fun linksIncomeAndBudgetItemsToKnownBanks() {
        val plan = BudgetPlan(
            incomeSources = listOf(IncomeSource("income", "Salary", 1_000, "bank-a")),
            buckets = listOf(
                BudgetBucket(
                    "expenses",
                    "Expenses",
                    10_000,
                    listOf(BudgetLineItem("rent", "Housing", "bank-a", 1_000)),
                ),
            ),
            banks = listOf(Bank("bank-a", "Primary bank")),
        )

        assertEquals("bank-a", plan.incomeSources.single().bankId)
        assertEquals("bank-a", plan.buckets.single().lineItems.single().bankId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownBankReferences() {
        BudgetPlan(
            incomeSources = listOf(IncomeSource("income", "Salary", 1_000, "missing-bank")),
            buckets = emptyList(),
        )
    }
}
