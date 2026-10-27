package com.geekyabs.glass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.app.ActivityOptions
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.unit.lerp as lerpDp

class MainActivity : ComponentActivity() {
    private val homePresses = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.black)
        Wallpaper.show(resources, LocalDate.now().toEpochDay(), loadWallpaper(this))
        setContent { Home(homePresses) }
    }

    // Pressing Home while already home goes back to the top.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homePresses.intValue++
    }
}

private val Inter = FontFamily(
    Font(R.font.inter_extralight, FontWeight.ExtraLight),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
)
private val Serif = FontFamily(Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic))
private val Legible = Shadow(Color.Black.copy(alpha = .45f), Offset(0f, 2f), 18f)
private val Ink = Color(0xFF111114) // glyphs on a focused (white) control

private fun text(
    size: TextUnit,
    alpha: Float = .9f,
    weight: FontWeight = FontWeight.Normal,
    spacing: TextUnit = TextUnit.Unspecified,
    family: FontFamily = Inter,
    align: TextAlign = TextAlign.Center,
) = TextStyle(
    color = Color.White.copy(alpha = alpha), fontSize = size, fontWeight = weight, letterSpacing = spacing,
    fontFamily = family, textAlign = align, shadow = Legible,
)

private val TileShape = RoundedCornerShape(12.dp)

/**
 * Keeps the D-pad inside an overlay (menu, picker) without blocking code that moves focus out of it,
 * e.g. back to the photo pill while the picker is still fading away.
 */
@OptIn(ExperimentalComposeUiApi::class)
private val TrapDpad: FocusProperties.() -> Unit = {
    exit = { if (it == FocusDirection.Left || it == FocusDirection.Right || it == FocusDirection.Up || it == FocusDirection.Down) FocusRequester.Cancel else FocusRequester.Default }
}

// This GPU repaints the whole screen every frame, and blurred text shadows and glass are what it struggles with.
// Each goes in its own layer: drawn once, then only re-composited until its content actually changes.
private val Cached = Modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)

/** [Cached] with room around the edges, so focus lift and shadows aren't cut off by the layer. Layout is unchanged. */
private fun Modifier.cachedWithBleed(bleed: Dp) = this
    .layout { measurable, constraints ->
        val b = bleed.roundToPx()
        val p = measurable.measure(constraints.offset(2 * b, 2 * b))
        layout(p.width - 2 * b, p.height - 2 * b) { p.place(-b, -b) }
    }
    .then(Cached)
    .padding(bleed)
private val DockPad = 16.dp
private val DockGap = 20.dp // wide enough that the focus ring never touches a neighbour
private const val PHOTO = "__photo" // focus key of the photo pill
private const val DockFocusScale = .06f // the ring carries focus, so tiles only lift a little
private val GridTile = 120.dp // same as a full-size dock tile, so the grid's columns sit under the dock's
private const val GridCols = 6
private val GridWidth = GridTile * GridCols + DockGap * (GridCols - 1)

@Composable
fun Home(homePresses: MutableIntState) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<App>()) }
    var appsVersion by remember { mutableIntStateOf(0) }
    var dock by remember { mutableStateOf(loadDock(ctx)) }
    var usage by remember { mutableStateOf(emptyList<Pair<App, Long>>()) } // today's foreground time, longest first
    val now = rememberNow()

    Broadcast(Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REMOVED, Intent.ACTION_PACKAGE_CHANGED, scheme = "package") { appsVersion++ }
    LaunchedEffect(appsVersion) { apps = withContext(Dispatchers.Default) { loadApps(ctx) } }
    var wall by remember { mutableStateOf(loadWallpaper(ctx)) } // picked photo, or null for a new one each day
    LaunchedEffect(now.toLocalDate(), wall) {
        withContext(Dispatchers.Default) { Wallpaper.show(ctx.resources, now.toLocalDate().toEpochDay(), wall) }
    }
    val pickWallpaper: (Int?) -> Unit = {
        wall = it
        saveWallpaper(ctx, it)
    }

    val byPkg = apps.associateBy { it.pkg }
    // ponytail: recounted every minute even while another app is in front; tie to the lifecycle if it ever shows up in a profile.
    LaunchedEffect(now, apps) {
        usage = withContext(Dispatchers.Default) {
            runCatching { screenTimeToday(ctx, byPkg.keys) }.getOrDefault(emptyList()).mapNotNull { (pkg, ms) -> byPkg[pkg]?.let { it to ms } }
        }
    }
    val dockApps = dock.mapNotNull { byPkg[it] }
    val togglePin: (String) -> Unit = { pkg ->
        dock = if (pkg in dock) dock - pkg else dock + pkg
        saveDock(ctx, dock)
    }
    val moveInDock: (String, Int) -> Unit = { pkg, delta ->
        dock = reorderDock(dock, dockApps.map { it.pkg }, pkg, delta)
        saveDock(ctx, dock)
    }

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                Wallpaper.sharp?.let { drawImage(it, dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.Low) }
            }
    ) {
        HomeScreen(now, dockApps, apps.filter { it.pkg !in dock }, apps.isNotEmpty(), homePresses.intValue, usage, wall, pickWallpaper, togglePin, moveInDock)
    }
}

private class Menu(val app: App, val anchor: Rect, val inDock: Boolean)

/** Layout facts the scroll needs. Plain fields, read only in callbacks and draw, so updating them never recomposes. */
private class Geometry {
    var column: LayoutCoordinates? = null
    var dock: LayoutCoordinates? = null
    var page = 0f

    /** Scroll offset of page 2: the dock becomes the top row. */
    fun page2(margin: Float): Float {
        val c = column
        val d = dock
        return if (c != null && d != null && c.isAttached && d.isAttached) c.localPositionOf(d, Offset.Zero).y - margin else page
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(
    now: LocalDateTime,
    dockApps: List<App>,
    rest: List<App>, // everything not in the dock
    loaded: Boolean,
    homePresses: Int,
    usage: List<Pair<App, Long>>,
    wall: Int?,
    onPickWallpaper: (Int?) -> Unit,
    togglePin: (String) -> Unit,
    onMove: (String, Int) -> Unit,
) {
    val ctx = LocalContext.current
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    fun req(key: String) = requesters.getOrPut(key) { FocusRequester() }
    var caption by remember { mutableStateOf("") }
    var moving by remember { mutableStateOf<String?>(null) } // pkg being rearranged
    var menu by remember { mutableStateOf<Menu?>(null) }
    var picking by remember { mutableStateOf(false) } // wallpaper picker open
    var dockRing by remember { mutableStateOf<Rect?>(null) } // focused tile, window coords
    var gridRing by remember { mutableStateOf<Rect?>(null) }
    var dockFocused by remember { mutableStateOf(false) }
    var gridFocused by remember { mutableStateOf(false) }
    var refocus by remember { mutableStateOf<String?>(null) }
    val nowPlaying = rememberNowPlaying()
    val geo = remember { Geometry() }
    val scroll = remember { Animatable(0f) }
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(target) { scroll.animateTo(target, spring(dampingRatio = 1f, stiffness = 260f)) }

    fun first() = (dockApps.firstOrNull() ?: rest.firstOrNull())?.pkg
    LaunchedEffect(loaded) {
        if (loaded) {
            withFrameNanos { }
            first()?.let { runCatching { req(it).requestFocus() } }
        }
    }
    LaunchedEffect(refocus) {
        val key = refocus ?: return@LaunchedEffect
        withFrameNanos { }
        runCatching { req(key).requestFocus() }
        refocus = null
    }
    fun toTop() {
        menu = null
        picking = false
        moving = null
        refocus = first()
    }
    LaunchedEffect(homePresses) { if (homePresses > 0) toTop() }
    BackHandler { toTop() } // a launcher has nothing to go back to

    // Move mode: ◀ ▶ slide, OK/Back drop; everything else is held so focus can't wander off mid-move.
    fun moveKeys(pkg: String) = Modifier.onPreviewKeyEvent { e ->
        val down = e.type == KeyEventType.KeyDown
        when (e.key) {
            Key.DirectionLeft, Key.DirectionRight -> {
                if (down) onMove(pkg, if (e.key == Key.DirectionLeft) -1 else 1)
                true
            }
            Key.DirectionUp, Key.DirectionDown -> true
            Key.Back, Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                if (down && e.nativeKeyEvent.repeatCount == 0) moving = null
                true
            }
            else -> false
        }
    }
    val captionFor: (String) -> (Boolean) -> Unit = { label -> { f -> if (f) caption = label else if (caption == label) caption = "" } }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // Android hands the window its first focus on its own schedule, sometimes after ours above;
            // whenever focus comes in from outside, it lands on the first dock app, not the gear or a grid tile.
            .focusProperties { enter = { first()?.let(::req) ?: FocusRequester.Default } }
            .focusGroup(),
    ) {
        val density = LocalDensity.current
        val viewport = constraints.maxHeight.toFloat()
        val pageHeight = maxHeight
        val margin = with(density) { 30.dp.toPx() }
        val peek = with(density) { 180.dp.toPx() } // how much below the focused row stays visible: from the last row, the whole footer
        val slack = with(density) { 4.dp.toPx() } // ignore the focus lift
        geo.page = viewport
        fun progress() = (scroll.value / geo.page2(margin).coerceAtLeast(1f)).coerceIn(0f, 1f)

        Box(Modifier.fillMaxSize().drawBehind { progress().let { if (it > 0f) drawRect(Color.Black, alpha = .4f * it) } })
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .offset { IntOffset(0, -scroll.value.roundToInt()) }
                .onGloballyPositioned { geo.column = it }
                // Focus on page 1 shows page 1; anything below scrolls so the dock is the top row and the focused row stays in view.
                .onFocusedBoundsChanged { focused ->
                    val col = geo.column
                    if (focused == null || col == null || !focused.isAttached || !col.isAttached) return@onFocusedBoundsChanged
                    val top = col.localPositionOf(focused, Offset.Zero).y
                    val t = if (top < viewport - slack) 0f // slack: page 2's first row starts at exactly one viewport, give or take a layer's rounding
                    else minOf(top + focused.size.height + peek - viewport, col.size.height - viewport).coerceAtLeast(geo.page2(margin))
                    if (abs(t - target) > slack) target = t
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().height(pageHeight).padding(horizontal = 44.dp, vertical = 30.dp)) {
                Row(
                    Modifier.align(Alignment.TopStart).fillMaxWidth().graphicsLayer { alpha = 1f - progress() }.cachedWithBleed(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")), style = text(15.sp, .85f, FontWeight.Medium, align = TextAlign.Start))
                    nowPlaying?.let {
                        Spacer(Modifier.width(18.dp))
                        NowPlayingPill(it)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(GearIcon, "Settings") { ctx.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    Spacer(Modifier.width(12.dp))
                    val net = rememberNetwork()
                    IconButton(WifiIcon, net, dim = net == "Offline") { runCatching { ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) } }
                }

                Column(
                    Modifier.align(Alignment.Center).offset(y = (-34).dp).graphicsLayer { alpha = 1f - progress() },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Clock(running = target == 0f) // off screen on page 2: no point repainting every second
                    Spacer(Modifier.height(26.dp))
                    val q = QUOTES[Math.floorMod(now.toLocalDate().toEpochDay(), QUOTES.size.toLong()).toInt()]
                    BasicText(q.text, Modifier.widthIn(max = 620.dp).then(Cached), style = text(22.sp, .9f, family = Serif).copy(lineHeight = 31.sp))
                    Spacer(Modifier.height(14.dp))
                    BasicText(q.author.uppercase(), Cached, style = text(10.sp, .55f, FontWeight.Medium, .22.em))
                }

                if (dockApps.isNotEmpty()) BoxWithConstraints(Modifier.align(Alignment.BottomCenter)) {
                    // Tiles shrink to keep the whole dock on screen; past 96dp it scrolls instead.
                    val n = dockApps.size
                    val tileW = ((maxWidth - DockPad * 2 - DockGap * (n - 1)) / n).coerceIn(96.dp, 120.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.onGloballyPositioned { geo.dock = it }) {
                            LazyRow(
                                Cached.glass(RoundedCornerShape(12.dp + DockPad)).onFocusChanged { dockFocused = it.hasFocus }, // concentric with tiles
                                contentPadding = PaddingValues(DockPad),
                                horizontalArrangement = Arrangement.spacedBy(DockGap),
                            ) {
                                items(dockApps, key = { it.pkg }) { app ->
                                    val isMoving = moving == app.pkg
                                    Box(Modifier.animateItem()) {
                                        AppTile(
                                            app, tileW, req(app.pkg), focusScale = DockFocusScale,
                                            onFocus = { f ->
                                                captionFor(app.label)(f)
                                                if (!f && isMoving) moving = null
                                            },
                                            onFocusBounds = { dockRing = it },
                                            moving = isMoving,
                                            dimmed = moving != null && !isMoving,
                                            modifier = (if (isMoving) moveKeys(app.pkg) else Modifier).focusProperties { // dock ends here; don't jump up to the buttons
                                                if (app === dockApps.first()) left = FocusRequester.Cancel
                                                if (app === dockApps.last()) right = FocusRequester.Cancel
                                            },
                                        ) { menu = Menu(app, it, inDock = true) }
                                    }
                                }
                            }
                            FocusRing({ dockRing }, dockFocused && moving == null && menu == null, DockFocusScale, Modifier.matchParentSize())
                        }
                        Spacer(Modifier.height(10.dp))
                        BasicText(
                            if (moving != null) "← →  move      OK  done" else caption,
                            Modifier.height(18.dp).then(Cached),
                            style = text(13.sp, .85f, FontWeight.Medium),
                        )
                    }
                }
            }

            Column(Modifier.width(GridWidth).padding(bottom = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                nowPlaying?.let {
                    NowPlayingCard(it)
                    Spacer(Modifier.height(30.dp))
                }
                Box {
                    Column(Modifier.cachedWithBleed(16.dp).onFocusChanged { gridFocused = it.hasFocus }, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        rest.chunked(GridCols).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(DockGap)) {
                                row.forEachIndexed { i, app ->
                                    key(app.pkg) {
                                        AppTile(
                                            app, GridTile, req(app.pkg), showLabel = true, focusScale = DockFocusScale,
                                            onFocusBounds = { gridRing = it },
                                            modifier = Modifier.focusProperties { // rows end at their edges; don't leap to the dock or the buttons
                                                if (i == 0) left = FocusRequester.Cancel
                                                if (i == row.lastIndex) right = FocusRequester.Cancel
                                            },
                                        ) { menu = Menu(app, it, inDock = false) }
                                    }
                                }
                            }
                        }
                    }
                    FocusRing({ gridRing }, gridFocused && menu == null, DockFocusScale, Modifier.matchParentSize())
                }
                Spacer(Modifier.height(30.dp))
                if (usage.sumOf { it.second } >= 60_000) {
                    ScreenTimePill(usage)
                    Spacer(Modifier.height(12.dp))
                }
                PhotoPill(req(PHOTO)) { picking = true }
            }
        }

        menu?.let { m ->
            val pkg = m.app.pkg
            fun done(focus: String) {
                menu = null
                refocus = focus
            }
            AppMenu(
                m.anchor,
                if (m.inDock) listOf(
                    "Move" to {
                        done(pkg)
                        moving = pkg
                    },
                    "Unpin" to {
                        val i = dockApps.indexOfFirst { it.pkg == pkg }
                        done((dockApps.getOrNull(i + 1) ?: dockApps.getOrNull(i - 1))?.pkg ?: pkg)
                        togglePin(pkg)
                    },
                    "App info" to {
                        done(pkg)
                        openAppInfo(ctx, pkg)
                    },
                ) else listOf(
                    "Pin to dock" to { // focus follows the app into the dock, so you see where it landed
                        done(pkg)
                        togglePin(pkg)
                    },
                    "App info" to {
                        done(pkg)
                        openAppInfo(ctx, pkg)
                    },
                ),
            ) { done(pkg) }
        }

        AnimatedVisibility(picking, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            fun close() {
                picking = false
                refocus = PHOTO
            }
            WallpaperPicker(
                wall,
                onPick = {
                    onPickWallpaper(it)
                    close()
                },
                onDismiss = ::close,
                Modifier.animateEnterExit(enter = scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 1.04f)),
            )
        }
    }
}

/** Full-screen glass sheet: "Daily" plus every photo. OK picks; the new photo lands as the sheet fades out. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WallpaperPicker(chosen: Int?, onPick: (Int?) -> Unit, onDismiss: () -> Unit, modifier: Modifier) {
    val ctx = LocalContext.current
    val today = remember { Wallpaper.ofDay(LocalDate.now().toEpochDay()) }
    val thumbs = remember { mutableStateListOf<ImageBitmap?>().apply { repeat(Wallpaper.count) { add(null) } } }
    LaunchedEffect(Unit) { // decoded in parallel, each tile filling in as soon as its own is ready
        for (i in 0 until Wallpaper.count) launch { thumbs[i] = withContext(Dispatchers.Default) { Wallpaper.thumbnail(ctx.resources, i) } }
    }
    val start = remember { FocusRequester() }
    var ring by remember { mutableStateOf<Rect?>(null) }
    var gridFocused by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf(chosen) } // what the caption describes: the focused tile (null = Daily)
    BackHandler(onBack = onDismiss)
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { start.requestFocus() }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Cached.fillMaxSize().glass(RectangleShape, scrim = .55f, hairline = false))
        Column(
            modifier.focusProperties(TrapDpad).focusGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText("Wallpaper", style = text(15.sp, .6f, FontWeight.Medium))
            Spacer(Modifier.height(28.dp))
            Box {
                // Seven across keeps every photo on one screen: you see them all at once, no scrolling.
                Column(Modifier.onFocusChanged { gridFocused = it.hasFocus }, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    (listOf<Int?>(null) + (0 until Wallpaper.count)).chunked(7).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            row.forEach { i ->
                                Tile(
                                    108.dp, if (i == chosen) start else null, onClick = { onPick(i) },
                                    onFocus = { if (it) info = i }, onFocusBounds = { ring = it }, focusScale = DockFocusScale,
                                ) {
                                    thumbs[i ?: today]?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                                    if (i == null) { // Daily: today's photo, dimmed, under a shuffle glyph
                                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)))
                                        Image(rememberVectorPainter(ShuffleIcon), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(Color.White.copy(alpha = .9f)))
                                    }
                                    if (i == chosen) Box(
                                        Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp).background(Color.White, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Image(rememberVectorPainter(CheckIcon), null, Modifier.size(12.dp), colorFilter = ColorFilter.tint(Ink))
                                    }
                                }
                            }
                        }
                    }
                }
                FocusRing({ ring }, gridFocused, DockFocusScale, Modifier.matchParentSize())
            }
            Spacer(Modifier.height(28.dp))
            val i = info
            BasicText(if (i == null) "Daily" else Wallpaper.placeOf(i), style = text(15.sp, .95f, FontWeight.Medium))
            Spacer(Modifier.height(6.dp))
            BasicText(if (i == null) "A different photo every day" else Wallpaper.creditOf(i), style = text(13.sp, .55f))
        }
    }
}

/** Hold-OK menu: a glass panel of pills beside the tile. Focus stays inside until an item or Back closes it. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AppMenu(anchor: Rect, items: List<Pair<String, () -> Unit>>, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    val enter = remember { Animatable(0f) }
    BackHandler(onBack = onDismiss)
    LaunchedEffect(Unit) {
        launch { enter.animateTo(1f, tween(160, easing = FastOutSlowInEasing)) }
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    Box(Modifier.fillMaxSize().drawBehind { drawRect(Color.Black, alpha = .25f * enter.value) }) {
        Column(
            Modifier
                .layout { measurable, constraints ->
                    // Centred above the tile, or below it when there's no room above.
                    val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    val gap = 14.dp.roundToPx()
                    val x = (anchor.center.x - p.width / 2).roundToInt().coerceIn(gap, constraints.maxWidth - p.width - gap)
                    val above = (anchor.top - gap - p.height).roundToInt()
                    val y = if (above >= gap) above else (anchor.bottom + gap).roundToInt()
                    layout(constraints.maxWidth, constraints.maxHeight) { p.place(x, y) }
                }
                .graphicsLayer {
                    val s = .92f + .08f * enter.value
                    scaleX = s
                    scaleY = s
                    alpha = enter.value
                }
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .glass(RoundedCornerShape(24.dp), scrim = .5f) // concentric with the 18dp pills inside 6dp of padding
                .padding(6.dp)
                .focusProperties(TrapDpad)
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items.forEachIndexed { i, (label, action) -> MenuItem(label, if (i == 0) first else null, action) }
        }
    }
}

@Composable
private fun MenuItem(label: String, requester: FocusRequester?, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, tween(120), label = "item")
    BasicText(
        label,
        Modifier
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .widthIn(min = 196.dp)
            .background(Color.White.copy(alpha = f), CircleShape)
            .onFocusChanged { focused = it.isFocused }
            .tvClick(onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        style = text(14.sp, weight = FontWeight.Medium, align = TextAlign.Start).copy(shadow = Shadow.None),
        color = { lerp(Color.White.copy(alpha = .9f), Ink, f) },
    )
}

/** Hours:minutes with smaller seconds beside them; each digit rolls up when it changes. */
@Composable
private fun Clock(running: Boolean) {
    val ctx = LocalContext.current
    val second = rememberSecond(running)
    val t = remember(second) { LocalTime.now() }
    val hm = remember { DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(ctx)) "H:mm" else "h:mm") }
    Layout({
        Rolling(t.format(hm), text(108.sp, .96f, FontWeight.ExtraLight, (-0.03).em).copy(fontFeatureSettings = "tnum"))
        Rolling("%02d".format(t.second), text(34.sp, .5f, FontWeight.ExtraLight).copy(fontFeatureSettings = "tnum"))
    }) { (main, secs), constraints ->
        val m = main.measure(constraints.copy(minWidth = 0))
        val s = secs.measure(constraints.copy(minWidth = 0))
        // Seconds hang off the right edge, so hours:minutes stay centred over the quote. Same font at both sizes,
        // so the baseline is a fixed fraction of the line height (Inter: ascent / (ascent + descent) ≈ 0.8).
        layout(m.width, m.height) {
            m.place(0, 0)
            s.place(m.width + 10.dp.roundToPx(), (m.height - s.height) * 4 / 5)
        }
    }
}

@Composable
private fun Rolling(value: String, style: TextStyle, modifier: Modifier = Modifier) {
    Row(modifier) {
        value.forEachIndexed { i, c ->
            key(value.length - i) { // keyed from the right: minutes stay put when the hour gains a digit
                AnimatedContent(
                    c,
                    transitionSpec = {
                        (slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it * 2 / 5 } + fadeIn(tween(320))) togetherWith
                            (slideOutVertically(tween(320, easing = FastOutSlowInEasing)) { -it * 2 / 5 } + fadeOut(tween(180))) using
                            SizeTransform(clip = false)
                    },
                    label = "digit",
                ) { BasicText(it.toString(), Cached, style = style) }
            }
        }
    }
}

/** Glass pill beside the date while something plays; OK opens the app. */
@Composable
private fun NowPlayingPill(np: NowPlaying) {
    val ctx = LocalContext.current
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, label = "pill")
    val ink = lerp(Color.White.copy(alpha = .9f), Ink, f)
    Row(
        Modifier
            .height(40.dp)
            .widthIn(max = 380.dp)
            .graphicsLayer {
                val s = 1f + .06f * f
                scaleX = s
                scaleY = s
                alpha = if (np.playing) 1f else .55f + .45f * f
            }
            .shadow(lerpDp(0.dp, 10.dp, f), CircleShape)
            .glass(CircleShape)
            .background(Color.White.copy(alpha = f), CircleShape)
            .onFocusChanged { focused = it.isFocused }
            .tvClick({ launch(ctx, np.pkg) })
            .padding(start = 14.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(rememberVectorPainter(NoteIcon), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(ink))
        Spacer(Modifier.width(8.dp))
        BasicText(
            listOf(np.title, np.artist).filter { it.isNotEmpty() }.joinToString("  ·  "),
            style = text(13.sp, weight = FontWeight.Medium).copy(shadow = Shadow.None),
            color = { ink },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NowPlayingCard(np: NowPlaying) {
    rememberSecond() // moves the progress bar along
    val playPause = remember { FocusRequester() }
    Row(
        Cached
            .fillMaxWidth()
            .glass(RoundedCornerShape(12.dp + 16.dp))
            .padding(16.dp)
            .focusProperties {
                enter = { playPause } // arriving from anywhere lands on ⏯, not whichever button is nearest
                exit = { if (it == FocusDirection.Left || it == FocusDirection.Right) FocusRequester.Cancel else FocusRequester.Default }
            }
            .focusGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(72.dp).clip(TileShape).background(Color.White.copy(alpha = .08f)), contentAlignment = Alignment.Center) {
            if (np.art != null) Image(np.art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Image(rememberVectorPainter(NoteIcon), null, Modifier.size(28.dp), colorFilter = ColorFilter.tint(Color.White.copy(alpha = .5f)))
        }
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            BasicText(np.title, style = text(18.sp, .95f, FontWeight.Medium, align = TextAlign.Start), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            BasicText(np.artist, style = text(14.sp, .6f, align = TextAlign.Start), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (np.duration > 0) {
                val pos = np.positionAt(SystemClock.elapsedRealtime()).coerceIn(0, np.duration)
                val small = text(11.sp, .55f).copy(fontFeatureSettings = "tnum")
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(mss(pos), style = small)
                    Box(Modifier.weight(1f).padding(horizontal = 10.dp).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = .18f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(pos.toFloat() / np.duration).background(Color.White.copy(alpha = .85f)))
                    }
                    BasicText("−" + mss(np.duration - pos), style = small)
                }
            }
        }
        Spacer(Modifier.width(28.dp))
        IconButton(PrevIcon, "Previous") { np.controls.skipToPrevious() }
        Spacer(Modifier.width(12.dp))
        IconButton(if (np.playing) PauseIcon else PlayIcon, if (np.playing) "Pause" else "Play", Modifier.focusRequester(playPause)) {
            if (np.playing) np.controls.pause() else np.controls.play()
        }
        Spacer(Modifier.width(12.dp))
        IconButton(NextIcon, "Next") { np.controls.skipToNext() }
    }
}

private fun mss(ms: Long) = "%d:%02d".format(ms / 60_000, ms / 1000 % 60)

private fun hm(ms: Long): String {
    val m = ms / 60_000
    return when {
        m < 60 -> "${m}m"
        m % 60 == 0L -> "${m / 60}h"
        else -> "${m / 60}h ${m % 60}m"
    }
}

/** Text on glass: the material already gives contrast, so no shadow. */
private fun onGlass(size: TextUnit, alpha: Float, weight: FontWeight = FontWeight.Normal) =
    text(size, alpha, weight, align = TextAlign.Start).copy(shadow = Shadow.None, fontFeatureSettings = "tnum")

/** ⧗ 2h 14m today  |  [icon] 1h 20m  [icon] 40m  [icon] 12m — the top three apps shown by their icons. */
@Composable
private fun ScreenTimePill(usage: List<Pair<App, Long>>) {
    Row(Cached.height(44.dp).glass(CircleShape).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(rememberVectorPainter(HourglassIcon), null, Modifier.size(15.dp), colorFilter = ColorFilter.tint(Color.White.copy(alpha = .55f)))
        Spacer(Modifier.width(10.dp))
        BasicText(hm(usage.sumOf { it.second }), style = onGlass(14.sp, .95f, FontWeight.Medium))
        Spacer(Modifier.width(6.dp))
        BasicText("today", style = onGlass(13.sp, .5f))
        val top = usage.take(3).filter { it.second >= 60_000 }
        if (top.isNotEmpty()) {
            Box(Modifier.padding(horizontal = 16.dp).size(1.dp, 18.dp).background(Color.White.copy(alpha = .14f)))
            top.forEachIndexed { i, (app, ms) ->
                if (i > 0) Spacer(Modifier.width(18.dp))
                Image(app.icon, app.label, Modifier.size(20.dp).clip(RoundedCornerShape(5.dp)))
                Spacer(Modifier.width(8.dp))
                BasicText(hm(ms), style = onGlass(13.sp, .8f))
            }
        }
    }
}

/** Where the photo was taken, captioned the way Apple TV captions its aerials. OK opens the wallpaper picker. */
@Composable
private fun PhotoPill(requester: FocusRequester, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, label = "photo")
    val ink = lerp(Color.White, Ink, f)
    Row(
        Modifier
            .height(36.dp)
            .graphicsLayer {
                val s = 1f + .04f * f
                scaleX = s
                scaleY = s
            }
            .shadow(lerpDp(0.dp, 10.dp, f), CircleShape)
            .then(Cached)
            .glass(CircleShape, scrim = .5f)
            .background(Color.White.copy(alpha = f), CircleShape)
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .tvClick(onClick)
            .padding(start = 14.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(rememberVectorPainter(PinIcon), null, Modifier.size(14.dp), colorFilter = ColorFilter.tint(ink.copy(alpha = .55f + .45f * f)))
        Spacer(Modifier.width(8.dp))
        BasicText(Wallpaper.place, style = onGlass(12.sp, 1f, FontWeight.Medium), color = { ink.copy(alpha = .85f + .15f * f) })
        Spacer(Modifier.width(10.dp))
        BasicText(Wallpaper.credit, style = onGlass(12.sp, 1f), color = { ink.copy(alpha = .45f + .25f * f) })
    }
}

@Composable
private fun AppTile(
    app: App,
    width: Dp,
    requester: FocusRequester?,
    showLabel: Boolean = false,
    onFocus: (Boolean) -> Unit = {},
    onFocusBounds: (Rect) -> Unit = {},
    focusScale: Float = .1f,
    moving: Boolean = false,
    dimmed: Boolean = false,
    modifier: Modifier = Modifier,
    onLongClick: (Rect) -> Unit,
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val zoomFromTile: (Rect) -> Unit = { r ->
        val opts = ActivityOptions.makeScaleUpAnimation(view, r.left.toInt(), r.top.toInt(), r.width.toInt(), r.height.toInt())
        launch(ctx, app.pkg, opts.toBundle())
    }
    Tile(
        width, requester, onClick = zoomFromTile, onLongClick = onLongClick, label = app.label.takeIf { showLabel }, onFocus = onFocus,
        onFocusBounds = onFocusBounds, focusScale = focusScale, moving = moving, dimmed = dimmed, modifier = modifier,
    ) {
        Box(Modifier.fillMaxSize().background(app.tint))
        if (app.banner != null) Image(app.banner, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Image(app.icon, app.label, Modifier.fillMaxHeight(.5f))
    }
}

/** tvOS-style tile: edge-to-edge content; focus lifts it (scale + shadow), adds a top sheen, fades the label in. */
@Composable
private fun Tile(
    width: Dp,
    requester: FocusRequester?,
    onClick: (Rect) -> Unit,
    onLongClick: ((Rect) -> Unit)? = null,
    label: String? = null,
    onFocus: (Boolean) -> Unit = {},
    onFocusBounds: (Rect) -> Unit = {}, // window bounds of the (unscaled) tile while focused
    focusScale: Float = .1f, // how much focus enlarges the tile
    moving: Boolean = false,
    dimmed: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val lift by animateFloatAsState(if (focused) 1f else 0f, spring(dampingRatio = .75f, stiffness = 500f), label = "lift")
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(stiffness = 1500f), label = "press")
    val move by animateFloatAsState(if (moving) 1f else 0f, spring(dampingRatio = .6f, stiffness = 500f), label = "move")
    val dim by animateFloatAsState(if (dimmed) .45f else 1f, label = "dim")
    val wiggle = if (moving) {
        rememberInfiniteTransition(label = "wiggle").animateFloat(-1.4f, 1.4f, infiniteRepeatable(tween(140), RepeatMode.Reverse), label = "angle")
    } else null
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width, width * 9 / 16)
                .onGloballyPositioned {
                    bounds = it.boundsInWindow()
                    if (focused) onFocusBounds(bounds) // keeps the ring on the tile while the dock reflows
                }
                .graphicsLayer {
                    val s = 1f + focusScale * lift + .06f * move - .06f * press
                    scaleX = s
                    scaleY = s
                    rotationZ = wiggle?.value ?: 0f
                    alpha = dim
                }
                .shadow(lerpDp(1.dp, 12.dp, lift + .4f * move - .5f * press), TileShape)
                .clip(TileShape)
                .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
                .then(modifier)
                .onFocusChanged {
                    focused = it.isFocused
                    onFocus(it.isFocused)
                    if (it.isFocused) onFocusBounds(bounds)
                }
                .tvClick({ onClick(bounds) }, onLongClick?.let { f -> { f(bounds) } }, onPress = { pressed = it })
                .drawWithContent {
                    drawContent()
                    if (lift > 0f) drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = .22f * lift), .5f to Color.Transparent))
                },
            contentAlignment = Alignment.Center,
            content = content,
        )
        if (label != null) {
            Spacer(Modifier.height(10.dp))
            BasicText(
                label,
                Modifier.width(width).graphicsLayer { alpha = lift },
                style = text(12.sp, .9f, FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One white ring that springs from tile to tile, so the eye can follow focus as it moves. */
@Composable
private fun FocusRing(target: () -> Rect?, visible: Boolean, tileScale: Float, modifier: Modifier) {
    var origin by remember { mutableStateOf<Offset?>(null) }
    // Animate in the ring's own coordinates: when the whole page scrolls, tile and ring move together and nothing lags.
    val local = origin?.let { o -> target()?.translate(-o) }
    val rect = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
    var placed by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (visible && local != null) 1f else 0f, tween(160), label = "ring")
    LaunchedEffect(local) {
        val t = local ?: return@LaunchedEffect
        if (placed) rect.animateTo(t, spring(dampingRatio = .8f, stiffness = 420f))
        else {
            rect.snapTo(t) // first appearance: no fly-in from the corner
            placed = true
        }
    }
    val paint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { style = android.graphics.Paint.Style.STROKE } }
    Canvas(modifier.onGloballyPositioned { origin = it.positionInWindow() }) {
        if (alpha == 0f) return@Canvas
        val r = rect.value
        val gap = 3.dp.toPx()
        // The focused tile is scaled by [tileScale]; the ring sits [gap] outside it, with a concentric corner radius.
        val hw = r.width * (1 + tileScale) / 2 + gap
        val hh = r.height * (1 + tileScale) / 2 + gap
        val c = r.center
        val radius = 12.dp.toPx() * (1 + tileScale) + gap
        paint.strokeWidth = 2.dp.toPx()
        paint.color = Color.White.copy(alpha = alpha).toArgb()
        paint.setShadowLayer(8.dp.toPx(), 0f, 0f, Color.White.copy(alpha = .35f * alpha).toArgb()) // soft glow
        drawIntoCanvas { it.nativeCanvas.drawRoundRect(c.x - hw, c.y - hh, c.x + hw, c.y + hh, radius, radius, paint) }
    }
}

/** Icon-only glass circle; focused = solid white with a dark glyph (tvOS button idiom). */
@Composable
private fun IconButton(icon: ImageVector, description: String, modifier: Modifier = Modifier, dim: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val f by animateFloatAsState(if (focused) 1f else 0f, label = "focus")
    Box(
        Modifier
            .size(40.dp)
            .graphicsLayer { val s = 1f + .08f * f; scaleX = s; scaleY = s }
            .shadow(lerpDp(0.dp, 10.dp, f), CircleShape)
            .glass(CircleShape)
            .background(Color.White.copy(alpha = f), CircleShape)
            .then(modifier)
            .onFocusChanged { focused = it.isFocused }
            .tvClick(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            rememberVectorPainter(icon),
            description,
            Modifier.size(19.dp),
            colorFilter = ColorFilter.tint(lerp(Color.White.copy(alpha = if (dim) .35f else .9f), Ink, f)),
        )
    }
}

/**
 * D-pad OK = click on release; holding OK = long click (fires once, suppresses the click).
 * Only a press that began here counts: when a long press opens something new under the finger,
 * the still-held key's repeats and release must not click it.
 */
private fun Modifier.tvClick(onClick: () -> Unit, onLongClick: (() -> Unit)? = null, onPress: (Boolean) -> Unit = {}): Modifier = composed {
    var longFired by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    onKeyEvent { e ->
        if (e.key != Key.DirectionCenter && e.key != Key.Enter && e.key != Key.NumPadEnter) return@onKeyEvent false
        val repeat = e.nativeKeyEvent.repeatCount
        when (e.type) {
            KeyEventType.KeyDown -> when {
                repeat == 0 -> {
                    pressed = true
                    onPress(true)
                }
                repeat == 1 && pressed && onLongClick != null -> {
                    longFired = true
                    onLongClick()
                }
            }
            KeyEventType.KeyUp -> {
                if (pressed) {
                    onPress(false)
                    if (!longFired) onClick()
                }
                pressed = false
                longFired = false
            }
        }
        true
    }.onFocusChanged {
        if (!it.isFocused && pressed) {
            pressed = false
            longFired = false
            onPress(false)
        }
    }.focusable()
}

@Composable
private fun Broadcast(vararg actions: String, scheme: String? = null, onReceive: () -> Unit) {
    val ctx = LocalContext.current
    val current by rememberUpdatedState(onReceive)
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = current()
        }
        val filter = IntentFilter().apply {
            actions.forEach(::addAction)
            scheme?.let(::addDataScheme)
        }
        ctx.registerReceiver(receiver, filter)
        onDispose { ctx.unregisterReceiver(receiver) }
    }
}

@Composable
private fun rememberNow(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    Broadcast(Intent.ACTION_TIME_TICK, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_SCREEN_ON) {
        now = LocalDateTime.now()
    }
    return now
}

/** Wall-clock millis, refreshed just after every whole second while Home is on screen and [running] (stops behind other apps). */
@Composable
private fun rememberSecond(running: Boolean = true): Long {
    var t by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, running) {
        if (running) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                t = System.currentTimeMillis()
                delay(1005 - t % 1000) // a few ms past the boundary, so LocalTime.now() is already on the new second
            }
        }
    }
    return t
}

@Composable
private fun rememberNetwork(): String {
    val ctx = LocalContext.current
    var state by remember { mutableStateOf("Offline") }
    DisposableEffect(Unit) {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                state = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi‑Fi"
                    else -> "Online"
                }
            }

            override fun onLost(network: Network) {
                state = "Offline"
            }
        }
        cm.registerDefaultNetworkCallback(callback, Handler(Looper.getMainLooper()))
        onDispose { cm.unregisterNetworkCallback(callback) }
    }
    return state
}
