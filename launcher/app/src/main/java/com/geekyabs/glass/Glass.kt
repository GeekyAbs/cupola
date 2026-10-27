package com.geekyabs.glass

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random

/**
 * Android 11 has no RenderEffect, so the glass is "baked": the wallpaper is a still photo,
 * we blur it once, and every glass surface paints the blurred patch that sits behind it.
 */
object Wallpaper {
    // NASA ISS photos (public domain), ordered so auroras, city lights and the Milky Way alternate day to day.
    // Places condensed from NASA's own descriptions (images.nasa.gov).
    private val WALLS = listOf(
        Triple(R.drawable.wall9, "Aurora australis south of Perth, Australia", "Expedition 74 · April 2026"),
        Triple(R.drawable.wall17, "The Nile and the Red Sea at night", "Expedition 71 · June 2024"),
        Triple(R.drawable.wall13, "The Milky Way over the coast of Kenya and Somalia", "Expedition 73 · October 2025"),
        Triple(R.drawable.wall2, "Aurora australis off the coast of New Zealand", "Expedition 72 · September 2024"),
        Triple(R.drawable.wall12, "Airglow over Europe, North Africa and the Mediterranean", "Expedition 73 · November 2025"),
        Triple(R.drawable.wall20, "Aurora swirling between Australia and Antarctica", "Expedition 65 · October 2021"),
        Triple(R.drawable.wall14, "The Milky Way above southern Iran", "Expedition 73 · August 2025"),
        Triple(R.drawable.wall10, "Aurora above the city lights of Europe and Russia", "Expedition 74 · February 2026"),
        Triple(R.drawable.wall5, "Aurora australis over the southern Indian Ocean", "Expedition 74 · May 2026"),
        Triple(R.drawable.wall23, "Stars and airglow over the Indian Ocean", "Expedition 65 · May 2021"),
        Triple(R.drawable.wall15, "Aurora borealis over the city lights of Canada", "Expedition 72 · March 2025"),
        Triple(R.drawable.wall18, "Aurora over western Kazakhstan, near the Caspian Sea", "Expedition 69 · September 2023"),
        Triple(R.drawable.wall7, "Aurora australis and the Milky Way over the Indian Ocean", "Expedition 74 · April 2026"),
        Triple(R.drawable.wall11, "Airglow over village lights in the Sahara, Algeria", "Expedition 73 · November 2025"),
        Triple(R.drawable.wall1, "Aurora australis between Australia and Antarctica", "Expedition 73 · June 2025"),
        Triple(R.drawable.wall21, "Aurora australis between Asia and Antarctica", "Expedition 65 · August 2021"),
        Triple(R.drawable.wall24, "The Milky Way above the Indian Ocean", "Expedition 64 · April 2021"),
        Triple(R.drawable.wall25, "Aurora over the city lights of Sweden and Finland", "Expedition 64 · January 2021"),
        Triple(R.drawable.wall3, "Aurora australis at orbital sunset over the South Pacific", "Expedition 72 · October 2024"),
        Triple(R.drawable.wall16, "Aurora borealis over Manitoba, Canada", "Expedition 72 · October 2024"),
        Triple(R.drawable.wall22, "Aurora australis beneath the stars, southern Indian Ocean", "Expedition 65 · August 2021"),
        Triple(R.drawable.wall8, "Aurora australis southwest of Perth during a solar storm", "Expedition 74 · June 2026"),
        Triple(R.drawable.wall19, "Aurora over the southern Indian Ocean", "Expedition 65 · October 2021"),
        Triple(R.drawable.wall4, "Aurora australis southeast of New Zealand", "Expedition 73 · June 2025"),
        Triple(R.drawable.wall6, "Aurora australis south of New Zealand", "Expedition 72 · October 2024"),
    )

    var sharp by mutableStateOf<ImageBitmap?>(null)
        private set
    var place by mutableStateOf("")
        private set
    var credit by mutableStateOf("")
        private set
    var blurred by mutableStateOf<ImageBitmap?>(null)
        private set
    var w = 0
        private set
    var h = 0
        private set
    private var shown = -1

    val count get() = WALLS.size
    fun ofDay(epochDay: Long) = Math.floorMod(epochDay, WALLS.size.toLong()).toInt()
    fun placeOf(i: Int) = WALLS[i].second
    fun creditOf(i: Int) = "${WALLS[i].third} · NASA"

    /** Shows photo [chosen], or with null the day's photo, a different one each day. Safe off the main thread. */
    @Synchronized
    fun show(res: Resources, epochDay: Long, chosen: Int?) {
        val i = chosen ?: ofDay(epochDay)
        if (i == shown) return
        shown = i
        w = res.displayMetrics.widthPixels
        h = res.displayMetrics.heightPixels
        val bmp = BitmapFactory.decodeResource(res, WALLS[i].first)
        val soft = blur(bmp) // before swapping anything, so the photo and its glass change together
        place = placeOf(i)
        credit = creditOf(i)
        sharp = bmp.asImageBitmap()
        blurred = soft.asImageBitmap()
    }

    /** Small copy for the picker (240×135, ~130 KB). */
    fun thumbnail(res: Resources, i: Int): ImageBitmap =
        BitmapFactory.decodeResource(res, WALLS[i].first, BitmapFactory.Options().apply { inSampleSize = 8 }).asImageBitmap()

    /** 1/8 downscale + three box-blur passes ≈ gaussian, then saturated; upscaled again (bilinear) at draw time. */
    private fun blur(src: Bitmap): Bitmap {
        var b = src
        repeat(3) { b = Bitmap.createScaledBitmap(b, b.width / 2, b.height / 2, true) }
        val bw = b.width
        val bh = b.height
        val px = IntArray(bw * bh).also { b.getPixels(it, 0, bw, 0, 0, bw, bh) }
        val tmp = IntArray(bw * bh)
        repeat(3) {
            boxPass(px, tmp, bw, bh, 4, horizontal = true)
            boxPass(tmp, px, bw, bh, 4, horizontal = false)
        }
        // Vibrancy baked in once: as a per-draw colour filter it cost this GPU ~10 ms a frame.
        val vibrancy = Paint().apply { colorFilter = ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(1.5f) }) }
        return Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888).also {
            Canvas(it).drawBitmap(Bitmap.createBitmap(px, bw, bh, Bitmap.Config.ARGB_8888), 0f, 0f, vibrancy)
        }
    }

    // ponytail: naive O(n·r) box blur, fine for a 240×135 image; sliding-window sum if it ever runs on big bitmaps.
    private fun boxPass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val n = 2 * r + 1
        for (line in 0 until lines) {
            for (i in 0 until len) {
                var rs = 0
                var gs = 0
                var bs = 0
                for (k in -r..r) {
                    val j = (i + k).coerceIn(0, len - 1)
                    val p = src[if (horizontal) line * w + j else j * w + line]
                    rs += p shr 16 and 0xFF
                    gs += p shr 8 and 0xFF
                    bs += p and 0xFF
                }
                dst[if (horizontal) line * w + i else i * w + line] =
                    (0xFF shl 24) or (rs / n shl 16) or (gs / n shl 8) or (bs / n)
            }
        }
    }
}

/** Fine monochrome grain, tiled over every material: reads as frosted, and hides banding. */
private val grain: ShaderBrush by lazy {
    val size = 128
    val rnd = Random(7)
    val px = IntArray(size * size) { if (rnd.nextBoolean()) 0x0AFFFFFF else 0x0A000000 }
    val bmp = Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
    ShaderBrush(ImageShader(bmp, TileMode.Repeated, TileMode.Repeated))
}

private val patchPaint = Paint(Paint.FILTER_BITMAP_FLAG)
private val hairlineBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = .14f), Color.White.copy(alpha = .03f)))

/** Apple-style dark material: blurred backdrop, darkened (not milky), grain, hairline edge. */
fun Modifier.glass(shape: Shape, scrim: Float = .40f, hairline: Boolean = true): Modifier = composed {
    var origin by remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { origin = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            Wallpaper.blurred?.let { drawPatch(it.asAndroidBitmap(), origin) }
            drawRect(Color(0xFF0E0E10).copy(alpha = scrim))
            drawRect(grain)
        }
        .then(if (hairline) Modifier.border(0.5.dp, hairlineBrush, shape) else Modifier)
}

/**
 * Just the part of the blurred photo under this surface. Drawing the whole screen-sized image and letting the clip
 * cut it down made the GPU shade every pixel of the screen once per surface (~15 ms a frame here).
 * The source rect snaps to whole pixels of the small bitmap, so the destination is widened to match and the clip trims it.
 */
private fun DrawScope.drawPatch(b: Bitmap, origin: Offset) {
    val s = b.width.toFloat() / Wallpaper.w
    val l = floor(origin.x * s).coerceAtLeast(0f)
    val t = floor(origin.y * s).coerceAtLeast(0f)
    val r = ceil((origin.x + size.width) * s).coerceAtMost(b.width.toFloat())
    val btm = ceil((origin.y + size.height) * s).coerceAtMost(b.height.toFloat())
    if (r <= l || btm <= t) return // off screen
    val dst = RectF(l / s - origin.x, t / s - origin.y, r / s - origin.x, btm / s - origin.y)
    drawIntoCanvas { it.nativeCanvas.drawBitmap(b, Rect(l.toInt(), t.toInt(), r.toInt(), btm.toInt()), dst, patchPaint) }
}

