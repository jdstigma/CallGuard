package com.callguard.app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallLogScreen(
    entries: List<CallEntry>,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<CallEntry?>(null) }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        val suspicious = entries.count { it.isSuspicious }
        Text(
            "${entries.size} calls logged · $suspicious flagged as suspicious",
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = onRefresh, shape = RoundedCornerShape(10.dp)) { Text("Refresh") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    val where = CsvExporter.export(context, entries)
                    Toast.makeText(context, "Saved: $where", Toast.LENGTH_LONG).show()
                },
                shape = RoundedCornerShape(10.dp),
            ) { Text("Export CSV") }
        }
        Text(
            "Tip: tap a call to add a note",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
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
private fun CallRow(entry: CallEntry, onClick: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM d, HH:mm:ss", Locale.US) }
    val bg = if (entry.isSuspicious) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(bg)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                entry.cachedName?.takeIf { it.isNotBlank() } ?: entry.number,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (entry.isSuspicious) {
                Text("⚠ flagged", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
        Text(
            "${fmt.format(Date(entry.timestampMillis))} · ${entry.typeLabel} · ${entry.durationSeconds}s",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        entry.note?.let { note ->
            Text(
                "Note: $note",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
