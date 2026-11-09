package com.fintracker.app.data.backup

import com.fintracker.app.domain.budget.Bank
import com.fintracker.app.domain.budget.BudgetBucket
import com.fintracker.app.domain.budget.BudgetLineItem
import com.fintracker.app.domain.budget.BudgetPlan
import com.fintracker.app.domain.budget.IncomeSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.Properties
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Versioned, password-encrypted portable backup for a complete local budget. */
object BudgetBackupCodec {
    private val MAGIC = "FINTRACKER-BACKUP-1\n".toByteArray(Charsets.US_ASCII)
    private const val FORMAT_VERSION = 1
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val KDF_ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val MIN_PASSWORD_LENGTH = 8
    private const val MAX_PAYLOAD_BYTES = 5 * 1024 * 1024
    private const val MAX_BACKUP_FILE_BYTES = MAX_PAYLOAD_BYTES + 128
    private const val MAX_COLLECTION_SIZE = 2_000

    fun encode(plan: BudgetPlan, password: CharArray): ByteArray {
        require(password.size >= MIN_PASSWORD_LENGTH) { "Use a backup password with at least 8 characters." }
        val payload = serialize(plan)
        require(payload.size <= MAX_PAYLOAD_BYTES) { "This budget backup is too large." }

        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_BYTES).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(MAGIC)
        val encrypted = cipher.doFinal(payload)
        return ByteArrayOutputStream(MAGIC.size + SALT_BYTES + IV_BYTES + encrypted.size).use { output ->
            output.write(MAGIC)
            output.write(salt)
            output.write(iv)
            output.write(encrypted)
            output.toByteArray()
        }
    }

    fun decode(backup: ByteArray, password: CharArray): BudgetPlan {
        require(password.size >= MIN_PASSWORD_LENGTH) { "Enter the backup password." }
        require(backup.size in (MAGIC.size + SALT_BYTES + IV_BYTES + 16)..MAX_BACKUP_FILE_BYTES) {
            "This file is not a valid Fin Tracker backup."
        }
        require(backup.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            "This file is not a supported Fin Tracker backup."
        }

        val saltStart = MAGIC.size
        val ivStart = saltStart + SALT_BYTES
        val encryptedStart = ivStart + IV_BYTES
        val salt = backup.copyOfRange(saltStart, ivStart)
        val iv = backup.copyOfRange(ivStart, encryptedStart)
        val encrypted = backup.copyOfRange(encryptedStart, backup.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(MAGIC)
        val payload = try {
            cipher.doFinal(encrypted)
        } catch (_: AEADBadTagException) {
            throw IllegalArgumentException("The password is incorrect or the backup file is damaged.")
        }
        return deserialize(payload)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, KDF_ITERATIONS, KEY_BITS)
        return try {
            SecretKeySpec(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
                "AES",
            )
        } finally {
            spec.clearPassword()
        }
    }

    private fun serialize(plan: BudgetPlan): ByteArray {
        val properties = Properties().apply {
            setProperty("formatVersion", FORMAT_VERSION.toString())
            setProperty("bank.count", plan.banks.size.toString())
            plan.banks.forEachIndexed { index, bank ->
                setProperty("bank.$index.id", bank.id)
                setProperty("bank.$index.name", bank.name)
            }
            setProperty("income.count", plan.incomeSources.size.toString())
            plan.incomeSources.forEachIndexed { index, income ->
                setProperty("income.$index.id", income.id)
                setProperty("income.$index.name", income.name)
                setProperty("income.$index.amountInPaise", income.amountInPaise.toString())
                income.bankId?.let { setProperty("income.$index.bankId", it) }
            }
            setProperty("bucket.count", plan.buckets.size.toString())
            plan.buckets.forEachIndexed { bucketIndex, bucket ->
                val prefix = "bucket.$bucketIndex"
                setProperty("$prefix.id", bucket.id)
                setProperty("$prefix.name", bucket.name)
                setProperty("$prefix.shareBasisPoints", bucket.shareBasisPoints.toString())
                setProperty("$prefix.item.count", bucket.lineItems.size.toString())
                bucket.lineItems.forEachIndexed { itemIndex, item ->
                    val itemPrefix = "$prefix.item.$itemIndex"
                    setProperty("$itemPrefix.id", item.id)
                    setProperty("$itemPrefix.label", item.label)
                    setProperty("$itemPrefix.amountInPaise", item.amountInPaise.toString())
                    item.bankId?.let { setProperty("$itemPrefix.bankId", it) }
                }
            }
        }
        return ByteArrayOutputStream().use { output ->
            properties.store(output, "Fin Tracker local budget backup")
            output.toByteArray()
        }
    }

    private fun deserialize(payload: ByteArray): BudgetPlan {
        require(payload.size <= MAX_PAYLOAD_BYTES) { "This budget backup is too large." }
        val properties = Properties().apply { load(ByteArrayInputStream(payload)) }
        require(properties.requiredInt("formatVersion") == FORMAT_VERSION) {
            "This backup uses an unsupported format version."
        }
        val banks = (0 until properties.collectionCount("bank.count")).map { index ->
            Bank(properties.required("bank.$index.id"), properties.required("bank.$index.name"))
        }
        val incomeSources = (0 until properties.collectionCount("income.count")).map { index ->
            IncomeSource(
                id = properties.required("income.$index.id"),
                name = properties.required("income.$index.name"),
                amountInPaise = properties.requiredLong("income.$index.amountInPaise"),
                bankId = properties.getProperty("income.$index.bankId"),
            )
        }
        val buckets = (0 until properties.collectionCount("bucket.count")).map { bucketIndex ->
            val prefix = "bucket.$bucketIndex"
            val lineItems = (0 until properties.collectionCount("$prefix.item.count")).map { itemIndex ->
                val itemPrefix = "$prefix.item.$itemIndex"
                BudgetLineItem(
                    id = properties.required("$itemPrefix.id"),
                    label = properties.required("$itemPrefix.label"),
                    bankId = properties.getProperty("$itemPrefix.bankId"),
                    amountInPaise = properties.requiredLong("$itemPrefix.amountInPaise"),
                )
            }
            BudgetBucket(
                id = properties.required("$prefix.id"),
                name = properties.required("$prefix.name"),
                shareBasisPoints = properties.requiredInt("$prefix.shareBasisPoints"),
                lineItems = lineItems,
            )
        }
        return BudgetPlan(incomeSources = incomeSources, buckets = buckets, banks = banks)
    }

    private fun Properties.required(key: String): String =
        getProperty(key) ?: throw IllegalArgumentException("The backup is missing required data.")

    private fun Properties.requiredInt(key: String): Int =
        required(key).toIntOrNull() ?: throw IllegalArgumentException("The backup contains invalid data.")

    private fun Properties.requiredLong(key: String): Long =
        required(key).toLongOrNull() ?: throw IllegalArgumentException("The backup contains invalid data.")

    private fun Properties.collectionCount(key: String): Int {
        val count = requiredInt(key)
        require(count in 0..MAX_COLLECTION_SIZE) { "The backup contains too many items." }
        return count
    }
}
