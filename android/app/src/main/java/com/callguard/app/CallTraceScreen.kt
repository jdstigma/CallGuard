package com.callguard.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.core.content.ContextCompat
import com.callguard.app.ui.CGCard
import com.callguard.app.ui.SectionHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallTraceScreen() {
    val context = LocalContext.current
    val fmt = remember { SimpleDateFormat("EEE MMM d, yyyy · h:mm a", Locale.US) }
    var log by remember { mutableStateOf(TraceLog.all(context)) }

    fun placeTrace() {
        try {
            context.startActivity(Intent(Intent.ACTION_CALL, Uri.fromParts("tel", "*57", null)))
            TraceLog.add(context, System.currentTimeMillis())
            log = TraceLog.all(context)
            Toast.makeText(context, "Placed *57 and logged the trace", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Fall back to opening the dialer prefilled (no auto-log — user must press call).
            try {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", "*57", null)))
            } catch (_: Exception) {
                Toast.makeText(context, "Couldn't start the call — dial *57 manually", Toast.LENGTH_LONG).show()
            }
        }
    }

    val callPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) placeTrace()
        else {
            Toast.makeText(context, "Opening the dialer instead — press call to trace", Toast.LENGTH_LONG).show()
            try {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", "*57", null)))
            } catch (_: Exception) { }
        }
    }

    fun trace() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) placeTrace() else callPermLauncher.launch(Manifest.permission.CALL_PHONE)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            "Call trace (*57)",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Right after a harassing call ends — before any other call comes in — trace it. " +
                "This tells your carrier to log the true originating line for that call, in a form " +
                "police can subpoena. You won't see the result; it goes to the carrier and police.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { trace() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Filled.TrackChanges, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Call *57 now", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Tapping places the *57 call and logs the time below. *57 usually carries a small " +
                "per-use fee and is most reliable on landlines; on some wireless carriers it may " +
                "not be supported, in which case the police-report-to-subpoena route is what unmasks the caller.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        SectionHeader("Your trace log")
        Spacer(Modifier.height(4.dp))
        Text(
            "Bring these times to police so they know exactly which calls you traced.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        if (log.isEmpty()) {
            CGCard {
                Text(
                    "No traces logged yet",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Each time you tap “Call *57 now,” the date and time are recorded here.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            CGCard {
                log.forEachIndexed { i, t ->
                    if (i > 0) Spacer(Modifier.height(8.dp))
                    Text(
                        fmt.format(Date(t)),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
