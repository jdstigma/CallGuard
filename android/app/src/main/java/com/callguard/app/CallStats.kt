package com.callguard.app

import android.provider.CallLog

/** Aggregated stats for one phone number. */
data class NumberStat(
    val number: String,
    val name: String?,
    val totalCount: Int,
    val flaggedCount: Int,
    val firstSeenMillis: Long,
    val lastSeenMillis: Long,
)

/** Whole-log summary shown on the Profile tab. */
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
    }
}
