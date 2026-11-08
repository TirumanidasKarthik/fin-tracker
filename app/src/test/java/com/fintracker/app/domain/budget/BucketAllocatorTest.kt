package com.fintracker.app.domain.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BucketAllocatorTest {
    @Test
    fun fullyAllocatedIncomeIsDistributedExactlyDownToThePaise() {
        val buckets = listOf(
            BudgetBucket("needs", "Needs", 3_333),
            BudgetBucket("goals", "Goals", 3_333),
            BudgetBucket("savings", "Savings", 3_334),
        )

        val result = BucketAllocator.allocate(incomeInPaise = 3, buckets = buckets)

        assertEquals(listOf(1L, 1L, 1L), result.buckets.map { it.amountInPaise })
        assertEquals(0L, result.unallocatedInPaise)
    }

    @Test
    fun partialAllocationKeepsTheRemainderUnallocated() {
        val result = BucketAllocator.allocate(
            incomeInPaise = 10_000,
            buckets = listOf(BudgetBucket("needs", "Needs", 8_000)),
        )

        assertEquals(8_000L, result.buckets.single().amountInPaise)
        assertEquals(2_000L, result.unallocatedInPaise)
    }

    @Test
    fun rejectsSharesAboveOneHundredPercent() {
        val buckets = listOf(
            BudgetBucket("needs", "Needs", 6_000),
            BudgetBucket("goals", "Goals", 4_001),
        )

        assertThrows(IllegalArgumentException::class.java) {
            BucketAllocator.allocate(10_000, buckets)
        }
    }

    @Test
    fun rejectsDuplicateBucketIds() {
        val buckets = listOf(
            BudgetBucket("same", "Needs", 4_000),
            BudgetBucket("same", "Goals", 2_000),
        )

        assertThrows(IllegalArgumentException::class.java) {
            BucketAllocator.allocate(10_000, buckets)
        }
    }

    @Test
    fun rejectsNegativeIncome() {
        assertThrows(IllegalArgumentException::class.java) {
            BucketAllocator.allocate(-1, emptyList())
        }
    }
}

