package com.callguard.app

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalysisScreen(entries: List<CallEntry>) {
    val context = LocalContext.current
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
    val normalColor = Color(0xFF1FBFA6)

    val pieSlices = listOf(
        Slice("Flagged", stats.flaggedCalls, flaggedColor),
        Slice("Normal", (stats.totalCalls - stats.flaggedCalls).coerceAtLeast(0), normalColor),
    )
    val typeBars = listOf(
        Bar("Incoming", stats.incoming, Color(0xFF185FA5)),
        Bar("Missed", stats.missed, Color(0xFFBA7517)),
        Bar("Rejected", stats.rejected, Color(0xFF534AB7)),
    )
    val topBars = stats.perNumber.take(5).map {
        Bar(
            label = it.name?.takeIf { n -> n.isNotBlank() } ?: it.number,
            value = it.totalCount,
            color = if (it.flaggedCount > 0) flaggedColor else Color(0xFF185FA5),
        )
    }

    LazyColumn(Modifier.padding(16.dp).fillMaxSize()) {
        item {
            SubHeader("Date range")
            Spacer(Modifier.height(8.dp))
            Row {
                RangeButton("All", rangeDays == 0) { rangeDays = 0 }
                RangeButton("7d", rangeDays == 7) { rangeDays = 7 }
                RangeButton("30d", rangeDays == 30) { rangeDays = 30 }
                RangeButton("90d", rangeDays == 90) { rangeDays = 90 }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val uri = ChartImageExporter.export(context, stats, rangeLabel)
                    if (uri != null) {
                        ChartImageExporter.share(context, uri)
                        Toast.makeText(context, "Saved to Pictures — opening share…", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Image export failed", Toast.LENGTH_LONG).show()
                    }
                },
                shape = RoundedCornerShape(10.dp),
            ) { Text("Export / share charts (PNG)") }
            Spacer(Modifier.height(20.dp))

            SubHeader("Flagged vs. normal calls")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PieChart(pieSlices, Modifier.size(140.dp))
                Spacer(Modifier.width(20.dp))
                ChartLegend(pieSlices)
            }
            Spacer(Modifier.height(20.dp))

            SubHeader("Call types")
            Spacer(Modifier.height(8.dp))
            BarChart(typeBars)
            Spacer(Modifier.height(20.dp))

            SubHeader("Top numbers by call count")
            Spacer(Modifier.height(8.dp))
            if (topBars.isEmpty()) Text("No calls yet.") else BarChart(topBars)
            Spacer(Modifier.height(24.dp))

            SubHeader("Summary")
            Spacer(Modifier.height(8.dp))
            StatLine("Total calls", stats.totalCalls.toString())
            StatLine("Flagged as suspicious", stats.flaggedCalls.toString(), highlight = stats.flaggedCalls > 0)
            StatLine("Unique numbers", stats.uniqueNumbers.toString())
            StatLine("Incoming", stats.incoming.toString())
            StatLine("Missed", stats.missed.toString())
            StatLine("Rejected", stats.rejected.toString())
            Spacer(Modifier.height(16.dp))

            SubHeader("By number (most calls first)")
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
        items(stats.perNumber) { n -> NumberStatRow(n) }
    }
}

@Composable
private fun SubHeader(title: String) {
    Text(
        title,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun RangeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val mod = Modifier.padding(end = 6.dp)
    if (selected) {
        Button(onClick = onClick, modifier = mod, shape = RoundedCornerShape(10.dp)) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = mod, shape = RoundedCornerShape(10.dp)) { Text(label) }
    }
}

@Composable
private fun StatLine(label: String, value: String, highlight: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onBackground)
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun NumberStatRow(n: NumberStat) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.US) }
    val bg = if (n.flaggedCount > 0) MaterialTheme.colorScheme.errorContainer else Color.Transparent
    Column(
        Modifier.fillMaxWidth().background(bg).padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                n.name?.takeIf { it.isNotBlank() } ?: n.number,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text("${n.totalCount} calls", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
        if (n.flaggedCount > 0) {
            Text(
                "⚠ ${n.flaggedCount} flagged",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }
        Text("First: ${fmt.format(Date(n.firstSeenMillis))}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Last:  ${fmt.format(Date(n.lastSeenMillis))}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
