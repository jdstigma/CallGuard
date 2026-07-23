package com.callguard.app

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callguard.app.ui.CGCard
import com.callguard.app.ui.SectionHeader

@Composable
fun MyInfoScreen(
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf(profile.fullName) }
    var phone by remember { mutableStateOf(profile.phone) }
    var email by remember { mutableStateOf(profile.email) }
    var city by remember { mutableStateOf(profile.addressCity) }
    var state by remember { mutableStateOf(profile.state) }
    var carrier by remember { mutableStateOf(profile.carrier) }
    var harassmentType by remember { mutableStateOf(profile.harassmentType) }
    var fccNo by remember { mutableStateOf(profile.fccComplaintNumber) }
    var policeNo by remember { mutableStateOf(profile.policeCaseNumber) }
    var carrierNo by remember { mutableStateOf(profile.carrierCaseNumber) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text(
            "My info",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Enter this once. Every document auto-fills from it. Stored only on this phone — " +
                "nothing is uploaded.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        CGCard {
            SectionHeader("About you")
            Spacer(Modifier.height(12.dp))
            Field("Full name", fullName) { fullName = it }
            Field("Phone number", phone) { phone = it }
            Field("Email", email) { email = it }
            Field("City", city) { city = it }
            Field("State (2-letter, e.g. CA)", state) { state = it }
            Field("Carrier (e.g. Verizon)", carrier) { carrier = it }
        }
        Spacer(Modifier.height(16.dp))

        CGCard {
            SectionHeader("Type of harassment")
            Spacer(Modifier.height(4.dp))
            Text(
                "This tailors the wording of your documents. Pick \"Aggressive\" if calls " +
                    "involve threats or abuse — that unlocks an incident timeline built from your notes.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row {
                HarassmentType.entries.forEach { type ->
                    FilterChip(
                        selected = harassmentType == type,
                        onClick = { harassmentType = type },
                        label = { Text(type.shortLabel) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        CGCard {
            SectionHeader("Case numbers")
            Spacer(Modifier.height(4.dp))
            Text(
                "Fill these in as you file each one — they cross-reference into your documents.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Field("FCC complaint #", fccNo) { fccNo = it }
            Field("Police case #", policeNo) { policeNo = it }
            Field("Carrier case #", carrierNo) { carrierNo = it }
        }
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                onSave(
                    UserProfile(
                        fullName = fullName.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                        addressCity = city.trim(),
                        state = state.trim().uppercase(),
                        carrier = carrier.trim(),
                        harassmentType = harassmentType,
                        fccComplaintNumber = fccNo.trim(),
                        policeCaseNumber = policeNo.trim(),
                        carrierCaseNumber = carrierNo.trim(),
                    )
                )
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
        ) { Text("Save", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}
