package com.callguard.app

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.callguard.app.ui.CGCard
import com.callguard.app.ui.SectionHeader

@Composable
fun StateResourcesScreen(profile: UserProfile) {
    val context = LocalContext.current
    var selectedUsps by remember { mutableStateOf(profile.state) }
    var expanded by remember { mutableStateOf(false) }
    val selected = Contacts.forUsps(selectedUsps)

    fun open(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
        }
    }

    fun dial(phone: String) {
        try {
            val digits = phone.filter { it.isDigit() || it == '+' }
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
        } catch (e: Exception) {
            Toast.makeText(context, "Can't open dialer", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text(
            "State help",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "The traceback process is federal, so it works the same in every state. What " +
                "changes is where you file locally. Federal channels are your primary route; " +
                "your state Attorney General is an extra complaint you can add.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        // --- Federal (applies everywhere) ---
        SectionHeader("Federal — file these first")
        Spacer(Modifier.height(10.dp))
        Contacts.federal.forEach { c ->
            CGCard {
                Text(c.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(c.purpose, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { open(c.url) }, shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open site")
                    }
                    c.phone?.let { p ->
                        OutlinedButton(onClick = { dial(p) }, shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(p)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(12.dp))

        // --- State AG picker ---
        SectionHeader("Your state's Attorney General")
        Spacer(Modifier.height(10.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    selected?.let { "${it.name} (${it.usps})" } ?: "Select your state",
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                Contacts.states.forEach { st ->
                    DropdownMenuItem(
                        text = { Text("${st.name} (${st.usps})") },
                        onClick = {
                            selectedUsps = st.usps
                            expanded = false
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (selected != null) {
            CGCard {
                Text(selected.agOffice, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(
                    "File a robocall / caller-ID spoofing complaint with your state. Reference " +
                        "your FCC complaint number if you have one.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { open(selected.agUrl) }, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open complaint site")
                }
            }
        } else {
            Text(
                "Pick your state above, or set it once on the My info screen.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(20.dp))

        // --- Local police ---
        SectionHeader("Your local police")
        Spacer(Modifier.height(10.dp))
        CGCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalPolice, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(10.dp))
                Text("File a police report", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "This is the step that lets police subpoena your carrier. Use your city or " +
                    "county police non-emergency line — not 911. Bring the police report cover " +
                    "note and evidence summary from the Documents screen.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val city = profile.addressCity
                    val q = Uri.encode("$city police non-emergency number".trim())
                    open("https://www.google.com/search?q=$q")
                },
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Find my police non-emergency line")
            }
        }
        Spacer(Modifier.height(20.dp))

        // --- Verify / disclaimer ---
        CGCard {
            Text(
                "Details change",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Government sites and numbers are reviewed periodically but can change. If a " +
                    "link is outdated, use the official government finder to locate your current " +
                    "state office.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { open(Contacts.OFFICIAL_STATE_FINDER) }) {
                Text("Open USA.gov official finder")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
