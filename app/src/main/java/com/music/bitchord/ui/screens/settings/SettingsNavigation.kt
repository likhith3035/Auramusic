package com.music.bitchord.ui.screens.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global deep-link navigation controller for Settings sub-pages.
 *
 * Allows player controls, audio output pills, and menus to request navigation
 * directly into specific settings domains without adding state bloat to MainActivity.
 */
object SettingsNavigation {

    private val _pendingRoute = MutableStateFlow<SettingsRoute?>(null)
    val pendingRoute: StateFlow<SettingsRoute?> = _pendingRoute.asStateFlow()

    fun navigateTo(route: SettingsRoute) {
        _pendingRoute.value = route
    }

    fun clear() {
        _pendingRoute.value = null
    }
}

