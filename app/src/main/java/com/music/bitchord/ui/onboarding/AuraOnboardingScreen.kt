package com.music.bitchord.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.audio.rememberArtistSampleController
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.taste.MusicTasteStore
import com.music.bitchord.ui.components.AuraWaveformBrandMark
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.theme.NothingThemeTokens

private enum class OnboardingStep {
    WELCOME_AND_VIBE,
    CONNECT,
}

/**
 * First-run welcome and onboarding wizard for AuraMusic covering listener name,
 * visual look & startup chime, and YouTube Music sign-in or instant Guest start.
 * Music taste personalization lives exclusively inside Settings.
 */
@Composable
fun AuraOnboardingScreen(
    onSignInGoogle: () -> Unit,
    onSkipSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nothingOsMode by AppSettings.nothingOsMode.collectAsStateWithLifecycle()
    val startupChime by AppSettings.startupChimeEnabled.collectAsStateWithLifecycle()
    val initialNickname by AppSettings.listenerNickname.collectAsStateWithLifecycle()

    var step by remember { mutableStateOf(OnboardingStep.WELCOME_AND_VIBE) }
    var nickname by remember(initialNickname) {
        mutableStateOf(initialNickname.ifBlank { ListenTogether.nickname() })
    }

    val focusManager = LocalFocusManager.current

    fun persistProfile() {
        val cleaned = nickname.trim().take(40)
        AppSettings.setListenerNickname(cleaned)
        if (cleaned.isNotBlank()) {
            ListenTogether.setNickname(cleaned)
        }
    }

    fun finishOnboarding(signIn: Boolean) {
        persistProfile()
        AppSettings.setOnboardingCompleted(true)
        if (signIn) onSignInGoogle() else onSkipSignIn()
    }

    BackHandler(enabled = true) {
        if (step == OnboardingStep.CONNECT) {
            step = OnboardingStep.WELCOME_AND_VIBE
        } else {
            finishOnboarding(signIn = false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (nothingOsMode) NothingThemeTokens.Black else Color.Black),
    ) {
        if (!nothingOsMode) {
            AuraLivingMeshBackground()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Segmented progress bar + Back / Skip header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val activeAccent = if (nothingOsMode) NothingThemeTokens.Red else Color.White
                    OnboardingStep.entries.forEachIndexed { idx, _ ->
                        val filled = idx <= step.ordinal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (filled) activeAccent else Color.White.copy(alpha = 0.2f),
                                ),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (step.ordinal > 0) {
                        IconButton(
                            onClick = { step = OnboardingStep.entries[step.ordinal - 1] },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                            )
                        }
                    } else {
                        Spacer(Modifier.size(36.dp))
                    }

                    TextButton(
                        onClick = { finishOnboarding(signIn = false) },
                    ) {
                        Text(
                            text = "Skip",
                            color = Color.White.copy(alpha = 0.72f),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(tween(240)) togetherWith fadeOut(tween(180))
                },
                label = "onboarding_step_content",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { currentStep ->
                when (currentStep) {
                    OnboardingStep.WELCOME_AND_VIBE -> {
                        WelcomeVibeStep(
                            nickname = nickname,
                            onNicknameChange = { nickname = it.take(40) },
                            nothingOsMode = nothingOsMode,
                            onNothingOsModeChange = AppSettings::setNothingOsMode,
                            startupChime = startupChime,
                            onStartupChimeChange = AppSettings::setStartupChimeEnabled,
                            onContinue = {
                                focusManager.clearFocus()
                                persistProfile()
                                step = OnboardingStep.CONNECT
                            },
                        )
                    }

                    OnboardingStep.CONNECT -> {
                        StepConnectContent(
                            nickname = nickname,
                            nothingOsMode = nothingOsMode,
                            onSignInGoogle = { finishOnboarding(signIn = true) },
                            onStartListening = { finishOnboarding(signIn = false) },
                            onBackToLook = { step = OnboardingStep.WELCOME_AND_VIBE },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeVibeStep(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    nothingOsMode: Boolean,
    onNothingOsModeChange: (Boolean) -> Unit,
    startupChime: Boolean,
    onStartupChimeChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    val haptics = rememberHaptics()
    val ctaBg = if (nothingOsMode) NothingThemeTokens.Red else Color.White
    val ctaContent = if (nothingOsMode) Color.White else Color(0xFF0A0B0F)
    val fieldAccent = if (nothingOsMode) NothingThemeTokens.Red else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(14.dp))
        AuraWaveformBrandMark(
            width = 148.dp,
            height = 80.dp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Welcome to AuraMusic",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Lossless streaming, direct YouTube audio links, and synced listening rooms in a fast native player.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            label = { Text("Your name (optional)") },
            placeholder = { Text("Used for personal greetings & parties") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onContinue() }),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF14151B),
                unfocusedContainerColor = Color(0xFF14151B),
                focusedBorderColor = fieldAccent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = fieldAccent,
                unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Choose your interface look",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LookOptionCard(
                    title = "Liquid Glass",
                    subtitle = "Frosted mesh glow",
                    selected = !nothingOsMode,
                    swatchColor = Color(0xFF38BDF8),
                    accentColor = Color.White,
                    surfaceColor = Color(0xFF181A24),
                    onClick = {
                        haptics.play(Haptic.Select)
                        onNothingOsModeChange(false)
                    },
                    modifier = Modifier.weight(1f),
                )
                LookOptionCard(
                    title = "Nothing OS",
                    subtitle = "Pitch black & Red",
                    selected = nothingOsMode,
                    swatchColor = NothingThemeTokens.Red,
                    accentColor = NothingThemeTokens.Red,
                    surfaceColor = NothingThemeTokens.Surface,
                    onClick = {
                        haptics.play(Haptic.Select)
                        onNothingOsModeChange(true)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            color = if (nothingOsMode) NothingThemeTokens.Surface else Color(0xFF14151B),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.12f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStartupChimeChange(!startupChime) },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = fieldAccent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Startup chime",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        text = "Play soft harmonic cue on cold launch",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
                Switch(
                    checked = startupChime,
                    onCheckedChange = onStartupChimeChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = fieldAccent,
                        checkedBorderColor = fieldAccent,
                        checkedThumbColor = ctaContent,
                    ),
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                haptics.play(Haptic.Tap)
                onContinue()
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = ctaBg,
                contentColor = ctaContent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Text(
                text = "Continue",
                color = ctaContent,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun LookOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    swatchColor: Color,
    accentColor: Color,
    surfaceColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accentColor else Color.White.copy(alpha = 0.14f),
        ),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(swatchColor),
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.62f),
            )
        }
    }
}

@Composable
private fun StepConnectContent(
    nickname: String,
    nothingOsMode: Boolean,
    onSignInGoogle: () -> Unit,
    onStartListening: () -> Unit,
    onBackToLook: () -> Unit,
) {
    val accentColor = if (nothingOsMode) NothingThemeTokens.Red else Color.White
    val greeting = if (nickname.isNotBlank()) "You are all set, $nickname" else "Your soundstage is ready"
    val lookLabel = if (nothingOsMode) "Nothing OS look active" else "Liquid Glass look active"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f))
                .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(30.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "$lookLabel. Connect your YouTube Music library or start listening right away as a Guest.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "You can pick favorite languages, singers, and music directors anytime inside Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.56f),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(26.dp))

        Button(
            onClick = onSignInGoogle,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_g),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Connect YouTube Music",
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onStartListening,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, accentColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            Text(
                text = "Start Listening as Guest",
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }

        Spacer(Modifier.height(10.dp))

        TextButton(onClick = onBackToLook) {
            Text(
                text = "Change interface look",
                color = Color.White.copy(alpha = 0.65f),
            )
        }
    }
}

/**
 * Standalone reusable modal wrapper for tuning music languages, singers, and music directors
 * from anywhere in Android (What's New CTA, Settings, or deep-link overlay) while managing
 * its own disposable sample audio preview controller.
 */
@Composable
fun AuraMainTasteEditorOverlay(
    onDismiss: () -> Unit,
) {
    val savedTaste by MusicTasteStore.preferences.collectAsStateWithLifecycle()
    val nothingOsMode by AppSettings.nothingOsMode.collectAsStateWithLifecycle()
    val previewController = rememberArtistSampleController()
    val previewScope = rememberCoroutineScope()

    com.music.bitchord.ui.taste.MusicTasteEditorDialog(
        initialLanguages = savedTaste.selectedLanguageIds,
        initialArtists = savedTaste.selectedArtists,
        nothingOsMode = nothingOsMode,
        onDismiss = {
            previewController.stop()
            onDismiss()
        },
        onSave = { langs, artists ->
            previewController.stop()
            MusicTasteStore.saveSelection(
                languageIds = langs,
                artists = artists,
                markConfigured = true,
            )
            MusicTasteStore.notifyTasteUpdated(syncSubscriptions = savedTaste.autoSubscribeOnSignIn)
            onDismiss()
        },
        onPreviewArtist = { artist ->
            previewController.togglePreview(artist, previewScope)
        },
        previewingArtistKey = previewController.playingKey,
        previewLoadingArtistKey = previewController.loadingKey,
    )
}

