package com.mynabbit.nabbit.data

import android.content.Context
import com.mynabbit.nabbit.alerts.AlertCenter
import com.mynabbit.nabbit.alerts.Notifier
import com.mynabbit.nabbit.model.ReminderAction
import com.mynabbit.nabbit.model.Times
import com.mynabbit.nabbit.model.WhenPreset

/** Done, 15 min, or Tonight, from any of the places they appear: notification, full-screen alert, or Home. */
object Reminders {
    fun perform(context: Context, action: ReminderAction, id: String) {
        when (action) {
            ReminderAction.DONE -> Store.complete(id)
            ReminderAction.SNOOZE_15 -> Store.snooze(id, System.currentTimeMillis() + 15 * 60_000L)
            ReminderAction.TONIGHT -> Store.snooze(
                id,
                WhenPreset.TONIGHT.dueAt()?.let(Times::toMillis) ?: (System.currentTimeMillis() + 3_600_000L),
            )
        }
        Notifier.cancel(context, id)
        if (AlertCenter.current.value == id) AlertCenter.dismiss()
    }
}
