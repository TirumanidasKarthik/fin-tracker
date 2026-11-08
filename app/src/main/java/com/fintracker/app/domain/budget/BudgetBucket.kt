package com.fintracker.app.domain.budget

const val FULL_ALLOCATION_BASIS_POINTS = 10_000

/** A named share of monthly income. One basis point represents 0.01 percent. */
data class BudgetBucket(
    val id: String,
    val name: String,
    val shareBasisPoints: Int,
    val lineItems: List<BudgetLineItem> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "Bucket id must not be blank." }
        require(name.isNotBlank()) { "Bucket name must not be blank." }
        require(shareBasisPoints in 0..FULL_ALLOCATION_BASIS_POINTS) {
            "Bucket share must be between 0 and 10,000 basis points."
        }
        require(lineItems.map(BudgetLineItem::id).distinct().size == lineItems.size) {
            "Line item ids must be unique within a bucket."
        }
    }

    val plannedAmountInPaise: Long
        get() = lineItems.sumOf(BudgetLineItem::amountInPaise)
}

data class BudgetLineItem(
    val id: String,
    val label: String,
    val bankId: String?,
    val amountInPaise: Long,
) {
    init {
        require(id.isNotBlank()) { "Line item id must not be blank." }
        require(label.isNotBlank()) { "Line item label must not be blank." }
        require(amountInPaise >= 0) { "Line item amount must not be negative." }
    }
}

data class IncomeSource(
    val id: String,
    val name: String,
    val amountInPaise: Long,
    val bankId: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Income source id must not be blank." }
        require(name.isNotBlank()) { "Income source name must not be blank." }
        require(amountInPaise >= 0) { "Income source amount must not be negative." }
    }
}

data class Bank(
    val id: String,
    val name: String,
) {
    init {
        require(id.isNotBlank()) { "Bank id must not be blank." }
        require(name.isNotBlank()) { "Bank name must not be blank." }
    }
}

data class BudgetPlan(
    val incomeSources: List<IncomeSource>,
    val buckets: List<BudgetBucket>,
    val banks: List<Bank> = emptyList(),
) {
    init {
        require(banks.map(Bank::id).distinct().size == banks.size) { "Bank ids must be unique." }
        require(incomeSources.map(IncomeSource::id).distinct().size == incomeSources.size) {
            "Income source ids must be unique."
        }
        val bankIds = banks.map(Bank::id).toSet()
        require(incomeSources.all { it.bankId == null || it.bankId in bankIds }) {
            "Income sources must reference a bank in this budget."
        }
        require(buckets.flatMap { it.lineItems }.all { it.bankId == null || it.bankId in bankIds }) {
            "Budget items must reference a bank in this budget."
        }
        require(buckets.map(BudgetBucket::id).distinct().size == buckets.size) {
            "Bucket ids must be unique."
        }
        require(buckets.flatMap(BudgetBucket::lineItems).map(BudgetLineItem::id).distinct().size ==
            buckets.sumOf { it.lineItems.size }
        ) {
            "Line item ids must be unique across the budget."
        }
        require(buckets.sumOf { it.shareBasisPoints.toLong() } <= FULL_ALLOCATION_BASIS_POINTS) {
            "Bucket targets cannot add up to more than 100 percent."
        }
    }

    val monthlyIncomeInPaise: Long
        get() = incomeSources.sumOf(IncomeSource::amountInPaise)

    val plannedAmountInPaise: Long
        get() = buckets.sumOf(BudgetBucket::plannedAmountInPaise)

    val remainingInPaise: Long
        get() = monthlyIncomeInPaise - plannedAmountInPaise
}

data class AllocatedBucket(
    val bucket: BudgetBucket,
    val amountInPaise: Long,
)

data class IncomeAllocation(
    val incomeInPaise: Long,
    val buckets: List<AllocatedBucket>,
    val unallocatedInPaise: Long,
)

