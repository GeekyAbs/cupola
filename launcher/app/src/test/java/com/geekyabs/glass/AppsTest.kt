package com.geekyabs.glass

import android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED
import android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED
import android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE
import org.junit.Assert.assertEquals
import org.junit.Test

class AppsTest {
    private val dock = listOf("a", "gone", "b", "c")
    private val visible = listOf("a", "b", "c")

    @Test
    fun reorder() {
        assertEquals(listOf("b", "a", "c", "gone"), reorderDock(dock, visible, "a", 1))
        assertEquals(listOf("a", "c", "b", "gone"), reorderDock(dock, visible, "c", -1))
        assertEquals(dock, reorderDock(dock, visible, "a", -1)) // already first
        assertEquals(dock, reorderDock(dock, visible, "c", 1)) // already last
        assertEquals(dock, reorderDock(dock, visible, "x", 1)) // not pinned
    }

    @Test
    fun foregroundTimes() {
        val events = listOf(
            Triple("yt", ACTIVITY_RESUMED, 400L), // opened before midnight (1000): counts from midnight
            Triple("sp", ACTIVITY_PAUSED, 900L), // not in front: ignored
            Triple("yt", ACTIVITY_PAUSED, 1_600L), // yt 600
            Triple("sp", ACTIVITY_RESUMED, 1_600L),
            Triple("tv", ACTIVITY_RESUMED, 1_900L), // switching apps closes the last: sp 300
            Triple("tv", SCREEN_NON_INTERACTIVE, 2_000L), // tv 100
            Triple("sp", ACTIVITY_RESUMED, 3_000L), // still open at now: sp +500
        )
        assertEquals(mapOf("yt" to 600L, "sp" to 800L, "tv" to 100L), foregroundTimes(events, 1_000L, 3_500L))
    }
}
