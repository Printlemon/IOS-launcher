package com.example.ioslauncher

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val LocalIcons = staticCompositionLocalOf<Map<String, ImageBitmap>> { emptyMap() }
val LocalSettings = staticCompositionLocalOf { LauncherSettings() }

fun launchApp(ctx: Context, pkg: String) {
    ctx.packageManager.getLaunchIntentForPackage(pkg)?.let {
        ctx.startActivity(it)
        SettingsStore(ctx).pushRecent(pkg)
    }
}

/** Staggered 0..1 progress for the unlock reveal: top rows first, dock last. */
private fun stagger(p: Float, i: Int) = ((p - i * 0.05f) / 0.7f).coerceIn(0f, 1f)

@Composable
fun LauncherApp(apps: List<AppEntry>, unlockKey: Int, store: SettingsStore) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF0A84FF))) {
        var settings by remember { mutableStateOf(store.load()) }
        var showSettings by remember { mutableStateOf(false) }
        var showSpotlight by remember { mutableStateOf(false) }

        val iconKey = remember(settings) { settings.copy(hidden = emptySet()) }
        val icons by produceState(emptyMap<String, ImageBitmap>(), apps, iconKey) {
            value = withContext(Dispatchers.Default) {
                apps.associate { it.pkg to IconRenderer.render(it.drawable, iconKey) }
            }
        }
        val visible = remember(apps, settings.hidden) { apps.filter { it.pkg !in settings.hidden } }
        val dockApps = remember(visible) { visible.take(4) }
        val pages = remember(visible) { visible.drop(4).chunked(24) }
        val pager = rememberPagerState { pages.size + 1 }

        // Unlock animation: home screen settles from 116% scale, rows un-blur and fade in
        // one after another, and the dock rises last. Replays on every unlock.
        val progress = remember { Animatable(0f) }
        val ready = icons.isNotEmpty()
        LaunchedEffect(unlockKey, ready) {
            if (ready) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(850, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
            }
        }

        BackHandler(enabled = showSettings || showSpotlight) {
            showSettings = false; showSpotlight = false
        }

        CompositionLocalProvider(LocalIcons provides icons, LocalSettings provides settings) {
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures(onLongPress = { showSettings = true }) }
                    .pointerInput(Unit) {
                        var total = 0f
                        detectVerticalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = { if (total > 140f) showSpotlight = true },
                            onDragCancel = {}
                        ) { _, d -> total += d }
                    }
            ) {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        val s = 1.16f - 0.16f * progress.value
                        scaleX = s; scaleY = s
                    }
                ) { page ->
                    if (page < pages.size) IconPage(pages[page], progress.value) else AppLibrary(visible)
                }
                Row(
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                        .padding(bottom = (settings.sizeDp + 58f).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(pager.pageCount) {
                        Box(
                            Modifier.size(7.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = if (it == pager.currentPage) 0.95f else 0.4f))
                        )
                    }
                }
                Dock(
                    dockApps, progress.value,
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp)
                )

                AnimatedVisibility(
                    visible = showSpotlight,
                    enter = fadeIn(tween(180)) + scaleIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f), initialScale = 0.92f),
                    exit = fadeOut(tween(120))
                ) { Spotlight(visible, store) { showSpotlight = false } }

                AnimatedVisibility(
                    visible = showSettings,
                    enter = slideInVertically(tween(320)) { it } + fadeIn(tween(200)),
                    exit = slideOutVertically(tween(240)) { it } + fadeOut(tween(200))
                ) {
                    SettingsSheet(
                        apps = apps, current = settings,
                        onApply = { settings = it; store.save(it); showSettings = false },
                        onDismiss = { showSettings = false }
                    )
                }
            }
        }
    }
}

@Composable
fun IconPage(apps: List<AppEntry>, p: Float) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(top = 24.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        apps.chunked(4).forEachIndexed { r, row ->
            val t = stagger(p, r)
            Row(
                Modifier.fillMaxWidth().graphicsLayer {
                    alpha = t
                    translationY = (1f - t) * 24.dp.toPx()
                    renderEffect = if (t < 1f) {
                        val b = (1f - t) * 18.dp.toPx()
                        BlurEffect(b, b, TileMode.Decal)
                    } else null
                }
            ) {
                row.forEach { AppCell(it, Modifier.weight(1f)) }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun AppCell(app: AppEntry, modifier: Modifier = Modifier, showLabel: Boolean = true) {
    val ctx = LocalContext.current
    val icon = LocalIcons.current[app.pkg]
    val size = LocalSettings.current.sizeDp.dp
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (pressed) 0.86f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "press"
    )
    Column(
        modifier.graphicsLayer { scaleX = s; scaleY = s }
            .clickable(interactionSource = src, indication = null) { launchApp(ctx, app.pkg) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) Image(icon, app.label, Modifier.size(size)) else Spacer(Modifier.size(size))
        if (showLabel) Text(
            app.label, Modifier.padding(top = 6.dp).width(68.dp),
            color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 1f), 3f))
        )
    }
}

@Composable
fun AppRow(app: AppEntry, onClick: () -> Unit) {
    val icon = LocalIcons.current[app.pkg]
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Image(icon, app.label, Modifier.size(46.dp)) else Spacer(Modifier.size(46.dp))
        Spacer(Modifier.width(12.dp))
        Text(app.label, color = Color.White, fontSize = 17.sp)
    }
}

@Composable
fun Dock(apps: List<AppEntry>, p: Float, modifier: Modifier) {
    val t = stagger(p, 5)
    val shape = RoundedCornerShape(38.dp)
    val h = (LocalSettings.current.sizeDp + 36f).dp
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp).height(h)
            .graphicsLayer { translationY = (1f - t) * 160.dp.toPx(); alpha = t }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.30f), Color.White.copy(0.14f))))
            .border(0.8.dp, Brush.verticalGradient(listOf(Color.White.copy(0.65f), Color.White.copy(0.08f))), shape)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) { apps.forEach { AppCell(it, showLabel = false) } }
}

@Composable
fun AppLibrary(apps: List<AppEntry>) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    val shown = remember(apps, q) { apps.filter { it.label.contains(q, ignoreCase = true) } }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp).padding(top = 16.dp)) {
        Box(
            Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(0.22f)).padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (q.isEmpty()) Text("App Library", color = Color.White.copy(0.6f), fontSize = 16.sp)
            BasicTextField(
                q, { q = it }, singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(Color.White)
            )
        }
        LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 140.dp)) {
            items(shown, key = { it.pkg }) { app -> AppRow(app) { launchApp(ctx, app.pkg) } }
        }
    }
}
