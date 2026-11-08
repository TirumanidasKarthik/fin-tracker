package com.fintracker.app.domain.budget

interface BudgetRepository {
    fun loadPlan(): BudgetPlan
    fun addIncomeSource(name: String, amountInPaise: Long)
    fun updateIncomeSource(sourceId: String, name: String, amountInPaise: Long)
    fun deleteIncomeSource(sourceId: String)
    fun updateBucketTarget(bucketId: String, shareBasisPoints: Int)
    fun updateLineItemAmount(lineItemId: String, amountInPaise: Long)
    fun addBucket(name: String, shareBasisPoints: Int)
    fun addLineItem(bucketId: String, label: String, account: String, amountInPaise: Long)
    fun deleteLineItem(lineItemId: String)
}
