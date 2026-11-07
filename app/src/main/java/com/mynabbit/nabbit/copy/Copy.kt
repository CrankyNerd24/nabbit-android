package com.mynabbit.nabbit.copy

import com.mynabbit.nabbit.data.Prefs
import com.mynabbit.nabbit.model.Reminder
import com.mynabbit.nabbit.model.Times
import com.mynabbit.nabbit.model.Verb

/**
 * How Nabbit talks. Plain is the brief's voice; Sassy, Salty, and Sweary turn it up.
 * Whatever the tone, the swearing is at the task, never at the person, and there's
 * still no guilt.
 *
 * Salty (16+) and Sweary (18+) need an age check, which comes with Google Play's Age
 * Signals in a later version. Until then they aren't offered, and a saved one reads
 * as Plain.
 */
enum class Tone(val title: String, val sample: String, val offered: Boolean) {
    PLAIN("Plain", "“Still here. Dentist. Go.”", true),
    SASSY("Sassy", "“Still here. Dentist. I'm not going anywhere.”", true),
    SALTY("Salty", "“Still here. Dentist. Let's knock this crap out.” Mild swearing.", false),
    SWEARY("Sweary", "“Still here. Dentist. Let's get this shit done.” Full swearing.", false);

    companion object {
        val current: Tone get() = Prefs.tone.value.takeIf { it.offered } ?: PLAIN
    }
}

/** Every line whose wording depends on the tone. Add new user-facing lines here, in every tone. */
object Copy {
    private val tone: Tone get() = Tone.current

    // Nudges and the full-screen alert

    /** Gets more direct with each nudge: 0 is the first alert, 3 the last nudge. */
    fun nudge(reminder: Reminder, step: Int): String {
        val text = reminder.text
        val thing = reminder.thing
        return when (tone) {
            Tone.PLAIN -> when (step) {
                0 -> "It's time."
                1 -> "$text. Right now. ${reminder.verb.push}"
                2 -> "Still here. $thing. Go."
                else -> "Last one. Do it now or push it to tonight. No judgment."
            }
            Tone.SASSY -> when (step) {
                0 -> "It's go time."
                1 -> "$text. Now-ish. Actually, now."
                2 -> "Still here. $thing. I'm not going anywhere."
                else -> "Last one, I promise. Do it now or punt it to tonight. Zero judgment."
            }
            Tone.SALTY -> when (step) {
                0 -> "Hey. It's time."
                1 -> "$text. Right damn now."
                2 -> "Still here. $thing. Let's knock this crap out."
                else -> "Last one. Do the damn thing or push it to tonight. No judgment."
            }
            Tone.SWEARY -> when (step) {
                0 -> "Hey. It's fucking time."
                1 -> "$text. Right fucking now."
                2 -> "Still here. $thing. Let's get this shit done."
                else -> "Last one. Do the fucking thing or push it to tonight. No judgment."
            }
        }
    }

    // Celebrations

    val firstTry: String
        get() = when (tone) {
            Tone.PLAIN -> "First try. Look at you."
            Tone.SASSY -> "First try? Who even are you?"
            Tone.SALTY -> "First damn try. Look at you."
            Tone.SWEARY -> "First fucking try. Look at you."
        }

    val finallyHandled: String
        get() = when (tone) {
            Tone.PLAIN -> "Finally handled. That one's been hanging around."
            Tone.SASSY -> "Finally. That one was getting way too comfy."
            Tone.SALTY -> "Finally handled. That one was a pain in the ass."
            Tone.SWEARY -> "Finally fucking handled. That one was a pain in the ass."
        }

    val clearBoard: String
        get() = when (tone) {
            Tone.PLAIN -> "Clear board. Nothing left today."
            Tone.SASSY -> "Clear board. Go be fabulous."
            Tone.SALTY -> "Clear board. Nothing left. Hell yes."
            Tone.SWEARY -> "Clear board. Fuck yes. Nothing left."
        }

    /** The rotating done lines for the current tone. */
    val doneLines: List<String>
        get() = when (tone) {
            Tone.PLAIN -> listOf(
                "Handled.", "Done and gone.", "That's off your plate.", "One less thing in your head.",
                "Off the fridge.", "Crossed off.", "Nice. Next.", "Sorted.", "That's taken care of.",
                "Done. Breathe.", "Look at you go.", "Checked off.", "Out of your head, off the list.",
                "Done is done.", "Nailed it.", "Boom. Handled.", "Brain space freed.",
                "That one's history.", "Clean.", "Small win. Real win.",
            )
            Tone.SASSY -> listOf(
                "Look at you, functioning.", "Done. Show-off.", "Handled. Who even are you?",
                "Crossed off. Smug face allowed.", "Another one bites the dust.", "Done and dusted.",
                "Nailed it. Obviously.", "Look who's on a roll.", "Gone. Poof.",
                "One less thing. You're welcome, brain.", "Done. Take a bow.", "Flawless. Basically.",
                "Chef's kiss.", "Productive? In this economy?", "Iconic behavior.",
                "Off the list and out of your hair.", "Main character energy.", "Done. Mic drop.",
            )
            Tone.SALTY -> listOf(
                "Hell yes. Handled.", "Done. Damn right.", "Knocked that crap out.", "Done. Badass.",
                "Hell yeah, crossed off.", "Kicked its ass.", "Damn, look at you.",
                "That's one less damn thing.", "Boom. Done. Hell yes.", "Handled like a damn pro.",
                "That crap's done.", "Smashed it.", "Done and damn well dusted.", "Hell of a job.",
                "Off the damn list.", "Damn fine work.", "Took care of that crap.", "Hell, that was easy.",
            )
            Tone.SWEARY -> listOf(
                "Fuck yes. Handled.", "Done. Hell fucking yes.", "Knocked that shit out.", "Done. Absolute badass.",
                "That shit's handled.", "Fuck yeah, crossed off.", "Kicked its fucking ass.", "Holy shit, look at you.",
                "One less fucking thing.", "Boom. Fucking done.", "Handled like a goddamn pro.",
                "Smashed the shit out of it.", "Done and fucking dusted.", "Fucking legend.",
                "Off the damn list. Fuck yes.", "Shit got done.", "Hell of a fucking job.", "That was easy as shit.",
            )
        }

    /** Whether the plain voice's task-aware lines ("Dentist called…") are in the mix. */
    val usesTaskLines: Boolean get() = tone == Tone.PLAIN

    // Banners and prompts

    fun saved(text: String, dueAt: Long): String = "$savedLead $text, ${Times.dueDescription(dueAt)}."

    private val savedLead: String
        get() = when (tone) {
            Tone.PLAIN -> "Got it."
            Tone.SASSY -> "Locked in."
            Tone.SALTY -> "Hell yes."
            Tone.SWEARY -> "Fuck yes."
        }

    fun moved(thing: String, dueAt: Long): String {
        val time = Times.dueDescription(dueAt)
        return when (tone) {
            Tone.PLAIN -> "$thing moved to $time."
            Tone.SASSY -> "$thing bumped to $time. Fine."
            Tone.SALTY -> "$thing pushed to $time. Damn it, fine."
            Tone.SWEARY -> "$thing pushed to $time. Fine. Fucking fine."
        }
    }

    fun removed(thing: String): String = when (tone) {
        Tone.PLAIN -> "$thing reminder's gone."
        Tone.SASSY -> "$thing? Never heard of it."
        Tone.SALTY -> "$thing reminder's gone. To hell with it."
        Tone.SWEARY -> "$thing reminder's gone. Fuck it."
    }

    val missed: String
        get() = when (tone) {
            Tone.PLAIN -> "Missed that. Say it like this: call the dentist tomorrow."
            Tone.SASSY -> "Say what? Try it like this: call the dentist tomorrow."
            Tone.SALTY -> "What the hell was that? Try: call the dentist tomorrow."
            Tone.SWEARY -> "What the fuck was that? Try: call the dentist tomorrow."
        }

    fun hotKeyOffer(text: String, count: Int): String = when (tone) {
        Tone.PLAIN -> "You've added $text $count times. Want a hot key for it?"
        Tone.SASSY -> "$text, $count times now. A hot key, maybe? Just saying."
        Tone.SALTY -> "$text, $count damn times. Want a hot key?"
        Tone.SWEARY -> "$text, $count fucking times. Want a hot key?"
    }

    /** After a tip in Settings. */
    val tipThanks: String
        get() = when (tone) {
            Tone.PLAIN -> "Thank you. That keeps Nabbit going."
            Tone.SASSY -> "Look at you, keeping the fridge lights on. Thank you."
            Tone.SALTY -> "Well, damn. Thank you. Seriously."
            Tone.SWEARY -> "Holy shit, thank you. Seriously."
        }

    /** "Call who?" on the second tap. */
    fun thingPrompt(verb: Verb): String {
        val prompt = verb.thingPrompt
        return when (tone) {
            Tone.PLAIN -> prompt
            Tone.SASSY -> "$prompt Spill."
            Tone.SALTY -> prompt.replace("?", " the hell?")
            Tone.SWEARY -> prompt.replace("?", " the fuck?")
        }
    }
}
