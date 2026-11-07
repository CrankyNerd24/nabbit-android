package com.mynabbit.nabbit.support

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview

/**
 * When to ask for a Google Play review: only right after the whole day is cleared (the
 * clear board), never on first open, mid-capture, or after a tip. Play decides on its
 * own whether to show anything and caps how often; this keeps Nabbit from spending that
 * on every clear board.
 */
object ReviewPrompt {
    /** Set by Home while the mic listens or the New verb box is open, so nothing pops up mid-sentence. */
    @Volatile var micListening = false
    @Volatile var dialogOpen = false
    val busy: Boolean get() = micListening || dialogOpen

    private fun prefs(context: Context) = context.getSharedPreferences("review", Context.MODE_PRIVATE)

    /**
     * Counts a cleared day and says whether to ask: the 3rd, then every 30th, at least
     * 120 days since the last ask. A milestone missed because Home was busy waits for the
     * next clear board.
     */
    fun recordClearedDay(context: Context): Boolean {
        val prefs = prefs(context)
        val count = prefs.getInt("clearedDays", 0) + 1
        prefs.edit { putInt("clearedDays", count) }
        val days = (System.currentTimeMillis() - prefs.getLong("lastAsked", 0)) / 86_400_000L
        if (days < 120) return false
        if (count == 3 || (count > 3 && count % 30 == 0)) prefs.edit { putBoolean("pending", true) }
        return prefs.getBoolean("pending", false)
    }

    suspend fun ask(activity: Activity) {
        prefs(activity).edit {
            putLong("lastAsked", System.currentTimeMillis())
            putBoolean("pending", false)
        }
        runCatching {
            val manager = ReviewManagerFactory.create(activity)
            manager.launchReview(activity, manager.requestReview())
        }
    }
}
