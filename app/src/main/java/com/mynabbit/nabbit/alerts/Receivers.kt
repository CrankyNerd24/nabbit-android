package com.mynabbit.nabbit.alerts

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mynabbit.nabbit.NabbitApp
import com.mynabbit.nabbit.data.Reminders
import com.mynabbit.nabbit.data.Store
import com.mynabbit.nabbit.model.NudgeTiming
import com.mynabbit.nabbit.model.ReminderAction
import com.mynabbit.nabbit.widgets.Widgets

/** An alert or nudge is due: post it, then line up the next step. */
class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NabbitApp.ensureStarted(context)
        val id = intent.getStringExtra(EXTRA_REMINDER) ?: return
        val step = intent.getIntExtra(EXTRA_STEP, 0)
        val reminder = Store.reminder(id)?.takeIf { !it.isDone } ?: return
        val now = System.currentTimeMillis()
        // Fired late (say, the phone was off): post the step it's really at.
        val current = maxOf(step, NudgeTiming.step(reminder.dueAt, now))
        if (now >= NudgeTiming.stepAt(reminder.dueAt, 0)) {
            Notifier.post(context, reminder, current)
            if (current == 0) AlertCenter.show(id)
        }
        Scheduler.reschedule(context)
        // "Now" and "Slipped" on the widget.
        Widgets.update(context)
    }

    companion object {
        const val ACTION = "com.mynabbit.nabbit.ALERT"
        const val EXTRA_REMINDER = "reminder"
        const val EXTRA_STEP = "step"
    }
}

/** Done, 15 min, or Tonight tapped on a notification, without opening the app. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NabbitApp.ensureStarted(context)
        val id = intent.getStringExtra(EXTRA_REMINDER) ?: return
        val action = ReminderAction.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_ACTION) } ?: return
        Reminders.perform(context, action, id)
    }

    companion object {
        const val ACTION = "com.mynabbit.nabbit.ACTION"
        const val EXTRA_REMINDER = "reminder"
        const val EXTRA_ACTION = "action"
    }
}

/** Alarms don't survive a restart, a clock change, or an app update. Puts them back. */
class RescheduleReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        NabbitApp.ensureStarted(context)
        Scheduler.reschedule(context)
    }
}
