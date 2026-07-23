package com.callguard.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// CallGuard brand palette — "Trust" (navy + teal).
// Navy carries the chrome (drawer, headers); teal is the single action accent;
// red is reserved ONLY for the flagged / harassment signal so it stays loud.
// ---------------------------------------------------------------------------

val Navy = Color(0xFF0F1E33)        // primary — drawer, app bar, dark surfaces
val NavyDeep = Color(0xFF0A1626)    // deepest navy (drawer bg in dark contexts)
val NavyMuted = Color(0xFF243449)   // raised navy surface / dividers on navy

val Teal = Color(0xFF1FBFA6)        // secondary — the one call-to-action accent
val TealDeep = Color(0xFF0F6E56)    // text/icons that sit on a teal fill
val TealSoft = Color(0xFFE1F5EE)    // teal-tinted container background

val FlagRed = Color(0xFFB00020)     // the harassment / flagged signal — reserved
val FlagRedSoft = Color(0xFFFFE0E0) // flagged row background

val Slate = Color(0xFF5F6B7A)       // secondary text on light surfaces
val SlateLight = Color(0xFFAEBBCC)  // muted text on navy
val PageBg = Color(0xFFF4F6F9)      // app background (light mode)
val CardBg = Color(0xFFFFFFFF)      // card / surface (light mode)
val HairLine = Color(0xFFE2E7EE)    // hairline dividers on light

// Dark-mode counterparts
val NavySurfaceDark = Color(0xFF16233A)
val PageBgDark = Color(0xFF0B1220)
val CardBgDark = Color(0xFF16202E)
