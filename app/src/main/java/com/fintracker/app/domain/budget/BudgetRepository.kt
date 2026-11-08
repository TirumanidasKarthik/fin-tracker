package com.fintracker.app.domain.budget

interface BudgetRepository {
    fun loadPlan(): BudgetPlan
    fun addBank(name: String)
    fun deleteBank(bankId: String)
    fun addIncomeSource(name: String, amountInPaise: Long, bankId: String?)
    fun updateIncomeSource(sourceId: String, name: String, amountInPaise: Long, bankId: String?)
    fun deleteIncomeSource(sourceId: String)
    fun updateBucketTarget(bucketId: String, shareBasisPoints: Int)
    fun updateLineItem(lineItemId: String, amountInPaise: Long, bankId: String?)
    fun addBucket(name: String, shareBasisPoints: Int)
    fun deleteBucket(bucketId: String)
    fun addLineItem(bucketId: String, label: String, bankId: String?, amountInPaise: Long)
    fun deleteLineItem(lineItemId: String)
}
