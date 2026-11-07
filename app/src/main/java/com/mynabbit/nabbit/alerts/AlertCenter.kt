package com.mynabbit.nabbit.alerts

import com.mynabbit.nabbit.data.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Which reminder the full-screen alert is showing. It opens when an alert fires with
 * the app open, when a notification is tapped, or when the app comes forward with
 * something due.
 */
object AlertCenter {
    private val _current = MutableStateFlow<String?>(null)
    val current: StateFlow<String?> = _current

    fun show(id: String) {
        if (_current.value == null) _current.value = id
    }

    fun dismiss() {
        _current.value = null
    }

    /** The earliest reminder that's due and not yet slipped. */
    fun showDue() {
        if (_current.value != null) return
        val now = System.currentTimeMillis()
        Store.openReminders.firstOrNull { it.dueAt <= now && !it.isSlipped(now) }?.let { show(it.id) }
    }
}
