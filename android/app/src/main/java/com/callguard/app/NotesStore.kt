package com.callguard.app

import android.content.Context

/**
 * Stores your per-call notes in SharedPreferences, keyed by the call log's
 * unique _ID. No database, no dependencies — the note follows the call across
 * refreshes and app restarts.
 */
object NotesStore {

    private const val PREFS = "call_notes"

    fun get(context: Context, id: Long): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(id.toString(), null)?.takeIf { it.isNotBlank() }
    }

    fun set(context: Context, id: Long, note: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (note.isBlank()) remove(id.toString()) else putString(id.toString(), note)
        }.apply()
    }

    // Per-call severity tag, stored in the same prefs file under a "sev_" prefix.

    fun getSeverity(context: Context, id: Long): Severity {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Severity.fromName(prefs.getString("sev_$id", null))
    }

    fun setSeverity(context: Context, id: Long, severity: Severity) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (severity == Severity.Unset) remove("sev_$id") else putString("sev_$id", severity.name)
        }.apply()
    }
}
