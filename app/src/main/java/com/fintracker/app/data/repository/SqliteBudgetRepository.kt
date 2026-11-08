package com.fintracker.app.data.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.fintracker.app.data.local.BudgetDatabase
import com.fintracker.app.domain.budget.BudgetBucket
import com.fintracker.app.domain.budget.BudgetLineItem
import com.fintracker.app.domain.budget.BudgetPlan
import com.fintracker.app.domain.budget.BudgetRepository
import com.fintracker.app.domain.budget.Bank
import com.fintracker.app.domain.budget.IncomeSource
import com.fintracker.app.domain.budget.FULL_ALLOCATION_BASIS_POINTS
import java.util.UUID

class SqliteBudgetRepository(private val database: BudgetDatabase) : BudgetRepository {
    override fun loadPlan(): BudgetPlan {
        val db = database.readableDatabase
        val banks = db.query(
            BudgetDatabase.TABLE_BANK,
            arrayOf("id", "name"),
            null,
            null,
            null,
            null,
            "sort_order ASC, name COLLATE NOCASE ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(Bank(cursor.getString(0), cursor.getString(1)))
            }
        }
        val incomeSources = db.query(
            BudgetDatabase.TABLE_INCOME_SOURCE,
            arrayOf("id", "name", "amount_paise", "bank_id"),
            null,
            null,
            null,
            null,
            "sort_order ASC, name COLLATE NOCASE ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        IncomeSource(
                            id = cursor.getString(0),
                            name = cursor.getString(1),
                            amountInPaise = cursor.getLong(2),
                            bankId = cursor.getString(3),
                        ),
                    )
                }
            }
        }

        val buckets = mutableListOf<BudgetBucket>()
        db.query(
            BudgetDatabase.TABLE_BUCKET,
            arrayOf("id", "name", "target_share_basis_points"),
            null,
            null,
            null,
            null,
            "sort_order ASC, name COLLATE NOCASE ASC",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(0)
                buckets += BudgetBucket(
                    id = bucketId,
                    name = cursor.getString(1),
                    shareBasisPoints = cursor.getInt(2),
                    lineItems = loadLineItems(db, bucketId),
                )
            }
        }

        return BudgetPlan(incomeSources = incomeSources, buckets = buckets, banks = banks)
    }

    override fun addBank(name: String) {
        val cleanedName = name.trim()
        require(cleanedName.isNotEmpty()) { "Bank name is required." }
        require(loadPlan().banks.none { it.name.equals(cleanedName, ignoreCase = true) }) {
            "A bank with this name already exists."
        }
        val db = database.writableDatabase
        db.insertOrThrow(
            BudgetDatabase.TABLE_BANK,
            null,
            ContentValues().apply {
                put("id", UUID.randomUUID().toString())
                put("name", cleanedName)
                put("sort_order", nextSortOrder(db, BudgetDatabase.TABLE_BANK))
            },
        )
    }

    override fun deleteBank(bankId: String) {
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            val usageCount = db.rawQuery(
                "SELECT (SELECT COUNT(*) FROM ${BudgetDatabase.TABLE_INCOME_SOURCE} WHERE bank_id = ?) + " +
                    "(SELECT COUNT(*) FROM ${BudgetDatabase.TABLE_LINE_ITEM} WHERE bank_id = ?)",
                arrayOf(bankId, bankId),
            ).use { cursor ->
                check(cursor.moveToFirst()) { "Bank usage could not be checked." }
                cursor.getInt(0)
            }
            check(usageCount == 0) { "This bank is in use. Assign another bank before removing it." }
            val count = db.delete(BudgetDatabase.TABLE_BANK, "id = ?", arrayOf(bankId))
            check(count == 1) { "Bank was not found." }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun addIncomeSource(name: String, amountInPaise: Long, bankId: String?) {
        val cleanedName = validateIncomeSource(name, amountInPaise)
        validateBankId(bankId)
        require(loadPlan().incomeSources.none { it.name.equals(cleanedName, ignoreCase = true) }) {
            "An income source with this name already exists."
        }
        val db = database.writableDatabase
        db.insertOrThrow(
            BudgetDatabase.TABLE_INCOME_SOURCE,
            null,
            ContentValues().apply {
                put("id", UUID.randomUUID().toString())
                put("name", cleanedName)
                put("amount_paise", amountInPaise)
                if (bankId == null) putNull("bank_id") else put("bank_id", bankId)
                put("sort_order", nextSortOrder(db, BudgetDatabase.TABLE_INCOME_SOURCE))
            },
        )
    }

    override fun updateIncomeSource(sourceId: String, name: String, amountInPaise: Long, bankId: String?) {
        val cleanedName = validateIncomeSource(name, amountInPaise)
        validateBankId(bankId)
        val sources = loadPlan().incomeSources
        check(sources.any { it.id == sourceId }) { "Income source was not found." }
        require(sources.none { it.id != sourceId && it.name.equals(cleanedName, ignoreCase = true) }) {
            "An income source with this name already exists."
        }
        val count = database.writableDatabase.update(
            BudgetDatabase.TABLE_INCOME_SOURCE,
            ContentValues().apply {
                put("name", cleanedName)
                put("amount_paise", amountInPaise)
                if (bankId == null) putNull("bank_id") else put("bank_id", bankId)
            },
            "id = ?",
            arrayOf(sourceId),
        )
        check(count == 1) { "Income source could not be updated." }
    }

    override fun deleteIncomeSource(sourceId: String) {
        val count = database.writableDatabase.delete(
            BudgetDatabase.TABLE_INCOME_SOURCE,
            "id = ?",
            arrayOf(sourceId),
        )
        check(count == 1) { "Income source was not found." }
    }

    private fun validateIncomeSource(name: String, amountInPaise: Long): String {
        val cleanedName = name.trim()
        require(cleanedName.isNotEmpty()) { "Income source name is required." }
        require(amountInPaise >= 0) { "Income cannot be negative." }
        return cleanedName
    }

    private fun validateBankId(bankId: String?) {
        require(!bankId.isNullOrBlank()) { "Select a bank for this item." }
        check(loadPlan().banks.any { it.id == bankId }) { "The selected bank was not found." }
    }

    override fun updateBucketTarget(bucketId: String, shareBasisPoints: Int) {
        require(shareBasisPoints in 0..FULL_ALLOCATION_BASIS_POINTS) {
            "Target must be between 0% and 100%."
        }
        val plan = loadPlan()
        check(plan.buckets.any { it.id == bucketId }) { "Budget bucket was not found." }
        val otherTargets = plan.buckets.filterNot { it.id == bucketId }.sumOf { it.shareBasisPoints }
        require(otherTargets + shareBasisPoints <= FULL_ALLOCATION_BASIS_POINTS) {
            "Bucket targets cannot add up to more than 100%."
        }
        val count = database.writableDatabase.update(
            BudgetDatabase.TABLE_BUCKET,
            ContentValues().apply { put("target_share_basis_points", shareBasisPoints) },
            "id = ?",
            arrayOf(bucketId),
        )
        check(count == 1) { "Bucket target could not be updated." }
    }

    override fun updateLineItem(lineItemId: String, amountInPaise: Long, bankId: String?) {
        require(amountInPaise >= 0) { "Amount cannot be negative." }
        validateBankId(bankId)
        val count = database.writableDatabase.update(
            BudgetDatabase.TABLE_LINE_ITEM,
            ContentValues().apply {
                put("amount_paise", amountInPaise)
                if (bankId == null) putNull("bank_id") else put("bank_id", bankId)
            },
            "id = ?",
            arrayOf(lineItemId),
        )
        check(count == 1) { "Budget line item was not found." }
    }

    override fun addBucket(name: String, shareBasisPoints: Int) {
        val cleanedName = name.trim()
        require(cleanedName.isNotEmpty()) { "Bucket name is required." }
        require(shareBasisPoints in 0..FULL_ALLOCATION_BASIS_POINTS) {
            "Target must be between 0% and 100%."
        }
        val plan = loadPlan()
        require(plan.buckets.none { it.name.equals(cleanedName, ignoreCase = true) }) {
            "A bucket with this name already exists."
        }
        require(plan.buckets.sumOf { it.shareBasisPoints } + shareBasisPoints <=
            FULL_ALLOCATION_BASIS_POINTS
        ) {
            "Bucket targets cannot add up to more than 100%."
        }
        val db = database.writableDatabase
        db.insertOrThrow(
            BudgetDatabase.TABLE_BUCKET,
            null,
            ContentValues().apply {
                put("id", UUID.randomUUID().toString())
                put("name", cleanedName)
                put("target_share_basis_points", shareBasisPoints)
                put("sort_order", nextSortOrder(db, BudgetDatabase.TABLE_BUCKET))
            },
        )
    }

    override fun addLineItem(bucketId: String, label: String, bankId: String?, amountInPaise: Long) {
        val cleanedLabel = label.trim()
        require(cleanedLabel.isNotEmpty()) { "Category name is required." }
        require(amountInPaise >= 0) { "Amount cannot be negative." }
        validateBankId(bankId)
        check(loadPlan().buckets.any { it.id == bucketId }) { "Budget bucket was not found." }

        val db = database.writableDatabase
        db.insertOrThrow(
            BudgetDatabase.TABLE_LINE_ITEM,
            null,
            ContentValues().apply {
                put("id", UUID.randomUUID().toString())
                put("bucket_id", bucketId)
                put("label", cleanedLabel)
                put("account", "")
                if (bankId == null) putNull("bank_id") else put("bank_id", bankId)
                put("amount_paise", amountInPaise)
                put("sort_order", nextSortOrder(db, BudgetDatabase.TABLE_LINE_ITEM, "bucket_id = ?", arrayOf(bucketId)))
            },
        )
    }

    override fun deleteLineItem(lineItemId: String) {
        val count = database.writableDatabase.delete(
            BudgetDatabase.TABLE_LINE_ITEM,
            "id = ?",
            arrayOf(lineItemId),
        )
        check(count == 1) { "Budget line item was not found." }
    }

    private fun loadLineItems(db: SQLiteDatabase, bucketId: String): List<BudgetLineItem> =
        db.query(
            BudgetDatabase.TABLE_LINE_ITEM,
            arrayOf("id", "label", "bank_id", "amount_paise"),
            "bucket_id = ?",
            arrayOf(bucketId),
            null,
            null,
            "sort_order ASC, label COLLATE NOCASE ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        BudgetLineItem(
                            id = cursor.getString(0),
                            label = cursor.getString(1),
                            bankId = cursor.getString(2),
                            amountInPaise = cursor.getLong(3),
                        ),
                    )
                }
            }
        }

    private fun nextSortOrder(
        db: SQLiteDatabase,
        table: String,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
    ): Int = db.rawQuery(
        "SELECT COALESCE(MAX(sort_order), -1) + 1 FROM $table" +
            if (selection != null) " WHERE $selection" else "",
        selectionArgs,
    ).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getInt(0)
    }
}
