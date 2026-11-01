package com.music.bitchord.ui.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.ai.AuraAiService
import com.music.bitchord.data.ai.AuraRadarCandidate
import com.music.bitchord.data.ai.AuraRadarResult
import com.music.bitchord.data.ai.AuraRadarService
import com.music.bitchord.data.ai.AuraRadarState
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Aura brand accent colors — kept for AI identity elements, not general UI
private val AuraTeal = Color(0xFF2DD4BF)
private val AuraIndigo = Color(0xFF6366F1)

/**
 * Radar Modality Tabs separating ambient room listening from manual lyric typing.
 */
enum class AuraRadarTab(val title: String, val icon: ImageVector) {
    AMBIENT_RADAR("Ambient Audio", Icons.Rounded.GraphicEq),
    LYRIC_SEARCH("Search by Lyrics", Icons.Rounded.Search),
}

/**
 * Aura Radar — song recognition overlay.
 *
 * Improvements over previous version:
 * - Manual "Tap to Listen" button instead of auto-starting (user is ready when they tap)
 * - 8-second recording for better Whisper transcription coverage
 * - Live countdown timer so user knows how long to hold the phone
 * - Retry with 12-second extended capture on second attempt
 * - Transcribed lyric is shown before search so user can verify / correct
 * - Graceful fallback: silence/no-lyrics auto-switches to Lyric Search with transcript pre-filled
 * - Specific NOT_FOUND reason messages (silence, no lyrics found, no catalog match)
 * - Keyboard gap fix: imePadding() applied once on bottom container only
 * - Themed to match the app's MaterialTheme color system
 */
@Composable
fun AuraRadarOverlay(
    onClose: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onQueueSong: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(AuraRadarTab.AMBIENT_RADAR) }

    // Ambient tab state
    var ambientState by remember { mutableStateOf(AuraRadarState.IDLE) }
    var ambientResult by remember { mutableStateOf<AuraRadarResult?>(null) }
    var ambientStatusMessage by remember { mutableStateOf("") }
    var ambientTranscript by remember { mutableStateOf("") }
    var recordingProgress by remember { mutableStateOf(0f) }  // 0f..1f during recording
    var retryCount by remember { mutableIntStateOf(0) }

    // Lyric tab state
    var lyricQuery by remember { mutableStateOf("") }
    var lyricState by remember { mutableStateOf(AuraRadarState.IDLE) }
    var lyricResult by remember { mutableStateOf<AuraRadarResult?>(null) }
    // Pre-fill from failed ambient transcript so user can correct and retry
    var lyricPreFill by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val audioRecorder = rememberAudioRecorder()

    // Sonar pulse animations
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sonar")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse1",
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "alpha1",
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse2",
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "alpha2",
    )

    /**
     * Start ambient listening. durationMs controls how long we capture audio.
     * First attempt = 8 seconds. Retry = 12 seconds for better coverage.
     */
    fun startAmbientListening(durationMs: Long = 8000L) {
        ambientState = AuraRadarState.LISTENING
        ambientResult = null
        ambientTranscript = ""
        ambientStatusMessage = if (durationMs > 8000L) "Extended listening (${durationMs / 1000}s)..." else "Listening for ${durationMs / 1000} seconds..."
        recordingProgress = 0f

        scope.launch {
            // Permission check
            if (!audioRecorder.hasPermission) {
                ambientStatusMessage = "Requesting microphone permission..."
                audioRecorder.requestPermission()
                delay(1200)
                if (!audioRecorder.hasPermission) {
                    ambientState = AuraRadarState.ERROR
                    ambientStatusMessage = "Microphone permission denied. Please grant it in Settings."
                    return@launch
                }
            }

            // Animate progress bar in parallel with recording
            val progressJob = launch {
                val steps = 80
                val stepDelay = durationMs / steps
                repeat(steps) { i ->
                    recordingProgress = (i + 1f) / steps
                    delay(stepDelay)
                }
                recordingProgress = 1f
            }

            ambientStatusMessage = "Hold phone near the speaker..."
            val audioBytes = audioRecorder.recordSample(durationMs = durationMs)
            progressJob.cancel()
            recordingProgress = 1f

            if (audioBytes == null || audioBytes.isEmpty()) {
                ambientState = AuraRadarState.NOT_FOUND
                ambientStatusMessage = "No audio captured. Check microphone access."
                return@launch
            }

            ambientState = AuraRadarState.ANALYZING
            ambientStatusMessage = "Transcribing audio with Whisper AI..."

            val transcribedText = AuraAiService.transcribeAudio(audioBytes)

            // Handle silence / no recognizable lyrics
            if (transcribedText.isNullOrBlank() || transcribedText.length < 3 || transcribedText == ".") {
                ambientState = AuraRadarState.NOT_FOUND
                ambientStatusMessage = "No lyrics detected in recording."
                // Auto-offer lyric search fallback but don't pre-fill garbage
                lyricPreFill = ""
                return@launch
            }

            // Show user what was transcribed before searching
            ambientTranscript = transcribedText
            ambientStatusMessage = "Matching \"$transcribedText\" in catalog..."

            val res = AuraRadarService.identifySong(transcribedText)
            if (res != null) {
                ambientResult = res
                ambientState = AuraRadarState.FOUND
                ambientStatusMessage = ""
            } else {
                ambientState = AuraRadarState.NOT_FOUND
                ambientStatusMessage = "No catalog match found for \"$transcribedText\"."
                // Pre-fill lyric search with the transcript so user can correct spelling
                lyricPreFill = transcribedText
            }
        }
    }

    // Lyric search
    fun searchLyric(query: String) {
        if (query.isBlank()) return
        lyricState = AuraRadarState.ANALYZING
        lyricResult = null
        scope.launch {
            val res = AuraRadarService.identifySong(query)
            if (res != null) {
                lyricResult = res
                lyricState = AuraRadarState.FOUND
            } else {
                lyricState = AuraRadarState.NOT_FOUND
            }
        }
    }

    // When switching to lyric tab from a failed ambient pass, pre-fill the query
    LaunchedEffect(selectedTab) {
        if (selectedTab == AuraRadarTab.LYRIC_SEARCH && lyricPreFill.isNotBlank() && lyricQuery.isBlank()) {
            lyricQuery = lyricPreFill
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Keyboard gap fix: statusBarsPadding + navigationBarsPadding on outer column.
                // imePadding() is only on the inner lyric-search input container — never doubled.
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // --- TOP BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuraOrbIcon(
                        size = 28.dp,
                        isThinking = ambientState == AuraRadarState.ANALYZING || lyricState == AuraRadarState.ANALYZING,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AURA RADAR",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.2.sp,
                                ),
                                color = AuraTeal,
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = AuraTeal.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    AuraTeal.copy(alpha = 0.4f),
                                ),
                            ) {
                                Text(
                                    text = "ACOUSTIC AI",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                    color = AuraTeal,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                )
                            }
                        }
                        Text(
                            text = "Song Recognition & Lyric Search",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Radar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outline),
            )

            // --- TAB CONTROL ---
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AuraRadarTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val tabBg by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            label = "tabBg",
                        )
                        val tabText by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "tabText",
                        )
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = tabBg,
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .clickable { selectedTab = tab },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) AuraTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.5.sp,
                                    ),
                                    color = tabText,
                                )
                            }
                        }
                    }
                }
            }

            // --- CONTENT ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (selectedTab == AuraRadarTab.AMBIENT_RADAR) {
                    AmbientRadarContent(
                        state = ambientState,
                        result = ambientResult,
                        statusMessage = ambientStatusMessage,
                        transcript = ambientTranscript,
                        recordingProgress = recordingProgress,
                        retryCount = retryCount,
                        pulse1 = pulse1,
                        alpha1 = alpha1,
                        pulse2 = pulse2,
                        alpha2 = alpha2,
                        onTapToListen = {
                            retryCount = 0
                            startAmbientListening(durationMs = 8000L)
                        },
                        onRetry = {
                            retryCount++
                            // Extended duration on retry
                            startAmbientListening(durationMs = if (retryCount >= 2) 12000L else 8000L)
                        },
                        onSwitchToLyrics = { selectedTab = AuraRadarTab.LYRIC_SEARCH },
                        onPlaySong = onPlaySong,
                        onQueueSong = onQueueSong,
                        onClose = onClose,
                    )
                } else {
                    LyricSearchContent(
                        query = lyricQuery,
                        onQueryChange = { lyricQuery = it },
                        state = lyricState,
                        result = lyricResult,
                        onSearch = { searchLyric(it) },
                        onPlaySong = onPlaySong,
                        onQueueSong = onQueueSong,
                        onClose = onClose,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Ambient Radar Content
// ---------------------------------------------------------------------------

@Composable
private fun AmbientRadarContent(
    state: AuraRadarState,
    result: AuraRadarResult?,
    statusMessage: String,
    transcript: String,
    recordingProgress: Float,
    retryCount: Int,
    pulse1: Float,
    alpha1: Float,
    pulse2: Float,
    alpha2: Float,
    onTapToListen: () -> Unit,
    onRetry: () -> Unit,
    onSwitchToLyrics: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onQueueSong: (Song) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (state == AuraRadarState.FOUND) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (state == AuraRadarState.FOUND) Arrangement.Top else Arrangement.Center,
    ) {
        when (state) {
            // -------- IDLE: waiting for user to tap --------
            AuraRadarState.IDLE -> {
                Spacer(Modifier.height(16.dp))

                // Big radar orb — tap to start
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.5.dp,
                            color = AuraTeal,
                            shape = CircleShape,
                        )
                        .clickable(onClick = onTapToListen),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = AuraTeal,
                            modifier = Modifier.size(52.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "TAP TO LISTEN",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = AuraTeal,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Hold your phone near the speaker,\nthen tap the button above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Records 8 seconds of audio for best accuracy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))
                SwitchToLyricsButton(onSwitchToLyrics)
            }

            // -------- LISTENING: recording in progress --------
            AuraRadarState.LISTENING -> {
                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Animated sonar rings
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = AuraIndigo.copy(alpha = alpha1),
                            radius = size.minDimension / 2 * pulse1,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                        drawCircle(
                            color = AuraTeal.copy(alpha = alpha2),
                            radius = size.minDimension / 2.3f * pulse2,
                            style = Stroke(width = 1.2.dp.toPx()),
                        )
                    }
                    // Core disc
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, AuraTeal, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = AuraTeal,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Progress bar shows recording duration
                LinearProgressIndicator(
                    progress = { recordingProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(3.dp)
                        .clip(CircleShape),
                    color = AuraTeal,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Spacer(Modifier.height(14.dp))

                // Status pill
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, AuraTeal.copy(alpha = 0.5f)),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = AuraTeal,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "LISTENING...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = AuraTeal,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // -------- ANALYZING: transcribing + searching --------
            AuraRadarState.ANALYZING -> {
                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = AuraIndigo.copy(alpha = alpha1 * 0.5f),
                            radius = size.minDimension / 2 * pulse1,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                        drawCircle(
                            color = AuraTeal.copy(alpha = alpha2 * 0.5f),
                            radius = size.minDimension / 2.3f * pulse2,
                            style = Stroke(width = 1.2.dp.toPx()),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, AuraTeal.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = AuraTeal,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Show what was transcribed so user can see in real-time
                if (transcript.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "HEARD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp,
                                ),
                                color = AuraTeal,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "\"$transcript\"",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, AuraTeal.copy(alpha = 0.4f)),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = AuraTeal,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "ANALYZING ACOUSTIC PATTERN...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = AuraTeal,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // -------- FOUND: show match card --------
            AuraRadarState.FOUND -> {
                result?.let { match ->
                    // Show transcribed lyric above the card
                    if (transcript.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Matched: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "\"$transcript\"",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    RadarMatchCard(
                        match = match,
                        onPlaySong = onPlaySong,
                        onQueueSong = onQueueSong,
                        onRetry = onRetry,
                        onClose = onClose,
                    )
                }
            }

            // -------- NOT FOUND: specific error with retry --------
            AuraRadarState.NOT_FOUND, AuraRadarState.ERROR -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = if (state == AuraRadarState.ERROR) "Microphone Error" else "No Match Found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = statusMessage.ifBlank {
                                "Ensure music is clearly audible, or try the Lyric Search tab."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        // Show what was transcribed so user knows what Aura heard
                        if (transcript.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Heard: \"$transcript\"",
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                        }

                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (state != AuraRadarState.ERROR) {
                                Button(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    shape = RoundedCornerShape(percent = 50),
                                ) {
                                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (retryCount == 0) "Try Again" else "Listen Longer",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Button(
                                onClick = onSwitchToLyrics,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = AuraTeal,
                                ),
                                shape = RoundedCornerShape(percent = 50),
                            ) {
                                Text("Lyric Search", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun SwitchToLyricsButton(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = AuraTeal,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Know the lyrics? Switch to Lyric Search",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Lyric Search Content
// ---------------------------------------------------------------------------

@Composable
private fun LyricSearchContent(
    query: String,
    onQueryChange: (String) -> Unit,
    state: AuraRadarState,
    result: AuraRadarResult?,
    onSearch: (String) -> Unit,
    onPlaySong: (Song) -> Unit,
    onQueueSong: (Song) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            // imePadding() lives HERE — only in the content that has a text field.
            // The outer overlay Column does NOT have imePadding(), so no double-padding gap.
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Search input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 0.5.dp,
                    color = if (query.isNotBlank())
                        AuraTeal
                    else
                        MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(24.dp),
                )
                .clickable {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = if (query.isNotBlank()) AuraTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Type 3+ lyric words (e.g. 'kallara chusedhi')...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.5.sp,
                    ),
                    cursorBrush = SolidColor(AuraTeal),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        onSearch(query)
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
            if (query.isNotBlank()) {
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Search button
        Button(
            onClick = {
                keyboardController?.hide()
                onSearch(query)
            },
            enabled = query.isNotBlank() && state != AuraRadarState.ANALYZING,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            ),
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
        ) {
            if (state == AuraRadarState.ANALYZING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text("Searching...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("Identify Track by Lyrics", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Suggestion chips
        Text(
            text = "TRY SUGGESTED LYRIC FRAGMENTS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start),
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LyricChip("🇮🇳 Telugu: kallara chusedhi") {
                onQueryChange("kallara chusedhi"); onSearch("kallara chusedhi")
            }
            LyricChip("🔥 Devara: all hail the tiger") {
                onQueryChange("all hail the tiger Fear Song"); onSearch("all hail the tiger Fear Song")
            }
            LyricChip("🎵 Telugu: samajavaragamana") {
                onQueryChange("samajavaragamana"); onSearch("samajavaragamana")
            }
            LyricChip("✨ Hindi: apna bana le") {
                onQueryChange("apna bana le"); onSearch("apna bana le")
            }
            LyricChip("⚡ Global: blinding lights") {
                onQueryChange("blinding lights the weeknd"); onSearch("blinding lights the weeknd")
            }
        }

        Spacer(Modifier.height(20.dp))

        // Results area
        when (state) {
            AuraRadarState.FOUND -> {
                result?.let { match ->
                    RadarMatchCard(
                        match = match,
                        onPlaySong = onPlaySong,
                        onQueueSong = onQueueSong,
                        onRetry = { onSearch(query) },
                        onClose = onClose,
                    )
                }
            }

            AuraRadarState.NOT_FOUND -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No Match Found",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Try using 3–4 consecutive words. For Telugu/Hindi, use Romanized spelling (e.g. 'kallara chusedhi').",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            AuraRadarState.IDLE -> {
                // Helpful guidance card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "💡 How Lyric Search Works",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AuraTeal,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Type any 3–5 words you remember from the song. Aura's AI engine matches phonetic and transliterated lyrics across Telugu, Hindi, Tamil, and English.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            else -> {}
        }
    }
}

// ---------------------------------------------------------------------------
// Shared Match Card
// ---------------------------------------------------------------------------

@Composable
private fun RadarMatchCard(
    match: AuraRadarResult,
    onPlaySong: (Song) -> Unit,
    onQueueSong: (Song) -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Match confidence badge
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = AuraTeal.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, AuraTeal.copy(alpha = 0.5f)),
            ) {
                Text(
                    text = "${(match.confidence * 100).toInt()}% MATCH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                    ),
                    color = AuraTeal,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            // Artwork
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (!match.song.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = match.song.thumbnailUrl,
                        contentDescription = match.song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = match.song.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = match.song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            // Play & Queue buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        onPlaySong(match.song)
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = { onQueueSong(match.song) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier.height(42.dp),
                ) {
                    Icon(Icons.Rounded.PlaylistAdd, null, modifier = Modifier.size(18.dp))
                }
            }

            // Alternative candidates
            if (match.candidates.size > 1) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "OTHER POSSIBLE MATCHES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                match.candidates.drop(1).take(3).forEach { candidate ->
                    CandidateRow(
                        candidate = candidate,
                        onPlaySong = { onPlaySong(candidate.song); onClose() },
                        onQueueSong = { onQueueSong(candidate.song) },
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Candidate row
// ---------------------------------------------------------------------------

@Composable
private fun CandidateRow(
    candidate: AuraRadarCandidate,
    onPlaySong: () -> Unit,
    onQueueSong: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onPlaySong),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (!candidate.song.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = candidate.song.thumbnailUrl,
                        contentDescription = candidate.song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.song.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = candidate.song.artist,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(6.dp))

            // Confidence badge
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    if (candidate.confidence >= 0.90f) AuraTeal.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outline,
                ),
            ) {
                Text(
                    text = "${(candidate.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (candidate.confidence >= 0.90f) AuraTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            Spacer(Modifier.width(4.dp))

            IconButton(onClick = onPlaySong, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            IconButton(onClick = onQueueSong, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Rounded.PlaylistAdd,
                    contentDescription = "Queue",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Lyric chip
// ---------------------------------------------------------------------------

@Composable
private fun LyricChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}
