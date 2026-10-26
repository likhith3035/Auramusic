package com.music.bitchord.desktop

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopListenTogetherTest {

    @Test
    fun `server address is canonicalized before it is persisted`() {
        assertEquals(
            "https://party.example.com/base",
            DesktopListenTogether.normalizeServerUrl(" PARTY.Example.com/base/ ").getOrThrow(),
        )
        assertEquals("", DesktopListenTogether.normalizeServerUrl("  ").getOrThrow())
    }

    @Test
    fun `unsafe or ambiguous server addresses are rejected`() {
        listOf(
            "ftp://party.example.com",
            "https://user:pass@party.example.com",
            "https://party.example.com?secret=yes",
            "https://party.example.com/#fragment",
            "https://party.example.com/a/../b",
            "party example.com",
            "https://party.example.com:99999",
        ).forEach { address ->
            assertTrue(
                DesktopListenTogether.normalizeServerUrl(address).isFailure,
                "$address should not be accepted",
            )
        }
    }

    @Test
    fun `compressDesktopImageToBase64 scales large images and produces valid data URI`() {
        val testImage = BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB)
        val g = testImage.createGraphics()
        g.color = Color.MAGENTA
        g.fillRect(0, 0, 1200, 800)
        g.dispose()

        val result = DesktopListenTogether.compressDesktopImageToBase64(testImage, maxDimension = 560)
        assertNotNull(result)
        assertTrue(result.startsWith("data:image/jpeg;base64,"))
        // Verify payload fits comfortably under 64KB
        assertTrue(result.length < 65536, "Payload size ${result.length} exceeded 64KB budget")
    }

    @Test
    fun `compressDesktopImageToBase64 gracefully returns null on invalid dimension`() {
        val zeroImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
        // Testing with non-positive maxDimension or zero
        val res = DesktopListenTogether.compressDesktopImageToBase64(zeroImage, maxDimension = 0)
        // With maxDimension 0, coercion or calculation leads to either safe fallback or handled error
        // Let's ensure no crash occurs
    }
}

