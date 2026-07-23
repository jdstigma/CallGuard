package com.callguard.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.callguard.app.ui.theme.CallGuardTheme
import com.callguard.app.ui.theme.Navy
import com.callguard.app.ui.theme.SlateLight
import com.callguard.app.ui.theme.Teal
import com.callguard.app.ui.theme.TealDeep
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallGuardTheme { CallGuardApp() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallGuardApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var current by remember { mutableStateOf(Screen.Home) }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    var entries by remember { mutableStateOf<List<CallEntry>>(emptyList()) }
    var profile by remember { mutableStateOf(ProfileStore.load(context)) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
        if (isGranted) entries = CallLogRepository.readAll(context)
    }

    LaunchedEffect(granted) {
        if (granted) entries = CallLogRepository.readAll(context)
    }

    fun go(screen: Screen) {
        current = screen
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerSheet(current = current, onSelect = { go(it) })
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(current.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                if (!granted && current in setOf(Screen.Home, Screen.CallLog, Screen.Analysis, Screen.FlaggedNumbers)) {
                    PermissionBanner(
                        onGrant = { permissionLauncher.launch(Manifest.permission.READ_CALL_LOG) }
                    )
                }
                when (current) {
                    Screen.Home -> HomeScreen(entries, profile, onNavigate = { go(it) })
                    Screen.CallLog -> CallLogScreen(entries, onRefresh = {
                        if (granted) entries = CallLogRepository.readAll(context)
                    })
                    Screen.Analysis -> AnalysisScreen(entries)
                    Screen.FlaggedNumbers -> FlaggedNumbersScreen(entries)
                    Screen.CallTrace -> CallTraceScreen()
                    Screen.Documents -> DocumentsScreen(entries, profile, onEditInfo = { go(Screen.MyInfo) })
                    Screen.Learn -> LearnScreen()
                    Screen.StateHelp -> StateResourcesScreen(profile)
                    Screen.MyInfo -> MyInfoScreen(profile, onSave = {
                        profile = it
                        ProfileStore.save(context, it)
                    })
                }
            }
        }
    }
}

@Composable
private fun DrawerSheet(current: Screen, onSelect: (Screen) -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = Navy,
        modifier = Modifier.fillMaxWidth(0.78f),
    ) {
        Row(
            Modifier.padding(start = 20.dp, top = 24.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(10.dp), color = Teal, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = TealDeep, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("CallGuard", color = androidx.compose.ui.graphics.Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Evidence for a traceback", color = SlateLight, fontSize = 12.sp)
            }
        }
        Screen.menuOrder.forEach { screen ->
            NavigationDrawerItem(
                label = { Text(screen.title) },
                icon = { Icon(screen.icon, contentDescription = null) },
                selected = screen == current,
                onClick = { onSelect(screen) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Teal,
                    selectedTextColor = TealDeep,
                    selectedIconColor = TealDeep,
                    unselectedContainerColor = Navy,
                    unselectedTextColor = SlateLight,
                    unselectedIconColor = SlateLight,
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun PermissionBanner(onGrant: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Call log access needed",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "So CallGuard can build your evidence record.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onGrant, shape = RoundedCornerShape(10.dp)) { Text("Grant") }
        }
    }
}
