package com.callguard.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The left-drawer destinations. Order here is the order they appear in the menu.
 */
enum class Screen(
    val title: String,
    val icon: ImageVector,
) {
    Home("Home", Icons.Filled.Home),
    CallLog("Call log", Icons.Filled.Phone),
    Analysis("Analysis", Icons.Filled.BarChart),
    FlaggedNumbers("Flagged numbers", Icons.Filled.Flag),
    Documents("Documents", Icons.Filled.Description),
    Learn("Learn", Icons.AutoMirrored.Filled.MenuBook),
    StateHelp("State help", Icons.Filled.AccountBalance),
    MyInfo("My info", Icons.Filled.Person);

    companion object {
        val menuOrder: List<Screen> = entries.toList()
    }
}
