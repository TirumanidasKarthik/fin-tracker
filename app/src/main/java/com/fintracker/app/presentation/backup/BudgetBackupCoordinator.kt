package com.fintracker.app.presentation.backup

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.fintracker.app.data.backup.BudgetBackupStore
import com.fintracker.app.domain.budget.BudgetPlan
import com.fintracker.app.presentation.budget.BudgetController
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService

/** Owns the Android document-picker, passphrase, and restore-confirmation flow. */
class BudgetBackupCoordinator(
    private val activity: Activity,
    private val controller: BudgetController,
    private val executor: ExecutorService,
    private val onBudgetRestored: (BudgetPlan) -> Unit,
) {
    private val store = BudgetBackupStore(activity)
    private var pendingExportPassword: CharArray? = null

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri = data?.data
        when (requestCode) {
            REQUEST_CREATE_BACKUP -> {
                val password = pendingExportPassword
                pendingExportPassword = null
                if (resultCode == Activity.RESULT_OK && uri != null && password != null) writeBackup(uri, password)
                else password?.let { Arrays.fill(it, '\u0000') }
            }
            REQUEST_OPEN_BACKUP -> if (resultCode == Activity.RESULT_OK && uri != null) askBackupPassword(uri)
        }
    }

    fun requestExport() {
        val password = passwordField("Backup password")
        val confirmation = passwordField("Confirm password")
        val form = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 8, 48, 4)
            addView(password)
            addView(confirmation, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Create encrypted backup")
            .setMessage("Choose a password with at least 8 characters. You will need it to restore this file.")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val first = password.text.toString()
                val second = confirmation.text.toString()
                when {
                    first.length < 8 -> password.error = "Use at least 8 characters."
                    first != second -> confirmation.error = "Passwords do not match."
                    else -> {
                        pendingExportPassword?.let { Arrays.fill(it, '\u0000') }
                        pendingExportPassword = first.toCharArray()
                        dialog.dismiss()
                        launchCreateBackup()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun launchCreateBackup() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "FinTracker-backup-$date.ftbackup")
        }
        activity.startActivityForResult(intent, REQUEST_CREATE_BACKUP)
    }

    private fun writeBackup(uri: Uri, password: CharArray) {
        executor.execute {
            val result = runCatching { store.write(uri, controller.loadPlan(), password) }
            Arrays.fill(password, '\u0000')
            activity.runOnUiThread {
                result.onSuccess { toast("Encrypted budget backup saved.") }
                    .onFailure { toast(it.message ?: "Could not save the backup.") }
            }
        }
    }

    fun requestImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
        }
        activity.startActivityForResult(intent, REQUEST_OPEN_BACKUP)
    }

    private fun askBackupPassword(uri: Uri) {
        val password = passwordField("Backup password")
        AlertDialog.Builder(activity)
            .setTitle("Unlock budget backup")
            .setMessage("Enter the password used when this backup was created.")
            .setView(LinearLayout(activity).apply {
                setPadding(48, 8, 48, 4)
                addView(password)
            })
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Restore", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val passphrase = password.text.toString().toCharArray()
                        if (passphrase.size < 8) {
                            password.error = "Enter the backup password."
                        } else {
                            dialog.dismiss()
                            readAndConfirmBackup(uri, passphrase)
                        }
                    }
                }
                dialog.show()
            }
    }

    private fun readAndConfirmBackup(uri: Uri, password: CharArray) {
        executor.execute {
            val result = runCatching { store.read(uri, password) }
            Arrays.fill(password, '\u0000')
            activity.runOnUiThread {
                result.onSuccess(::confirmBackupRestore)
                    .onFailure { toast(it.message ?: "Could not read this backup.") }
            }
        }
    }

    private fun confirmBackupRestore(plan: BudgetPlan) {
        AlertDialog.Builder(activity)
            .setTitle("Replace current budget?")
            .setMessage("Restoring replaces all current income sources, banks, buckets, categories, targets, and planned amounts. This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Replace budget") { _, _ ->
                executor.execute {
                    runCatching { controller.replacePlan(plan) }
                        .onSuccess { restored -> activity.runOnUiThread { onBudgetRestored(restored); toast("Budget restored from backup.") } }
                        .onFailure { error -> activity.runOnUiThread { toast(error.message ?: "Could not restore the backup.") } }
                }
            }
            .show()
    }

    private fun passwordField(hintText: String) = EditText(activity).apply {
        hint = hintText
        setSingleLine(true)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        textSize = 16f
    }

    private fun toast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val REQUEST_CREATE_BACKUP = 1001
        const val REQUEST_OPEN_BACKUP = 1002
    }
}
