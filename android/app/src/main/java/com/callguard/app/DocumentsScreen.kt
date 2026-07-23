package com.callguard.app

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callguard.app.ui.CGCard

@Composable
fun DocumentsScreen(
    entries: List<CallEntry>,
    profile: UserProfile,
    onEditInfo: () -> Unit,
) {
    val context = LocalContext.current

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(
                "Documents",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Each PDF is filled from your call log and My info, then saved to Downloads and " +
                    "opened in the share sheet.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            if (!profile.isReadyForDocuments) {
                CGCard {
                    Text(
                        "Add your info first",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Documents fill in better with your name and phone number. You can still " +
                            "generate them now — blanks appear as [PLACEHOLDERS] you complete by hand.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onEditInfo, shape = RoundedCornerShape(10.dp)) {
                        Text("Go to My info")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        fun generate(type: DocumentType) {
            val result = DocumentGenerator.generate(context, type, profile, entries)
            if (result.uri != null) {
                DocumentGenerator.share(context, result.uri)
                Toast.makeText(context, "Saved: ${result.path}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Could not generate PDF", Toast.LENGTH_LONG).show()
            }
        }

        // The bundle comes first and is visually emphasized.
        item {
            DocumentCard(DocumentType.EvidencePacket, emphasized = true) { generate(DocumentType.EvidencePacket) }
            Spacer(Modifier.height(16.dp))
            Text(
                "Or generate documents individually",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
        }

        items(DocumentType.entries.filter { it != DocumentType.EvidencePacket }) { type ->
            DocumentCard(type) { generate(type) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DocumentCard(type: DocumentType, emphasized: Boolean = false, onGenerate: () -> Unit) {
    CGCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (emphasized) Icons.Filled.Description else Icons.Filled.PictureAsPdf,
                contentDescription = null,
                tint = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                type.displayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (emphasized) 17.sp else 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            type.blurb,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = onGenerate, shape = RoundedCornerShape(10.dp)) {
                Text(if (emphasized) "Generate packet PDF" else "Generate PDF")
            }
        }
    }
}
