package com.mynabbit.nabbit.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mynabbit.nabbit.MainActivity
import com.mynabbit.nabbit.NabbitApp
import com.mynabbit.nabbit.data.Prefs
import com.mynabbit.nabbit.data.Store
import com.mynabbit.nabbit.model.Reminder
import com.mynabbit.nabbit.model.ReminderAction
import com.mynabbit.nabbit.model.Times
import com.mynabbit.nabbit.ui.NabbitTheme
import com.mynabbit.nabbit.ui.ThemeColor

/** What's next, with Done, 15 min, and Tonight right on it. Medium also shows the two after it. */
class NextUpWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        NabbitApp.ensureStarted(context)
        provideContent { NextUp() }
    }

    companion object {
        val SMALL = DpSize(150.dp, 150.dp)
        val MEDIUM = DpSize(300.dp, 150.dp)
    }
}

class NextUpReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextUpWidget()
}

@Composable
private fun NextUp() {
    val data by Store.data.collectAsState()
    val theme = NabbitTheme.named(Prefs.themeId.collectAsState().value)
    val now = System.currentTimeMillis()
    val open = data.reminders.filter { !it.isDone }.sortedBy { it.dueAt }
    val next = open.firstOrNull()
    val wide = LocalSize.current.width >= NextUpWidget.MEDIUM.width
    val fill = when {
        next == null -> theme.tap1
        next.isSlipped(now) -> theme.alert
        else -> theme.tap3
    }
    val openApp = actionStartActivity(Intent().setClassName("com.mynabbit.nabbit", MainActivity::class.java.name))

    Row(
        GlanceModifier.fillMaxSize().background(theme.tap1.color).cornerRadius(16.dp).padding(8.dp),
    ) {
        Column(
            GlanceModifier.defaultWeight().fillMaxHeight().background(fill.color).cornerRadius(10.dp)
                .padding(10.dp).clickable(openApp),
        ) {
            if (next == null) {
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Clear board.\nNothing left today.", style = text(fill, 16, bold = true))
                }
            } else {
                Text(label(next, now), style = text(fill, 13, bold = true), maxLines = 1)
                Text(next.text, style = text(fill, 18, bold = true), maxLines = 2)
                Spacer(GlanceModifier.defaultWeight())
                // Words, not symbols: a moon reads as Do Not Disturb on Android. On the small
                // widget Done gets its own row so 15 min and Tonight fit spelled out.
                if (wide) {
                    Row(GlanceModifier.fillMaxWidth()) {
                        Pill("Done", next, ReminderAction.DONE, theme.done, GlanceModifier.defaultWeight())
                        Spacer(GlanceModifier.width(4.dp))
                        Pill("15 min", next, ReminderAction.SNOOZE_15, ThemeColor(0xFFFFFF), GlanceModifier)
                        Spacer(GlanceModifier.width(4.dp))
                        Pill("Tonight", next, ReminderAction.TONIGHT, ThemeColor(0xFFFFFF), GlanceModifier)
                    }
                } else {
                    Pill("Done", next, ReminderAction.DONE, theme.done, GlanceModifier.fillMaxWidth())
                    Spacer(GlanceModifier.height(4.dp))
                    Row(GlanceModifier.fillMaxWidth()) {
                        Pill("15 min", next, ReminderAction.SNOOZE_15, ThemeColor(0xFFFFFF), GlanceModifier.defaultWeight())
                        Spacer(GlanceModifier.width(4.dp))
                        Pill("Tonight", next, ReminderAction.TONIGHT, ThemeColor(0xFFFFFF), GlanceModifier.defaultWeight())
                    }
                }
            }
        }
        val rest = open.drop(1).take(2)
        if (wide && rest.isNotEmpty()) {
            Spacer(GlanceModifier.width(8.dp))
            Column(GlanceModifier.width(120.dp).padding(4.dp)) {
                Text("Then", style = text(theme.tap1, 12, bold = true))
                rest.forEach { reminder ->
                    Spacer(GlanceModifier.height(6.dp))
                    Text(reminder.text, style = text(theme.tap1, 14, bold = true), maxLines = 1)
                    Text(label(reminder, now), style = text(theme.tap1, 12), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Pill(title: String, reminder: Reminder, action: ReminderAction, fill: ThemeColor, modifier: GlanceModifier) {
    Box(
        modifier.height(36.dp).background(fill.color).cornerRadius(8.dp).padding(horizontal = 8.dp)
            .clickable(
                actionRunCallback<WidgetActionCallback>(
                    actionParametersOf(Widgets.reminderKey to reminder.id, Widgets.actionKey to action.name),
                ),
            )
            .semantics { contentDescription = "${action.title}: ${reminder.text}" },
        contentAlignment = Alignment.Center,
    ) {
        Text(title, style = text(fill, 13, bold = true), maxLines = 1)
    }
}

/** "Slipped", "Now", "3:00 PM", "Tomorrow 9:00 AM", or "Mon 9:00 AM". */
private fun label(reminder: Reminder, now: Long): String = when {
    reminder.isSlipped(now) -> "Slipped"
    reminder.dueAt <= now -> "Now"
    else -> Times.rowLabel(reminder.dueAt)
}

internal fun text(on: ThemeColor, size: Int, bold: Boolean = false) = TextStyle(
    color = ColorProvider(on.text.color),
    fontSize = size.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
)
