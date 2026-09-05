package com.agon.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agon.app.data.BrowserState
import com.agon.app.data.DeskSession
import com.agon.app.data.DeskStore
import com.agon.app.data.SysInfo
import com.agon.app.ui.components.GlassCard
import com.agon.app.ui.components.HeroHeader
import com.agon.app.ui.components.SectionHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val sens by DeskSession.sens.collectAsState()
    val theme by DeskSession.themeMode.collectAsState()
    val autoPresent by DeskSession.autoPresent.collectAsState()
    val home by DeskSession.browserHome.collectAsState()

    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HeroHeader(
            title = "الإعدادات",
            subtitle = "تُحفظ التفضيلات محلياً عبر DataStore وتبقى بعد إغلاق التطبيق",
        )

        // ── Device ──
        SectionHeader("الجهاز", "Huawei Y7a / P Smart 2021 (PPA-LX2)")
        GlassCard(fillAlpha = 0.10f) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DeviceRow("الطراز", "Huawei Y7a (PPA-LX2)")
                DeviceRow("النظام", "Android 10 • EMUI 10.1.1")
                DeviceRow("العرض", "720×1600 • 60Hz")
                DeviceRow("المسار", "أ — سطح مكتب داخل التطبيق (بدون روت)")
                DeviceRow("Wi-Fi", SysInfo.wifiLabel(ctx))
                DeviceRow("الشبكة", SysInfo.netLabel(ctx))
            }
        }

        // ── Touchpad ──
        SectionHeader("لوحة اللمس", "سرعة المؤشر وسلوك الجلسة")
        GlassCard(fillAlpha = 0.10f) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneAndroid, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("حساسية المؤشر", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(String.format("%.1f", sens), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = sens,
                    onValueChange = { DeskSession.sens.value = it },
                    onValueChangeFinished = {
                        CoroutineScope(Dispatchers.IO).launch { DeskStore.saveSens(DeskSession.sens.value) }
                    },
                    valueRange = 0.4f..3.5f,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("عرض تلقائي عند الاتصال", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        Text("إظهار قائمة ابدأ فور اكتشاف شاشة Presentation", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = autoPresent,
                        onCheckedChange = { DeskSession.autoPresent.value = it },
                    )
                }
            }
        }

        // ── Theme ──
        SectionHeader("المظهر", "مظهر تطبيق الهاتف (سطح المكتب داكن دائماً)")
        GlassCard(fillAlpha = 0.10f) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeChip("النظام", "system", theme)
                ThemeChip("داكن", "dark", theme)
                ThemeChip("فاتح", "light", theme)
            }
        }

        // ── Browser ──
        SectionHeader("المتصفح", "الصفحة الرئيسية للشاشة الكبيرة")
        GlassCard(fillAlpha = 0.10f) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الرئيسية الحالية:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(home, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            DeskSession.browserHome.value = BrowserState.current.value.ifBlank { home }
                            CoroutineScope(Dispatchers.IO).launch {
                                DeskStore.saveBrowser(DeskSession.browserHome.value, BrowserState.current.value)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Home, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("اجعل الحالية رئيسية", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            DeskSession.browserHome.value = "https://www.google.com"
                            CoroutineScope(Dispatchers.IO).launch {
                                DeskStore.saveBrowser("https://www.google.com", BrowserState.current.value)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("إعادة الافتراضي", fontSize = 12.sp) }
                }
            }
        }

        // ── System shortcuts ──
        SectionHeader("اختصارات النظام", "فتح شاشات أندرويد الأصلية")
        GlassCard(fillAlpha = 0.10f) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SysBtn("إعدادات البث (Cast)") {
                    if (activity != null) SysInfo.openCastSettings(activity)
                }
                SysBtn("إعدادات Wi-Fi") {
                    openSys(ctx, Settings.ACTION_WIFI_SETTINGS)
                }
                SysBtn("إعدادات العرض") {
                    openSys(ctx, Settings.ACTION_DISPLAY_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= 26) {
                    SysBtn("إعدادات الإشعارات") {
                        try {
                            ctx.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        // ── Danger ──
        SectionHeader("البيانات", "إدارة التخزين المحلي")
        GlassCard(fillAlpha = 0.10f) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        DeskSession.closeAll()
                        DeskSession.startOpen.value = false
                        DeskSession.contextMenu.value = null
                        DeskSession.setCursor(0.5f, 0.45f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("إغلاق كل النوافذ وتصفير المؤشر") }
                Button(
                    onClick = {
                        com.agon.app.data.NotesRepo.let { repo ->
                            DeskSession.notes.value = emptyList()
                            CoroutineScope(Dispatchers.IO).launch { DeskStore.saveNotes("[]") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("مسح كل الملاحظات")
                }
            }
        }

        GlassCard(fillAlpha = 0.08f) {
            Row {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "DeskCast v1.0 — صُمم لـ Huawei Y7a بدون روت وبدون Freeform. كل النوافذ تعمل داخل التطبيق عبر Presentation API.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DeviceRow(label: String, value: String) {
    Row {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RowScope.ThemeChip(label: String, value: String, current: String) {
    val active = current == value
    Box(
        Modifier.weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .border(
                1.dp,
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp),
            )
            .clickable {
                DeskSession.themeMode.value = value
                CoroutineScope(Dispatchers.IO).launch { DeskStore.saveTheme(value) }
            }
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SysBtn(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.SettingsSuggest, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
        Text("‹", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun openSys(ctx: android.content.Context, action: String) {
    try {
        ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {}
}
