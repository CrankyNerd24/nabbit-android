package com.mynabbit.nabbit.celebration

import com.mynabbit.nabbit.copy.Copy
import com.mynabbit.nabbit.model.NudgeTiming
import com.mynabbit.nabbit.model.Reminder
import com.mynabbit.nabbit.model.Verb
import java.util.UUID

/** What happens the moment something's marked done. Scales with effort, never blocks a tap. */
data class Celebration(val size: Size, val line: String, val id: String = UUID.randomUUID().toString()) {
    enum class Size {
        /** Routine things and things done ahead of time: a pop and a light tap. */
        SMALL,

        /** Done on the first alert or nudge. */
        MEDIUM,

        /** Slipped or pushed back more than once: full-screen confetti. */
        BIG,

        /** Nothing left today: the empty-fridge screen. */
        CLEAR_BOARD;

        /** How long the overlay stays, always under 1.5 seconds. */
        val millis: Long
            get() = when (this) {
                SMALL -> 700
                MEDIUM -> 1000
                BIG, CLEAR_BOARD -> 1400
            }
    }

    companion object {
        /** Worked out before the reminder is marked done, while slipped and nudge state still apply. */
        fun size(reminder: Reminder, now: Long = System.currentTimeMillis()): Size = when {
            reminder.isSlipped(now) || reminder.snoozeCount >= 2 -> Size.BIG
            reminder.verbRaw == Verb.take.raw -> Size.SMALL
            reminder.dueAt <= now && reminder.snoozeCount == 0 && NudgeTiming.step(reminder.dueAt, now) <= 1 -> Size.MEDIUM
            else -> Size.SMALL
        }

        fun make(reminder: Reminder, size: Size): Celebration = Celebration(
            size,
            when (size) {
                Size.CLEAR_BOARD -> Copy.clearBoard
                Size.BIG -> Copy.finallyHandled
                Size.MEDIUM -> Copy.firstTry
                Size.SMALL -> CelebrationLines.next(reminder)
            },
        )
    }
}

/** Rotating lines, never the same one twice in a row. */
object CelebrationLines {
    private var last: String? = null

    fun next(reminder: Reminder): String {
        val general = Copy.doneLines
        // In the plain voice, about one in three is about the task itself.
        val pool = if (Copy.usesTaskLines && (0 until 3).random() == 0) listOf(taskLine(reminder)) else general
        val line = pool.filter { it != last }.randomOrNull() ?: general.firstOrNull { it != last } ?: "Handled."
        last = line
        return line
    }

    private fun taskLine(reminder: Reminder): String {
        val thing = reminder.thing
        if (reminder.verbRaw == "call" && thing.contains("dentist", ignoreCase = true)) {
            return "Dentist called. Your teeth thank you."
        }
        return when (reminder.verbRaw) {
            "call" -> "$thing called. Done."
            "pickUp" -> "$thing picked up."
            "take" -> "$thing taken. Good."
            "pay" -> "$thing paid. Money stuff handled."
            "buy" -> "$thing bought."
            "email" -> "$thing emailed. Sent and done."
            "book" -> "$thing booked."
            "goTo" -> "Made it to $thing."
            else -> "${reminder.text}. Done and gone."
        }
    }
}
