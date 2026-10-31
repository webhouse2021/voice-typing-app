package com.bengali.voicetyping

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bengali.voicetyping.ui.VoiceTypingScreen
import com.bengali.voicetyping.ui.theme.BengaliVoiceTypingTheme
import com.bengali.voicetyping.viewmodel.VoiceTypingViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VoiceTypingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BengaliVoiceTypingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BengaliVoiceTypingApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cleanup()
    }
}

@Composable
fun BengaliVoiceTypingApp(viewModel: VoiceTypingViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Android runtime permission launcher for microphone access
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onMicClick()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    VoiceTypingScreen(
        uiState = uiState,
        onMicToggle = {
            viewModel.handleMicToggleRequest {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onCopyText = { viewModel.copyToClipboard() },
        onClearText = { viewModel.clearText() },
        onShareText = { viewModel.shareTranscribedText() },
        onLanguageSelected = { localeTag -> viewModel.setLanguage(localeTag) },
        onAddPunctuation = { punct -> viewModel.appendPunctuation(punct) },
        onTextManualChange = { newText -> viewModel.updateTranscribedText(newText) },
        onDismissError = { viewModel.dismissError() }
    )
}