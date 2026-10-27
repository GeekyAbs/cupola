package com.geekyabs.glass

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import java.time.LocalDate
import java.time.ZoneId

/** banner = the app's 16:9 Android TV artwork; apps without one get icon on [tint]. */
class App(val pkg: String, val label: String, val banner: ImageBitmap?, val icon: ImageBitmap, val tint: Color)

fun loadApps(ctx: Context): List<App> {
    val pm = ctx.packageManager
    return listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        .flatMap { pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(it), 0) }
        .distinctBy { it.activityInfo.packageName } // leanback entry wins: it carries the banner
        .filter { it.activityInfo.packageName != ctx.packageName && "factory" !in it.activityInfo.packageName.lowercase() } // hide vendor service menus
        .map { ri ->
            val a = ri.activityInfo
            val icon = render(a.loadIcon(pm), 144, 144)
            App(a.packageName, ri.loadLabel(pm).toString(), a.loadBanner(pm)?.let { render(it, 320, 180).asImageBitmap() }, icon.asImageBitmap(), tintOf(icon))
        }
        .sortedBy { it.label.lowercase() }
}

private fun render(d: Drawable, w: Int, h: Int): Bitmap =
    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { d.setBounds(0, 0, w, h); d.draw(Canvas(it)) }

/** Icon's average opaque colour, pushed toward black so white text and the icon sit on it well. */
private fun tintOf(icon: Bitmap): Color {
    var r = 0L
    var g = 0L
    var b = 0L
    var n = 0L
    for (y in 0 until icon.height step 4) for (x in 0 until icon.width step 4) {
        val p = icon.getPixel(x, y)
        if (p ushr 24 < 128) continue
        r += p shr 16 and 0xFF
        g += p shr 8 and 0xFF
        b += p and 0xFF
        n++
    }
    if (n == 0L) return Color(0xFF1C1C1E)
    return lerp(Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt()), Color.Black, .55f)
}

fun launch(ctx: Context, pkg: String, options: Bundle? = null) {
    val pm = ctx.packageManager
    val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: return
    runCatching { ctx.startActivity(intent, options) }
}

fun openAppInfo(ctx: Context, pkg: String) {
    runCatching { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))) }
}

private const val DEFAULT_DOCK =
    "com.google.android.youtube.tv,com.netflix.ninja,in.startv.hotstar,com.spotify.tv.android,org.videolan.vlc"

private fun prefs(ctx: Context) = ctx.getSharedPreferences("glass", Context.MODE_PRIVATE)

fun loadDock(ctx: Context): List<String> =
    prefs(ctx).getString("dock", DEFAULT_DOCK)!!.split(',').filter { it.isNotEmpty() }

/**
 * Moves [pkg] by [delta] slots among the dock apps actually shown ([visible]).
 * Pins whose app is uninstalled stay saved at the end, so they come back if it's reinstalled.
 */
fun reorderDock(dock: List<String>, visible: List<String>, pkg: String, delta: Int): List<String> {
    val i = visible.indexOf(pkg)
    val j = i + delta
    if (i < 0 || j !in visible.indices) return dock
    return visible.toMutableList().apply { add(j, removeAt(i)) } + dock.filter { it !in visible }
}

fun saveDock(ctx: Context, dock: List<String>) =
    prefs(ctx).edit().putString("dock", dock.joinToString(",")).apply()

/** The wallpaper picked in the picker, or null for a different photo each day. */
fun loadWallpaper(ctx: Context): Int? = prefs(ctx).getInt("wall", -1).takeIf { it >= 0 }

fun saveWallpaper(ctx: Context, index: Int?) = prefs(ctx).edit().putInt("wall", index ?: -1).apply()

/** Today's foreground time for [pkgs], longest first. Empty until usage access is granted over adb. */
fun screenTimeToday(ctx: Context, pkgs: Set<String>): List<Pair<String, Long>> {
    val now = System.currentTimeMillis()
    val dayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    // Raw events, not queryUsageStats: its daily buckets roll over at UTC midnight, not local midnight.
    val q = ctx.getSystemService(UsageStatsManager::class.java).queryEvents(dayStart - 6 * 3_600_000, now) // catch sessions begun before midnight
    val events = ArrayList<Triple<String, Int, Long>>()
    val e = UsageEvents.Event()
    while (q.getNextEvent(e)) events += Triple(e.packageName, e.eventType, e.timeStamp)
    // The screensaver is a launchable app too, but its time isn't anyone watching.
    val dreams = Settings.Secure.getString(ctx.contentResolver, "screensaver_components").orEmpty().split(',').map { it.substringBefore('/') }
    return foregroundTimes(events, dayStart, now).filterKeys { it in pkgs && it !in dreams }.toList().sortedByDescending { it.second }
}

/** Foreground ms per package within [dayStart, now], from (package, event type, time) usage events in time order. */
fun foregroundTimes(events: List<Triple<String, Int, Long>>, dayStart: Long, now: Long): Map<String, Long> {
    val total = HashMap<String, Long>()
    var fg: String? = null
    var since = 0L
    fun close(t: Long) {
        fg?.let { total[it] = (total[it] ?: 0L) + (t - maxOf(since, dayStart)).coerceAtLeast(0L) }
        fg = null
    }
    for ((pkg, type, t) in events) when (type) {
        UsageEvents.Event.ACTIVITY_RESUMED -> if (pkg != fg) {
            close(t)
            fg = pkg
            since = t
        }
        UsageEvents.Event.ACTIVITY_PAUSED -> if (pkg == fg) close(t)
        UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.DEVICE_SHUTDOWN -> close(t)
    }
    close(now)
    return total
}
