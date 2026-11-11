package com.music.bitchord.data.ai

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuraAccessGateTest {

    @Test
    fun testUnlockPhraseWithSpaces() {
        assertTrue(AuraAccessGate.isAuraAiUnlockCode("auramusic by likhith"))
        assertTrue(AuraAccessGate.isAuraAiUnlockCode("AuraMusic By Likhith"))
        assertTrue(AuraAccessGate.isAuraAiUnlockCode("  auramusic   by   likhith  "))
    }

    @Test
    fun testUnlockPhraseWithoutSpaces() {
        assertTrue(AuraAccessGate.isAuraAiUnlockCode("auramusicbylikhith"))
        assertTrue(AuraAccessGate.isAuraAiUnlockCode("AuraMusicByLikhith"))
    }

    @Test
    fun testNormalSearchQueryDoesNotUnlock() {
        assertFalse(AuraAccessGate.isAuraAiUnlockCode("equalizer"))
        assertFalse(AuraAccessGate.isAuraAiUnlockCode("dark mode"))
        assertFalse(AuraAccessGate.isAuraAiUnlockCode("likhith"))
        assertFalse(AuraAccessGate.isAuraAiUnlockCode(""))
    }

    @Test
    fun testAdminTelemetryUnlockPhrase() {
        assertTrue(AuraAccessGate.isAdminTelemetryUnlockCode("adminofauramusic"))
        assertTrue(AuraAccessGate.isAdminTelemetryUnlockCode("admin of aura music"))
        assertTrue(AuraAccessGate.isAdminTelemetryUnlockCode("Admin Of Aura Music"))
        assertTrue(AuraAccessGate.isAdminTelemetryUnlockCode("  admin  of  auramusic  "))
        assertFalse(AuraAccessGate.isAdminTelemetryUnlockCode("admin"))
        assertFalse(AuraAccessGate.isAdminTelemetryUnlockCode("auramusic"))
        assertFalse(AuraAccessGate.isAdminTelemetryUnlockCode(""))
    }

    @Test
    fun testVerifyAdminRollNumber() {
        assertTrue(AuraAccessGate.verifyAdminRollNumber("3035"))
        assertTrue(AuraAccessGate.verifyAdminRollNumber(" 3035 "))
        assertFalse(AuraAccessGate.verifyAdminRollNumber("1234"))
        assertFalse(AuraAccessGate.verifyAdminRollNumber(""))
    }
}
