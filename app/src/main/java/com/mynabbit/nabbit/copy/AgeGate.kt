package com.mynabbit.nabbit.copy

import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.android.play.agesignals.AgeSignalsAccessRequest
import com.google.android.play.agesignals.AgeSignalsManagerFactory
import com.google.android.play.agesignals.AgeSignalsRequest
import com.google.android.play.agesignals.model.AgeSignalsStatus
import com.mynabbit.nabbit.data.Prefs
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Salty needs 16+ and Sweary 18+, checked with Google Play's Age Signals. The lowest age
 * the person's shared range confirms is remembered, so Play's sharing sheet shows once.
 */
object AgeGate {
    fun minimumAge(tone: Tone): Int? = when (tone) {
        Tone.PLAIN, Tone.SASSY -> null
        Tone.SALTY -> 16
        Tone.SWEARY -> 18
    }

    fun allows(tone: Tone, floor: Int): Boolean = minimumAge(tone)?.let { floor >= it } ?: true

    /**
     * Asks Play to share the age range, then unlocks the tone if it clears the gate.
     * Returns the line to show under the picker when it stays locked, or null if it unlocked.
     */
    suspend fun unlock(activity: Activity, tone: Tone): String? {
        val minimum = minimumAge(tone) ?: return null
        return try {
            val manager = AgeSignalsManagerFactory.create(activity)
            val access = manager.requestAgeSignalsAccess(
                AgeSignalsAccessRequest.builder().setActivity(activity).build(),
            ).await()
            when (access.ageSignalsStatus()) {
                AgeSignalsStatus.SHARED -> Unit
                AgeSignalsStatus.VERIFICATION_REQUIRED ->
                    return "Google Play needs to confirm your age first, so ${tone.title} stays locked for now."
                // Declined on Play's sheet, or Play can't share an age range here (some regions,
                // emulators, and apps not installed from Play). Nothing to blame the person for.
                else -> return "Google Play didn't share an age range, so ${tone.title} stays locked."
            }
            // No lower bound means under the lowest gate.
            val floor = manager.checkAgeSignals(AgeSignalsRequest.builder().build()).await().ageLower() ?: 0
            Prefs.setAgeFloor(floor)
            if (floor >= minimum) {
                Prefs.setTone(tone)
                null
            } else {
                "${tone.title} unlocks at $minimum."
            }
        } catch (_: Exception) {
            "Age check isn't available here, so ${tone.title} stays locked."
        }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }
}
