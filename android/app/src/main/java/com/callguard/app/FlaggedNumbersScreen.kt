package com.callguard.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callguard.app.ui.CGCard
import com.callguard.app.ui.SeverityBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One flagged number with its rolled-up stats and the notes taken on its calls. */
private data class FlaggedNumber(
    val number: String,
    val name: String?,
    val total: Int,
    val flagged: Int,
    val firstSeen: Long,
    val lastSeen: Long,
    val threatening: Int,
    val spoken: Int,
    val silent: Int,
    val notedCalls: List<CallEntry>, // calls with a note or severity tag, chronological
)

private fun flaggedNumbers(entries: List<CallEntry>): List<FlaggedNumber> =
    entries.groupBy { it.number }
        .filter { (_, calls) -> calls.any { it.isSuspicious } }
        .map { (number, calls) ->
            FlaggedNumber(
                number = number,
                name = calls.firstNotNullOfOrNull { it.cachedName?.takeIf { n -> n.isNotBlank() } },
                total = calls.size,
                flagged = calls.count { it.isSuspicious },
                firstSeen = calls.minOf { it.timestampMillis },
                lastSeen = calls.maxOf { it.timestampMillis },
                threatening = calls.count { it.severity == Severity.Threatening },
                spoken = calls.count { it.severity == Severity.Spoken },
                silent = calls.count { it.severity == Severity.Silent },
                notedCalls = calls
                    .filter { !it.note.isNullOrBlank() || it.severity != Severity.Unset }
                    .sortedBy { it.timestampMillis },
            )
        }
        .sortedWith(compareByDescending<FlaggedNumber> { it.threatening }.thenByDescending { it.flagged })

@Composable
fun FlaggedNumbersScreen(entries: List<CallEntry>) {
    val flagged = flaggedNumbers(entries)

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(
                "Flagged numbers",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Only numbers with at least one flagged call, with their stats and every note " +
                    "you've taken on them. Sorted by threatening tags, then flagged count.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }

        if (flagged.isEmpty()) {
            item {
                CGCard {
                    Text(
                        "No flagged numbers yet",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "A number appears here once it has a call matching the harassment pattern " +
                            "(a short/silent call from a number not in your contacts).",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(flagged) { fn ->
                FlaggedNumberCard(fn)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun FlaggedNumberCard(fn: FlaggedNumber) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US) }
    CGCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                fn.name?.takeIf { it.isNotBlank() } ?: fn.number,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "${fn.flagged} flagged",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${fn.total} total calls · first ${fmt.format(Date(fn.firstSeen))} · last ${fmt.format(Date(fn.lastSeen))}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Severity summary badges
        if (fn.threatening + fn.spoken + fn.silent > 0) {
            Spacer(Modifier.height(8.dp))
            Row {
                if (fn.threatening > 0) { CountBadge(Severity.Threatening, fn.threatening); Spacer(Modifier.width(6.dp)) }
                if (fn.spoken > 0) { CountBadge(Severity.Spoken, fn.spoken); Spacer(Modifier.width(6.dp)) }
                if (fn.silent > 0) { CountBadge(Severity.Silent, fn.silent) }
            }
        }

        // Notes on this number
        if (fn.notedCalls.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            Text(
                "Notes (${fn.notedCalls.size})",
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            fn.notedCalls.forEach { call ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        fmt.format(Date(call.timestampMillis)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (call.severity != Severity.Unset) {
                        Spacer(Modifier.width(8.dp))
                        SeverityBadge(call.severity)
                    }
                }
                call.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        note,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CountBadge(severity: Severity, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SeverityBadge(severity)
        Spacer(Modifier.width(4.dp))
        Text("×$count", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
