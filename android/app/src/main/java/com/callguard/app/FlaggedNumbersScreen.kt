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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

/**
 * One row on the flagged list: either a single flagged number, or a named
 * branch — several numbers grouped under one caller identity.
 */
private data class FlaggedGroup(
    val key: String,            // branch name, or the number itself when ungrouped
    val label: String,          // what we show: branch name / contact name / number
    val isBranch: Boolean,
    val numbers: List<String>,  // member numbers (size 1 unless a branch)
    val total: Int,
    val flagged: Int,
    val firstSeen: Long,
    val lastSeen: Long,
    val threatening: Int,
    val spoken: Int,
    val silent: Int,
    val notedCalls: List<CallEntry>,
    val allCalls: List<CallEntry>,
)

private fun buildGroups(entries: List<CallEntry>, branches: Map<String, String>): List<FlaggedGroup> {
    // Group calls by branch name when assigned, else by the number itself.
    val byKey = entries.groupBy { branches[it.number] ?: it.number }
    return byKey
        .filter { (_, calls) -> calls.any { it.isSuspicious } }
        .map { (key, calls) ->
            val isBranch = branches.containsValue(key) && calls.any { branches[it.number] == key }
            val numbers = calls.map { it.number }.distinct()
            val contactName = calls.firstNotNullOfOrNull { it.cachedName?.takeIf { n -> n.isNotBlank() } }
            FlaggedGroup(
                key = key,
                label = if (isBranch) key else (contactName ?: key),
                isBranch = isBranch,
                numbers = numbers,
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
        .sortedWith(compareByDescending<FlaggedGroup> { it.threatening }.thenByDescending { it.flagged })
}

@Composable
fun FlaggedNumbersScreen(entries: List<CallEntry>) {
    val context = LocalContext.current
    var branchVersion by remember { mutableStateOf(0) } // bump to re-read the store
    val branches = remember(branchVersion) { BranchStore.all(context) }
    val groups = remember(entries, branches) { buildGroups(entries, branches) }

    var selectedKey by remember { mutableStateOf<String?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(setOf<String>()) }
    var showNameDialog by remember { mutableStateOf(false) }

    val selected = selectedKey?.let { k -> groups.firstOrNull { it.key == k } }

    if (selected != null) {
        BackHandler { selectedKey = null }
        GroupDetail(
            g = selected,
            onBack = { selectedKey = null },
            onUngroup = {
                BranchStore.removeBranch(context, selected.key)
                branchVersion++
                selectedKey = null
            },
        )
        return
    }

    if (selectionMode) BackHandler { selectionMode = false; checked = emptySet() }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Flagged numbers",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = {
                    selectionMode = !selectionMode
                    checked = emptySet()
                }) {
                    Text(if (selectionMode) "Cancel" else "Group")
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                if (selectionMode)
                    "Check the numbers that are really the same caller, then name the group."
                else
                    "Tap a number for stats, charts, and notes. Use Group to file several " +
                        "spoofed numbers under one caller name.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            if (selectionMode) {
                Button(
                    onClick = { if (checked.isNotEmpty()) showNameDialog = true },
                    enabled = checked.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        if (checked.isEmpty()) "Select numbers to group"
                        else "Group ${checked.size} under one caller…"
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        if (groups.isEmpty()) {
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
            items(groups, key = { it.key }) { g ->
                GroupListCard(
                    g = g,
                    selectionMode = selectionMode,
                    checked = g.key in checked,
                    onToggleCheck = {
                        checked = if (g.key in checked) checked - g.key else checked + g.key
                    },
                    onOpen = { selectedKey = g.key },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    if (showNameDialog) {
        // Prefill with a contact name from the selection, else an existing branch name.
        val selectedGroups = groups.filter { it.key in checked }
        val prefill = selectedGroups.firstOrNull { it.isBranch }?.key
            ?: selectedGroups.firstNotNullOfOrNull { g ->
                g.allCalls.firstNotNullOfOrNull { it.cachedName?.takeIf { n -> n.isNotBlank() } }
            } ?: ""
        GroupNameDialog(
            initial = prefill,
            count = selectedGroups.sumOf { it.numbers.size },
            onDismiss = { showNameDialog = false },
            onConfirm = { name ->
                BranchStore.assign(context, selectedGroups.flatMap { it.numbers }, name)
                branchVersion++
                showNameDialog = false
                selectionMode = false
                checked = emptySet()
            },
        )
    }
}

@Composable
private fun GroupNameDialog(
    initial: String,
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name this caller") },
        text = {
            Column {
                Text(
                    "$count number${if (count == 1) "" else "s"} will be filed under this caller name. " +
                        "Documents will report them as one identity.",
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("e.g. Unknown Night Caller") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text("Group") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GroupListCard(
    g: FlaggedGroup,
    selectionMode: Boolean,
    checked: Boolean,
    onToggleCheck: () -> Unit,
    onOpen: () -> Unit,
) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            if (selectionMode) onToggleCheck() else onOpen()
        },
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
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (selectionMode) {
                        Checkbox(checked = checked, onCheckedChange = { onToggleCheck() })
                        Spacer(Modifier.width(4.dp))
                    }
                    if (g.isBranch) {
                        Icon(
                            Icons.Filled.Folder,
                            contentDescription = "Caller group",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            g.label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            (if (g.isBranch) "${g.numbers.size} numbers · " else "") +
                                "${g.total} calls · ${g.flagged} flagged · last ${fmt.format(Date(g.lastSeen))}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!selectionMode) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (g.threatening + g.spoken + g.silent > 0 || g.notedCalls.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (g.threatening > 0) { CountBadge(Severity.Threatening, g.threatening); Spacer(Modifier.width(6.dp)) }
                    if (g.spoken > 0) { CountBadge(Severity.Spoken, g.spoken); Spacer(Modifier.width(6.dp)) }
                    if (g.silent > 0) { CountBadge(Severity.Silent, g.silent); Spacer(Modifier.width(6.dp)) }
                    if (g.notedCalls.isNotEmpty()) {
                        Text(
                            "${g.notedCalls.size} note${if (g.notedCalls.size == 1) "" else "s"}",
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
private fun GroupDetail(g: FlaggedGroup, onBack: () -> Unit, onUngroup: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US) }
    var confirmUngroup by remember { mutableStateOf(false) }

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

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (g.isBranch) {
                Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                g.label,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (g.isBranch) {
            Text(
                "Caller group · ${g.numbers.size} numbers",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (g.label != g.key) {
            Text(g.key, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(g.total.toString(), "calls", Modifier.weight(1f))
            StatTile(g.flagged.toString(), "flagged", Modifier.weight(1f), highlight = g.flagged > 0)
            StatTile(
                (if (g.isBranch) g.numbers.size else g.notedCalls.size).toString(),
                if (g.isBranch) "numbers" else "notes",
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(24.dp))

        val normal = (g.total - g.flagged).coerceAtLeast(0)
        val pie = listOf(
            Slice("Flagged", g.flagged, Color(0xFFB00020)),
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

        val incoming = g.allCalls.count { it.type == CallLog.Calls.INCOMING_TYPE }
        val missed = g.allCalls.count { it.type == CallLog.Calls.MISSED_TYPE }
        val rejected = g.allCalls.count { it.type == CallLog.Calls.REJECTED_TYPE }
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

        if (g.threatening + g.spoken + g.silent > 0) {
            SectionHeader("Severity")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (g.threatening > 0) { CountBadge(Severity.Threatening, g.threatening); Spacer(Modifier.width(10.dp)) }
                if (g.spoken > 0) { CountBadge(Severity.Spoken, g.spoken); Spacer(Modifier.width(10.dp)) }
                if (g.silent > 0) { CountBadge(Severity.Silent, g.silent) }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (g.isBranch) {
            SectionHeader("Numbers in this group")
            Spacer(Modifier.height(8.dp))
            CGCard {
                g.numbers.forEachIndexed { i, n ->
                    if (i > 0) Spacer(Modifier.height(6.dp))
                    val calls = g.allCalls.count { it.number == n }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(n, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "$calls call${if (calls == 1) "" else "s"}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { confirmUngroup = true }, shape = RoundedCornerShape(10.dp)) {
                Text("Ungroup these numbers")
            }
            Spacer(Modifier.height(24.dp))
        }

        SectionHeader("Seen")
        Spacer(Modifier.height(8.dp))
        Text("First: ${fmt.format(Date(g.firstSeen))}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Last:  ${fmt.format(Date(g.lastSeen))}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        if (g.notedCalls.isNotEmpty()) {
            SectionHeader("Notes (${g.notedCalls.size})")
            Spacer(Modifier.height(4.dp))
            g.notedCalls.forEach { call ->
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        fmt.format(Date(call.timestampMillis)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (g.isBranch) {
                        Spacer(Modifier.width(8.dp))
                        Text(call.number, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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

    if (confirmUngroup) {
        AlertDialog(
            onDismissRequest = { confirmUngroup = false },
            title = { Text("Ungroup “${g.label}”?") },
            text = { Text("The ${g.numbers.size} numbers go back to standing alone. Notes and tags are kept.") },
            confirmButton = {
                TextButton(onClick = { confirmUngroup = false; onUngroup() }) { Text("Ungroup") }
            },
            dismissButton = { TextButton(onClick = { confirmUngroup = false }) { Text("Cancel") } },
        )
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
