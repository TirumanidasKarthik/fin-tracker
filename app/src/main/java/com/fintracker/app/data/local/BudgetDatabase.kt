package com.fintracker.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

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
        createBankTable(db)
        createIncomeSourceTable(db, includeBankColumn = true)
        db.execSQL(
            """CREATE TABLE budget_line_item (
                id TEXT PRIMARY KEY NOT NULL,
                bucket_id TEXT NOT NULL REFERENCES budget_bucket(id) ON DELETE CASCADE,
                label TEXT NOT NULL,
                account TEXT NOT NULL DEFAULT '',
                bank_id TEXT REFERENCES $TABLE_BANK(id) ON DELETE SET NULL,
                amount_paise INTEGER NOT NULL CHECK (amount_paise >= 0),
                sort_order INTEGER NOT NULL
            )""".trimIndent(),
        )
        db.execSQL("CREATE INDEX index_budget_line_item_bucket ON budget_line_item(bucket_id, sort_order)")
        seedSampleBudget(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createIncomeSourceTable(db, includeBankColumn = false)
            db.execSQL(
                """INSERT INTO $TABLE_INCOME_SOURCE (id, name, amount_paise, sort_order)
                   SELECT 'income-salary', 'Salary', monthly_income_paise, 0
                   FROM $TABLE_SETTINGS WHERE id = $SETTINGS_ID""".trimIndent(),
            )
        }
        if (oldVersion < 3) {
            createBankTable(db)
            db.execSQL("ALTER TABLE $TABLE_INCOME_SOURCE ADD COLUMN bank_id TEXT REFERENCES $TABLE_BANK(id) ON DELETE SET NULL")
            db.execSQL("ALTER TABLE $TABLE_LINE_ITEM ADD COLUMN bank_id TEXT REFERENCES $TABLE_BANK(id) ON DELETE SET NULL")
            migrateLegacyAccountsToBanks(db)
        }
    }

    private fun createIncomeSourceTable(db: SQLiteDatabase, includeBankColumn: Boolean) {
        val bankColumn = if (includeBankColumn) "bank_id TEXT REFERENCES $TABLE_BANK(id) ON DELETE SET NULL," else ""
        db.execSQL(
            """CREATE TABLE $TABLE_INCOME_SOURCE (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                amount_paise INTEGER NOT NULL CHECK (amount_paise >= 0),
                $bankColumn
                sort_order INTEGER NOT NULL
            )""".trimIndent(),
        )
    }

    private fun createBankTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE_BANK (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                sort_order INTEGER NOT NULL
            )""".trimIndent(),
        )
    }

    private fun migrateLegacyAccountsToBanks(db: SQLiteDatabase) {
        val accounts = db.rawQuery(
            "SELECT DISTINCT TRIM(account) FROM $TABLE_LINE_ITEM WHERE TRIM(account) <> '' ORDER BY account COLLATE NOCASE",
            null,
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }
        accounts.forEachIndexed { index, name ->
            val bankId = "legacy-bank-${UUID.randomUUID()}"
            db.insertOrThrow(
                TABLE_BANK,
                null,
                ContentValues().apply {
                    put("id", bankId)
                    put("name", name)
                    put("sort_order", index)
                },
            )
            db.update(
                TABLE_LINE_ITEM,
                ContentValues().apply { put("bank_id", bankId) },
                "TRIM(account) = ?",
                arrayOf(name),
            )
        }
    }

    private fun seedSampleBudget(db: SQLiteDatabase) {
        val buckets = listOf(
            BucketSeed("expenses", "Expenses", 5_000, listOf(
                LineSeed("expense-rent", "Housing", "bank-primary", 1_000_000),
                LineSeed("expense-groceries", "Groceries", "bank-primary", 500_000),
                LineSeed("expense-utilities", "Utilities", "bank-savings", 250_000),
                LineSeed("expense-transport", "Transport", "bank-savings", 250_000),
                LineSeed("expense-subscriptions", "Subscriptions", "bank-primary", 100_000),
                LineSeed("expense-household", "Household", "bank-savings", 400_000),
            )),
            BucketSeed("emergency", "Emergency Fund", 1_000, listOf(
                LineSeed("emergency-savings", "Emergency savings", "bank-primary", 500_000),
            )),
            BucketSeed("investments", "Investments", 1_500, listOf(
                LineSeed("investment-index-fund", "Index fund", "bank-primary", 300_000),
                LineSeed("investment-growth-fund", "Growth fund", "bank-primary", 200_000),
                LineSeed("investment-bonds", "Bonds", "bank-savings", 150_000),
                LineSeed("investment-other", "Other investments", "bank-savings", 100_000),
            )),
            BucketSeed("goals", "Goals", 1_500, listOf(
                LineSeed("goal-home", "Home goal", "bank-primary", 500_000),
                LineSeed("goal-travel", "Travel goal", "bank-savings", 250_000),
            )),
            BucketSeed("guilt-free", "Guilt free", 500, listOf(
                LineSeed("guilt-free-main", "Personal spending", "bank-savings", 250_000),
            )),
            BucketSeed("generic-savings", "Generic Savings", 500, listOf(
                LineSeed("generic-savings-main", "General savings", "bank-primary", 250_000),
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
                TABLE_BANK,
                null,
                ContentValues().apply {
                    put("id", "bank-primary")
                    put("name", "Primary bank")
                    put("sort_order", 0)
                },
            )
            db.insertOrThrow(
                TABLE_BANK,
                null,
                ContentValues().apply {
                    put("id", "bank-savings")
                    put("name", "Savings bank")
                    put("sort_order", 1)
                },
            )
            db.insertOrThrow(
                TABLE_INCOME_SOURCE,
                null,
                ContentValues().apply {
                    put("id", "income-salary")
                    put("name", "Main income")
                    put("amount_paise", SAMPLE_MONTHLY_INCOME_PAISE)
                    put("bank_id", "bank-primary")
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
                            put("account", "")
                            put("bank_id", line.bankId)
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
        val bankId: String,
        val amountInPaise: Long,
    )

    companion object {
        const val TABLE_SETTINGS = "budget_settings"
        const val TABLE_INCOME_SOURCE = "budget_income_source"
        const val TABLE_BANK = "budget_bank"
        const val TABLE_BUCKET = "budget_bucket"
        const val TABLE_LINE_ITEM = "budget_line_item"
        const val SETTINGS_ID = 1
        const val SAMPLE_MONTHLY_INCOME_PAISE = 5_000_000L
        private const val DATABASE_NAME = "fin_tracker_budget.db"
        private const val DATABASE_VERSION = 3
    }
}
