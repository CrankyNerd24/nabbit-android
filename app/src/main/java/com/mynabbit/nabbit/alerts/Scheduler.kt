package com.mynabbit.nabbit.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.core.content.edit
import com.mynabbit.nabbit.MainActivity
import com.mynabbit.nabbit.data.AlertStyle
import com.mynabbit.nabbit.data.Prefs
import com.mynabbit.nabbit.data.Store
import com.mynabbit.nabbit.model.NudgeTiming

/**
 * Keeps one alarm per open reminder: the next thing it has coming, either the first
 * alert (step 0) or a nudge (1 to 3). When one fires, `AlertReceiver` posts it and
 * schedules the next step. Android has no 64-notification cap like iPhone, so every
 * open reminder gets its alarm.
 */
object Scheduler {
    private const val SCHEDULED_KEY = "scheduledIds"

    /** Whether alarms land on the exact minute. Without it Android may run them a few minutes late. */
    fun canScheduleExact(context: Context): Boolean =
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /** Replaces every alarm with the next step of each open reminder. */
    fun reschedule(context: Context) {
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences("scheduler", Context.MODE_PRIVATE)
        val open = Store.openReminders
        val openIds = open.map { it.id }.toSet()

        // Alarms for reminders that are done or gone.
        prefs.getStringSet(SCHEDULED_KEY, emptySet())!!.filter { it !in openIds }.forEach { cancel(context, it) }

        for (reminder in open) {
            val step = (0..NudgeTiming.COUNT).firstOrNull { NudgeTiming.stepAt(reminder.dueAt, it) > now }
            if (step == null) {
                // Slipped: nothing left to ring.
                cancel(context, reminder.id)
            } else {
                schedule(context, reminder.id, step, NudgeTiming.stepAt(reminder.dueAt, step))
            }
        }
        prefs.edit { putStringSet(SCHEDULED_KEY, openIds) }
    }

    fun schedule(context: Context, reminderId: String, step: Int, at: Long) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val operation = pendingIntent(context, reminderId, step)
        if (!canScheduleExact(context)) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            return
        }
        if (step == 0 && Prefs.alertStyle.value == AlertStyle.ALARM) {
            // Shows as an upcoming alarm in the status bar and the clock, like a real alarm.
            val show = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            manager.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), operation)
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    fun cancel(context: Context, reminderId: String) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, reminderId, 0))
    }

    /** One per reminder (the data URI), so a newer step replaces the older one. */
    private fun pendingIntent(context: Context, reminderId: String, step: Int): PendingIntent {
        val intent = Intent(context, AlertReceiver::class.java)
            .setAction(AlertReceiver.ACTION)
            .setData("nabbit://reminder/$reminderId".toUri())
            .putExtra(AlertReceiver.EXTRA_REMINDER, reminderId)
            .putExtra(AlertReceiver.EXTRA_STEP, step)
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
