package com.mynabbit.nabbit.model

import com.mynabbit.nabbit.data.Prefs
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** When nudges fire. */
object NudgeTiming {
    const val DEFAULT_MINUTES = 10

    /** Nudges after the first alert. The last one is the final sound. */
    const val COUNT = 3

    /** The gaps Settings offers, in minutes. */
    val choices = listOf(5, 10, 15, 20, 30)

    val minutes: Int
        get() = Prefs.nudgeMinutes.value.takeIf { it in choices } ?: DEFAULT_MINUTES

    val intervalMillis: Long get() = minutes * 60_000L

    /** When step `step` fires: 0 is the first alert, 1 to 3 the nudges. */
    fun stepAt(dueAt: Long, step: Int): Long = dueAt + step * intervalMillis

    fun lastNudgeAt(dueAt: Long): Long = stepAt(dueAt, COUNT)

    /** How far along a due reminder is, 0 to 3. */
    fun step(dueAt: Long, now: Long = System.currentTimeMillis()): Int =
        ((now - dueAt) / intervalMillis).toInt().coerceIn(0, COUNT)
}

/** The third tap. Tapping a chip saves the reminder. */
enum class WhenPreset(val title: String) {
    IN_ONE_HOUR("In 1 hour"),
    TONIGHT("Tonight"),
    TOMORROW_MORNING("Tomorrow AM"),
    NEXT_MONDAY("Next Monday"),
    PICK_DAY("Pick day & time");

    /** The due time for this chip, or null for Pick day & time, which needs a picker. */
    fun dueAt(now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val morning = Prefs.morningHour.value
        val tonightHour = Prefs.tonightHour.value
        return when (this) {
            IN_ONE_HOUR -> now.plusHours(1)
            TONIGHT -> {
                // Already past tonight's time: an hour from now still reads as tonight.
                val tonight = now.toLocalDate().atTime(tonightHour, 0)
                if (tonight.isAfter(now)) tonight else now.plusHours(1)
            }
            TOMORROW_MORNING -> now.toLocalDate().plusDays(1).atTime(morning, 0)
            // From tomorrow, so on a Monday this means a week out, not today.
            NEXT_MONDAY -> now.toLocalDate().plusDays(1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).atTime(morning, 0)
            PICK_DAY -> null
        }
    }
}

object Times {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun toMillis(time: LocalDateTime): Long = time.atZone(zone).toInstant().toEpochMilli()

    fun toLocal(millis: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)

    fun startOfToday(): Long = toMillis(LocalDate.now().atStartOfDay())

    fun startOfTomorrow(): Long = toMillis(LocalDate.now().plusDays(1).atStartOfDay())

    /** "7:00 PM", in the phone's own 12- or 24-hour style. */
    fun clock(millis: Long): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()).format(toLocal(millis))

    fun hourLabel(hour: Int): String = clock(toMillis(LocalDate.now().atTime(hour, 0)))

    /** The next Morning time: today's if it's still ahead, otherwise tomorrow's. */
    fun nextMorning(): LocalDateTime {
        val today = LocalDate.now().atTime(Prefs.morningHour.value, 0)
        return if (today.isAfter(LocalDateTime.now())) today else today.plusDays(1)
    }

    /**
     * How a due time reads back: "tonight at 7:00 PM", "tomorrow at 9:00 AM",
     * "Monday at 9:00 AM", or "Oct 20 at 9:00 AM".
     */
    fun dueDescription(millis: Long, now: LocalDateTime = LocalDateTime.now()): String {
        val due = toLocal(millis)
        val time = clock(millis)
        return when (val days = ChronoUnit.DAYS.between(now.toLocalDate(), due.toLocalDate())) {
            0L -> if (due.hour >= 17) "tonight at $time" else "today at $time"
            1L -> "tomorrow at $time"
            in 2L..6L -> "${due.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())} at $time"
            else -> "${DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()).format(due)} at $time"
        }
    }

    /** "7:00 PM" today, "Tomorrow 9:00 AM", or "Mon 9:00 AM", for rows. */
    fun rowLabel(millis: Long): String {
        val due = toLocal(millis)
        val today = LocalDate.now()
        return when (due.toLocalDate()) {
            today -> clock(millis)
            today.plusDays(1) -> "Tomorrow ${clock(millis)}"
            else -> "${due.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${clock(millis)}"
        }
    }
}
