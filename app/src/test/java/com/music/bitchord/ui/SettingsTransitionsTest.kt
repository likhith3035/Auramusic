package com.music.bitchord.ui

import com.music.bitchord.ui.screens.settings.SettingsRoute
import com.music.bitchord.ui.screens.settings.SettingsTransitions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTransitionsTest {
    @Test
    fun `navigating from hub to subpage is forward`() {
        assertTrue(SettingsTransitions.isForward(SettingsRoute.HUB, SettingsRoute.AUDIO))
        assertTrue(SettingsTransitions.isForward(SettingsRoute.HUB, SettingsRoute.APPEARANCE))
    }

    @Test
    fun `navigating from subpage to hub is reverse`() {
        assertFalse(SettingsTransitions.isForward(SettingsRoute.AUDIO, SettingsRoute.HUB))
        assertFalse(SettingsTransitions.isForward(SettingsRoute.NETWORK, SettingsRoute.HUB))
    }
}

