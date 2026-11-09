package com.fintracker.app.data.backup

import com.fintracker.app.domain.budget.Bank
import com.fintracker.app.domain.budget.BudgetBucket
import com.fintracker.app.domain.budget.BudgetLineItem
import com.fintracker.app.domain.budget.BudgetPlan
import com.fintracker.app.domain.budget.IncomeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BudgetBackupCodecTest {
    private val password = "my-local-backup-passphrase".toCharArray()

    @Test
    fun encryptedBackupRoundTripsAllBudgetData() {
        val original = samplePlan()

        val restored = BudgetBackupCodec.decode(BudgetBackupCodec.encode(original, password), password)

        assertEquals(original, restored)
    }

    @Test
    fun rejectsIncorrectPasswordAndDamagedBackup() {
        val backup = BudgetBackupCodec.encode(samplePlan(), password)

        assertThrows(IllegalArgumentException::class.java) {
            BudgetBackupCodec.decode(backup, "incorrect-passphrase".toCharArray())
        }
        assertThrows(IllegalArgumentException::class.java) {
            BudgetBackupCodec.decode(backup.copyOf().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }, password)
        }
    }

    @Test
    fun usesFreshSaltAndNonceForEachExport() {
        val first = BudgetBackupCodec.encode(samplePlan(), password)
        val second = BudgetBackupCodec.encode(samplePlan(), password)

        assertEquals(first.size, second.size)
        assert(first.contentEquals(second).not())
    }

    @Test
    fun requiresStrongEnoughPassword() {
        assertThrows(IllegalArgumentException::class.java) {
            BudgetBackupCodec.encode(samplePlan(), "short".toCharArray())
        }
    }

    private fun samplePlan() = BudgetPlan(
        incomeSources = listOf(IncomeSource("income-1", "Salary, monthly", 8_250_000, "bank-1")),
        buckets = listOf(
            BudgetBucket(
                "bucket-1",
                "Goals 💡",
                2_500,
                listOf(BudgetLineItem("item-1", "Home: down payment", "bank-1", 2_000_000)),
            ),
        ),
        banks = listOf(Bank("bank-1", "Main bank")),
    )
}
