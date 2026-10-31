package com.bengali.voicetyping.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bengali.voicetyping.viewmodel.VoiceTypingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTypingScreen(
    uiState: VoiceTypingUiState,
    onMicToggle: () -> Unit,
    onCopyText: () -> Unit,
    onClearText: () -> Unit,
    onShareText: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onAddPunctuation: (String) -> Unit,
    onTextManualChange: (String) -> Unit,
    onDismissError: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            onDismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "বাংলা ভয়েস টাইপিং",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "Bengali Speech-to-Text",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                actions = {
                    LanguageSelectorChip(
                        current = uiState.selectedLanguage,
                        onSelect = onLanguageSelected
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            MicControlBottomSection(
                isListening = uiState.isListening,
                rmsLevel = uiState.rmsLevel,
                onMicToggle = onMicToggle
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BengaliPunctuationBar(onAddPunctuation = onAddPunctuation)

            TranscriptionDisplayCard(
                transcribedText = uiState.transcribedText,
                partialText = uiState.partialText,
                isListening = uiState.isListening,
                charCount = uiState.charCount,
                wordCount = uiState.wordCount,
                onTextChange = onTextManualChange,
                onCopy = onCopyText,
                onClear = onClearText,
                onShare = onShareText
            )
        }
    }
}

@Composable
fun TranscriptionDisplayCard(
    transcribedText: String,
    partialText: String,
    isListening: Boolean,
    charCount: Int,
    wordCount: Int,
    onTextChange: (String) -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$charCount অক্ষর  ·  $wordCount শব্দ",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = onCopy,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "কপি করুন",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "কপি করুন",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "শেয়ার করুন",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "মুছুন",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = transcribedText,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                placeholder = {
                    Text(
                        text = if (isListening) "শুনছি... এখন বাংলায় কথা বলুন।" else "নিচের মাইক্রোফোন বাটনে ট্যাপ করে কথা বলা শুরু করুন...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 26.sp
                        )
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    lineHeight = 28.sp
                ),
                shape = RoundedCornerShape(14.dp)
            )

            AnimatedVisibility(
                visible = partialText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "রিয়েল-টাইম রেকগনিশন: $partialText",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun BengaliPunctuationBar(onAddPunctuation: (String) -> Unit) {
    val punctuations = listOf(
        "। " to "দাঁড়ি",
        ", " to "কমা",
        "? " to "জিজ্ঞাসা",
        "! " to "বিস্ময়",
        "— " to "ড্যাশ",
        ": " to "কোলন"
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(punctuations) { (symbol, _) ->
            OutlinedButton(
                onClick = { onAddPunctuation(symbol.trim()) },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = symbol.trim(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MicControlBottomSection(
    isListening: Boolean,
    rmsLevel: Float,
    onMicToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val micButtonColor by animateColorAsState(
        targetValue = if (isListening) Color(0xFFE11D48) else MaterialTheme.colorScheme.primary,
        label = "micColor"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(bottom = 24.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isListening) "শুনছি... বাংলায় স্পষ্ট করে বলুন" else "কথা বলতে মাইক্রোফোনে ট্যাপ করুন",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isListening) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(contentAlignment = Alignment.Center) {
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFFE11D48).copy(alpha = 0.2f))
                )
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(micButtonColor)
                    .clickable { onMicToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = if (isListening) "থামুন" else "শুরু করুন",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun LanguageSelectorChip(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .padding(end = 12.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val isBD = current == "bn-BD"
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isBD) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onSelect("bn-BD") }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "বাংলাদেশ (BD)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isBD) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (!isBD) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onSelect("bn-IN") }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "ভারত (IN)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (!isBD) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}