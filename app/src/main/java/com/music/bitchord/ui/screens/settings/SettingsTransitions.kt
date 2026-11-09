package com.music.bitchord.ui.screens.settings

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

object SettingsTransitions {
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    const val SLIDE_DURATION_MS = 280

    fun isForward(initial: SettingsRoute, target: SettingsRoute): Boolean {
        return initial == SettingsRoute.HUB && target != SettingsRoute.HUB
    }

    fun transitionSpec(initial: SettingsRoute, target: SettingsRoute): ContentTransform {
        val forward = isForward(initial, target)
        val enter = if (forward) {
            slideInHorizontally(
                animationSpec = tween(SLIDE_DURATION_MS, easing = EmphasizedDecelerateEasing),
                initialOffsetX = { fullWidth -> (fullWidth * 0.95f).toInt() },
            ) + fadeIn(tween(200))
        } else {
            slideInHorizontally(
                animationSpec = tween(SLIDE_DURATION_MS, easing = EmphasizedDecelerateEasing),
                initialOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
            ) + fadeIn(tween(200))
        }

        val exit = if (forward) {
            slideOutHorizontally(
                animationSpec = tween(SLIDE_DURATION_MS, easing = EmphasizedAccelerateEasing),
                targetOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
            ) + fadeOut(tween(150))
        } else {
            slideOutHorizontally(
                animationSpec = tween(SLIDE_DURATION_MS, easing = EmphasizedAccelerateEasing),
                targetOffsetX = { fullWidth -> (fullWidth * 0.95f).toInt() },
            ) + fadeOut(tween(150))
        }

        return enter togetherWith exit
    }
}

