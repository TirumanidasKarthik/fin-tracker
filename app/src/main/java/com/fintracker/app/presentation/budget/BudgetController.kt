package com.fintracker.app.presentation.budget

import com.fintracker.app.domain.budget.BudgetPlan
import com.fintracker.app.domain.budget.BudgetRepository

class BudgetController(private val repository: BudgetRepository) {
    fun loadPlan(): BudgetPlan = repository.loadPlan()

    fun execute(command: BudgetCommand): BudgetPlan {
        when (command) {
            is BudgetCommand.AddIncomeSource -> repository.addIncomeSource(command.name, command.amountInPaise)
            is BudgetCommand.UpdateIncomeSource -> repository.updateIncomeSource(
                command.sourceId,
                command.name,
                command.amountInPaise,
            )
            is BudgetCommand.DeleteIncomeSource -> repository.deleteIncomeSource(command.sourceId)
            is BudgetCommand.UpdateTarget -> repository.updateBucketTarget(
                command.bucketId,
                command.shareBasisPoints,
            )
            is BudgetCommand.UpdateLineItem -> repository.updateLineItemAmount(
                command.lineItemId,
                command.amountInPaise,
            )
            is BudgetCommand.AddBucket -> repository.addBucket(command.name, command.shareBasisPoints)
            is BudgetCommand.AddLineItem -> repository.addLineItem(
                command.bucketId,
                command.label,
                command.account,
                command.amountInPaise,
            )
            is BudgetCommand.DeleteLineItem -> repository.deleteLineItem(command.lineItemId)
        }
        return repository.loadPlan()
    }
}
