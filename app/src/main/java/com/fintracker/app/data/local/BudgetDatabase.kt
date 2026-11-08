package com.fintracker.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BudgetDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE budget_settings (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                monthly_income_paise INTEGER NOT NULL CHECK (monthly_income_paise >= 0)
            )""".trimIndent(),
        )
        db.execSQL(
            """CREATE TABLE budget_bucket (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                target_share_basis_points INTEGER NOT NULL CHECK (target_share_basis_points BETWEEN 0 AND 10000),
                sort_order INTEGER NOT NULL
            )""".trimIndent(),
        )
        createIncomeSourceTable(db)
        db.execSQL(
            """CREATE TABLE budget_line_item (
                id TEXT PRIMARY KEY NOT NULL,
                bucket_id TEXT NOT NULL REFERENCES budget_bucket(id) ON DELETE CASCADE,
                label TEXT NOT NULL,
                account TEXT NOT NULL DEFAULT '',
                amount_paise INTEGER NOT NULL CHECK (amount_paise >= 0),
                sort_order INTEGER NOT NULL
            )""".trimIndent(),
        )
        db.execSQL("CREATE INDEX index_budget_line_item_bucket ON budget_line_item(bucket_id, sort_order)")
        seedSampleBudget(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createIncomeSourceTable(db)
            db.execSQL(
                """INSERT INTO $TABLE_INCOME_SOURCE (id, name, amount_paise, sort_order)
                   SELECT 'income-salary', 'Salary', monthly_income_paise, 0
                   FROM $TABLE_SETTINGS WHERE id = $SETTINGS_ID""".trimIndent(),
            )
        }
    }

    private fun createIncomeSourceTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE_INCOME_SOURCE (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                amount_paise INTEGER NOT NULL CHECK (amount_paise >= 0),
                sort_order INTEGER NOT NULL
            )""".trimIndent(),
        )
    }

    private fun seedSampleBudget(db: SQLiteDatabase) {
        val buckets = listOf(
            BucketSeed("expenses", "Expenses", 5_000, listOf(
                LineSeed("expense-rent", "Housing", "Account A", 1_000_000),
                LineSeed("expense-groceries", "Groceries", "Account A", 500_000),
                LineSeed("expense-utilities", "Utilities", "Account B", 250_000),
                LineSeed("expense-transport", "Transport", "Account B", 250_000),
                LineSeed("expense-subscriptions", "Subscriptions", "Account A", 100_000),
                LineSeed("expense-household", "Household", "Account B", 400_000),
            )),
            BucketSeed("emergency", "Emergency Fund", 1_000, listOf(
                LineSeed("emergency-savings", "Emergency savings", "Account A", 500_000),
            )),
            BucketSeed("investments", "Investments", 1_500, listOf(
                LineSeed("investment-index-fund", "Index fund", "Account A", 300_000),
                LineSeed("investment-growth-fund", "Growth fund", "Account A", 200_000),
                LineSeed("investment-bonds", "Bonds", "Account B", 150_000),
                LineSeed("investment-other", "Other investments", "Account B", 100_000),
            )),
            BucketSeed("goals", "Goals", 1_500, listOf(
                LineSeed("goal-home", "Home goal", "Account A", 500_000),
                LineSeed("goal-travel", "Travel goal", "Account B", 250_000),
            )),
            BucketSeed("guilt-free", "Guilt free", 500, listOf(
                LineSeed("guilt-free-main", "Personal spending", "Account B", 250_000),
            )),
            BucketSeed("generic-savings", "Generic Savings", 500, listOf(
                LineSeed("generic-savings-main", "General savings", "Account A", 250_000),
            )),
        )

        db.beginTransaction()
        try {
            db.insertOrThrow(
                TABLE_SETTINGS,
                null,
                ContentValues().apply {
                    put("id", SETTINGS_ID)
                    put("monthly_income_paise", SAMPLE_MONTHLY_INCOME_PAISE)
                },
            )
            db.insertOrThrow(
                TABLE_INCOME_SOURCE,
                null,
                ContentValues().apply {
                    put("id", "income-salary")
                    put("name", "Main income")
                    put("amount_paise", SAMPLE_MONTHLY_INCOME_PAISE)
                    put("sort_order", 0)
                },
            )
            buckets.forEachIndexed { bucketIndex, bucket ->
                db.insertOrThrow(
                    TABLE_BUCKET,
                    null,
                    ContentValues().apply {
                        put("id", bucket.id)
                        put("name", bucket.name)
                        put("target_share_basis_points", bucket.targetShareBasisPoints)
                        put("sort_order", bucketIndex)
                    },
                )
                bucket.lines.forEachIndexed { lineIndex, line ->
                    db.insertOrThrow(
                        TABLE_LINE_ITEM,
                        null,
                        ContentValues().apply {
                            put("id", line.id)
                            put("bucket_id", bucket.id)
                            put("label", line.label)
                            put("account", line.account)
                            put("amount_paise", line.amountInPaise)
                            put("sort_order", lineIndex)
                        },
                    )
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private data class BucketSeed(
        val id: String,
        val name: String,
        val targetShareBasisPoints: Int,
        val lines: List<LineSeed>,
    )

    private data class LineSeed(
        val id: String,
        val label: String,
        val account: String,
        val amountInPaise: Long,
    )

    companion object {
        const val TABLE_SETTINGS = "budget_settings"
        const val TABLE_INCOME_SOURCE = "budget_income_source"
        const val TABLE_BUCKET = "budget_bucket"
        const val TABLE_LINE_ITEM = "budget_line_item"
        const val SETTINGS_ID = 1
        const val SAMPLE_MONTHLY_INCOME_PAISE = 5_000_000L
        private const val DATABASE_NAME = "fin_tracker_budget.db"
        private const val DATABASE_VERSION = 2
    }
}
