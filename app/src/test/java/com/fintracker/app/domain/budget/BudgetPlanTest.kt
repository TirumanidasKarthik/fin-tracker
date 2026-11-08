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
                        BudgetLineItem("housing", "Housing", "Account A", 80_100),
                        BudgetLineItem("utilities", "Utilities", "Account B", 1_200),
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
}
