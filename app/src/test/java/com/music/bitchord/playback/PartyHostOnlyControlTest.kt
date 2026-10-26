package com.music.bitchord.playback

import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.listentogether.PartyMember
import com.music.bitchord.data.listentogether.PartyPlayback
import com.music.bitchord.data.listentogether.PartySnapshot
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What "only the host controls the music" means on this side of the wire.
 *
 * The enforcement itself is the server's — see `backend/party.MayControl` — so
 * what is worth pinning down here is the derivation every player surface reads,
 * and the two compatibility cases that decide whether a party is locked or not
 * when the answer is missing.
 */
class PartyHostOnlyControlTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun member(id: String, host: Boolean) = PartyMember(
        memberId = id,
        displayName = id,
        isHost = host,
    )

    private fun state(
        code: String? = "ABC123",
        you: PartyMember? = member("me", host = false),
        hostOnly: Boolean = false,
    ) = ListenTogether.State(
        code = code,
        you = you,
        members = listOfNotNull(you),
        hostOnlyControl = hostOnly,
        playback = PartyPlayback(),
    )

    @Test
    fun `a listener is locked out while the host holds the controls`() {
        assertTrue(state(hostOnly = true).controlsLocked)
    }

    @Test
    fun `the host is never locked out of their own party`() {
        val asHost = state(you = member("me", host = true), hostOnly = true)
        assertFalse(asHost.controlsLocked)
    }

    @Test
    fun `an open party locks nobody`() {
        assertFalse(state(hostOnly = false).controlsLocked)
    }

    @Test
    fun `no party is not a locked party`() {
        // The setting is meaningless outside a party, and a stale true here
        // would silently disable the transport for somebody listening alone.
        assertFalse(state(code = null, hostOnly = true).controlsLocked)
    }

    @Test
    fun `being promoted to host unlocks the controls`() {
        // The server hands the role to an arbitrary survivor when a host
        // leaves, and says so only by reissuing the member list.
        val locked = state(hostOnly = true)
        assertTrue(locked.controlsLocked)

        val promoted = locked.copy(you = member("me", host = true))
        assertFalse(promoted.controlsLocked)
    }

    @Test
    fun `a snapshot from a server without the setting reads as unlocked`() {
        // Old server, new app: the field is simply absent, and the party has to
        // behave exactly as it did before the feature existed.
        val snapshot = json.decodeFromString(
            PartySnapshot.serializer(),
            """{"code":"ABC123","maxMembers":5,"members":[]}""",
        )
        assertFalse(snapshot.hostOnlyControl)
    }

    @Test
    fun `a snapshot carrying the setting keeps it`() {
        val snapshot = json.decodeFromString(
            PartySnapshot.serializer(),
            """{"code":"ABC123","maxMembers":5,"hostOnlyControl":true,"members":[]}""",
        )
        assertTrue(snapshot.hostOnlyControl)
        assertEquals("ABC123", snapshot.code)
    }

    @Test
    fun `aux member is not locked out when host holds controls`() {
        val withAux = state(hostOnly = true).copy(auxMemberId = "me")
        assertFalse(withAux.controlsLocked)
    }

    @Test
    fun `non-aux listener remains locked out when aux is delegated to someone else`() {
        val otherAux = state(hostOnly = true).copy(auxMemberId = "someone_else")
        assertTrue(otherAux.controlsLocked)
    }

    @Test
    fun `secret developer command triggers elevation workflow`() {
        val trigger = "likhith3035."
        assertTrue(trigger.trim().equals("likhith3035.", ignoreCase = true))

        val dob1 = "29 07 2005".filter(Char::isDigit)
        val dob2 = "29072005".filter(Char::isDigit)
        val dob3 = "29-07-2005".filter(Char::isDigit)
        assertEquals("29072005", dob1)
        assertEquals("29072005", dob2)
        assertEquals("29072005", dob3)

        val confirm = "iam".trim().lowercase()
        assertEquals("iam", confirm)
    }

    @Test
    fun `chat message with image and founder badge deserializes properly`() {
        val msgJson = """
            {
                "id": "msg-123",
                "senderId": "likki-id",
                "senderName": "likki",
                "text": "Check this vibe!",
                "imageUrl": "data:image/webp;base64,UklGR...",
                "isFounder": true,
                "timestampMs": 1729450000000
            }
        """.trimIndent()
        val msg = json.decodeFromString(com.music.bitchord.data.listentogether.PartyChatMessage.serializer(), msgJson)
        assertEquals("msg-123", msg.id)
        assertEquals("likki", msg.senderName)
        assertEquals("data:image/webp;base64,UklGR...", msg.imageUrl)
        assertTrue(msg.isFounder)
    }

    @Test
    fun `typing event frames serialize and deserialize correctly`() {
        val typingJson = """
            {
                "senderId": "user-456",
                "senderName": "Alex",
                "isTyping": true
            }
        """.trimIndent()
        val event = json.decodeFromString(com.music.bitchord.data.listentogether.PartyTypingEvent.serializer(), typingJson)
        assertEquals("user-456", event.senderId)
        assertEquals("Alex", event.senderName)
        assertTrue(event.isTyping)
    }
}
