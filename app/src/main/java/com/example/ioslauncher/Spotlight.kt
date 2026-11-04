package com.example.ioslauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Spotlight(apps: List<AppEntry>, store: SettingsStore, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val kb = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus(); kb?.show() }

    val suggestions = remember(apps) {
        val byPkg = apps.associateBy { it.pkg }
        store.recents().mapNotNull { byPkg[it] }.ifEmpty { apps.take(4) }
    }
    val results = remember(apps, q) {
        if (q.isBlank()) emptyList()
        else apps.filter { it.label.contains(q, ignoreCase = true) }.sortedBy { !it.label.startsWith(q, ignoreCase = true) }
    }
    val list = if (q.isBlank()) suggestions else results
    val shape = RoundedCornerShape(22.dp)
    val none = remember { MutableInteractionSource() }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = none, indication = null, onClick = onDismiss)
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().height(46.dp).clip(shape)
                    .background(Color.White.copy(alpha = 0.20f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.30f), shape)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    if (q.isEmpty()) Text("Search", color = Color.White.copy(alpha = 0.6f), fontSize = 17.sp)
                    BasicTextField(
                        q, { q = it }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            results.firstOrNull()?.let { launchApp(ctx, it.pkg); onDismiss() }
                        })
                    )
                }
                Text("Cancel", color = Color.White, fontSize = 15.sp, modifier = Modifier.clickable(onClick = onDismiss))
            }
            Text(
                if (q.isBlank()) "Suggestions" else "Applications",
                color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp,
                modifier = Modifier.padding(start = 8.dp, top = 18.dp, bottom = 8.dp)
            )
            LazyColumn(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.12f))))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                items(list, key = { it.pkg }) { app -> AppRow(app) { launchApp(ctx, app.pkg); onDismiss() } }
            }
        }
    }
}
