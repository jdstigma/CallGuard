package com.callguard.app

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes the call log to a CSV in the phone's public Downloads folder so you can
 * hand it to your carrier / the police. Returns a human-readable location.
 *
 * Requires the app's minSdk to be 29 (Android 10) or higher, which uses the
 * modern MediaStore API and needs no storage permission.
 */
object CsvExporter {

    private val stamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
    private val rowTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun export(context: Context, entries: List<CallEntry>): String {
        val fileName = "CallGuard_evidence_${stamp.format(Date())}.csv"
        val csv = buildString {
            append("Timestamp,Number,ContactName,Type,DurationSeconds,Suspicious,Note\n")
            entries.forEach { e ->
                append(rowTime.format(Date(e.timestampMillis))).append(',')
                append(csvCell(e.number)).append(',')
                append(csvCell(e.cachedName ?: "")).append(',')
                append(e.typeLabel).append(',')
                append(e.durationSeconds).append(',')
                append(if (e.isSuspicious) "YES" else "").append(',')
                append(csvCell(e.note ?: "")).append('\n')
            }
        }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return "Failed to create file"
        resolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
        return "Downloads/$fileName"
    }

    private fun csvCell(value: String): String {
        val needsQuote = value.contains(',') || value.contains('"') || value.contains('\n')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }
}
