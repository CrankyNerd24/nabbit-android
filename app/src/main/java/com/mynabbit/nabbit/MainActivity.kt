package com.mynabbit.nabbit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mynabbit.nabbit.alerts.AlertCenter
import com.mynabbit.nabbit.alerts.Scheduler
import com.mynabbit.nabbit.ui.NabbitRoot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handle(intent)
        // Over the lock screen while an alert is up, and back to normal once it's handled.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AlertCenter.current.collect { id ->
                    setShowWhenLocked(id != null)
                    setTurnScreenOn(id != null)
                }
            }
        }
        setContent { NabbitRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        // Lines alarms back up and shows anything that came due while Nabbit was away.
        Scheduler.reschedule(this)
        AlertCenter.showDue()
    }

    private fun handle(intent: Intent?) {
        if (intent?.action == ACTION_SHOW_ALERT) {
            intent.getStringExtra(EXTRA_REMINDER)?.let(AlertCenter::show)
        }
    }

    companion object {
        const val ACTION_SHOW_ALERT = "com.mynabbit.nabbit.SHOW_ALERT"
        const val EXTRA_REMINDER = "reminder"
    }
}
