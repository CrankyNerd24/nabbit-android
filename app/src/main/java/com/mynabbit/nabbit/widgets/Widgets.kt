package com.mynabbit.nabbit.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.mynabbit.nabbit.NabbitApp
import com.mynabbit.nabbit.data.Reminders
import com.mynabbit.nabbit.model.ReminderAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Redraws both widgets. Called whenever reminders, the theme, or the verbs change, and as alerts fire. */
object Widgets {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun update(context: Context) {
        val app = context.applicationContext
        scope.launch {
            runCatching { NextUpWidget().updateAll(app) }
            runCatching { QuickAddWidget().updateAll(app) }
        }
    }

    val reminderKey = ActionParameters.Key<String>("reminder")
    val actionKey = ActionParameters.Key<String>("action")
}

/** Done, 15 min, or Tonight on the Next up widget, without opening the app. */
class WidgetActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        NabbitApp.ensureStarted(context)
        val id = parameters[Widgets.reminderKey] ?: return
        val action = ReminderAction.entries.firstOrNull { it.name == parameters[Widgets.actionKey] } ?: return
        Reminders.perform(context, action, id)
        NextUpWidget().updateAll(context)
    }
}
