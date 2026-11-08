package com.fintracker.app.presentation.budget

import com.fintracker.app.domain.budget.BudgetPlan
import com.fintracker.app.domain.budget.BudgetRepository

class BudgetController(private val repository: BudgetRepository) {
    fun loadPlan(): BudgetPlan = repository.loadPlan()

    fun execute(command: BudgetCommand): BudgetPlan {
        when (command) {
            is BudgetCommand.AddBank -> repository.addBank(command.name)
            is BudgetCommand.DeleteBank -> repository.deleteBank(command.bankId)
            is BudgetCommand.AddIncomeSource -> repository.addIncomeSource(
                command.name,
                command.amountInPaise,
                command.bankId,
            )
            is BudgetCommand.UpdateIncomeSource -> repository.updateIncomeSource(
                command.sourceId,
                command.name,
                command.amountInPaise,
                command.bankId,
            )
            is BudgetCommand.DeleteIncomeSource -> repository.deleteIncomeSource(command.sourceId)
            is BudgetCommand.UpdateTarget -> repository.updateBucketTarget(
                command.bucketId,
                command.shareBasisPoints,
            )
            is BudgetCommand.UpdateLineItem -> repository.updateLineItem(
                command.lineItemId,
                command.amountInPaise,
                command.bankId,
            )
            is BudgetCommand.AddBucket -> repository.addBucket(command.name, command.shareBasisPoints)
            is BudgetCommand.AddLineItem -> repository.addLineItem(
                command.bucketId,
                command.label,
                command.bankId,
                command.amountInPaise,
            )
            is BudgetCommand.DeleteLineItem -> repository.deleteLineItem(command.lineItemId)
        }
        return repository.loadPlan()
    }
}
