package com.fintracker.app.domain.budget

/** Pure domain logic: no Android dependencies, so it can be tested on the JVM. */
object BucketAllocator {
    fun allocate(incomeInPaise: Long, buckets: List<BudgetBucket>): IncomeAllocation {
        require(incomeInPaise >= 0) { "Income must not be negative." }
        require(buckets.map(BudgetBucket::id).distinct().size == buckets.size) {
            "Bucket ids must be unique."
        }

        val totalShare = buckets.sumOf { it.shareBasisPoints.toLong() }
        require(totalShare <= FULL_ALLOCATION_BASIS_POINTS) {
            "Bucket shares cannot add up to more than 100 percent."
        }

        val incomeWholeUnits = incomeInPaise / FULL_ALLOCATION_BASIS_POINTS
        val incomeRemainder = incomeInPaise % FULL_ALLOCATION_BASIS_POINTS
        val fractionalRemainders = LongArray(buckets.size)
        val amounts = buckets.mapIndexed { index, bucket ->
            val share = bucket.shareBasisPoints.toLong()
            val wholeAmount = incomeWholeUnits * share + (incomeRemainder * share) / FULL_ALLOCATION_BASIS_POINTS
            fractionalRemainders[index] = (incomeRemainder * share) % FULL_ALLOCATION_BASIS_POINTS
            wholeAmount
        }.toMutableList()

        if (totalShare == FULL_ALLOCATION_BASIS_POINTS.toLong()) {
            val remainingPaise = incomeInPaise - amounts.sum()
            val priority = buckets.indices.sortedWith(
                compareByDescending<Int> { fractionalRemainders[it] }.thenBy { it },
            )
            repeat(remainingPaise.toInt()) { amounts[priority[it]] += 1 }
        }

        val allocated = buckets.mapIndexed { index, bucket ->
            AllocatedBucket(bucket = bucket, amountInPaise = amounts[index])
        }
        val unallocated = incomeInPaise - amounts.sum()

        return IncomeAllocation(
            incomeInPaise = incomeInPaise,
            buckets = allocated,
            unallocatedInPaise = unallocated,
        )
    }
}

