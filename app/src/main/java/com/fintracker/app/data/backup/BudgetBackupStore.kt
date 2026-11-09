package com.fintracker.app.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.fintracker.app.domain.budget.BudgetPlan
import java.io.ByteArrayOutputStream

/** Reads and writes encrypted backup files through Android's user-selected document provider. */
class BudgetBackupStore(context: Context) {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver

    fun write(uri: Uri, plan: BudgetPlan, password: CharArray) {
        val bytes = BudgetBackupCodec.encode(plan, password)
        contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: error("Could not open the selected file for writing.")
    }

    fun read(uri: Uri, password: CharArray): BudgetPlan {
        val bytes = contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= MAX_BACKUP_FILE_BYTES) { "The selected backup file is too large." }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: error("Could not open the selected backup file.")
        return BudgetBackupCodec.decode(bytes, password)
    }

    private companion object {
        const val MAX_BACKUP_FILE_BYTES = 5 * 1024 * 1024 + 128
    }
}
