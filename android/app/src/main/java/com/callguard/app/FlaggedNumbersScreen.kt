package com.callguard.app

import android.provider.CallLog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callguard.app.ui.CGCard
import com.callguard.app.ui.SectionHeader
import com.callguard.app.ui.SeverityBadge
import com.callguard.app.ui.StatTile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One flagged number with its rolled-up stats and the calls behind it. */
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
    val notedCalls: List<CallEntry>, // calls with a note or severity, chronological
    val allCalls: List<CallEntry>,   // every call from this number, newest first
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
                allCalls = calls.sortedByDescending { it.timestampMillis },
            )
        }
        .sortedWith(compareByDescending<FlaggedNumber> { it.threatening }.thenByDescending { it.flagged })

@Composable
fun FlaggedNumbersScreen(entries: List<CallEntry>) {
    val flagged = remember(entries) { flaggedNumbers(entries) }
    var selectedNumber by remember { mutableStateOf<String?>(null) }
    val selected = selectedNumber?.let { num -> flagged.firstOrNull { it.number == num } }

    if (selected != null) {
        BackHandler { selectedNumber = null }
        NumberDetail(selected, onBack = { selectedNumber = null })
    } else {
        FlaggedList(flagged, onOpen = { selectedNumber = it })
    }
}

@Composable
private fun FlaggedList(flagged: List<FlaggedNumber>, onOpen: (String) -> Unit) {
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
                "Numbers with at least one flagged call. Tap one for its stats, charts, and notes.",
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
                FlaggedListCard(fn) { onOpen(fn.number) }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun FlaggedListCard(fn: FlaggedNumber, onClick: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        fn.name?.takeIf { it.isNotBlank() } ?: fn.number,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "${fn.total} calls · ${fn.flagged} flagged · last ${fmt.format(Date(fn.lastSeen))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (fn.threatening + fn.spoken + fn.silent > 0 || fn.notedCalls.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (fn.threatening > 0) { CountBadge(Severity.Threatening, fn.threatening); Spacer(Modifier.width(6.dp)) }
                    if (fn.spoken > 0) { CountBadge(Severity.Spoken, fn.spoken); Spacer(Modifier.width(6.dp)) }
                    if (fn.silent > 0) { CountBadge(Severity.Silent, fn.silent); Spacer(Modifier.width(6.dp)) }
                    if (fn.notedCalls.isNotEmpty()) {
                        Text(
                            "${fn.notedCalls.size} note${if (fn.notedCalls.size == 1) "" else "s"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberDetail(fn: FlaggedNumber, onBack: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { onBack() }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(8.dp))
            Text("Flagged numbers", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(12.dp))

        Text(
            fn.name?.takeIf { it.isNotBlank() } ?: fn.number,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!fn.name.isNullOrBlank()) {
            Text(fn.number, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(fn.total.toString(), "calls", Modifier.weight(1f))
            StatTile(fn.flagged.toString(), "flagged", Modifier.weight(1f), highlight = fn.flagged > 0)
            StatTile(fn.notedCalls.size.toString(), "notes", Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))

        // Flagged vs normal pie
        val normal = (fn.total - fn.flagged).coerceAtLeast(0)
        val pie = listOf(
            Slice("Flagged", fn.flagged, Color(0xFFB00020)),
            Slice("Normal", normal, Color(0xFF1FBFA6)),
        )
        SectionHeader("Flagged vs. normal")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PieChart(pie, Modifier.size(130.dp))
            Spacer(Modifier.width(20.dp))
            ChartLegend(pie)
        }
        Spacer(Modifier.height(24.dp))

        // Call types bar
        val incoming = fn.allCalls.count { it.type == CallLog.Calls.INCOMING_TYPE }
        val missed = fn.allCalls.count { it.type == CallLog.Calls.MISSED_TYPE }
        val rejected = fn.allCalls.count { it.type == CallLog.Calls.REJECTED_TYPE }
        val typeBars = listOf(
            Bar("Incoming", incoming, Color(0xFF185FA5)),
            Bar("Missed", missed, Color(0xFFBA7517)),
            Bar("Rejected", rejected, Color(0xFF534AB7)),
        ).filter { it.value > 0 }
        if (typeBars.isNotEmpty()) {
            SectionHeader("Call types")
            Spacer(Modifier.height(8.dp))
            BarChart(typeBars)
            Spacer(Modifier.height(24.dp))
        }

        // Severity
        if (fn.threatening + fn.spoken + fn.silent > 0) {
            SectionHeader("Severity")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (fn.threatening > 0) { CountBadge(Severity.Threatening, fn.threatening); Spacer(Modifier.width(10.dp)) }
                if (fn.spoken > 0) { CountBadge(Severity.Spoken, fn.spoken); Spacer(Modifier.width(10.dp)) }
                if (fn.silent > 0) { CountBadge(Severity.Silent, fn.silent) }
            }
            Spacer(Modifier.height(24.dp))
        }

        SectionHeader("Seen")
        Spacer(Modifier.height(8.dp))
        Text("First: ${fmt.format(Date(fn.firstSeen))}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Last:  ${fmt.format(Date(fn.lastSeen))}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        // Notes
        if (fn.notedCalls.isNotEmpty()) {
            SectionHeader("Notes (${fn.notedCalls.size})")
            Spacer(Modifier.height(4.dp))
            fn.notedCalls.forEach { call ->
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
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
                    Text(note, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
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
