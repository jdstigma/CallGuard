package com.callguard.app

import android.content.Context
import android.provider.CallLog

/**
 * Reads the device's system call log via the CallLog content provider.
 * Requires the READ_CALL_LOG permission (granted at runtime).
 */
object CallLogRepository {

    fun readAll(context: Context): List<CallEntry> {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE,
        )

        val entries = mutableListOf<CallEntry>()

        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC", // newest first
        )?.use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
            val idxNumber = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val idxName = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val idxDate = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val idxDuration = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            val idxType = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idxId)
                val rawNumber = cursor.getString(idxNumber)
                entries += CallEntry(
                    id = id,
                    number = if (rawNumber.isNullOrBlank()) "Unknown / withheld" else rawNumber,
                    cachedName = cursor.getString(idxName),
                    timestampMillis = cursor.getLong(idxDate),
                    durationSeconds = cursor.getLong(idxDuration),
                    type = cursor.getInt(idxType),
                    note = NotesStore.get(context, id),
                )
            }
        }

        return entries
    }

    /** Numbers seen more than once — useful for spotting a repeat harasser
     *  even when the digits differ only slightly. */
    fun repeatOffenders(entries: List<CallEntry>): Map<String, Int> =
        entries.filter { it.isSuspicious }
            .groupingBy { it.number }
            .eachCount()
            .filter { it.value > 1 }
}
