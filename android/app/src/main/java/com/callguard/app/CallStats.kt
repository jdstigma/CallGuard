package com.callguard.app

import android.provider.CallLog
import java.util.Calendar

/** Aggregated stats for one phone number. */
data class NumberStat(
    val number: String,
    val name: String?,
    val totalCount: Int,
    val flaggedCount: Int,
    val firstSeenMillis: Long,
    val lastSeenMillis: Long,
)

/** Totals over one time window (e.g. last 7 days). */
data class WindowStat(
    val label: String,
    val total: Int,
    val flagged: Int,
    val uniqueNumbers: Int,
)

/**
 * Extra evidence metrics that strengthen a complaint: the 7/30/90/all-time
 * breakdown, plus the pattern details officials look for (overnight calls, the
 * busiest hour, how relentless the campaign is per day).
 */
data class StatsExtras(
    val firstCallMillis: Long?,
    val lastCallMillis: Long?,
    val busiestHour: Int?,        // 0-23, or null if no calls
    val busiestHourCount: Int,
    val overnightCount: Int,      // calls arriving 10 PM–6 AM
    val avgPerDay: Double,
    val windows: List<WindowStat>,
)

/** Whole-log summary shown on the Analysis screen. */
data class CallStats(
    val totalCalls: Int,
    val flaggedCalls: Int,
    val uniqueNumbers: Int,
    val incoming: Int,
    val missed: Int,
    val rejected: Int,
    val perNumber: List<NumberStat>, // sorted: most calls first, then most flagged
) {
    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000

        fun from(entries: List<CallEntry>): CallStats {
            val perNumber = entries
                .groupBy { it.number }
                .map { (number, calls) ->
                    NumberStat(
                        number = number,
                        name = calls.firstNotNullOfOrNull { it.cachedName?.takeIf { n -> n.isNotBlank() } },
                        totalCount = calls.size,
                        flaggedCount = calls.count { it.isSuspicious },
                        firstSeenMillis = calls.minOf { it.timestampMillis },
                        lastSeenMillis = calls.maxOf { it.timestampMillis },
                    )
                }
                .sortedWith(compareByDescending<NumberStat> { it.totalCount }.thenByDescending { it.flaggedCount })

            return CallStats(
                totalCalls = entries.size,
                flaggedCalls = entries.count { it.isSuspicious },
                uniqueNumbers = perNumber.size,
                incoming = entries.count { it.type == CallLog.Calls.INCOMING_TYPE },
                missed = entries.count { it.type == CallLog.Calls.MISSED_TYPE },
                rejected = entries.count { it.type == CallLog.Calls.REJECTED_TYPE },
                perNumber = perNumber,
            )
        }

        /** Stats over only the last [days] days. */
        fun windowed(entries: List<CallEntry>, days: Int): CallStats {
            val cutoff = System.currentTimeMillis() - days.toLong() * DAY_MS
            return from(entries.filter { it.timestampMillis >= cutoff })
        }

        /** The 7 / 30 / 90 / all-time breakdown plus pattern metrics. */
        fun extras(entries: List<CallEntry>): StatsExtras {
            val windows = listOf(
                windowStat(entries, "Last 7 Days", 7),
                windowStat(entries, "Last 30 Days", 30),
                windowStat(entries, "Last 90 Days", 90),
                windowStat(entries, "All Time", null),
            )
            if (entries.isEmpty()) {
                return StatsExtras(null, null, null, 0, 0, 0.0, windows)
            }

            val cal = Calendar.getInstance()
            val hourCounts = IntArray(24)
            var overnight = 0
            entries.forEach {
                cal.timeInMillis = it.timestampMillis
                val h = cal.get(Calendar.HOUR_OF_DAY)
                hourCounts[h]++
                if (h >= 22 || h < 6) overnight++
            }
            val busiest = hourCounts.indices.maxByOrNull { hourCounts[it] }
            val first = entries.minOf { it.timestampMillis }
            val last = entries.maxOf { it.timestampMillis }
            val spanDays = (((last - first) / DAY_MS) + 1).coerceAtLeast(1)
            val avg = entries.size.toDouble() / spanDays

            return StatsExtras(
                firstCallMillis = first,
                lastCallMillis = last,
                busiestHour = busiest,
                busiestHourCount = busiest?.let { hourCounts[it] } ?: 0,
                overnightCount = overnight,
                avgPerDay = avg,
                windows = windows,
            )
        }

        private fun windowStat(entries: List<CallEntry>, label: String, days: Int?): WindowStat {
            val s = if (days == null) from(entries) else windowed(entries, days)
            return WindowStat(label, s.totalCalls, s.flaggedCalls, s.uniqueNumbers)
        }
    }
}
