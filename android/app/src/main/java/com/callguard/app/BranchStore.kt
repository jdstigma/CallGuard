package com.callguard.app

import android.content.Context

/**
 * Groups multiple phone numbers under one "caller name" branch. A spoofing
 * campaign hits you from dozens of numbers that are really one harasser —
 * a branch lets the app (and the generated documents) treat them as a single
 * identity, e.g. Caller "Night Harasser" — 47 numbers, 52 calls.
 *
 * Stored locally in SharedPreferences: key = the phone number as shown in the
 * call log, value = the branch (caller) name. A number belongs to at most one
 * branch; nothing leaves the device.
 */
object BranchStore {

    private const val PREFS = "number_branches"

    /** Every number → branch-name assignment. */
    fun all(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.all.mapNotNull { (k, v) ->
            (v as? String)?.takeIf { it.isNotBlank() }?.let { k to it }
        }.toMap()
    }

    fun branchOf(context: Context, number: String): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(number, null)?.takeIf { it.isNotBlank() }

    /** Assign [numbers] to [branchName] (moves them if already in another branch). */
    fun assign(context: Context, numbers: Collection<String>, branchName: String) {
        val name = branchName.trim()
        if (name.isEmpty() || numbers.isEmpty()) return
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        numbers.forEach { editor.putString(it, name) }
        editor.apply()
    }

    /** Dissolve a branch — its numbers go back to standing alone. */
    fun removeBranch(context: Context, branchName: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        prefs.all.forEach { (k, v) -> if (v == branchName) editor.remove(k) }
        editor.apply()
    }
}
