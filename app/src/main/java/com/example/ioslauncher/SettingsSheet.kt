package com.example.ioslauncher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val swatches = listOf(
    0xFF0A84FF, 0xFFFF375F, 0xFFFF9F0A, 0xFFFFD60A, 0xFF30D158, 0xFF64D2FF, 0xFFBF5AF2, 0xFFFFFFFF
).map { it.toInt() }

@Composable
fun SettingsSheet(
    apps: List<AppEntry>,
    current: LauncherSettings,
    onApply: (LauncherSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var d by remember { mutableStateOf(current) }
    val icons = LocalIcons.current
    // Live preview: rendered from the draft, applied only when the user taps Apply.
    val preview = remember(d.copy(hidden = emptySet()), apps) {
        apps.take(4).map { IconRenderer.render(it.drawable, d) }
    }
    val none = remember { MutableInteractionSource() }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
            .clickable(interactionSource = none, indication = null, onClick = onDismiss)
    ) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Brush.verticalGradient(listOf(Color(0xF01C1C1E), Color(0xF5101012))))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding()
        ) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp)) {
                item { Text("Icon Appearance", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF5B8CFF), Color(0xFFB65CFF), Color(0xFFFF7A59))))
                            .padding(vertical = 22.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            preview.forEach { Image(it, null, Modifier.size(d.sizeDp.dp)) }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconStyle.values().forEach { st ->
                            val sel = d.style == st
                            Box(
                                Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(19.dp))
                                    .background(if (sel) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.14f))
                                    .clickable { d = d.copy(style = st) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(st.name, color = if (sel) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                if (d.style == IconStyle.Tinted) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            swatches.forEach { c ->
                                Box(
                                    Modifier.size(32.dp).clip(CircleShape).background(Color(c))
                                        .then(if (d.tint == c) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                                        .clickable { d = d.copy(tint = c) }
                                )
                            }
                        }
                    }
                    item { SliderRow("Tint intensity", d.tintIntensity, 0.2f..1f) { d = d.copy(tintIntensity = it) } }
                }
                if (d.style == IconStyle.Glass) {
                    item { SliderRow("Glass intensity", d.glass, 0f..1f) { d = d.copy(glass = it) } }
                }
                item { SliderRow("Brightness", d.brightness, 0.6f..1.3f) { d = d.copy(brightness = it) } }
                item { SliderRow("Icon size", d.sizeDp, 48f..70f) { d = d.copy(sizeDp = it) } }
                item { SliderRow("Corner radius", d.radius, 0.12f..0.5f) { d = d.copy(radius = it) } }
                item {
                    Text(
                        "Hidden apps", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)
                    )
                }
                items(apps, key = { it.pkg }) { app ->
                    val icon = icons[app.pkg]
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) Image(icon, null, Modifier.size(36.dp)) else Spacer(Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(app.label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = app.pkg in d.hidden,
                            onCheckedChange = { on ->
                                d = d.copy(hidden = if (on) d.hidden + app.pkg else d.hidden - app.pkg)
                            }
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.14f)).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) { Text("Cancel", color = Color.White, fontSize = 16.sp) }
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0A84FF)).clickable { onApply(d) },
                    contentAlignment = Alignment.Center
                ) { Text("Apply", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.padding(top = 6.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
