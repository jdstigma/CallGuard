package com.callguard.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callguard.app.ui.ActionCard
import com.callguard.app.ui.CGCard
import com.callguard.app.ui.SectionHeader
import com.callguard.app.ui.StatTile

@Composable
fun HomeScreen(
    entries: List<CallEntry>,
    profile: UserProfile,
    onNavigate: (Screen) -> Unit,
) {
    val stats = CallStats.from(entries)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Your call evidence",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "CallGuard logs the harassing calls and builds the documents that get them traced.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        // --- At-a-glance stats ---
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(stats.totalCalls.toString(), "calls logged", Modifier.weight(1f))
            StatTile(stats.flaggedCalls.toString(), "flagged", Modifier.weight(1f), highlight = stats.flaggedCalls > 0)
            StatTile(stats.uniqueNumbers.toString(), "numbers", Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))

        // --- The document checklist (guides the user through filing) ---
        SectionHeader("What to file next")
        Spacer(Modifier.height(4.dp))
        Text(
            "Work down the list. Each document auto-fills from your call log and My info.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        ActionCard(
            header = "FCC Complaint",
            title = "File with the FCC",
            subtitle = "The federal record of the spoofing campaign. Free, online, 10 minutes.",
            buttonText = "Build it",
            icon = Icons.Filled.Description,
            done = profile.fccComplaintNumber.isNotBlank(),
            onClick = { onNavigate(Screen.Documents) },
        )
        Spacer(Modifier.height(12.dp))

        ActionCard(
            header = "Harassment–Police Report",
            title = "File a police report",
            subtitle = "This is what lets police subpoena your carrier and unmask the real caller.",
            buttonText = "Build it",
            icon = Icons.Filled.Gavel,
            done = profile.policeCaseNumber.isNotBlank(),
            onClick = { onNavigate(Screen.Documents) },
        )
        Spacer(Modifier.height(12.dp))

        ActionCard(
            header = "Carrier Case",
            title = "Open a carrier harassment case",
            subtitle = "Your phone company logs the pattern and can block in bulk. Script included.",
            buttonText = "Build it",
            icon = Icons.Filled.SupportAgent,
            done = profile.carrierCaseNumber.isNotBlank(),
            onClick = { onNavigate(Screen.Documents) },
        )
        Spacer(Modifier.height(24.dp))

        // --- Orientation card for first-time users ---
        CGCard {
            Text(
                "How this works",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "An app can only see the number the network delivered — which for a spoofed " +
                    "call is fake. It cannot reveal who is really calling. What CallGuard does is " +
                    "turn your call log into court-ready evidence, so your carrier and the police " +
                    "can trace the real origin. Read the full walkthrough under Learn.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
