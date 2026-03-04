package chang.sllj.homeassetkeeper.ui.util

import android.content.Context
import chang.sllj.homeassetkeeper.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val MEDIUM_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy")

private val SHORT_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M/d/yy")

/** Formats an epoch-ms timestamp to "Jan 5, 2025". */
fun Long.toFormattedDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(MEDIUM_FORMATTER)

/** Formats an epoch-ms timestamp to "1/5/25". */
fun Long.toShortDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(SHORT_FORMATTER)

/** Formats Long cents to a dollar string, e.g. 1299L → "$12.99". */
fun Long.toCurrencyString(): String = "$${this / 100}.%02d".format(this % 100)

/** Returns human-readable countdown: "3 days", "Today", "Expired". */
fun Long.toDaysLabel(context: Context): String = when {
    this < 0L   -> context.getString(R.string.date_expired)
    this == 0L  -> context.getString(R.string.date_today)
    this == 1L  -> context.getString(R.string.date_one_day)
    else        -> context.getString(R.string.date_days, this)
}
