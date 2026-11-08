package com.fintracker.app.domain.budget

data class BucketBankAmount(
    val bucketId: String,
    val bucketName: String,
    val amountInPaise: Long,
)

data class BankBucketAllocation(
    val bankId: String?,
    val bankName: String,
    val bucketAmounts: List<BucketBankAmount>,
) {
    val totalInPaise: Long
        get() = bucketAmounts.sumOf(BucketBankAmount::amountInPaise)
}

/** Summarizes planned bucket items by the bank assigned to each item. */
object BankAllocator {
    fun summarize(banks: List<Bank>, buckets: List<BudgetBucket>): List<BankBucketAllocation> {
        val allocations = banks.mapNotNull { bank ->
            allocationFor(bank.id, bank.name, buckets)
        }.toMutableList()

        allocationFor(bankId = null, bankName = "Unassigned", buckets = buckets)?.let(allocations::add)
        return allocations
    }

    private fun allocationFor(
        bankId: String?,
        bankName: String,
        buckets: List<BudgetBucket>,
    ): BankBucketAllocation? {
        val amounts = buckets.mapNotNull { bucket ->
            val amount = bucket.lineItems
                .filter { it.bankId == bankId }
                .sumOf(BudgetLineItem::amountInPaise)
            amount.takeIf { it > 0L }?.let { BucketBankAmount(bucket.id, bucket.name, it) }
        }
        return amounts.takeIf { it.isNotEmpty() }?.let {
            BankBucketAllocation(bankId, bankName, it)
        }
    }
}
