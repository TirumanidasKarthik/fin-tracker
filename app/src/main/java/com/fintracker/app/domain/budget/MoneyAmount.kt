package com.fintracker.app.domain.budget

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object MoneyAmount {
    fun parseRupeesToPaise(input: String): Long? {
        val normalized = input.trim().replace(",", "").replace("₹", "").trim()
        if (!normalized.matches(Regex("\\d+(\\.\\d{1,2})?"))) return null
        return try {
            BigDecimal(normalized)
                .movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact()
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun parsePercentToBasisPoints(input: String): Int? {
        val normalized = input.trim().removeSuffix("%").trim()
        if (!normalized.matches(Regex("\\d+(\\.\\d{1,2})?"))) return null
        return try {
            val basisPoints = BigDecimal(normalized)
                .movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .intValueExact()
            basisPoints.takeIf { it in 0..FULL_ALLOCATION_BASIS_POINTS }
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun formatRupees(amountInPaise: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            currency = Currency.getInstance("INR")
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        return formatter.format(BigDecimal.valueOf(amountInPaise, 2))
    }

    fun toInputRupees(amountInPaise: Long): String =
        BigDecimal.valueOf(amountInPaise, 2).stripTrailingZeros().toPlainString()

    fun formatPercent(basisPoints: Int): String {
        val whole = basisPoints / 100
        val fractional = basisPoints % 100
        if (fractional == 0) return "$whole%"
        val decimals = "%02d".format(fractional).trimEnd('0')
        return "$whole.$decimals%"
    }

    /** Formats the share of [total] represented by [amount], rounded to 0.01%. */
    fun formatPercentOf(amount: Long, total: Long): String {
        require(amount >= 0) { "Amount cannot be negative." }
        require(total >= 0) { "Total cannot be negative." }
        if (total == 0L) return formatPercent(0)
        val basisPoints = BigDecimal.valueOf(amount)
            .multiply(BigDecimal.valueOf(FULL_ALLOCATION_BASIS_POINTS.toLong()))
            .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
            .intValueExact()
        return formatPercent(basisPoints)
    }
}
