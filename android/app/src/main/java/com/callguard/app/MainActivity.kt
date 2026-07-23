package com.callguard.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CallGuard — reads the system call log, flags the silent-stranger pattern,
 * shows a per-number profile, and exports a CSV for your carrier / the police.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { CallGuardScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallGuardScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    var entries by remember { mutableStateOf<List<CallEntry>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
        if (isGranted) entries = CallLogRepository.readAll(context)
    }

    LaunchedEffect(granted) {
        if (granted) entries = CallLogRepository.readAll(context)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("CallGuard") }) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (!granted) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "CallGuard needs permission to read your call log so it can " +
                            "build an evidence record of the harassing calls.",
                        Modifier.padding(bottom = 12.dp)
                    )
                    Button(onClick = {
                        permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                    }) { Text("Grant call log access") }
                }
                return@Column
            }

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Calls") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Profile") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Guide") }
                )
            }

            when (selectedTab) {
                0 -> CallsTab(
                    entries = entries,
                    onRefresh = { entries = CallLogRepository.readAll(context) },
                    onExport = {
                        val where = CsvExporter.export(context, entries)
                        Toast.makeText(context, "Saved: $where", Toast.LENGTH_LONG).show()
                    }
                )
                1 -> ProfileTab(entries = entries)
                2 -> HelpTab()
            }
        }
    }
}

@Composable
private fun CallsTab(
    entries: List<CallEntry>,
    onRefresh: () -> Unit,
    onExport: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var editing by remember { mutableStateOf<CallEntry?>(null) }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        val suspicious = entries.count { it.isSuspicious }
        Text(
            "${entries.size} calls logged · $suspicious flagged as suspicious",
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = onRefresh) { Text("Refresh") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onExport) { Text("Export CSV") }
        }
        Text(
            "Tip: tap a call to add a note",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize()) {
            items(entries) { entry -> CallRow(entry) { editing = entry } }
        }
    }

    editing?.let { entry ->
        NoteDialog(
            entry = entry,
            onDismiss = { editing = null },
            onSave = { text ->
                NotesStore.set(context, entry.id, text)
                editing = null
                onRefresh() // reload so the note shows and rides along in the CSV
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteDialog(
    entry: CallEntry,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(entry.note ?: "") }
    val who = entry.cachedName?.takeIf { it.isNotBlank() } ?: entry.number
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note for $who") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("e.g. silent, hung up after 20s") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ProfileTab(entries: List<CallEntry>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var rangeDays by remember { mutableIntStateOf(0) } // 0 = all time

    val filtered = remember(entries, rangeDays) {
        if (rangeDays == 0) entries
        else {
            val cutoff = System.currentTimeMillis() - rangeDays.toLong() * 24 * 60 * 60 * 1000
            entries.filter { it.timestampMillis >= cutoff }
        }
    }
    val stats = CallStats.from(filtered)
    val rangeLabel = when (rangeDays) {
        7 -> "Last 7 days"
        30 -> "Last 30 days"
        90 -> "Last 90 days"
        else -> "All time"
    }

    val flaggedColor = Color(0xFFB00020)
    val normalColor = Color(0xFF2E7D32)

    val pieSlices = listOf(
        Slice("Flagged", stats.flaggedCalls, flaggedColor),
        Slice("Normal", (stats.totalCalls - stats.flaggedCalls).coerceAtLeast(0), normalColor),
    )
    val typeBars = listOf(
        Bar("Incoming", stats.incoming, Color(0xFF1565C0)),
        Bar("Missed", stats.missed, Color(0xFFEF6C00)),
        Bar("Rejected", stats.rejected, Color(0xFF6A1B9A)),
    )
    val topBars = stats.perNumber.take(5).map {
        Bar(
            label = it.name?.takeIf { n -> n.isNotBlank() } ?: it.number,
            value = it.totalCount,
            color = if (it.flaggedCount > 0) flaggedColor else Color(0xFF1565C0),
        )
    }

    LazyColumn(Modifier.padding(16.dp).fillMaxSize()) {
        item {
            // --- Date range selector ---
            Text("Date range", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Row {
                RangeButton("All", rangeDays == 0) { rangeDays = 0 }
                RangeButton("7d", rangeDays == 7) { rangeDays = 7 }
                RangeButton("30d", rangeDays == 30) { rangeDays = 30 }
                RangeButton("90d", rangeDays == 90) { rangeDays = 90 }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                val uri = ChartImageExporter.export(context, stats, rangeLabel)
                if (uri != null) {
                    ChartImageExporter.share(context, uri)
                    Toast.makeText(context, "Saved to Pictures — opening share…", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Image export failed", Toast.LENGTH_LONG).show()
                }
            }) { Text("Export / share charts (PNG)") }
            Spacer(Modifier.height(20.dp))

            // --- Pie: flagged vs normal ---
            Text("Flagged vs. normal calls", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PieChart(pieSlices, Modifier.size(140.dp))
                Spacer(Modifier.width(20.dp))
                ChartLegend(pieSlices)
            }
            Spacer(Modifier.height(20.dp))

            // --- Bar: call types ---
            Text("Call types", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            BarChart(typeBars)
            Spacer(Modifier.height(20.dp))

            // --- Bar: top numbers by call count (red = has flags) ---
            Text("Top numbers by call count", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            if (topBars.isEmpty()) Text("No calls yet.") else BarChart(topBars)
            Spacer(Modifier.height(24.dp))

            // --- Summary ---
            Text("Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            StatLine("Total calls", stats.totalCalls.toString())
            StatLine("Flagged as suspicious", stats.flaggedCalls.toString(), highlight = stats.flaggedCalls > 0)
            StatLine("Unique numbers", stats.uniqueNumbers.toString())
            StatLine("Incoming", stats.incoming.toString())
            StatLine("Missed", stats.missed.toString())
            StatLine("Rejected", stats.rejected.toString())
            Spacer(Modifier.height(16.dp))

            Text("By number (most calls first)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
        }
        items(stats.perNumber) { n -> NumberStatRow(n) }
    }
}

@Composable
private fun HelpTab() {
    LazyColumn(Modifier.padding(16.dp).fillMaxSize()) {
        item {
            HelpSection(
                "What CallGuard does",
                "Reads your phone's call log, flags the “silent stranger” pattern " +
                    "(incoming calls from numbers not in your contacts that last ≤15 " +
                    "seconds), and lets you export everything as evidence."
            )
            HelpSection(
                "Calls tab",
                "• Every call, newest first. Flagged ones are highlighted red.\n" +
                    "• Tap any call to add a note (e.g. “silent, hung up after 20s”). " +
                    "Notes stick to the call and ride along in the export.\n" +
                    "• Refresh re-reads the call log.\n" +
                    "• Export CSV saves a file to your Downloads folder."
            )
            HelpSection(
                "Profile tab",
                "• Pick a date range (All / 7d / 30d / 90d).\n" +
                    "• Pie: flagged vs. normal. Bars: call types and your top " +
                    "numbers (red = has flagged calls).\n" +
                    "• Per-number list shows total calls, flagged count, and the " +
                    "first/last time each number reached you.\n" +
                    "• Export / share charts (PNG) saves an image to Pictures and " +
                    "opens the share sheet."
            )
            HelpSection(
                "Getting evidence off the phone",
                "1. On the Calls tab, tap Export CSV.\n" +
                    "2. Copy that file from Downloads to your PC.\n" +
                    "3. On the PC, open the CallGuard Control Panel and use “Make " +
                    "charts + PDF” to build the one-page evidence summary."
            )
            HelpSection(
                "Important limit",
                "This app documents calls — it cannot reveal who is really calling " +
                    "when the number is spoofed. Only your phone carrier and the police " +
                    "can unmask that, via a traceback / subpoena. The CSV and PDF are the " +
                    "evidence that gets them to act. See the FCC complaint and police " +
                    "cover-note documents in the project."
            )
        }
    }
}

@Composable
private fun HelpSection(title: String, body: String) {
    Column(Modifier.padding(bottom = 16.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(4.dp))
        Text(body, fontSize = 14.sp, color = Color(0xFF333333))
    }
}

@Composable
private fun RangeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val mod = Modifier.padding(end = 6.dp)
    if (selected) {
        Button(onClick = onClick, modifier = mod) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = mod) { Text(label) }
    }
}

@Composable
private fun StatLine(label: String, value: String, highlight: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) Color(0xFFB00020) else Color.Unspecified
        )
    }
}

@Composable
private fun NumberStatRow(n: NumberStat) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.US) }
    val bg = if (n.flaggedCount > 0) Color(0xFFFFE0E0) else Color.Transparent
    Column(
        Modifier.fillMaxWidth().background(bg).padding(vertical = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                n.name?.takeIf { it.isNotBlank() } ?: n.number,
                fontWeight = FontWeight.SemiBold
            )
            Text("${n.totalCount} calls", fontWeight = FontWeight.Medium)
        }
        if (n.flaggedCount > 0) {
            Text(
                "⚠ ${n.flaggedCount} flagged",
                fontSize = 13.sp,
                color = Color(0xFFB00020),
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            "First: ${fmt.format(Date(n.firstSeenMillis))}",
            fontSize = 13.sp,
            color = Color.Gray
        )
        Text(
            "Last:  ${fmt.format(Date(n.lastSeenMillis))}",
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun CallRow(entry: CallEntry, onClick: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM d, HH:mm:ss", Locale.US) }
    val bg = if (entry.isSuspicious) Color(0xFFFFE0E0) else Color.Transparent
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(bg)
            .padding(vertical = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                entry.cachedName?.takeIf { it.isNotBlank() } ?: entry.number,
                fontWeight = FontWeight.SemiBold
            )
            if (entry.isSuspicious) {
                Text("⚠ flagged", color = Color(0xFFB00020), fontSize = 12.sp)
            }
        }
        Text(
            "${fmt.format(Date(entry.timestampMillis))} · ${entry.typeLabel} · " +
                "${entry.durationSeconds}s",
            fontSize = 13.sp,
            color = Color.Gray
        )
        entry.note?.let { note ->
            Text(
                "Note: $note",
                fontSize = 13.sp,
                color = Color(0xFF1565C0),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
