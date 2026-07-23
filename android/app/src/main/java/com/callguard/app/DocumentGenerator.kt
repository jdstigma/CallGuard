package com.callguard.app

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.Color as AColor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One document the app can produce, filled from the user's info + call stats. */
enum class DocumentType(val displayName: String, val fileSlug: String, val blurb: String) {
    FccComplaint(
        "FCC complaint",
        "FCC_complaint",
        "Federal record of the spoofing campaign. Paste the description into consumercomplaints.fcc.gov.",
    ),
    PoliceReport(
        "Harassment–police report cover note",
        "police_report",
        "Cover note + talking points to hand police so they can subpoena your carrier.",
    ),
    CarrierScript(
        "Carrier call script",
        "carrier_script",
        "Word-for-word script for opening a documented harassment case with your carrier.",
    ),
    IncidentTimeline(
        "Incident timeline",
        "incident_timeline",
        "Chronological log built from your dated notes — shows the pattern and any escalation.",
    ),
    EvidenceSummary(
        "Evidence summary",
        "evidence_summary",
        "One-page snapshot of your call statistics to attach to any filing.",
    ),
    CallTraceRecording(
        "Call trace (*57) & recording guide",
        "call_trace_recording",
        "How to trace a call with *57 and record audio lawfully for your records.",
    ),
}

/** A block of content in a generated document. */
private sealed interface Block {
    data class Title(val text: String) : Block
    data class Heading(val text: String) : Block
    data class Body(val text: String) : Block
    data class Bullet(val text: String) : Block
    data class Table(val headers: List<String>, val rows: List<List<String>>) : Block
    data class Gap(val points: Float) : Block
}

object DocumentGenerator {

    private val stamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
    private val human = SimpleDateFormat("MMMM d, yyyy", Locale.US)
    private val rangeFmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
    private val incidentFmt = SimpleDateFormat("EEE MMM d, yyyy · h:mm a", Locale.US)

    // US Letter at 72 dpi.
    private const val PAGE_W = 612
    private const val PAGE_H = 792
    private const val MARGIN = 54f

    data class Result(val uri: Uri?, val path: String)

    fun generate(
        context: Context,
        type: DocumentType,
        profile: UserProfile,
        entries: List<CallEntry>,
    ): Result {
        val stats = CallStats.from(entries)
        val blocks = buildBlocks(type, profile, stats, entries)
        val doc = renderPdf(blocks)

        val fileName = "CallGuard_${type.fileSlug}_${stamp.format(Date())}.pdf"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            doc.close()
            return Result(null, "Failed to create file")
        }
        resolver.openOutputStream(uri)?.use { doc.writeTo(it) }
        doc.close()
        return Result(uri, "Downloads/$fileName")
    }

    fun share(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share CallGuard document"))
    }

    // -- Rendering engine ---------------------------------------------------

    private fun renderPdf(blocks: List<Block>): PdfDocument {
        val pdf = PdfDocument()
        val title = paint(AColor.rgb(0x0F, 0x1E, 0x33), 20f, bold = true)
        val heading = paint(AColor.rgb(0x0F, 0x1E, 0x33), 13f, bold = true)
        val body = paint(AColor.rgb(0x22, 0x22, 0x22), 11f)
        val rule = Paint().apply {
            color = AColor.rgb(0xCC, 0xCC, 0xCC)
            strokeWidth = 0.7f
            isAntiAlias = true
        }
        val contentWidth = PAGE_W - MARGIN * 2

        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
        var canvas: Canvas = page.canvas
        var y = MARGIN

        fun newPage() {
            pdf.finishPage(page)
            pageNum++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            canvas = page.canvas
            y = MARGIN
        }

        fun ensure(space: Float) {
            if (y + space > PAGE_H - MARGIN) newPage()
        }

        fun drawWrapped(text: String, p: Paint, lineGap: Float, indent: Float = 0f) {
            val avail = contentWidth - indent
            text.split("\n").forEach { rawLine ->
                wrap(rawLine, p, avail).forEach { line ->
                    ensure(lineGap)
                    canvas.drawText(line, MARGIN + indent, y + p.textSize, p)
                    y += lineGap
                }
            }
        }

        blocks.forEach { block ->
            when (block) {
                is Block.Title -> { drawWrapped(titleCase(block.text), title, 26f); y += 6f }
                is Block.Heading -> { ensure(24f); y += 8f; drawWrapped(titleCase(block.text), heading, 18f) }
                is Block.Body -> drawWrapped(block.text, body, 16f)
                is Block.Bullet -> {
                    ensure(16f)
                    canvas.drawText("•", MARGIN, y + body.textSize, body)
                    drawWrapped(block.text, body, 16f, indent = 16f)
                }
                is Block.Table -> {
                    val cols = block.headers.size
                    if (cols > 0) {
                        val colW = contentWidth / cols
                        ensure(20f)
                        block.headers.forEachIndexed { i, h ->
                            canvas.drawText(h, MARGIN + i * colW, y + heading.textSize, heading)
                        }
                        y += 16f
                        ensure(2f)
                        canvas.drawLine(MARGIN, y, MARGIN + contentWidth, y, rule)
                        y += 8f
                        block.rows.forEach { row ->
                            ensure(15f)
                            row.forEachIndexed { i, c ->
                                canvas.drawText(c, MARGIN + i * colW, y + body.textSize, body)
                            }
                            y += 15f
                        }
                    }
                }
                is Block.Gap -> { y += block.points }
            }
        }
        pdf.finishPage(page)
        return pdf
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var line = StringBuilder()
        for (w in words) {
            val candidate = if (line.isEmpty()) w else "$line $w"
            if (paint.measureText(candidate) <= maxWidth || line.isEmpty()) {
                line = StringBuilder(candidate)
            } else {
                lines.add(line.toString())
                line = StringBuilder(w)
            }
        }
        if (line.isNotEmpty()) lines.add(line.toString())
        return lines
    }

    private fun paint(color: Int, size: Float, bold: Boolean = false) = Paint().apply {
        this.color = color
        textSize = size
        isAntiAlias = true
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }

    /** Capitalize each word for document titles/headings; preserve acronyms and *57. */
    private fun titleCase(s: String): String = s.split(" ").joinToString(" ") { w ->
        when {
            w.isEmpty() -> w
            // already an acronym / all-caps token (FCC, ID, DC) — leave it
            w.any { it.isLetter() } && w.filter { it.isLetter() }.all { it.isUpperCase() } -> w
            else -> w.replaceFirstChar { it.uppercaseChar() }
        }
    }

    // -- Shared content helpers --------------------------------------------

    private fun v(value: String, placeholder: String) = value.ifBlank { "[$placeholder]" }

    private fun dateRange(entries: List<CallEntry>): Pair<String, String> {
        if (entries.isEmpty()) return "[FIRST DATE]" to "[MOST RECENT DATE]"
        val first = entries.minOf { it.timestampMillis }
        val last = entries.maxOf { it.timestampMillis }
        return rangeFmt.format(Date(first)) to rangeFmt.format(Date(last))
    }

    private fun hourLabel(h: Int): String {
        val period = if (h < 12) "AM" else "PM"
        val hr = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return "$hr $period"
    }

    /** The 7/30/90/all table plus pattern metrics, shared by every evidence doc. */
    private fun statsSection(entries: List<CallEntry>): List<Block> {
        val extras = CallStats.extras(entries)
        val blocks = mutableListOf<Block>(
            Block.Heading("Call Statistics By Time Window"),
            Block.Table(
                listOf("Window", "Calls", "Flagged", "Numbers"),
                extras.windows.map {
                    listOf(it.label, it.total.toString(), it.flagged.toString(), it.uniqueNumbers.toString())
                },
            ),
            Block.Gap(4f),
        )
        extras.busiestHour?.let {
            blocks.add(Block.Body("Most calls arrive around ${hourLabel(it)} (${extras.busiestHourCount} calls in that hour)."))
        }
        if (extras.overnightCount > 0) {
            blocks.add(Block.Body("${extras.overnightCount} calls arrived overnight, between 10 PM and 6 AM."))
        }
        if (extras.avgPerDay > 0) {
            blocks.add(Block.Body("Average of ${String.format(Locale.US, "%.1f", extras.avgPerDay)} calls per day across the reporting period."))
        }
        return blocks
    }

    /** Wording that adapts to the kind of harassment being reported. */
    private fun patternSentence(profile: UserProfile, stats: CallStats): String {
        val n = stats.uniqueNumbers
        val spoof = "The use of $n different numbers is consistent with deliberate caller ID spoofing to harass and evade blocking."
        return when (profile.harassmentType) {
            HarassmentType.Aggressive ->
                "${stats.flaggedCalls} of these calls involve aggressive, abusive, or threatening conduct by the caller. Specific incidents, with dates and times, are documented in the attached incident timeline. $spoof"
            HarassmentType.Both ->
                "${stats.flaggedCalls} match a harassment pattern of silent or very short calls from numbers not in my contacts, and a number of the calls additionally involve aggressive or threatening conduct (documented with dates and times in the attached incident timeline). $spoof"
            HarassmentType.Silent ->
                "${stats.flaggedCalls} match a consistent harassment pattern: incoming calls from numbers not in my contacts on which the caller is silent and/or disconnects within seconds. $spoof"
        }
    }

    // -- Content builders ---------------------------------------------------

    private fun buildBlocks(
        type: DocumentType,
        profile: UserProfile,
        stats: CallStats,
        entries: List<CallEntry>,
    ): List<Block> = when (type) {
        DocumentType.FccComplaint -> fccComplaint(profile, stats, entries)
        DocumentType.PoliceReport -> policeReport(profile, stats, entries)
        DocumentType.CarrierScript -> carrierScript(profile, stats, entries)
        DocumentType.IncidentTimeline -> incidentTimeline(profile, entries)
        DocumentType.EvidenceSummary -> evidenceSummary(profile, stats, entries)
        DocumentType.CallTraceRecording -> callTraceRecording()
    }

    private fun fccComplaint(profile: UserProfile, stats: CallStats, entries: List<CallEntry>): List<Block> {
        val (first, last) = dateRange(entries)
        val name = v(profile.fullName, "YOUR FULL NAME")
        val phone = v(profile.phone, "YOUR CELL NUMBER")
        val blocks = mutableListOf<Block>(
            Block.Title("FCC Complaint — Caller ID Spoofing"),
            Block.Body("File online at consumercomplaints.fcc.gov → Phone → Unwanted Calls → issue type \"Caller ID Spoofing.\" Use the field notes below, then paste the description into the complaint's free-text box."),
            Block.Gap(6f),
            Block.Heading("Form Field Cheat-Sheet"),
            Block.Bullet("Your phone number: $phone"),
            Block.Bullet("Phone issue: Unwanted calls"),
            Block.Bullet("Sub-issue: Caller ID Spoofing"),
            Block.Bullet("Did you give consent? No"),
            Block.Bullet("Caller's number: Multiple / spoofed — ${stats.uniqueNumbers} different numbers (see description)"),
            Block.Bullet("Date(s) of calls: $first through $last"),
            Block.Bullet("Method: Phone call"),
        )
        blocks.addAll(statsSection(entries))
        blocks.add(Block.Heading("Description (Paste This)"))
        blocks.add(Block.Body("I am receiving a sustained campaign of harassing phone calls to my cell phone, $phone. Over the period $first to $last I have logged ${stats.totalCalls} calls from ${stats.uniqueNumbers} distinct phone numbers. ${patternSentence(profile, stats)}"))
        blocks.add(Block.Body("I did not consent to these calls. I am requesting FCC action against this illegal spoofing under the Truth in Caller ID Act and the TRACED Act."))
        blocks.add(Block.Body("Name: $name"))
        blocks.add(Block.Gap(10f))
        blocks.add(Block.Body("Generated by CallGuard on ${human.format(Date())}."))
        return blocks
    }

    private fun policeReport(profile: UserProfile, stats: CallStats, entries: List<CallEntry>): List<Block> {
        val (first, last) = dateRange(entries)
        val blocks = mutableListOf<Block>(
            Block.Title("Harassment — Police Report Cover Note"),
            Block.Body("Bring this to the police (in person is best) along with your CallGuard evidence summary and CSV. It states the facts plainly and cross-references your other filings so the file is self-contained."),
            Block.Gap(6f),
            Block.Heading("Complainant"),
            Block.Bullet("Date: ${human.format(Date())}"),
            Block.Bullet("Name: ${v(profile.fullName, "YOUR FULL NAME")}"),
            Block.Bullet("Contact: ${v(profile.phone, "YOUR PHONE")} · ${v(profile.email, "YOUR EMAIL")}"),
            Block.Bullet("Affected line: ${v(profile.phone, "YOUR CELL NUMBER")}"),
            Block.Bullet("Carrier: ${v(profile.carrier, "YOUR CARRIER")}"),
            Block.Bullet("Location: ${v(profile.addressCity, "CITY")}, ${v(profile.state, "ST")}"),
            Block.Heading("Nature Of Complaint"),
            Block.Body(
                if (profile.harassmentType.includesAggressive)
                    "Ongoing telephone harassment involving aggressive, abusive, or threatening calls, with caller ID spoofing used to evade blocking."
                else
                    "Ongoing telephone harassment via spoofed caller ID."
            ),
            Block.Heading("Summary Of Evidence"),
            Block.Body("Over the period $first to $last I have logged ${stats.totalCalls} calls from ${stats.uniqueNumbers} distinct phone numbers. ${patternSentence(profile, stats)}"),
        )
        blocks.addAll(statsSection(entries))
        if (profile.harassmentType.includesAggressive) {
            blocks.add(Block.Body("Specific threatening/abusive incidents are itemized in the attached CallGuard incident timeline, compiled from notes taken at the time of each call."))
        }
        blocks.add(Block.Heading("Cross-References"))
        blocks.add(Block.Bullet("FCC complaint number: ${v(profile.fccComplaintNumber, "FCC COMPLAINT #")}"))
        blocks.add(Block.Bullet("Carrier harassment case number: ${v(profile.carrierCaseNumber, "CARRIER CASE #")}"))
        blocks.add(Block.Heading("Request"))
        blocks.add(Block.Body("I am requesting a police report be filed so that a subpoena can be issued to my carrier for the true originating records of these calls (a traceback). The attached CSV and evidence summary document every call."))
        blocks.add(Block.Gap(10f))
        blocks.add(Block.Body("Generated by CallGuard on ${human.format(Date())}."))
        return blocks
    }

    private fun carrierScript(profile: UserProfile, stats: CallStats, entries: List<CallEntry>): List<Block> {
        val blocks = mutableListOf<Block>(
            Block.Title("Carrier Harassment Case — Call Script"),
            Block.Body("Call your carrier's fraud / harassment department (dial 611 from your phone, or use the customer-service number on your bill) and ask to open a documented harassment case."),
            Block.Gap(6f),
            Block.Heading("Word-For-Word Script"),
            Block.Body("\"I'm a ${v(profile.carrier, "CARRIER")} customer and I'm being harassed by repeated calls from different numbers that I believe are spoofed. I want to:"),
            Block.Bullet("Open a documented harassment case on my account."),
            Block.Bullet("Get a case / reference number for my records."),
            Block.Bullet("Turn on any free spam-blocking tools you offer."),
            Block.Bullet("Understand how the police can request a traceback of these calls.\""),
            Block.Heading("Write Down"),
            Block.Bullet("Case / reference number: ____________________  (save this in My info)"),
            Block.Bullet("Representative name and date: ____________________"),
            Block.Heading("Context To Give Them"),
            Block.Body("I have logged ${stats.totalCalls} calls from ${stats.uniqueNumbers} different numbers, ${stats.flaggedCalls} matching the harassment pattern. I am also filing an FCC complaint and a police report."),
        )
        blocks.addAll(statsSection(entries))
        blocks.add(Block.Gap(6f))
        blocks.add(Block.Body("Note: carrier tools block and document — they cannot reveal a spoofed caller to you directly. Only a police subpoena unmasks the origin."))
        blocks.add(Block.Gap(6f))
        blocks.add(Block.Body("Generated by CallGuard on ${human.format(Date())}."))
        return blocks
    }

    private fun incidentTimeline(profile: UserProfile, entries: List<CallEntry>): List<Block> {
        val documented = entries
            .filter { !it.note.isNullOrBlank() || it.severity != Severity.Unset }
            .sortedBy { it.timestampMillis }
        val blocks = mutableListOf<Block>(
            Block.Title("Harassment Incident Timeline"),
            Block.Body("Complainant: ${v(profile.fullName, "YOUR FULL NAME")} · ${v(profile.phone, "YOUR PHONE")}"),
            Block.Body("This is a chronological log of documented incidents, compiled from notes taken at or near the time of each call. It is intended to show the pattern of contact and any escalation of the harassment over time."),
            Block.Gap(4f),
        )
        if (documented.isEmpty()) {
            blocks.add(Block.Body("No incidents have been documented yet. To build this timeline, open the Call log, tap a harassing call, add a note describing what happened — for example \"silent for 30 seconds\", \"shouted threats\", or \"said he knew my address\" — and tag how serious it was (Silent / Spoken / Threatening). Each entry is timestamped to its call automatically, and they will appear here in order."))
            return blocks
        }

        val threatening = documented.count { it.severity == Severity.Threatening }
        val spoken = documented.count { it.severity == Severity.Spoken }
        val silent = documented.count { it.severity == Severity.Silent }
        blocks.add(Block.Heading("Documented Incidents (${documented.size})"))
        if (threatening + spoken + silent > 0) {
            blocks.add(Block.Body("Severity tags across these incidents: $threatening threatening, $spoken spoken, $silent silent."))
        }
        documented.forEach { e ->
            val who = e.cachedName?.takeIf { it.isNotBlank() } ?: e.number
            val tags = buildList {
                if (e.severity != Severity.Unset) add(e.severity.label)
                if (e.isSuspicious) add("flagged")
            }
            val tagStr = if (tags.isNotEmpty()) "  [${tags.joinToString(", ")}]" else ""
            blocks.add(Block.Bullet("${incidentFmt.format(Date(e.timestampMillis))} — $who$tagStr"))
            if (!e.note.isNullOrBlank()) blocks.add(Block.Body("     “${e.note}”"))
            blocks.add(Block.Gap(3f))
        }
        blocks.add(Block.Gap(8f))
        blocks.add(Block.Body("Earliest documented incident: ${incidentFmt.format(Date(documented.first().timestampMillis))}. Most recent: ${incidentFmt.format(Date(documented.last().timestampMillis))}."))
        blocks.add(Block.Body("Generated by CallGuard on ${human.format(Date())}."))
        return blocks
    }

    private fun evidenceSummary(profile: UserProfile, stats: CallStats, entries: List<CallEntry>): List<Block> {
        val (first, last) = dateRange(entries)
        val blocks = mutableListOf<Block>(
            Block.Title("CallGuard — Evidence Summary"),
            Block.Body("Complainant: ${v(profile.fullName, "YOUR FULL NAME")} · ${v(profile.phone, "YOUR PHONE")}"),
            Block.Body("Reporting period: $first to $last"),
            Block.Body("Generated: ${human.format(Date())}"),
        )
        blocks.addAll(statsSection(entries))
        blocks.add(Block.Heading("Totals"))
        blocks.add(Block.Bullet("Calls logged: ${stats.totalCalls}"))
        blocks.add(Block.Bullet("Flagged (harassment pattern): ${stats.flaggedCalls}"))
        blocks.add(Block.Bullet("Distinct numbers: ${stats.uniqueNumbers}"))
        blocks.add(Block.Bullet("Incoming: ${stats.incoming} · Missed: ${stats.missed} · Rejected: ${stats.rejected}"))
        blocks.add(Block.Heading("Top Numbers By Call Count"))
        stats.perNumber.take(10).forEach { n ->
            val nm = n.name?.takeIf { it.isNotBlank() } ?: n.number
            val flag = if (n.flaggedCount > 0) " — ${n.flaggedCount} flagged" else ""
            blocks.add(Block.Bullet("$nm: ${n.totalCount} calls$flag"))
        }
        blocks.add(Block.Gap(10f))
        blocks.add(Block.Body("This summary is generated from the device call log. A full per-call CSV is available via the Call log screen's Export."))
        return blocks
    }

    private fun callTraceRecording(): List<Block> = listOf(
        Block.Title("Call Trace (*57) And Recording For Records"),
        Block.Heading("Trace The Call: *57"),
        Block.Body("Immediately after a harassing call ends — before any other call comes in — dial *57 and press call. You will hear a confirmation tone or message. This tells your carrier to log the true originating line for that specific call, in a form law enforcement can subpoena. You will not see the result yourself; by design it goes to the carrier and police."),
        Block.Body("Notes: *57 usually carries a small per-use fee and must be done right after each call. It is most reliable on landlines; on wireless it may not be supported, in which case the police-report-to-subpoena route is what actually unmasks the caller."),
        Block.Bullet("*57 log — date/time of traced call: ____________________"),
        Block.Bullet("*57 log — date/time of traced call: ____________________"),
        Block.Bullet("*57 log — date/time of traced call: ____________________"),
        Block.Heading("Recording Calls For Your Records"),
        Block.Body("A recording of a threatening or abusive call can be powerful evidence — but call-recording law varies by state, and this matters legally:"),
        Block.Bullet("Some states allow recording if only ONE party (you) consents."),
        Block.Bullet("Other states require ALL parties on the call to consent. Recording without that consent can itself be a crime."),
        Block.Body("Before you record any call, confirm your own state's law — search \"[your state] call recording consent law\" or ask an attorney. Do not assume it is legal. If your state requires all-party consent and you cannot obtain it, rely on the call log, your written notes, and *57 instead."),
        Block.Body("If recording is lawful for you: use your phone's built-in call recording if available, or a separate recorder; save each file labeled with the date and time; never edit the audio; and keep copies in more than one place."),
        Block.Gap(10f),
        Block.Body("This page is general information, not legal advice. Generated by CallGuard on ${human.format(Date())}."),
    )
}
