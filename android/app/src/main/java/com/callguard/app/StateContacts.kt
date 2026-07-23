package com.callguard.app

/**
 * Per-state and federal contacts for reporting spoofed / harassing calls.
 *
 * USA-only. The traceback mechanism is federal (FCC / TRACED Act) and identical
 * in every state, so there is no per-state document pile — only WHO you contact
 * differs. Each state's Attorney General also accepts robocall/spoofing
 * complaints; the official .gov sites below are the current complaint portals.
 *
 * Reviewed July 2026. Government URLs and numbers change — the app also links to
 * USA.gov's always-current official finder as a safety net, and shows a Verify
 * option, so a stale row here never leaves a user stranded.
 */

/** A national contact that applies to every state. */
data class FederalContact(
    val name: String,
    val purpose: String,
    val url: String,
    val phone: String? = null,
)

/** One state / DC: its Attorney General consumer-complaint entry point. */
data class StateContact(
    val name: String,
    val usps: String,
    val agOffice: String,
    val agUrl: String,
)

object Contacts {

    val federal: List<FederalContact> = listOf(
        FederalContact(
            name = "FCC — Unwanted Calls",
            purpose = "File the federal spoofing complaint. This is the primary channel.",
            url = "https://consumercomplaints.fcc.gov",
            phone = "1-888-225-5322",
        ),
        FederalContact(
            name = "FTC — Report Fraud",
            purpose = "Report illegal robocalls and scam calls to the FTC.",
            url = "https://reportfraud.ftc.gov",
            phone = "1-877-382-4357",
        ),
        FederalContact(
            name = "National Do Not Call Registry",
            purpose = "Register your number (free) to strengthen your complaint.",
            url = "https://www.donotcall.gov",
            phone = "1-888-382-1222",
        ),
    )

    /** Always-current government fallback if a state row below is ever outdated. */
    const val OFFICIAL_STATE_FINDER = "https://www.usa.gov/state-consumer"

    val states: List<StateContact> = listOf(
        StateContact("Alabama", "AL", "Alabama Attorney General", "https://www.alabamaag.gov/consumer-complaint/"),
        StateContact("Alaska", "AK", "Alaska Department of Law", "https://law.alaska.gov/department/civil/consumer/cpindex.html"),
        StateContact("Arizona", "AZ", "Arizona Attorney General", "https://www.azag.gov/consumer"),
        StateContact("Arkansas", "AR", "Arkansas Attorney General", "https://arkansasag.gov/consumer-protection/"),
        StateContact("California", "CA", "California Attorney General", "https://oag.ca.gov/contact/consumer-complaint-against-business-or-company"),
        StateContact("Colorado", "CO", "Colorado Attorney General", "https://coag.gov/file-complaint/"),
        StateContact("Connecticut", "CT", "Connecticut Attorney General", "https://portal.ct.gov/ag"),
        StateContact("Delaware", "DE", "Delaware Attorney General", "https://attorneygeneral.delaware.gov/fraud/cpu/"),
        StateContact("District of Columbia", "DC", "DC Attorney General", "https://oag.dc.gov/consumer-protection"),
        StateContact("Florida", "FL", "Florida Attorney General", "https://www.myfloridalegal.com/consumer-protection"),
        StateContact("Georgia", "GA", "Georgia Consumer Protection Division", "https://consumer.georgia.gov/consumer-complaints"),
        StateContact("Hawaii", "HI", "Hawaii Office of Consumer Protection", "https://cca.hawaii.gov/ocp/"),
        StateContact("Idaho", "ID", "Idaho Attorney General", "https://www.ag.idaho.gov/consumer-protection/"),
        StateContact("Illinois", "IL", "Illinois Attorney General", "https://illinoisattorneygeneral.gov/consumer-protection/"),
        StateContact("Indiana", "IN", "Indiana Attorney General", "https://www.in.gov/attorneygeneral/consumer-protection-division/"),
        StateContact("Iowa", "IA", "Iowa Attorney General", "https://www.iowaattorneygeneral.gov/for-consumers"),
        StateContact("Kansas", "KS", "Kansas Attorney General", "https://www.ag.ks.gov/complaint-center"),
        StateContact("Kentucky", "KY", "Kentucky Attorney General", "https://www.ag.ky.gov/scams"),
        StateContact("Louisiana", "LA", "Louisiana Attorney General", "https://www.ag.state.la.us/Consumer"),
        StateContact("Maine", "ME", "Maine Attorney General", "https://www.maine.gov/ag/consumer/"),
        StateContact("Maryland", "MD", "Maryland Attorney General", "https://www.marylandattorneygeneral.gov/Pages/CPD/default.aspx"),
        StateContact("Massachusetts", "MA", "Massachusetts Attorney General", "https://www.mass.gov/how-to/file-a-consumer-complaint"),
        StateContact("Michigan", "MI", "Michigan Attorney General", "https://www.michigan.gov/ag/consumer-protection"),
        StateContact("Minnesota", "MN", "Minnesota Attorney General", "https://www.ag.state.mn.us/office/complaint.asp"),
        StateContact("Mississippi", "MS", "Mississippi Attorney General", "https://www.attorneygeneral.ms.gov/consumer"),
        StateContact("Missouri", "MO", "Missouri Attorney General", "https://ago.mo.gov/consumer-help/"),
        StateContact("Montana", "MT", "Montana Office of Consumer Protection", "https://dojmt.gov/consumer/"),
        StateContact("Nebraska", "NE", "Nebraska Attorney General", "https://protectthegoodlife.nebraska.gov/"),
        StateContact("Nevada", "NV", "Nevada Attorney General", "https://ag.nv.gov/Complaints/File_Complaint/"),
        StateContact("New Hampshire", "NH", "New Hampshire Attorney General", "https://www.doj.nh.gov/consumer/"),
        StateContact("New Jersey", "NJ", "New Jersey Division of Consumer Affairs", "https://www.njconsumeraffairs.gov/File-a-Complaint"),
        StateContact("New Mexico", "NM", "New Mexico Attorney General", "https://www.nmag.gov/consumer-protection/"),
        StateContact("New York", "NY", "New York Attorney General", "https://ag.ny.gov/consumer-frauds/filing-consumer-complaint"),
        StateContact("North Carolina", "NC", "North Carolina Attorney General", "https://ncdoj.gov/file-a-complaint/"),
        StateContact("North Dakota", "ND", "North Dakota Attorney General", "https://attorneygeneral.nd.gov/consumer-resources/"),
        StateContact("Ohio", "OH", "Ohio Attorney General", "https://www.ohioattorneygeneral.gov/complaint"),
        StateContact("Oklahoma", "OK", "Oklahoma Attorney General", "https://www.oag.ok.gov/consumer-protection-unit"),
        StateContact("Oregon", "OR", "Oregon Department of Justice", "https://www.doj.state.or.us/consumer-protection/"),
        StateContact("Pennsylvania", "PA", "Pennsylvania Attorney General", "https://www.attorneygeneral.gov/submit-a-complaint/"),
        StateContact("Rhode Island", "RI", "Rhode Island Attorney General", "https://riag.ri.gov/consumer-protection"),
        StateContact("South Carolina", "SC", "South Carolina Dept. of Consumer Affairs", "https://consumer.sc.gov/consumers/how-file-complaint"),
        StateContact("South Dakota", "SD", "South Dakota Attorney General", "https://consumer.sd.gov/"),
        StateContact("Tennessee", "TN", "Tennessee Division of Consumer Affairs", "https://www.tn.gov/commerce/consumer-affairs.html"),
        StateContact("Texas", "TX", "Texas Attorney General", "https://www.texasattorneygeneral.gov/consumer-protection/file-consumer-complaint"),
        StateContact("Utah", "UT", "Utah Division of Consumer Protection", "https://consumerprotection.utah.gov/complaints/"),
        StateContact("Vermont", "VT", "Vermont Attorney General", "https://ago.vermont.gov/cap"),
        StateContact("Virginia", "VA", "Virginia Attorney General", "https://www.oag.state.va.us/consumer-protection"),
        StateContact("Washington", "WA", "Washington Attorney General", "https://www.atg.wa.gov/file-complaint"),
        StateContact("West Virginia", "WV", "West Virginia Attorney General", "https://ago.wv.gov/consumerprotection/"),
        StateContact("Wisconsin", "WI", "Wisconsin Dept. of Agriculture, Trade & Consumer Protection", "https://datcp.wi.gov/Pages/Programs_Services/FileComplaint.aspx"),
        StateContact("Wyoming", "WY", "Wyoming Attorney General", "https://ag.wyo.gov/consumer-protection-unit"),
    )

    fun forUsps(code: String?): StateContact? {
        if (code.isNullOrBlank()) return null
        val c = code.trim().uppercase()
        return states.firstOrNull { it.usps == c }
    }
}
