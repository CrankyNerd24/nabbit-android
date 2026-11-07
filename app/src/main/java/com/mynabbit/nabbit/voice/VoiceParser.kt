package com.mynabbit.nabbit.voice

import com.mynabbit.nabbit.data.VerbStore
import com.mynabbit.nabbit.model.Verb
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/** What a spoken reminder said. Anything missing is asked for by the three-tap flow. */
data class SpokenReminder(
    val verb: Verb? = null,
    /** As heard ("dentist"); matched against saved things by the caller. */
    val thing: String? = null,
    val due: LocalDateTime? = null,
)

/** Turns "call the dentist tomorrow at 3" into a verb, a thing, and a time. English only. */
object VoiceParser {
    /** Other ways people say the built-in verbs. Added verbs are matched by their word. */
    private val builtInWords = mapOf(
        "pickUp" to listOf("pick up", "collect", "grab"),
        "goTo" to listOf("go to", "head to", "drive to", "stop at"),
        "call" to listOf("call", "phone", "ring"),
        "take" to listOf("take"),
        "pay" to listOf("pay"),
        "buy" to listOf("buy", "get"),
        "email" to listOf("email", "e mail", "message"),
        "book" to listOf("book", "schedule"),
    )

    private val leadIns = listOf("remind me to", "remind me", "i need to", "i have to", "i gotta",
        "need to", "have to", "don't forget to", "nabbit")
    private val fillers = setOf("the", "my", "a", "an", "some", "our", "to", "up", "at", "for", "with", "please")

    private val numberWords = mapOf(
        "a" to 1, "an" to 1, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
        "ten" to 10, "fifteen" to 15, "twenty" to 20, "thirty" to 30, "forty five" to 45,
    )

    private val weekdays = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

    fun parse(
        text: String,
        now: LocalDateTime = LocalDateTime.now(),
        morningHour: Int = 9,
        tonightHour: Int = 19,
        verbs: List<Verb> = VerbStore.visible(),
    ): SpokenReminder {
        var words = normalize(text)
        for (lead in leadIns) {
            if (words.startsWith("$lead ") || words == lead) words = words.removePrefix(lead).trim()
        }

        val (due, rest) = extractWhen(words, now, morningHour, tonightHour)

        // The earliest verb in what's left; on a tie, the longer phrase ("pick up" over "pick").
        var best: Pair<Verb, IntRange>? = null
        for (verb in verbs) {
            for (phrase in builtInWords[verb.raw] ?: listOf(normalize(verb.title))) {
                val range = wordRange(phrase, rest) ?: continue
                val current = best?.second
                if (current == null || range.first < current.first ||
                    (range.first == current.first && range.last > current.last)
                ) {
                    best = verb to range
                }
            }
        }
        val found = best ?: return SpokenReminder(due = due)

        val thing = rest.substring(found.second.last + 1).split(" ").filter { it.isNotEmpty() }.toMutableList()
        while (thing.isNotEmpty() && thing.first() in fillers) thing.removeAt(0)
        while (thing.isNotEmpty() && thing.last() in fillers) thing.removeAt(thing.lastIndex)
        return SpokenReminder(found.first, thing.joinToString(" ").ifEmpty { null }, due)
    }

    /** Finds a time in the words, and returns it with those words taken out. */
    private fun extractWhen(words: String, now: LocalDateTime, morningHour: Int, tonightHour: Int): Pair<LocalDateTime?, String> {
        var rest = words

        // "in 20 minutes", "in an hour", "in half an hour"
        Regex("""\bin half an hour\b""").find(rest)?.let {
            return now.plusMinutes(30) to tidy(rest.removeRange(it.range))
        }
        val amount = """(\d+|a|an|one|two|three|four|five|six|ten|fifteen|twenty|thirty|forty five)"""
        Regex("""\bin $amount (minute|minutes|min|mins|hour|hours)\b""").find(rest)?.let { match ->
            val count = number(match.groupValues[1])
            if (count != null) {
                val minutes = if (match.groupValues[2].startsWith("h")) count * 60L else count.toLong()
                return now.plusMinutes(minutes) to tidy(rest.removeRange(match.range))
            }
        }

        val today = now.toLocalDate()
        var day: java.time.LocalDate? = null
        var hour: Int? = null
        var minute = 0

        val tomorrow = Regex("""\btomorrow\b""").find(rest)
        val weekday = Regex("""\b(?:next |on |this )?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b""").find(rest)
        val todayWord = Regex("""\btoday\b""").find(rest)
        when {
            tomorrow != null -> {
                day = today.plusDays(1)
                rest = rest.removeRange(tomorrow.range)
            }
            weekday != null -> {
                // The next one after today, so "Monday" said on a Monday means a week out.
                val target = DayOfWeek.of(weekdays.indexOf(weekday.groupValues[1]) + 1)
                day = today.plusDays(1).with(TemporalAdjusters.nextOrSame(target))
                rest = rest.removeRange(weekday.range)
            }
            todayWord != null -> {
                day = today
                rest = rest.removeRange(todayWord.range)
            }
        }

        val at = Regex("""\bat (\d{1,2})(?::(\d{2}))? ?(am|pm|a m|p m)?\b""").find(rest)
        val atHour = at?.groupValues?.get(1)?.toIntOrNull()
        val evening = Regex("""\b(?:this )?(tonight|night|evening)\b""").find(rest)
        val morning = Regex("""\b(?:this |in the )?morning\b""").find(rest)
        when {
            at != null && atHour != null && atHour in 1..12 -> {
                val suffix = at.groupValues[3]
                minute = at.groupValues[2].toIntOrNull() ?: 0
                hour = when {
                    suffix.startsWith("p") -> if (atHour == 12) 12 else atHour + 12
                    suffix.startsWith("a") -> if (atHour == 12) 0 else atHour
                    // No am or pm: 1 to 7 means afternoon or evening.
                    else -> if (atHour <= 7) atHour + 12 else atHour
                }
                rest = rest.removeRange(at.range)
            }
            evening != null -> {
                hour = tonightHour
                day = day ?: today
                rest = rest.removeRange(evening.range)
            }
            morning != null -> {
                hour = morningHour
                rest = rest.removeRange(morning.range)
            }
        }

        if (day == null && hour == null) return null to tidy(rest)
        // "Today" with no time: an hour from now.
        if (day == today && hour == null) return now.plusHours(1) to tidy(rest)

        var date = (day ?: today).atTime(hour ?: morningHour, minute)
        if (!date.isAfter(now)) {
            // "At 3" or "in the morning" once that's passed means tomorrow;
            // "tonight" or "today at 3" once passed means an hour from now.
            date = if (day == null) date.plusDays(1) else now.plusHours(1)
        }
        return date to tidy(rest)
    }

    /** Lowercase, keep letters, digits, colons, and apostrophes, single spaces. */
    private fun normalize(text: String): String =
        tidy(text.lowercase().map { if (it.isLetterOrDigit() || it == ':' || it == '\'') it else ' ' }.joinToString(""))

    private fun tidy(text: String): String = text.split(" ").filter { it.isNotEmpty() }.joinToString(" ")

    private fun number(word: String): Int? = word.toIntOrNull() ?: numberWords[word]

    private fun wordRange(phrase: String, text: String): IntRange? =
        Regex("""\b${Regex.escape(phrase)}\b""").find(text)?.range
}
