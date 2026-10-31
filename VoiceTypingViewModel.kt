package com.bengali.voicetyping.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.bengali.voicetyping.recognition.SpeechEvent
import com.bengali.voicetyping.recognition.SpeechRecognizerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VoiceTypingUiState(
    val transcribedText: String = "",
    val partialText: String = "",
    val isListening: Boolean = false,
    val selectedLanguage: String = "bn-BD",
    val rmsLevel: Float = 0f,
    val charCount: Int = 0,
    val wordCount: Int = 0,
    val errorMessage: String? = null
)

class VoiceTypingViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VoiceTypingUiState())
    val uiState: StateFlow<VoiceTypingUiState> = _uiState.asStateFlow()

    private val speechRecognizerManager: SpeechRecognizerManager by lazy {
        SpeechRecognizerManager(getApplication()) { event ->
            handleSpeechEvent(event)
        }
    }

    private fun handleSpeechEvent(event: SpeechEvent) {
        when (event) {
            is SpeechEvent.ReadyForSpeech -> {
                _uiState.update { it.copy(isListening = true, errorMessage = null) }
            }
            is SpeechEvent.BeginningOfSpeech -> {
                _uiState.update { it.copy(isListening = true) }
            }
            is SpeechEvent.RmsChanged -> {
                _uiState.update { it.copy(rmsLevel = event.rmsdB) }
            }
            is SpeechEvent.PartialResult -> {
                _uiState.update { it.copy(partialText = event.partialText) }
            }
            is SpeechEvent.FinalResult -> {
                val newResult = event.text.trim()
                if (newResult.isNotEmpty()) {
                    _uiState.update { state ->
                        val combinedText = if (state.transcribedText.isBlank()) {
                            newResult
                        } else {
                            "${state.transcribedText} $newResult"
                        }
                        state.copy(
                            transcribedText = combinedText,
                            partialText = "",
                            isListening = false,
                            rmsLevel = 0f,
                            charCount = combinedText.length,
                            wordCount = calculateWordCount(combinedText)
                        )
                    }
                } else {
                    _uiState.update { it.copy(isListening = false, partialText = "", rmsLevel = 0f) }
                }
            }
            is SpeechEvent.EndOfSpeech -> {
                _uiState.update { it.copy(isListening = false, rmsLevel = 0f) }
            }
            is SpeechEvent.Error -> {
                _uiState.update {
                    it.copy(
                        isListening = false,
                        rmsLevel = 0f,
                        partialText = "",
                        errorMessage = event.message
                    )
                }
            }
        }
    }

    fun handleMicToggleRequest(requestPermissionAction: () -> Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            onMicClick()
        } else {
            requestPermissionAction()
        }
    }

    fun onMicClick() {
        if (_uiState.value.isListening) {
            speechRecognizerManager.stopListening()
            _uiState.update { it.copy(isListening = false, rmsLevel = 0f) }
        } else {
            _uiState.update { it.copy(errorMessage = null, partialText = "") }
            speechRecognizerManager.startListening(_uiState.value.selectedLanguage)
        }
    }

    fun onPermissionDenied() {
        _uiState.update {
            it.copy(errorMessage = "মাইক্রোফোন ব্যবহারের অনুমতি প্রয়োজন (Microphone permission is required)")
        }
    }

    fun copyToClipboard(): Boolean {
        val textToCopy = getEffectiveText()
        if (textToCopy.isBlank()) return false

        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Bengali Voice Text", textToCopy)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(getApplication(), "ক্লিপবোর্ডে কপি করা হয়েছে! (Copied to Clipboard)", Toast.LENGTH_SHORT).show()
        return true
    }

    fun shareTranscribedText() {
        val textToShare = getEffectiveText()
        if (textToShare.isBlank()) return

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, textToShare)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val shareIntent = Intent.createChooser(sendIntent, "বাংলা টেক্সট শেয়ার করুন").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        getApplication<Application>().startActivity(shareIntent)
    }

    fun clearText() {
        speechRecognizerManager.cancel()
        _uiState.update {
            it.copy(
                transcribedText = "",
                partialText = "",
                isListening = false,
                charCount = 0,
                wordCount = 0
            )
        }
    }

    fun updateTranscribedText(newText: String) {
        _uiState.update {
            it.copy(
                transcribedText = newText,
                charCount = newText.length,
                wordCount = calculateWordCount(newText)
            )
        }
    }

    fun appendPunctuation(punctuation: String) {
        _uiState.update { state ->
            val updated = state.transcribedText.trimEnd() + punctuation + " "
            state.copy(
                transcribedText = updated,
                charCount = updated.length,
                wordCount = calculateWordCount(updated)
            )
        }
    }

    fun setLanguage(languageTag: String) {
        if (_uiState.value.selectedLanguage == languageTag) return
        if (_uiState.value.isListening) {
            speechRecognizerManager.stopListening()
        }
        _uiState.update { it.copy(selectedLanguage = languageTag, isListening = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun getEffectiveText(): String {
        val main = _uiState.value.transcribedText.trim()
        val partial = _uiState.value.partialText.trim()
        return if (partial.isNotEmpty()) {
            if (main.isEmpty()) partial else "$main $partial"
        } else {
            main
        }
    }

    private fun calculateWordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).size
    }

    fun cleanup() {
        speechRecognizerManager.destroy()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}