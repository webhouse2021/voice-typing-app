package com.bengali.voicetyping.recognition

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

sealed class SpeechEvent {
    object ReadyForSpeech : SpeechEvent()
    object BeginningOfSpeech : SpeechEvent()
    data class RmsChanged(val rmsdB: Float) : SpeechEvent()
    data class PartialResult(val partialText: String) : SpeechEvent()
    data class FinalResult(val text: String) : SpeechEvent()
    object EndOfSpeech : SpeechEvent()
    data class Error(val errorCode: Int, val message: String) : SpeechEvent()
}

class SpeechRecognizerManager(
    private val context: Context,
    private val onEvent: (SpeechEvent) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening: Boolean = false
    private var currentLanguageTag: String = "bn-BD"

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onEvent(
                SpeechEvent.Error(
                    SpeechRecognizer.ERROR_CLIENT,
                    "Speech recognition service is not available on this device."
                )
            )
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onEvent(SpeechEvent.ReadyForSpeech)
            }

            override fun onBeginningOfSpeech() {
                onEvent(SpeechEvent.BeginningOfSpeech)
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Emits live volume level in decibels for waveform rendering
                onEvent(SpeechEvent.RmsChanged(rmsdB))
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                onEvent(SpeechEvent.EndOfSpeech)
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMessage = resolveErrorMessage(error)
                Log.e("BengaliSpeechRec", "SpeechRecognizer error: $error ($errorMessage)")
                onEvent(SpeechEvent.Error(error, errorMessage))
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull() ?: ""
                onEvent(SpeechEvent.FinalResult(recognizedText))
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Real-time Bengali streaming recognition results
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: ""
                if (partial.isNotBlank()) {
                    onEvent(SpeechEvent.PartialResult(partial))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening(languageTag: String = currentLanguageTag) {
        if (isListening) return
        currentLanguageTag = languageTag

        if (speechRecognizer == null) {
            initRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguageTag)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD", "bn-IN"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            isListening = false
            onEvent(SpeechEvent.Error(SpeechRecognizer.ERROR_CLIENT, e.localizedMessage ?: "Failed to start listening"))
        }
    }

    fun stopListening() {
        if (!isListening) return
        try {
            speechRecognizer?.stopListening()
        } finally {
            isListening = false
        }
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
        } finally {
            isListening = false
        }
    }

    fun destroy() {
        isListening = false
        try {
            speechRecognizer?.destroy()
        } finally {
            speechRecognizer = null
        }
    }

    fun isCurrentlyListening(): Boolean = isListening

    private fun resolveErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please verify your microphone."
            SpeechRecognizer.ERROR_CLIENT -> "Client-side recognition error."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice typing."
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error. Check your internet connection."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out while processing Bengali speech."
            SpeechRecognizer.ERROR_NO_MATCH -> "No Bengali speech recognized. Please speak clearly into the microphone."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please try again in a moment."
            SpeechRecognizer.ERROR_SERVER -> "Recognition server error. Please try again."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Timed out."
            else -> "Speech recognition error ($errorCode). Please try again."
        }
    }
}