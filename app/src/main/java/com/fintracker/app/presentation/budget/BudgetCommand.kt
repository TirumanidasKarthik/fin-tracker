package com.fintracker.app.presentation.budget

sealed interface BudgetCommand {
    data class AddIncomeSource(val name: String, val amountInPaise: Long) : BudgetCommand
    data class UpdateIncomeSource(
        val sourceId: String,
        val name: String,
        val amountInPaise: Long,
    ) : BudgetCommand
    data class DeleteIncomeSource(val sourceId: String) : BudgetCommand
    data class UpdateTarget(val bucketId: String, val shareBasisPoints: Int) : BudgetCommand
    data class UpdateLineItem(val lineItemId: String, val amountInPaise: Long) : BudgetCommand
    data class AddBucket(val name: String, val shareBasisPoints: Int) : BudgetCommand
    data class AddLineItem(
        val bucketId: String,
        val label: String,
        val account: String,
        val amountInPaise: Long,
    ) : BudgetCommand
    data class DeleteLineItem(val lineItemId: String) : BudgetCommand
}
