package com.music.bitchord.ui

import com.music.bitchord.ui.screens.settings.SettingsRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsRouteTest {
    @Test
    fun `hub is the root route`() {
        val root = SettingsRoute.HUB
        assertEquals("hub", root.id)
    }

    @Test
    fun `route resolution matches known category ids`() {
        assertEquals(SettingsRoute.AUDIO, SettingsRoute.fromId("audio"))
        assertEquals(SettingsRoute.APPEARANCE, SettingsRoute.fromId("appearance"))
        assertEquals(SettingsRoute.CONTENT, SettingsRoute.fromId("content"))
        assertEquals(SettingsRoute.STORAGE, SettingsRoute.fromId("storage"))
        assertEquals(SettingsRoute.NETWORK, SettingsRoute.fromId("network"))
        assertEquals(SettingsRoute.ADVANCED, SettingsRoute.fromId("advanced"))
        assertNull(SettingsRoute.fromId("unknown_route"))
    }
}

