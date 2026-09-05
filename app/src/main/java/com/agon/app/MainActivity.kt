package com.agon.app

import android.hardware.display.DisplayManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agon.app.data.DeskSession
import com.agon.app.presentation.DeskPresentation
import com.agon.app.ui.screens.AppsScreen
import com.agon.app.ui.screens.FilesScreen
import com.agon.app.ui.screens.HomeScreen
import com.agon.app.ui.screens.MirrorScreen
import com.agon.app.ui.screens.SettingsScreen
import com.agon.app.ui.screens.TouchpadScreen
import com.agon.app.ui.theme.AgonAppTheme

class MainActivity : ComponentActivity() {

    private var presentation: DeskPresentation? = null
    private var displayListener: DisplayManager.DisplayListener? = null
    @Volatile
    private var presentationBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        DeskSession.appCtx = applicationContext
        DeskSession.refreshDisplays(this)
        watchExternalDisplays()
        setContent {
            val themeMode by DeskSession.themeMode.collectAsState()
            val dark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            AgonAppTheme(darkTheme = dark) {
                MainApp()
            }
            PresentationHost()
        }
    }

    /** Path-A core: DisplayListener → Presentation bound to the external display id. */
    private fun watchExternalDisplays() {
        val dm = try {
            getSystemService(DISPLAY_SERVICE) as DisplayManager
        } catch (_: Throwable) {
            return
        }
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = onDisplaysChanged()
            override fun onDisplayRemoved(displayId: Int) = onDisplaysChanged()
            override fun onDisplayChanged(displayId: Int) = onDisplaysChanged()
        }
        displayListener = listener
        try { dm.registerDisplayListener(listener, null) } catch (_: Throwable) {}
    }

    private fun onDisplaysChanged() {
        try {
            DeskSession.refreshDisplays(this)
        } catch (_: Throwable) {}
        runOnUiThread {
            try {
                syncPresentation()
            } catch (_: Throwable) {}
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            DeskSession.refreshDisplays(this)
        } catch (_: Throwable) {}
        try {
            syncPresentation()
        } catch (_: Throwable) {}
    }

    override fun onPause() {
        // Under wireless mirroring the external display is often torn down while our
        // activity is paused; dismiss defensively so a stale window token can never
        // freeze re-entry into the app.
        try {
            syncPresentation()
        } catch (_: Throwable) {}
        super.onPause()
    }

    override fun onDestroy() {
        try {
            val dm = getSystemService(DISPLAY_SERVICE) as? DisplayManager
            displayListener?.let { dm?.unregisterDisplayListener(it) }
        } catch (_: Throwable) {}
        displayListener = null
        safeDismissPresentation()
        super.onDestroy()
    }

    /** Dismiss without ever throwing — called from lifecycle paths during casting. */
    private fun safeDismissPresentation() {
        val cur = presentation
        presentation = null
        DeskSession.presenting.value = false
        if (cur != null) {
            try {
                cur.dismiss()
            } catch (_: Throwable) {
                // Window token already gone (display removed) — safe to ignore.
            }
        }
    }

    private fun syncPresentation() {
        if (presentationBusy) return
        val target = try {
            DeskSession.presentationDisplay(this)
        } catch (_: Throwable) {
            null
        }
        if (target == null || !DeskSession.autoPresent.value) {
            safeDismissPresentation()
            return
        }
        // Re-validate the target display is still alive before touching any window.
        val stillAlive = try {
            val dm = getSystemService(DISPLAY_SERVICE) as? DisplayManager
            dm?.getDisplay(target.displayId) != null
        } catch (_: Throwable) {
            true
        }
        if (!stillAlive) {
            safeDismissPresentation()
            return
        }
        val cur = presentation
        if (cur != null) {
            val sameDisplay = try {
                cur.display.displayId == target.displayId
            } catch (_: Throwable) {
                false
            }
            if (sameDisplay) {
                try {
                    if (!cur.isShowing) cur.show()
                } catch (_: Throwable) {
                    safeDismissPresentation()
                }
                return
            }
            safeDismissPresentation()
        }
        presentationBusy = true
        try {
            val p = DeskPresentation(this, target)
            presentation = p
            try {
                p.show()
            } catch (_: Throwable) {
                // show() throws when the route vanished between detection and attach
                // (the exact "won't open the app while casting" crash) — stay alive
                // on the phone and let the next display event retry.
                presentation = null
                DeskSession.presenting.value = false
            }
        } catch (_: Throwable) {
            presentation = null
            DeskSession.presenting.value = false
        } finally {
            presentationBusy = false
        }
    }
}

/** Keeps the Presentation in sync from composition (covers autoPresent toggles). */
@Composable
private fun PresentationHost() {
    val ctx = LocalContext.current
    val activity = ctx as? MainActivity
    val displays by DeskSession.displays.collectAsState()
    val auto by DeskSession.autoPresent.collectAsState()
    LaunchedEffect(displays, auto) {
        // The Activity's listener already handles show/dismiss; this just refreshes state.
        if (activity == null) return@LaunchedEffect
    }
    DisposableEffect(Unit) { onDispose { } }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomNav(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") { HomeScreen() }
            composable("touch") { TouchpadScreen() }
            composable("mirror") { MirrorScreen() }
            composable("apps") { AppsScreen() }
            composable("files") { FilesScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}

@Composable
fun BottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem(navController, currentRoute, "home", "الرئيسية", Icons.Default.Cast, "home")
        NavItem(navController, currentRoute, "touch", "اللمس", Icons.Default.TouchApp, "home")
        NavItem(navController, currentRoute, "mirror", "المكتب", Icons.Default.Apps, "home")
        NavItem(navController, currentRoute, "apps", "التطبيقات", Icons.Default.Folder, "home")
        NavItem(navController, currentRoute, "settings", "الإعدادات", Icons.Default.Settings, "home")
    }
}

@Composable
private fun RowScope.NavItem(
    navController: NavHostController,
    currentRoute: String?,
    route: String,
    label: String,
    icon: ImageVector,
    root: String,
) {
    val selected = currentRoute == route
    Column(
        Modifier.weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable {
                navController.navigate(route) {
                    popUpTo(root)
                    launchSingleTop = true
                }
            }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
