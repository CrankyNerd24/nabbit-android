package com.mynabbit.nabbit

import com.mynabbit.nabbit.model.Verb
import com.mynabbit.nabbit.voice.VoiceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class VoiceParserTest {
    // A Wednesday afternoon.
    private val now = LocalDateTime.of(2026, 11, 4, 14, 0)

    private fun parse(text: String) = VoiceParser.parse(text, now = now, verbs = Verb.builtIns)

    @Test fun allThree() {
        val spoken = parse("Remind me to call the dentist tomorrow at 3")
        assertEquals(Verb.call, spoken.verb)
        assertEquals("dentist", spoken.thing)
        assertEquals(LocalDateTime.of(2026, 11, 5, 15, 0), spoken.due)
    }

    @Test fun inMinutes() {
        val spoken = parse("take meds in 20 minutes")
        assertEquals(Verb.take, spoken.verb)
        assertEquals("meds", spoken.thing)
        assertEquals(now.plusMinutes(20), spoken.due)
    }

    @Test fun longerPhraseWins() {
        val spoken = parse("pick up the kids tonight")
        assertEquals(Verb.pickUp, spoken.verb)
        assertEquals("kids", spoken.thing)
        assertEquals(LocalDateTime.of(2026, 11, 4, 19, 0), spoken.due)
    }

    @Test fun weekdayMeansNextOne() {
        // Said on a Wednesday, "Wednesday" is a week out, at the Morning time.
        assertEquals(LocalDateTime.of(2026, 11, 11, 9, 0), parse("pay rent wednesday").due)
        assertEquals(LocalDateTime.of(2026, 11, 9, 9, 0), parse("book haircut next monday").due)
    }

    @Test fun pastTimeMeansTomorrow() {
        assertEquals(LocalDateTime.of(2026, 11, 5, 9, 0), parse("email boss at 9 am").due)
    }

    @Test fun missingPieces() {
        val noWhen = parse("call mom")
        assertEquals("mom", noWhen.thing)
        assertNull(noWhen.due)
        assertNull(parse("tomorrow at 3").verb)
    }
}
