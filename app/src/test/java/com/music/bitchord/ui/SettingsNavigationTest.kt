package com.music.bitchord.ui

import com.music.bitchord.ui.screens.settings.SettingsNavigation
import com.music.bitchord.ui.screens.settings.SettingsRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsNavigationTest {

    @Test
    fun deepLinkNavigationFlow() {
        SettingsNavigation.clear()
        assertNull(SettingsNavigation.pendingRoute.value)

        SettingsNavigation.navigateTo(SettingsRoute.AUDIO)
        assertEquals(SettingsRoute.AUDIO, SettingsNavigation.pendingRoute.value)

        SettingsNavigation.navigateTo(SettingsRoute.APPEARANCE)
        assertEquals(SettingsRoute.APPEARANCE, SettingsNavigation.pendingRoute.value)

        SettingsNavigation.clear()
        assertNull(SettingsNavigation.pendingRoute.value)
    }
}

