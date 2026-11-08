package com.fintracker.app.presentation.budget

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.fintracker.app.domain.budget.BudgetBucket
import com.fintracker.app.domain.budget.BudgetLineItem
import com.fintracker.app.domain.budget.BudgetPlan
import com.fintracker.app.domain.budget.BankAllocator
import com.fintracker.app.domain.budget.BucketAllocator
import com.fintracker.app.domain.budget.FULL_ALLOCATION_BASIS_POINTS
import com.fintracker.app.domain.budget.IncomeSource
import com.fintracker.app.domain.budget.MoneyAmount
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min

class BudgetScreen(
    private val context: Context,
    private val plan: BudgetPlan,
    private val expandedBucketIds: MutableSet<String>,
    private val onCommand: (BudgetCommand) -> Unit,
) {
    private val palette = BudgetPalette
    private val targetAmounts = BucketAllocator.allocate(
        plan.monthlyIncomeInPaise,
        plan.buckets,
    ).buckets.associate { it.bucket.id to it.amountInPaise }

    fun createView(): View {
        val pageWidth = min(
            760,
            (context.resources.displayMetrics.widthPixels / context.resources.displayMetrics.density).toInt() - 32,
        ).coerceAtLeast(280)

        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(36))
        }
        page.addView(text("FIN TRACKER", 12f, palette.primary, Typeface.BOLD, letterSpacing = 0.12f))
        page.addView(
            text("Monthly budget", 30f, palette.ink, Typeface.BOLD),
            margins(top = 10),
        )
        page.addView(
            text("Review your income, targets, and planned amounts.", 15f, palette.muted),
            margins(top = 6),
        )
        page.addView(createIncomeCard(), margins(top = 22, width = LinearLayout.LayoutParams.MATCH_PARENT))
        page.addView(createSummaryCard(), margins(top = 14, width = LinearLayout.LayoutParams.MATCH_PARENT))
        page.addView(createBankAllocationCard(), margins(top = 14, width = LinearLayout.LayoutParams.MATCH_PARENT))

        val sectionHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        sectionHeader.addView(
            text("Buckets", 21f, palette.ink, Typeface.BOLD),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        sectionHeader.addView(actionButton("+ Add bucket") {
            showAddBucketDialog()
        })
        page.addView(sectionHeader, margins(top = 28, bottom = 10, width = LinearLayout.LayoutParams.MATCH_PARENT))

        plan.buckets.forEachIndexed { index, bucket ->
            page.addView(
                createBucketCard(bucket, index),
                margins(
                    top = if (index == 0) 0 else 12,
                    width = LinearLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }

        page.addView(
            text(
                "Your budget is saved on this phone only.",
                13f,
                palette.muted,
                gravity = Gravity.CENTER,
            ),
            margins(top = 20, width = LinearLayout.LayoutParams.MATCH_PARENT),
        )

        val scrollContent = FrameLayout(context)
        scrollContent.addView(
            page,
            FrameLayout.LayoutParams(dp(pageWidth), FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL),
        )
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            clipToPadding = false
            addView(scrollContent, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        }
        return FrameLayout(context).apply {
            setBackgroundColor(palette.background)
            addView(scroll, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
    }

    private fun createIncomeCard(): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = rounded(palette.primary, 24)
        }
        val topRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        topRow.addView(
            text("MONTHLY INCOME", 11f, palette.softMint, Typeface.BOLD, letterSpacing = 0.08f),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )
        topRow.addView(actionButton("+ Add source", filled = false) { showIncomeSourceDialog() })
        card.addView(topRow)
        card.addView(
            text(MoneyAmount.formatRupees(plan.monthlyIncomeInPaise), 34f, Color.WHITE, Typeface.BOLD),
            margins(top = 10),
        )
        if (plan.incomeSources.isEmpty()) {
            card.addView(text("Add an income source to get started.", 13f, palette.softMint), margins(top = 8))
        } else {
            plan.incomeSources.forEach { source ->
                val sourceRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    isClickable = true
                    isFocusable = true
                    contentDescription = "${source.name}, ${MoneyAmount.formatRupees(source.amountInPaise)}. Edit income source"
                    setOnClickListener { showIncomeSourceDialog(source) }
                }
                sourceRow.addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(text(source.name, 13f, palette.softMint))
                        bankName(source.bankId)?.let { addView(text(it, 11f, palette.softMint), margins(top = 2)) }
                    },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
                )
                sourceRow.addView(
                    text(MoneyAmount.formatRupees(source.amountInPaise), 13f, Color.WHITE, Typeface.BOLD, Gravity.END),
                )
                card.addView(sourceRow, margins(top = 8, width = LinearLayout.LayoutParams.MATCH_PARENT))
            }
        }
        card.addView(
            actionButton("Manage banks", filled = false) { showBankManagerDialog() },
            margins(top = 10),
        )
        return card
    }

    private fun createSummaryCard(): View {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(palette.surface, 20, palette.outline)
        }
        val summary = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val left = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        left.addView(text("Planned across buckets", 13f, palette.muted))
        left.addView(
            text(MoneyAmount.formatRupees(plan.plannedAmountInPaise), 23f, palette.ink, Typeface.BOLD),
            margins(top = 4),
        )
        summary.addView(left, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val ratio = plannedPercentOfIncome()
        summary.addView(
            text(ratio, 14f, if (plan.remainingInPaise < 0) palette.warning else palette.primary, Typeface.BOLD, Gravity.END),
        )
        card.addView(summary)

        val remainingText = when {
            plan.remainingInPaise < 0 -> "${MoneyAmount.formatRupees(-plan.remainingInPaise)} over income"
            plan.remainingInPaise > 0 -> "${MoneyAmount.formatRupees(plan.remainingInPaise)} left to assign"
            else -> "Income fully assigned"
        }
        val statusColor = if (plan.remainingInPaise < 0) palette.warningSurface else palette.successSurface
        val statusInk = if (plan.remainingInPaise < 0) palette.warning else palette.primary
        val status = text(remainingText, 14f, statusInk, Typeface.BOLD).apply {
            background = rounded(statusColor, 12)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        card.addView(status, margins(top = 12, width = LinearLayout.LayoutParams.MATCH_PARENT))
        val targetShare = plan.buckets.sumOf { it.shareBasisPoints }
        card.addView(
            text("Bucket targets total ${MoneyAmount.formatPercent(targetShare)}", 12f, palette.muted),
            margins(top = 10),
        )
        return card
    }

    private fun createBankAllocationCard(): View {
        val allocations = BankAllocator.summarize(plan.banks, plan.buckets)
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(palette.surface, 20, palette.outline)
        }
        card.addView(text("Bank allocation", 17f, palette.ink, Typeface.BOLD))
        card.addView(
            text("Planned amounts grouped by bank and bucket", 12f, palette.muted),
            margins(top = 3, bottom = 10),
        )

        if (allocations.isEmpty()) {
            card.addView(text("Add planned amounts to see what to keep in each bank.", 13f, palette.muted))
            return card
        }

        allocations.forEachIndexed { index, allocation ->
            if (index > 0) card.addView(divider(), margins(top = 9, bottom = 9))
            val bankRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            bankRow.addView(
                text(allocation.bankName, 15f, palette.primary, Typeface.BOLD),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
            )
            bankRow.addView(
                text(MoneyAmount.formatRupees(allocation.totalInPaise), 15f, palette.ink, Typeface.BOLD, Gravity.END),
            )
            card.addView(bankRow)

            allocation.bucketAmounts.forEach { amount ->
                val bucketRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                bucketRow.addView(
                    text(amount.bucketName, 12f, palette.muted),
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
                )
                bucketRow.addView(
                    text(MoneyAmount.formatRupees(amount.amountInPaise), 12f, palette.muted, gravity = Gravity.END),
                )
                card.addView(
                    bucketRow,
                    margins(top = 4, width = LinearLayout.LayoutParams.MATCH_PARENT),
                )
            }
        }
        return card
    }

    private fun createBucketCard(bucket: BudgetBucket, index: Int): View {
        val accent = palette.bucketAccents[index % palette.bucketAccents.size]
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(palette.surface, 20, palette.outline)
            clipToOutline = true
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(15), dp(14), dp(15))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                if (!expandedBucketIds.add(bucket.id)) expandedBucketIds.remove(bucket.id)
                card.getChildAt(1).visibility = if (bucket.id in expandedBucketIds) View.VISIBLE else View.GONE
            }
        }
        val initials = text(bucket.name.take(1).uppercase(), 15f, accent, Typeface.BOLD, Gravity.CENTER).apply {
            background = rounded(ColorUtils.withAlpha(accent, 28), 50)
            setPadding(dp(11), dp(11), dp(11), dp(11))
            minWidth = dp(40)
            minHeight = dp(40)
            contentDescription = "${bucket.name} bucket"
        }
        header.addView(initials, margins(end = 12))

        val bucketText = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        bucketText.addView(text(bucket.name, 16f, palette.ink, Typeface.BOLD))
        val targetAmount = targetAmounts[bucket.id] ?: 0L
        val targetLabel = text(
            "Target ${MoneyAmount.formatPercent(bucket.shareBasisPoints)} · ${MoneyAmount.formatRupees(targetAmount)}",
            12f,
            palette.primary,
            Typeface.BOLD,
        ).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { showTargetDialog(bucket) }
            contentDescription = "Edit ${bucket.name} target, ${MoneyAmount.formatPercent(bucket.shareBasisPoints)}"
        }
        bucketText.addView(targetLabel, margins(top = 5))
        header.addView(bucketText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val amountText = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        amountText.addView(
            text(MoneyAmount.formatRupees(bucket.plannedAmountInPaise), 16f, palette.ink, Typeface.BOLD, Gravity.END),
        )
        val targetDifference = bucket.plannedAmountInPaise - targetAmount
        val differenceLabel = when {
            targetDifference > 0 -> "${MoneyAmount.formatRupees(targetDifference)} over"
            targetDifference < 0 -> "${MoneyAmount.formatRupees(-targetDifference)} under"
            else -> "on target"
        }
        amountText.addView(
            text(
                differenceLabel,
                11f,
                if (targetDifference > 0) palette.warning else palette.muted,
                gravity = Gravity.END,
            ),
            margins(top = 4),
        )
        header.addView(amountText, margins(start = 8))
        card.addView(header)

        val details = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(4), dp(14), dp(14))
        }
        details.addView(
            divider(),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                bottomMargin = dp(8)
            },
        )
        if (bucket.lineItems.isEmpty()) {
            details.addView(text("No categories in this bucket yet.", 13f, palette.muted), margins(top = 4, bottom = 8))
        } else {
            bucket.lineItems.forEachIndexed { itemIndex, item ->
                details.addView(
                    createLineItemRow(item),
                    margins(
                        top = if (itemIndex == 0) 0 else 3,
                        width = LinearLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        }
        details.addView(
            actionButton("+ Add category", filled = false) { showAddLineItemDialog(bucket) },
            margins(top = 10),
        )
        details.visibility = if (bucket.id in expandedBucketIds) View.VISIBLE else View.GONE
        card.addView(details)
        return card
    }

    private fun createLineItemRow(item: BudgetLineItem): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(9), dp(4), dp(9))
            isClickable = true
            isFocusable = true
            contentDescription = "${item.label}, ${bankName(item.bankId) ?: "No bank selected"}, ${MoneyAmount.formatRupees(item.amountInPaise)}. Edit amount"
            setOnClickListener { showLineItemDialog(item) }
        }
        val labels = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(text(item.label, 14f, palette.ink, Typeface.NORMAL))
        bankName(item.bankId)?.let { labels.addView(text(it, 11f, palette.muted), margins(top = 3)) }
        row.addView(labels, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(
            text(MoneyAmount.formatRupees(item.amountInPaise), 14f, palette.ink, Typeface.BOLD, Gravity.END),
            margins(start = 8),
        )
        row.addView(text("›", 22f, palette.primary, Typeface.NORMAL, Gravity.CENTER), margins(start = 10))
        return row
    }

    private fun showIncomeSourceDialog(source: IncomeSource? = null) {
        val name = inputField("Source name", source?.name.orEmpty(), decimal = false)
        val amount = inputField("Monthly amount in rupees", source?.let { MoneyAmount.toInputRupees(it.amountInPaise) } ?: "0", decimal = true)
        val bank = bankSelector(source?.bankId)
        val error = errorText()
        val builder = AlertDialog.Builder(context)
            .setTitle(if (source == null) "Add income source" else "Edit income source")
            .setView(dialogContent(name, amount, bank.view, error))
            .setNegativeButton("Cancel", null)
            .setPositiveButton(if (source == null) "Add" else "Save", null)
        if (source != null) builder.setNeutralButton("Remove", null)
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val cleanedName = name.text.toString().trim()
                val amountInPaise = MoneyAmount.parseRupeesToPaise(amount.text.toString())
                when {
                    cleanedName.isBlank() -> error.text = "Enter a source name."
                    plan.incomeSources.any {
                        it.id != source?.id && it.name.equals(cleanedName, ignoreCase = true)
                    } -> error.text = "An income source with this name already exists."
                    amountInPaise == null -> error.text = "Enter a non-negative amount with up to two decimal places."
                    selectedBankId(bank) == null -> error.text = "Select a bank for this income source."
                    source == null -> {
                        dialog.dismiss()
                        onCommand(BudgetCommand.AddIncomeSource(cleanedName, amountInPaise, selectedBankId(bank)))
                    }
                    else -> {
                        dialog.dismiss()
                        onCommand(
                            BudgetCommand.UpdateIncomeSource(
                                source.id,
                                cleanedName,
                                amountInPaise,
                                selectedBankId(bank),
                            ),
                        )
                    }
                }
            }
            if (source != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    dialog.dismiss()
                    AlertDialog.Builder(context)
                        .setTitle("Remove ${source.name}?")
                        .setMessage("The total income and bucket targets will update after removal.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove") { _, _ ->
                            onCommand(BudgetCommand.DeleteIncomeSource(source.id))
                        }
                        .show()
                }
            }
        }
        dialog.show()
    }

    private fun showTargetDialog(bucket: BudgetBucket) {
        val field = inputField("Target percent", BigDecimal.valueOf(bucket.shareBasisPoints.toLong(), 2).stripTrailingZeros().toPlainString(), decimal = true)
        val error = errorText()
        val dialog = AlertDialog.Builder(context)
            .setTitle("${bucket.name} target")
            .setMessage("Set the share of income planned for this bucket.")
            .setView(dialogContent(field, error))
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
        showWithValidation(dialog) {
            val target = MoneyAmount.parsePercentToBasisPoints(field.text.toString())
            if (target == null) {
                error.text = "Enter a target from 0% to 100%."
                false
            } else if (plan.buckets.filterNot { it.id == bucket.id }.sumOf { it.shareBasisPoints } + target >
                FULL_ALLOCATION_BASIS_POINTS
            ) {
                error.text = "Bucket targets cannot add up to more than 100%."
                false
            } else {
                onCommand(BudgetCommand.UpdateTarget(bucket.id, target))
                true
            }
        }
    }

    private fun showLineItemDialog(item: BudgetLineItem) {
        val field = inputField("Amount in rupees", MoneyAmount.toInputRupees(item.amountInPaise), decimal = true)
        val bank = bankSelector(item.bankId)
        val error = errorText()
        val dialog = AlertDialog.Builder(context)
            .setTitle(item.label)
            .setMessage("Edit the planned amount and select the bank used for this category.")
            .setView(dialogContent(field, bank.view, error))
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Remove", null)
            .setPositiveButton("Save", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val amount = MoneyAmount.parseRupeesToPaise(field.text.toString())
                if (amount == null) error.text = "Enter a non-negative amount with up to two decimal places."
                else if (selectedBankId(bank) == null) error.text = "Select a bank for this category."
                else {
                    dialog.dismiss()
                    onCommand(BudgetCommand.UpdateLineItem(item.id, amount, selectedBankId(bank)))
                }
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                dialog.dismiss()
                AlertDialog.Builder(context)
                    .setTitle("Remove ${item.label}?")
                    .setMessage("This removes the category from your local budget.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Remove") { _, _ -> onCommand(BudgetCommand.DeleteLineItem(item.id)) }
                    .show()
            }
        }
        dialog.show()
    }

    private fun showAddLineItemDialog(bucket: BudgetBucket) {
        val label = inputField("Category name", "", decimal = false)
        val bank = bankSelector(null)
        val amount = inputField("Amount in rupees", "0", decimal = true)
        val error = errorText()
        val dialog = AlertDialog.Builder(context)
            .setTitle("Add to ${bucket.name}")
            .setView(dialogContent(label, bank.view, amount, error))
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add", null)
            .create()
        showWithValidation(dialog) {
            val parsedAmount = MoneyAmount.parseRupeesToPaise(amount.text.toString())
            when {
                label.text.toString().isBlank() -> {
                    error.text = "Enter a category name."
                    false
                }
                parsedAmount == null -> {
                    error.text = "Enter a non-negative amount with up to two decimal places."
                    false
                }
                selectedBankId(bank) == null -> {
                    error.text = "Select a bank for this category."
                    false
                }
                else -> {
                    onCommand(
                        BudgetCommand.AddLineItem(
                            bucketId = bucket.id,
                            label = label.text.toString(),
                            bankId = selectedBankId(bank),
                            amountInPaise = parsedAmount,
                        ),
                    )
                    true
                }
            }
        }
    }

    private fun showAddBankDialog() {
        val name = inputField("Bank name", "", decimal = false)
        val error = errorText()
        val dialog = AlertDialog.Builder(context)
            .setTitle("Add a bank")
            .setMessage("Bank names stay on this device and can be selected for income or categories.")
            .setView(dialogContent(name, error))
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add", null)
            .create()
        showWithValidation(dialog) {
            val cleanedName = name.text.toString().trim()
            when {
                cleanedName.isBlank() -> {
                    error.text = "Enter a bank name."
                    false
                }
                plan.banks.any { it.name.equals(cleanedName, ignoreCase = true) } -> {
                    error.text = "A bank with this name already exists."
                    false
                }
                else -> {
                    onCommand(BudgetCommand.AddBank(cleanedName))
                    true
                }
            }
        }
    }

    private fun showBankManagerDialog() {
        lateinit var manager: AlertDialog
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(4))
        }
        if (plan.banks.isEmpty()) {
            content.addView(text("No banks added yet.", 14f, palette.muted), margins(bottom = 8))
        } else {
            plan.banks.forEach { bank ->
                val usageCount = plan.incomeSources.count { it.bankId == bank.id } +
                    plan.buckets.flatMap { it.lineItems }.count { it.bankId == bank.id }
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                row.addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(text(bank.name, 15f, palette.ink))
                        if (usageCount > 0) {
                            addView(
                                text("Used by $usageCount budget item${if (usageCount == 1) "" else "s"}", 11f, palette.muted),
                                margins(top = 2),
                            )
                        }
                    },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
                )
                val remove = actionButton(if (usageCount > 0) "In use" else "Remove", filled = false) {
                    manager.dismiss()
                    AlertDialog.Builder(context)
                        .setTitle("Remove ${bank.name}?")
                        .setMessage("This bank is not assigned to any income source or category.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove") { _, _ -> onCommand(BudgetCommand.DeleteBank(bank.id)) }
                        .show()
                }
                if (usageCount > 0) {
                    remove.isEnabled = false
                    remove.isClickable = false
                    remove.isFocusable = false
                    remove.alpha = 0.55f
                }
                row.addView(remove)
                content.addView(row, margins(bottom = 6, width = LinearLayout.LayoutParams.MATCH_PARENT))
            }
        }
        content.addView(
            actionButton("+ Add bank") {
                manager.dismiss()
                showAddBankDialog()
            },
            margins(top = 8),
        )
        manager = AlertDialog.Builder(context)
            .setTitle("Manage banks")
            .setView(content)
            .setNegativeButton("Done", null)
            .create()
        manager.show()
    }

    private fun bankSelector(selectedBankId: String?): BankSelectionField {
        val options = listOf(BankOption(null, "No bank selected")) + plan.banks.map { BankOption(it.id, it.name) }
        val spinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, options).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(options.indexOfFirst { it.id == selectedBankId }.coerceAtLeast(0))
            minimumHeight = dp(48)
            contentDescription = "Select bank"
        }
        val field = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(palette.surface, 12, palette.outline)
            setPadding(dp(12), dp(8), dp(8), dp(4))
            addView(
                text("BANK ACCOUNT", 10f, palette.muted, Typeface.BOLD, letterSpacing = 0.08f),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
            addView(
                spinner,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
        }
        return BankSelectionField(field, spinner)
    }

    private fun selectedBankId(bank: BankSelectionField): String? =
        (bank.spinner.selectedItem as? BankOption)?.id

    private fun bankName(bankId: String?): String? = plan.banks.firstOrNull { it.id == bankId }?.name

    private data class BankOption(val id: String?, val name: String) {
        override fun toString(): String = name
    }

    private data class BankSelectionField(val view: View, val spinner: Spinner)

    private fun showAddBucketDialog() {
        val name = inputField("Bucket name", "", decimal = false)
        val target = inputField("Target percent", "0", decimal = true)
        val error = errorText()
        val dialog = AlertDialog.Builder(context)
            .setTitle("Add a bucket")
            .setView(dialogContent(name, target, error))
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add", null)
            .create()
        showWithValidation(dialog) {
            val basisPoints = MoneyAmount.parsePercentToBasisPoints(target.text.toString())
            val targetTotal = plan.buckets.sumOf { it.shareBasisPoints }
            when {
                name.text.toString().isBlank() -> {
                    error.text = "Enter a bucket name."
                    false
                }
                plan.buckets.any { it.name.equals(name.text.toString().trim(), ignoreCase = true) } -> {
                    error.text = "A bucket with this name already exists."
                    false
                }
                basisPoints == null -> {
                    error.text = "Enter a target from 0% to 100%."
                    false
                }
                targetTotal + basisPoints > FULL_ALLOCATION_BASIS_POINTS -> {
                    error.text = "Bucket targets cannot add up to more than 100%."
                    false
                }
                else -> {
                    onCommand(BudgetCommand.AddBucket(name.text.toString(), basisPoints))
                    true
                }
            }
        }
    }

    private fun showWithValidation(dialog: AlertDialog, validate: () -> Boolean) {
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (validate()) dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun inputField(hint: String, value: String, decimal: Boolean): EditText = EditText(context).apply {
        setSingleLine(true)
        setText(value)
        this.hint = hint
        textSize = 16f
        setTextColor(palette.ink)
        setHintTextColor(palette.muted)
        inputType = if (decimal) {
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        background = rounded(palette.surface, 12, palette.outline)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        if (value.isNotEmpty()) setSelection(text.length)
    }

    private fun errorText() = text("", 12f, palette.warning).apply {
        setPadding(dp(2), dp(4), dp(2), dp(0))
    }

    private fun dialogContent(vararg views: View): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(24), dp(8), dp(24), dp(4))
        views.forEachIndexed { index, view ->
            addView(
                view,
                margins(top = if (index == 0) 0 else 10, width = LinearLayout.LayoutParams.MATCH_PARENT),
            )
        }
    }

    private fun actionButton(label: String, filled: Boolean = true, action: () -> Unit): TextView =
        text(
            label,
            13f,
            if (filled) Color.WHITE else palette.primary,
            Typeface.BOLD,
            Gravity.CENTER,
        ).apply {
            setPadding(dp(13), dp(10), dp(13), dp(10))
            background = rounded(if (filled) palette.primary else palette.paleMint, 12)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

    private fun divider(): View = View(context).apply {
        setBackgroundColor(palette.outline)
    }

    private fun text(
        value: String,
        sizeSp: Float,
        color: Int,
        style: Int = Typeface.NORMAL,
        gravity: Int = Gravity.START,
        letterSpacing: Float = 0f,
    ) = TextView(context).apply {
        text = value
        textSize = sizeSp
        setTextColor(color)
        typeface = Typeface.create("sans-serif", style)
        this.gravity = gravity
        this.letterSpacing = letterSpacing
        includeFontPadding = false
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun rounded(fill: Int, radiusDp: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radiusDp).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun margins(
        top: Int = 0,
        bottom: Int = 0,
        start: Int = 0,
        end: Int = 0,
        width: Int = LinearLayout.LayoutParams.WRAP_CONTENT,
        height: Int = LinearLayout.LayoutParams.WRAP_CONTENT,
    ) = LinearLayout.LayoutParams(
        width,
        height,
    ).apply {
        topMargin = dp(top)
        bottomMargin = dp(bottom)
        marginStart = dp(start)
        marginEnd = dp(end)
    }

    private fun plannedPercentOfIncome(): String {
        if (plan.monthlyIncomeInPaise == 0L) return "0%"
        val percentage = BigDecimal.valueOf(plan.plannedAmountInPaise)
            .multiply(BigDecimal(100))
            .divide(BigDecimal.valueOf(plan.monthlyIncomeInPaise), 1, RoundingMode.HALF_UP)
            .stripTrailingZeros()
        return "${percentage.toPlainString()}% of income"
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private data object BudgetPalette {
        val background = Color.rgb(245, 244, 239)
        val surface = Color.WHITE
        val ink = Color.rgb(28, 48, 41)
        val primary = Color.rgb(39, 101, 83)
        val softMint = Color.rgb(222, 239, 229)
        val paleMint = Color.rgb(232, 243, 236)
        val muted = Color.rgb(102, 116, 109)
        val outline = Color.rgb(224, 229, 222)
        val warning = Color.rgb(156, 74, 35)
        val warningSurface = Color.rgb(255, 241, 228)
        val successSurface = Color.rgb(234, 245, 238)
        val bucketAccents = listOf(
            Color.rgb(40, 113, 91),
            Color.rgb(59, 111, 150),
            Color.rgb(112, 88, 151),
            Color.rgb(36, 126, 125),
            Color.rgb(180, 92, 66),
            Color.rgb(158, 117, 43),
        )
    }

    private object ColorUtils {
        fun withAlpha(color: Int, alpha: Int): Int = Color.argb(
            alpha,
            Color.red(color),
            Color.green(color),
            Color.blue(color),
        )
    }
}
