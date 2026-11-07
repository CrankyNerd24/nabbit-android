package com.mynabbit.nabbit.model

import kotlinx.serialization.Serializable

@Serializable
data class Reminder(
    /** Ties alarms, notifications, and their buttons back to this reminder. */
    val id: String,
    val verbRaw: String,
    val thing: String,
    /** Epoch milliseconds. */
    val dueAt: Long,
    val createdAt: Long,
    val completedAt: Long? = null,
    val snoozeCount: Int = 0,
) {
    val verb: Verb get() = Verb.fromRaw(verbRaw) ?: Verb.call
    val text: String get() = "${verb.title} $thing"
    val isDone: Boolean get() = completedAt != null

    /** Past the last nudge and still open. Worked out from the clock, since nudges fire while the app isn't open. */
    fun isSlipped(now: Long = System.currentTimeMillis()): Boolean =
        !isDone && now >= NudgeTiming.lastNudgeAt(dueAt)
}

/** The second tap: who or what the verb applies to. Most-used float to the front. */
@Serializable
data class Thing(
    val name: String,
    val verbRaw: String,
    val useCount: Int = 0,
    val lastUsed: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Pinned to Again? as a one-tap repeat. Only ever set by the person, after an offer. */
    val hotKey: Boolean = false,
    /** The hot key offer has been shown, so it isn't asked twice. */
    val hotKeyOffered: Boolean = false,
) {
    val verb: Verb get() = Verb.fromRaw(verbRaw) ?: Verb.call

    companion object {
        /** Uses before a hot key is offered. */
        const val HOT_KEY_THRESHOLD = 3
    }
}

/** The three buttons on every alert. Nothing else. */
enum class ReminderAction(val title: String) {
    DONE("Done"),
    SNOOZE_15("15 min"),
    TONIGHT("Tonight"),
}
