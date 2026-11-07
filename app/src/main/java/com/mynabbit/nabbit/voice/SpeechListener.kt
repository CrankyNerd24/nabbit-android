package com.mynabbit.nabbit.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Listens for one reminder and hands back the words. Prefers the phone's offline
 * recognizer, and stops on its own after a pause.
 */
class SpeechListener(private val context: Context) {
    enum class State { IDLE, LISTENING, UNAVAILABLE }

    var state by mutableStateOf(State.IDLE)
        private set
    var transcript by mutableStateOf("")
        private set

    private var recognizer: SpeechRecognizer? = null

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(onHeard: (String) -> Unit) {
        if (!isAvailable) {
            state = State.UNAVAILABLE
            return
        }
        stop()
        transcript = ""
        val speech = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speech
        speech.setRecognitionListener(object : RecognitionListener {
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    ?.let { transcript = it }
            }

            override fun onResults(results: Bundle?) {
                val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    ?: transcript
                finish()
                if (heard.isNotBlank()) onHeard(heard)
            }

            override fun onError(error: Int) {
                val heard = transcript
                finish()
                if (heard.isNotBlank()) onHeard(heard)
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        state = State.LISTENING
        speech.startListening(intent)
    }

    fun stop() {
        recognizer?.stopListening()
    }

    private fun finish() {
        recognizer?.destroy()
        recognizer = null
        state = State.IDLE
    }

    fun release() {
        recognizer?.destroy()
        recognizer = null
        state = State.IDLE
    }
}
