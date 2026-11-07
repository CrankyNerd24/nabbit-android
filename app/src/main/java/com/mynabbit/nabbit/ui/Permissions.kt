package com.mynabbit.nabbit.ui

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import com.mynabbit.nabbit.alerts.Notifier
import com.mynabbit.nabbit.alerts.Scheduler

/** The three things Android needs switched on for Nabbit to ring on time. */
object Permissions {
    fun notificationsOn(context: Context) = Notifier.canPost(context)
    fun exactAlarmsOn(context: Context) = Scheduler.canScheduleExact(context)
    fun fullScreenOn(context: Context) = Notifier.canUseFullScreen(context)

    fun allOn(context: Context) = notificationsOn(context) && exactAlarmsOn(context) && fullScreenOn(context)

    fun openNotificationSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    }

    fun openExactAlarmSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()),
        )
    }

    fun openFullScreenSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:${context.packageName}".toUri()),
            )
        }
    }
}
