package com.callguard.app

import android.content.Context

/**
 * Records the date/time of each *57 call trace the user places. *57 must be done
 * per-call and the result goes to the carrier, so keeping your own timestamped
 * log is what lets you tell police exactly which calls you traced.
 *
 * Stored locally in SharedPreferences as a comma-separated list of epoch millis.
 */
object TraceLog {

    private const val PREFS = "trace_log"
    private const val KEY = "times"

    fun add(context: Context, millis: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY, "").orEmpty()
        val updated = if (current.isBlank()) millis.toString() else "$current,$millis"
        prefs.edit().putString(KEY, updated).apply()
    }

    /** All trace times, most recent first. */
    fun all(context: Context): List<Long> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY, "").orEmpty()
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .sortedDescending()
    }
}
