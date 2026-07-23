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
    EvidenceSummary(
        "Evidence summary",
        "evidence_summary",
        "One-page snapshot of your call statistics to attach to any filing.",
    ),
}

/** A block of content in a generated document. */
private sealed interface Block {
    data class Title(val text: String) : Block
    data class Heading(val text: String) : Block
    data class Body(val text: String) : Block
    data class Bullet(val text: String) : Block
    data class Gap(val points: Float) : Block
}

object DocumentGenerator {

    private val stamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
    private val human = SimpleDateFormat("MMMM d, yyyy", Locale.US)
    private val rangeFmt = SimpleDateFormat("MMM d, yyyy", Locale.US)

    // US Letter at 72 dpi.
    private const val PAGE_W = 612
    private const val PAGE_H = 792
    private const val MARGIN = 54f

    /**
     * Builds the PDF, writes it to the public Downloads folder, and returns its
     * content Uri (shareable) plus a human-readable path. Null uri on failure.
     */
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
                val lines = wrap(rawLine, p, avail)
                lines.forEach { line ->
                    ensure(lineGap)
                    canvas.drawText(line, MARGIN + indent, y + p.textSize, p)
                    y += lineGap
                }
            }
        }

        blocks.forEach { block ->
            when (block) {
                is Block.Title -> { drawWrapped(block.text, title, 26f); y += 6f }
                is Block.Heading -> { ensure(24f); y += 8f; drawWrapped(block.text, heading, 18f) }
                is Block.Body -> drawWrapped(block.text, body, 16f)
                is Block.Bullet -> {
                    ensure(16f)
                    canvas.drawText("•", MARGIN, y + body.textSize, body)
                    drawWrapped(block.text, body, 16f, indent = 16f)
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

    // -- Content builders ---------------------------------------------------

    private fun v(value: String, placeholder: String) =
        value.ifBlank { "[$placeholder]" }

    private fun dateRange(entries: List<CallEntry>): Pair<String, String> {
        if (entries.isEmpty()) return "[FIRST DATE]" to "[MOST RECENT DATE]"
        val first = entries.minOf { it.timestampMillis }
        val last = entries.maxOf { it.timestampMillis }
        return rangeFmt.format(Date(first)) to rangeFmt.format(Date(last))
    }

    private fun buildBlocks(
        type: DocumentType,
        profile: UserProfile,
        stats: CallStats,
        entries: List<CallEntry>,
    ): List<Block> = when (type) {
        DocumentType.FccComplaint -> fccComplaint(profile, stats, entries)
        DocumentType.PoliceReport -> policeReport(profile, stats, entries)
        DocumentType.CarrierScript -> carrierScript(profile, stats)
        DocumentType.EvidenceSummary -> evidenceSummary(profile, stats, entries)
    }

    private fun fccComplaint(profile: UserProfile, stats: CallStats, entries: List<CallEntry>): List<Block> {
        val (first, last) = dateRange(entries)
        val name = v(profile.fullName, "YOUR FULL NAME")
        val phone = v(profile.phone, "YOUR CELL NUMBER")
        return listOf(
            Block.Title("FCC complaint — caller ID spoofing"),
            Block.Body("File online at consumercomplaints.fcc.gov → Phone → Unwanted Calls → issue type \"Caller ID Spoofing.\" Use the field notes below, then paste the description into the complaint's free-text box."),
            Block.Gap(6f),
            Block.Heading("Form field cheat-sheet"),
            Block.Bullet("Your phone number: $phone"),
            Block.Bullet("Phone issue: Unwanted calls"),
            Block.Bullet("Sub-issue: Caller ID Spoofing"),
            Block.Bullet("Did you give consent? No"),
            Block.Bullet("Caller's number: Multiple / spoofed — ${stats.uniqueNumbers} different numbers (see description)"),
            Block.Bullet("Date(s) of calls: $first through $last"),
            Block.Bullet("Method: Phone call"),
            Block.Heading("Description (paste this)"),
            Block.Body("I am receiving a sustained campaign of harassing phone calls to my cell phone, $phone. Over the period $first to $last I have logged ${stats.totalCalls} calls from ${stats.uniqueNumbers} distinct phone numbers. ${stats.flaggedCalls} of these match a consistent harassment pattern: incoming calls from numbers not in my contacts on which the caller is silent and/or disconnects within seconds."),
            Block.Body("The use of ${stats.uniqueNumbers} different numbers is consistent with deliberate caller ID spoofing to harass and evade blocking. I did not consent to these calls. I am requesting FCC action against this illegal spoofing under the Truth in Caller ID Act and the TRACED Act."),
            Block.Body("Name: $name"),
            Block.Gap(10f),
            Block.Body("Generated by CallGuard on ${human.format(Date())}."),
        )
    }

    private fun policeReport(profile: UserProfile, stats: CallStats, entries: List<CallEntry>): List<Block> {
        val (first, last) = dateRange(entries)
        return listOf(
            Block.Title("Harassment — police report cover note"),
            Block.Body("Bring this to the police (in person is best) along with your CallGuard evidence summary and CSV. It states the facts plainly and cross-references your other filings so the file is self-contained."),
            Block.Gap(6f),
            Block.Heading("Complainant"),
            Block.Bullet("Date: ${human.format(Date())}"),
            Block.Bullet("Name: ${v(profile.fullName, "YOUR FULL NAME")}"),
            Block.Bullet("Contact: ${v(profile.phone, "YOUR PHONE")} · ${v(profile.email, "YOUR EMAIL")}"),
            Block.Bullet("Affected line: ${v(profile.phone, "YOUR CELL NUMBER")}"),
            Block.Bullet("Carrier: ${v(profile.carrier, "YOUR CARRIER")}"),
            Block.Bullet("Location: ${v(profile.addressCity, "CITY")}, ${v(profile.state, "ST")}"),
            Block.Heading("Nature of complaint"),
            Block.Body("Ongoing telephone harassment via spoofed caller ID."),
            Block.Heading("Summary of evidence"),
            Block.Body("Over the period $first to $last I have logged ${stats.totalCalls} calls from ${stats.uniqueNumbers} distinct phone numbers. ${stats.flaggedCalls} match a consistent harassment pattern: incoming calls from numbers not in my contacts on which the caller is silent and/or disconnects within seconds. The use of ${stats.uniqueNumbers} different numbers is consistent with deliberate caller ID spoofing to harass and evade blocking."),
            Block.Heading("Cross-references"),
            Block.Bullet("FCC complaint number: ${v(profile.fccComplaintNumber, "FCC COMPLAINT #")}"),
            Block.Bullet("Carrier harassment case number: ${v(profile.carrierCaseNumber, "CARRIER CASE #")}"),
            Block.Heading("Request"),
            Block.Body("I am requesting a police report be filed so that a subpoena can be issued to my carrier for the true originating records of these calls (a traceback). The attached CSV and evidence summary document every call."),
            Block.Gap(10f),
            Block.Body("Generated by CallGuard on ${human.format(Date())}."),
        )
    }

    private fun carrierScript(profile: UserProfile, stats: CallStats): List<Block> {
        return listOf(
            Block.Title("Carrier harassment case — call script"),
            Block.Body("Call your carrier's fraud / harassment department (dial 611 from your phone, or use the customer-service number on your bill) and ask to open a documented harassment case."),
            Block.Gap(6f),
            Block.Heading("Word-for-word script"),
            Block.Body("\"I'm a ${v(profile.carrier, "CARRIER")} customer and I'm being harassed by repeated calls. Each call comes from a different number and the caller stays silent — I believe the numbers are spoofed. I want to:"),
            Block.Bullet("Open a documented harassment case on my account."),
            Block.Bullet("Get a case / reference number for my records."),
            Block.Bullet("Turn on any free spam-blocking tools you offer."),
            Block.Bullet("Understand how the police can request a traceback of these calls.\""),
            Block.Heading("Write down"),
            Block.Bullet("Case / reference number: ____________________  (save this in My info)"),
            Block.Bullet("Representative name and date: ____________________"),
            Block.Heading("Context to give them"),
            Block.Body("I have logged ${stats.totalCalls} calls from ${stats.uniqueNumbers} different numbers, ${stats.flaggedCalls} matching the silent-caller harassment pattern. I am also filing an FCC complaint and a police report."),
            Block.Gap(10f),
            Block.Body("Note: carrier tools block and document — they cannot reveal a spoofed caller to you directly. Only a police subpoena unmasks the origin."),
            Block.Gap(6f),
            Block.Body("Generated by CallGuard on ${human.format(Date())}."),
        )
    }

    private fun evidenceSummary(profile: UserProfile, stats: CallStats, entries: List<CallEntry>): List<Block> {
        val (first, last) = dateRange(entries)
        val blocks = mutableListOf<Block>(
            Block.Title("CallGuard — evidence summary"),
            Block.Body("Complainant: ${v(profile.fullName, "YOUR FULL NAME")} · ${v(profile.phone, "YOUR PHONE")}"),
            Block.Body("Reporting period: $first to $last"),
            Block.Body("Generated: ${human.format(Date())}"),
            Block.Heading("Totals"),
            Block.Bullet("Calls logged: ${stats.totalCalls}"),
            Block.Bullet("Flagged (silent-stranger pattern): ${stats.flaggedCalls}"),
            Block.Bullet("Distinct numbers: ${stats.uniqueNumbers}"),
            Block.Bullet("Incoming: ${stats.incoming} · Missed: ${stats.missed} · Rejected: ${stats.rejected}"),
            Block.Heading("Top numbers by call count"),
        )
        stats.perNumber.take(10).forEach { n ->
            val nm = n.name?.takeIf { it.isNotBlank() } ?: n.number
            val flag = if (n.flaggedCount > 0) " — ${n.flaggedCount} flagged" else ""
            blocks.add(Block.Bullet("$nm: ${n.totalCount} calls$flag"))
        }
        blocks.add(Block.Gap(10f))
        blocks.add(Block.Body("This summary is generated from the device call log. A full per-call CSV is available via the Call log screen's Export."))
        return blocks
    }
}
