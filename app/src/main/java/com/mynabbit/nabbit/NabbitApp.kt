package com.mynabbit.nabbit

import android.app.Application
import android.content.Context
import com.mynabbit.nabbit.alerts.Notifier
import com.mynabbit.nabbit.alerts.Scheduler
import com.mynabbit.nabbit.data.Prefs
import com.mynabbit.nabbit.data.Store

class NabbitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ensureStarted(this)
    }

    companion object {
        @Volatile private var started = false

        /** Loads settings and reminders and wires up alarms. Safe to call from receivers. */
        fun ensureStarted(context: Context) {
            if (started) return
            synchronized(this) {
                if (started) return
                val app = context.applicationContext
                Prefs.init(app)
                Store.init(app)
                Notifier.createChannels(app)
                Store.onChange = { Scheduler.reschedule(app) }
                started = true
            }
        }
    }
}
