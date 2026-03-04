package chang.sllj.homeassetkeeper.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Pure, stateless utility for warranty expiry calculations.
 *
 * All functions accept an explicit [nowMs] parameter (defaulting to
 * [System.currentTimeMillis]) to keep them fully testable without mocking the
 * system clock.
 *
 * Date arithmetic uses [java.time] APIs, which are available unconditionally
 * on minSdk 31 (Android 12) without core-library desugaring.
 */
object WarrantyCalculator {

    /** Describes where a warranty stands relative to the current moment. */
    enum class WarrantyStatus { ACTIVE, EXPIRING_SOON, EXPIRED }

    /**
     * Number of whole days between [nowMs] and [expiryDateMs].
     * Positive → not yet expired, negative → already expired.
     */
    fun daysUntilExpiry(
        expiryDateMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Long = ChronoUnit.DAYS.between(
        Instant.ofEpochMilli(nowMs),
        Instant.ofEpochMilli(expiryDateMs)
    )

    /** Returns true if [expiryDateMs] is strictly in the past. */
    fun isExpired(
        expiryDateMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean = expiryDateMs < nowMs

    /**
     * Returns true if the warranty expires within [thresholdDays] days from
     * [nowMs] but has not yet expired.
     */
    fun isExpiringSoon(
        expiryDateMs: Long,
        thresholdDays: Int,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (isExpired(expiryDateMs, nowMs)) return false
        return daysUntilExpiry(expiryDateMs, nowMs) <= thresholdDays
    }

    /**
     * Computes a [WarrantyStatus] for display in the UI.
     *
     * @param expiringSoonThresholdDays Items within this many days of expiry are
     * shown as EXPIRING_SOON (default 30).
     */
    fun warrantyStatus(
        expiryDateMs: Long,
        expiringSoonThresholdDays: Int = 30,
        nowMs: Long = System.currentTimeMillis()
    ): WarrantyStatus = when {
        isExpired(expiryDateMs, nowMs) -> WarrantyStatus.EXPIRED
        isExpiringSoon(expiryDateMs, expiringSoonThresholdDays, nowMs) -> WarrantyStatus.EXPIRING_SOON
        else -> WarrantyStatus.ACTIVE
    }

    /**
     * Calculates the warranty expiry timestamp given a [purchaseDateMs] and a
     * duration in whole months.
     *
     * Uses calendar-correct month arithmetic (e.g. Jan 31 + 1 month → Feb 28/29).
     * The result is midnight UTC on the computed expiry date.
     */
    fun expiryDateFromMonths(purchaseDateMs: Long, warrantyMonths: Int): Long {
        val purchaseDate = Instant.ofEpochMilli(purchaseDateMs)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
        return purchaseDate
            .plusMonths(warrantyMonths.toLong())
            .toEpochStartOfDayMs()
    }

    /**
     * Convenience overload for warranties expressed in whole years.
     */
    fun expiryDateFromYears(purchaseDateMs: Long, warrantyYears: Int): Long =
        expiryDateFromMonths(purchaseDateMs, warrantyYears * 12)

    /**
     * Human-readable label for a remaining duration, suitable for UI badges.
     *
     * Examples: "Expired", "Expires today", "3 days left", "2 months left",
     * "1 year left".
     */
    fun remainingLabel(
        expiryDateMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): String {
        val days = daysUntilExpiry(expiryDateMs, nowMs)
        return when {
            days < 0 -> "Expired"
            days == 0L -> "Expires today"
            days == 1L -> "1 day left"
            days < 31 -> "$days days left"
            days < 365 -> {
                val months = days / 30
                if (months == 1L) "1 month left" else "$months months left"
            }
            else -> {
                val years = days / 365
                if (years == 1L) "1 year left" else "$years years left"
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun LocalDate.toEpochStartOfDayMs(): Long =
        atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
