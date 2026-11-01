package com.music.bitchord.data.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CamelotWheelTest {

    @Test
    fun testParseCamelotKey() {
        val key8A = CamelotKey.fromString("8A")
        assertNotNull(key8A)
        assertEquals(8, key8A.number)
        assertEquals('A', key8A.letter)

        val key11B = CamelotKey.fromString("11b")
        assertNotNull(key11B)
        assertEquals(11, key11B.number)
        assertEquals('B', key11B.letter)

        assertNull(CamelotKey.fromString("13A"))
        assertNull(CamelotKey.fromString("0B"))
        assertNull(CamelotKey.fromString("invalid"))
    }

    @Test
    fun testKeyCompatibility() {
        val key8A = CamelotKey(8, 'A')
        val key8B = CamelotKey(8, 'B')
        val key7A = CamelotKey(7, 'A')
        val key9A = CamelotKey(9, 'A')
        val key4A = CamelotKey(4, 'A')

        // Identical key is compatible
        assertTrue(key8A.isCompatibleWith(key8A))

        // Relative major/minor is compatible
        assertTrue(key8A.isCompatibleWith(key8B))

        // Adjacent keys are compatible
        assertTrue(key8A.isCompatibleWith(key7A))
        assertTrue(key8A.isCompatibleWith(key9A))

        // Distant keys clash harmonically
        assertFalse(key8A.isCompatibleWith(key4A))
    }

    @Test
    fun testCamelotClockWrapAround() {
        val key12A = CamelotKey(12, 'A')
        val key1A = CamelotKey(1, 'A')
        val key11A = CamelotKey(11, 'A')

        // 12A wraps around to 1A and 11A
        assertTrue(key12A.isCompatibleWith(key1A))
        assertTrue(key12A.isCompatibleWith(key11A))
    }
}
