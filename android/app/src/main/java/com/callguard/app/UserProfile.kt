package com.callguard.app

import android.content.Context

/**
 * The details you enter once on the "My info" screen. Every generated document
 * (FCC complaint, police cover note, carrier script) pulls from here so you
 * never retype your name, number, or case numbers. Stored locally in
 * SharedPreferences — nothing leaves the device.
 */
data class UserProfile(
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val addressCity: String = "",
    val state: String = "",          // two-letter USPS code, e.g. "CA"
    val carrier: String = "",        // e.g. "Verizon", "AT&T", "T-Mobile"
    val fccComplaintNumber: String = "",
    val policeCaseNumber: String = "",
    val carrierCaseNumber: String = "",
) {
    /** True once the minimum needed to fill a document is present. */
    val isReadyForDocuments: Boolean
        get() = fullName.isNotBlank() && phone.isNotBlank()
}

object ProfileStore {

    private const val PREFS = "user_profile"

    fun load(context: Context): UserProfile {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return UserProfile(
            fullName = p.getString("fullName", "") ?: "",
            phone = p.getString("phone", "") ?: "",
            email = p.getString("email", "") ?: "",
            addressCity = p.getString("addressCity", "") ?: "",
            state = p.getString("state", "") ?: "",
            carrier = p.getString("carrier", "") ?: "",
            fccComplaintNumber = p.getString("fccComplaintNumber", "") ?: "",
            policeCaseNumber = p.getString("policeCaseNumber", "") ?: "",
            carrierCaseNumber = p.getString("carrierCaseNumber", "") ?: "",
        )
    }

    fun save(context: Context, profile: UserProfile) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            putString("fullName", profile.fullName)
            putString("phone", profile.phone)
            putString("email", profile.email)
            putString("addressCity", profile.addressCity)
            putString("state", profile.state)
            putString("carrier", profile.carrier)
            putString("fccComplaintNumber", profile.fccComplaintNumber)
            putString("policeCaseNumber", profile.policeCaseNumber)
            putString("carrierCaseNumber", profile.carrierCaseNumber)
        }.apply()
    }
}
