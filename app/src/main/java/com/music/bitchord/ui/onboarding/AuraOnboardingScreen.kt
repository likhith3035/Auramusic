package com.music.bitchord.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.R
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.AuraWaveformBrandMark
import com.music.bitchord.ui.icons.SfIcons

private enum class OnboardingStep {
    NAME,
    CONNECT,
}

/**
 * First-run welcome and onboarding screen for AuraMusic.
 * Features living harmonic mesh background, authentic vector soundwave branding,
 * and a 2-step setup flow for party nickname and YouTube account connection.
 */
@Composable
fun AuraOnboardingScreen(
    onSignInGoogle: () -> Unit,
    onSkipSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableStateOf(OnboardingStep.NAME) }
    var nickname by remember { mutableStateOf(ListenTogether.nickname()) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = true) {
        if (currentStep == OnboardingStep.CONNECT) {
            currentStep = OnboardingStep.NAME
        } else {
            AppSettings.setOnboardingCompleted(true)
            onSkipSignIn()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                focusManager.clearFocus()
            },
    ) {
        // Continuous, butter-smooth 60/120Hz ambient living mesh background
        AuraLivingMeshBackground()

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState == OnboardingStep.CONNECT) {
                    (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(320)))
                        .togetherWith(slideOutHorizontally(tween(380, easing = FastOutSlowInEasing)) { -it / 2 } + fadeOut(tween(260)))
                } else {
                    (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { -it / 2 } + fadeIn(tween(320)))
                        .togetherWith(slideOutHorizontally(tween(380, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(260)))
                }
            },
            label = "onboarding_step_content",
            modifier = Modifier.fillMaxSize(),
        ) { step ->
            when (step) {
                OnboardingStep.NAME -> {
                    StepNameContent(
                        nickname = nickname,
                        onNicknameChange = { nickname = it.take(40) },
                        onNext = {
                            focusManager.clearFocus()
                            if (nickname.isNotBlank()) {
                                ListenTogether.setNickname(nickname.trim())
                            }
                            currentStep = OnboardingStep.CONNECT
                        },
                    )
                }

                OnboardingStep.CONNECT -> {
                    StepConnectContent(
                        onBack = {
                            currentStep = OnboardingStep.NAME
                        },
                        onSignIn = {
                            AppSettings.setOnboardingCompleted(true)
                            onSignInGoogle()
                        },
                        onSkip = {
                            AppSettings.setOnboardingCompleted(true)
                            onSkipSignIn()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StepNameContent(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Spacer(Modifier.height(18.dp))

        // Aura vector waveform brand mark
        AuraWaveformBrandMark(
            width = 76.dp,
            height = 46.dp,
            animated = true,
        )

        Spacer(Modifier.height(28.dp))

        // Headline
        Text(
            text = stringResource(R.string.onboarding_title),
            color = Color.White,
            fontSize = 35.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 42.sp,
            letterSpacing = (-0.6).sp,
        )

        Spacer(Modifier.weight(1f))

        // Prompt
        Text(
            text = stringResource(R.string.onboarding_name_prompt),
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(14.dp))

        // Name input field
        BasicTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            cursorBrush = SolidColor(Color(0xFF00E5FF)),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onNext()
                },
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(29.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (nickname.isEmpty()) {
                        Text(
                            text = stringResource(R.string.onboarding_name_hint),
                            color = Color.White.copy(alpha = 0.42f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                    innerTextField()
                }
            },
        )

        Spacer(Modifier.height(10.dp))

        // Footer note
        Text(
            text = stringResource(R.string.onboarding_name_footer),
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(Modifier.height(28.dp))

        // Next Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(
                    if (nickname.isNotBlank()) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.18f)
                )
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.onboarding_next),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StepConnectContent(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Spacer(Modifier.height(14.dp))

        // Circular back button
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SfIcons.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.height(32.dp))

        // Headline
        Text(
            text = stringResource(R.string.onboarding_connect_title),
            color = Color.White,
            fontSize = 35.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 42.sp,
            letterSpacing = (-0.6).sp,
        )

        Spacer(Modifier.height(16.dp))

        // Description body
        Text(
            text = stringResource(R.string.onboarding_connect_body),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 16.sp,
            lineHeight = 23.sp,
        )

        Spacer(Modifier.weight(1f))

        // Sign in with Google Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(Color.White)
                .clickable(onClick = onSignIn),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_google_g),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.onboarding_sign_in_google),
                    color = Color(0xFF1F1F1F),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Skip sign in Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .clickable(onClick = onSkip),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.onboarding_skip_sign_in),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

