package com.agon.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agon.app.data.DeskApps
import com.agon.app.data.DeskSession
import com.agon.app.data.DeskStore
import com.agon.app.data.FileBrowser
import com.agon.app.data.SysInfo
import com.agon.app.service.DeskCastService
import com.agon.app.ui.components.GlassCard
import com.agon.app.ui.components.HeroHeader
import com.agon.app.ui.components.SectionHeader
import com.agon.app.ui.components.StatusPill
import kotlinx.coroutines.delay

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val displays by DeskSession.displays.collectAsState()
    val presenting by DeskSession.presenting.collectAsState()
    val sessionActive by DeskSession.sessionActive.collectAsState()
    val sessionStart by DeskSession.sessionStart.collectAsState()
    val wins by DeskSession.windows.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var notifAsked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        DeskSession.appCtx = ctx.applicationContext
        DeskSession.refreshDisplays(ctx)
        DeskStore.init(ctx)
        val p = DeskStore.load()
        DeskSession.sens.value = p.sens
        DeskSession.themeMode.value = p.theme
        DeskSession.browserHome.value = p.home
        DeskSession.browserLast.value = p.last
        com.agon.app.data.BrowserState.input.value = p.last
        com.agon.app.data.BrowserState.current.value = p.last
        if (p.tree != null) {
            try {
                val uri = Uri.parse(p.tree)
                DeskSession.treeUri.value = uri
                FileBrowser.setRoot(uri, ctx)
            } catch (_: Exception) {}
        }
        com.agon.app.data.NotesRepo.let { repo ->
            DeskSession.notes.value = repo.decode(p.notesJson)
            repo.defaultsIfEmpty()
        }
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
            DeskSession.refreshDisplays(ctx)
        }
    }

    // Live DisplayListener (path A core: onDisplayAdded/onDisplayRemoved)
    DisposableEffect(ctx) {
        val dm = ctx.getSystemService(android.content.Context.DISPLAY_SERVICE) as DisplayManager
        val l = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(id: Int) {
                DeskSession.refreshDisplays(ctx)
            }
            override fun onDisplayRemoved(id: Int) {
                DeskSession.refreshDisplays(ctx)
            }
            override fun onDisplayChanged(id: Int) {
                DeskSession.refreshDisplays(ctx)
            }
        }
        try { dm.registerDisplayListener(l, null) } catch (_: Exception) {}
        onDispose {
            try { dm.unregisterDisplayListener(l) } catch (_: Exception) {}
        }
    }

    // Runtime notification permission (Android 13+)
    val notifPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notifAsked = true }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !notifAsked) {
            notifAsked = true
            try { notifPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS) } catch (_: Exception) {}
        }
    }

    val presTarget = displays.firstOrNull { it.isPresentation }
    val glow by animateFloatAsState(if (presTarget != null) 1f else 0.35f, label = "castGlow")

    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HeroHeader(
            title = "سطح مكتب لاسلكي",
            subtitle = "Huawei Y7a • Android 10 • EMUI 10.1.1 — المسار (أ): سطح مكتب داخل التطبيق عبر Presentation",
        )

        // ── Session / cast status card ──
        GlassCard(fillAlpha = 0.16f) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    if (presTarget != null) listOf(Color(0xFF4ADE80), Color(0xFF0E7C66))
                                    else listOf(Color(0xFF38E1FF), Color(0xFF6D4AFF))
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (presTarget != null) Icons.Default.CastConnected else Icons.Default.Cast,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (presTarget != null) "شاشة لاسلكية متصلة" else "بانتظار شاشة لاسلكية",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                        )
                        Text(
                            if (presTarget != null) presTarget.name + " • ${presTarget.width}×${presTarget.height}"
                            else "اتصل عبر Miracast من إعدادات النظام أولاً",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusPill(
                        if (sessionActive) "نشطة" else "متوقفة",
                        if (sessionActive) Color(0xFF4ADE80) else Color(0xFFFFC24B),
                    )
                }

                if (presTarget == null) {
                    // Mirror-only explainer (spec requirement)
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFC24B).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFFFC24B).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFFC24B), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "جهاز الاستقبال الحالي يدعم Mirror فقط (تكرار الشاشة). لسطح مكتب مستقل فعّل Wireless Display / Miracast حتى تظهر شاشة Presentation.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                if (sessionActive && sessionStart > 0) {
                    val elapsed = SysInfo.fmtElapsed(now - sessionStart)
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("مدة الجلسة: $elapsed", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${wins.count { !it.minimized }} نوافذ مفتوحة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { glow },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { DeskCastService.start(ctx) },
                        enabled = !sessionActive,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("بدء الجلسة")
                    }
                    OutlinedButton(
                        onClick = { DeskCastService.stop(ctx) },
                        enabled = sessionActive,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("إيقاف")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (activity != null) SysInfo.openCastSettings(activity)
                            else {
                                try {
                                    ctx.startActivity(Intent(Settings.ACTION_CAST_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Tv, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("إعدادات البث")
                    }
                    OutlinedButton(
                        onClick = { DeskSession.refreshDisplays(ctx) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تحديث الشاشات")
                    }
                }
            }
        }

        // ── Displays ──
        SectionHeader("الشاشات المكتشفة", "DisplayManager.DISPLAY_CATEGORY_PRESENTATION • ${displays.size} شاشة")
        if (displays.isEmpty()) {
            GlassCard { Text("لا توجد شاشات — غريب! حتى الشاشة الداخلية يجب أن تظهر.", fontSize = 13.sp) }
        } else {
            displays.forEach { d ->
                GlassCard(fillAlpha = 0.10f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tv, null,
                            tint = if (d.isPresentation) Color(0xFF4ADE80) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "id=${d.id} • ${d.width}×${d.height}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (d.isPresentation) StatusPill("Presentation", Color(0xFF4ADE80))
                        else StatusPill("Mirror", Color(0xFFFFC24B))
                    }
                }
            }
        }

        // ── Quick launch ──
        SectionHeader("فتح سريع على الشاشة الكبيرة", "تُفتح النوافذ فوراً في العرض الخارجي والمعاينة")
        GlassCard(fillAlpha = 0.10f) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DeskApps.all.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { appId ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { DeskSession.openApp(appId) }
                                    .padding(6.dp),
                            ) {
                                com.agon.app.ui.components.AppIconBadge(appId, size = 52.dp)
                                Spacer(Modifier.height(4.dp))
                                Text(DeskApps.title(appId), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // ── System status ──
        SectionHeader("حالة النظام", "قراءات حية من أجهزة الاستشعار والشبكة")
        GlassCard(fillAlpha = 0.10f) {
            SysGrid(ctx)
        }

        // ── Help ──
        GlassCard(fillAlpha = 0.10f) {
            Row {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "خطوات الاتصال: ١) افتح إعدادات البث واتصل بالشاشة اللاسلكية ٢) ابدأ الجلسة ٣) استخدم تبويب اللمس للتحكم بالمؤشر ٤) اكتب من لوحة المفاتيح ليصل النص للتطبيق النشط.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SysGrid(ctx: android.content.Context) {
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            tick++
        }
    }
    tick.let {}
    val wifi = remember(tick) { SysInfo.wifiLabel(ctx) }
    val net = remember(tick) { SysInfo.netLabel(ctx) }
    val bat = remember(tick) { SysInfo.batteryPct(ctx) }
    val wins by DeskSession.windows.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SysCell("Wi-Fi", wifi, Modifier.weight(1f))
            SysCell("الشبكة", net, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SysCell("البطارية", if (bat >= 0) "$bat٪" else "—", Modifier.weight(1f))
            SysCell("النوافذ", "${wins.size} (${wins.count { !it.minimized }} مرئية)", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SysCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .padding(10.dp),
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
