package com.mynabbit.nabbit.alerts

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mynabbit.nabbit.MainActivity
import com.mynabbit.nabbit.R
import com.mynabbit.nabbit.copy.Copy
import com.mynabbit.nabbit.data.AlertStyle
import com.mynabbit.nabbit.data.Prefs
import com.mynabbit.nabbit.model.NudgeTiming
import com.mynabbit.nabbit.model.Reminder
import com.mynabbit.nabbit.model.ReminderAction

/**
 * Posts each alert and nudge. Two channels:
 * - Alarms: the first alert in Alarm style. Rings on the alarm volume (so silent mode
 *   doesn't stop it), keeps ringing until it's handled or the first nudge replaces it,
 *   and opens the full-screen alert.
 * - Nudges: the first alert in Notification style and every nudge. Follows the phone's
 *   sound, vibrate, and Do Not Disturb settings.
 */
object Notifier {
    const val ALARM_CHANNEL = "alarms"
    const val NUDGE_CHANNEL = "nudges"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarms = NotificationChannel(ALARM_CHANNEL, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "The first alert for each reminder, when Settings > Alerts is on Alarm."
            setSound(
                alarmSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val nudges = NotificationChannel(NUDGE_CHANNEL, "Nudges", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Nudges after the first alert, and the first alert when Settings > Alerts is on Notification."
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannels(listOf(alarms, nudges))
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Whether the alarm can open the full-screen alert over the lock screen. */
    fun canUseFullScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun notificationId(reminderId: String): Int = reminderId.hashCode()

    /** The alert (step 0) or a nudge (1 to 3) for a reminder. Each replaces the last. */
    fun post(context: Context, reminder: Reminder, step: Int) {
        if (!canPost(context)) return
        val asAlarm = step == 0 && Prefs.alertStyle.value == AlertStyle.ALARM
        val line = Copy.nudge(reminder, step)
        val builder = NotificationCompat.Builder(context, if (asAlarm) ALARM_CHANNEL else NUDGE_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.text)
            .setContentText(line)
            .setStyle(NotificationCompat.BigTextStyle().bigText(line))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (asAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(alertIntent(context, reminder.id, requestCode = 0))
            .setAutoCancel(true)
            .addAction(0, ReminderAction.DONE.title, actionIntent(context, reminder.id, ReminderAction.DONE))
            .addAction(0, ReminderAction.SNOOZE_15.title, actionIntent(context, reminder.id, ReminderAction.SNOOZE_15))
            .addAction(0, ReminderAction.TONIGHT.title, actionIntent(context, reminder.id, ReminderAction.TONIGHT))
        // Each step clears itself when the next is due; the last nudge stays until it's handled.
        if (step < NudgeTiming.COUNT) builder.setTimeoutAfter(NudgeTiming.intervalMillis)
        if (asAlarm) {
            builder.setFullScreenIntent(alertIntent(context, reminder.id, requestCode = 1), true)
                .setOngoing(true)
        }
        val notification = builder.build()
        if (asAlarm) notification.flags = notification.flags or Notification.FLAG_INSISTENT
        try {
            NotificationManagerCompat.from(context).notify(notificationId(reminder.id), notification)
        } catch (_: SecurityException) {
            // Notifications were turned off between the check and the post.
        }
    }

    fun cancel(context: Context, reminderId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(reminderId))
    }

    private fun alertIntent(context: Context, reminderId: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_SHOW_ALERT)
            .putExtra(MainActivity.EXTRA_REMINDER, reminderId)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            notificationId(reminderId) * 8 + requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(context: Context, reminderId: String, action: ReminderAction): PendingIntent {
        val intent = Intent(context, ActionReceiver::class.java)
            .setAction(ActionReceiver.ACTION)
            .putExtra(ActionReceiver.EXTRA_REMINDER, reminderId)
            .putExtra(ActionReceiver.EXTRA_ACTION, action.name)
        return PendingIntent.getBroadcast(
            context,
            notificationId(reminderId) * 8 + 2 + action.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
