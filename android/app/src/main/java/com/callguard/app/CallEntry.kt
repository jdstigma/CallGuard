package com.callguard.app

/**
 * How serious a documented call was. Lets the incident timeline show escalation
 * (a run of Silent calls building to Threatening ones) rather than a flat list.
 * [Unset] is the default until the user tags a call.
 */
enum class Severity(val label: String, val rank: Int) {
    Unset("Unset", 0),
    Silent("Silent", 1),
    Spoken("Spoken", 2),
    Threatening("Threatening", 3);

    companion object {
        fun fromName(name: String?): Severity =
            entries.firstOrNull { it.name == name } ?: Unset
    }
}

/**
 * One incoming call as read from the Android system call log.
 *
 * We only read what the OS already recorded — number shown, when, how long it
 * rang, and how it ended. For a spoofed call the [number] is the FAKE number the
 * network delivered; the real origin is not available on the device (see the
 * carrier guide).
 */
data class CallEntry(
    val id: Long,              // the call log's unique _ID — used to attach your note
    val number: String,        // number as shown (may be spoofed / blank / "Unknown")
    val cachedName: String?,   // contact name if the number is in your contacts, else null
    val timestampMillis: Long, // when the call happened (epoch millis)
    val durationSeconds: Long, // connected duration in seconds (0 for missed/rejected)
    val type: Int,             // CallLog.Calls.TYPE (INCOMING, MISSED, REJECTED, ...)
    val note: String? = null,  // your annotation (added later via the DB layer)
    val severity: Severity = Severity.Unset // how serious this call was, if tagged
) {
    val isKnownContact: Boolean get() = !cachedName.isNullOrBlank()

    /**
     * Heuristic flag for the "silent stranger" harassment pattern:
     * an incoming/missed/rejected call, from a number not in your contacts,
     * that either never connected or was answered but silent (very short).
     */
    val isSuspicious: Boolean
        get() {
            val incomingLike = type == android.provider.CallLog.Calls.INCOMING_TYPE ||
                type == android.provider.CallLog.Calls.MISSED_TYPE ||
                type == android.provider.CallLog.Calls.REJECTED_TYPE
            val silent = durationSeconds <= 15L // missed, or answered-but-silent
            return incomingLike && !isKnownContact && silent
        }

    val typeLabel: String
        get() = when (type) {
            android.provider.CallLog.Calls.INCOMING_TYPE -> "Incoming"
            android.provider.CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
            android.provider.CallLog.Calls.MISSED_TYPE -> "Missed"
            android.provider.CallLog.Calls.REJECTED_TYPE -> "Rejected"
            android.provider.CallLog.Calls.BLOCKED_TYPE -> "Blocked"
            else -> "Other"
        }
}
