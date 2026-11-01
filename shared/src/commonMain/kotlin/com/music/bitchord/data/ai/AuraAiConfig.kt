package com.music.bitchord.data.ai

/**
 * AI Provider Configuration & Multi-Tier Key Pool.
 */
object AuraAiConfig {
    private fun decode(bytes: IntArray, salt: Int = 90): String =
        bytes.map { (it xor salt).toChar() }.joinToString("")

    // Primary Gemini API Key
    val PRIMARY_GEMINI_KEY = decode(intArrayOf(27, 11, 116, 27, 56, 98, 8, 20, 108, 17, 51, 61, 105, 98, 54, 24, 15, 110, 34, 28, 49, 98, 108, 54, 13, 109, 22, 15, 28, 61, 5, 8, 60, 21, 14, 29, 104, 19, 28, 40, 18, 111, 99, 52, 22, 30, 20, 54, 42, 105, 48, 42, 61))
    // Backup Gemini API Key
    val BACKUP_GEMINI_KEY = decode(intArrayOf(27, 11, 116, 27, 56, 98, 8, 20, 108, 16, 40, 35, 10, 35, 62, 53, 63, 42, 21, 63, 99, 14, 21, 110, 46, 104, 119, 42, 61, 5, 107, 51, 8, 8, 20, 45, 41, 56, 54, 27, 111, 42, 3, 106, 27, 8, 60, 98, 14, 49, 3, 59, 45))

    // Primary Groq API Key
    val PRIMARY_GROQ_KEY = decode(intArrayOf(61, 41, 49, 5, 42, 59, 110, 43, 109, 15, 17, 13, 20, 44, 108, 18, 34, 104, 0, 25, 18, 60, 55, 16, 13, 29, 62, 35, 56, 105, 28, 3, 17, 46, 10, 56, 44, 56, 55, 3, 106, 34, 99, 32, 57, 0, 30, 20, 23, 106, 52, 109, 108, 110, 107, 40))
    // Backup Groq API Key
    val BACKUP_GROQ_KEY = decode(intArrayOf(61, 41, 49, 5, 62, 60, 16, 17, 52, 51, 60, 8, 20, 14, 42, 61, 54, 61, 109, 31, 105, 59, 24, 9, 13, 29, 62, 35, 56, 105, 28, 3, 108, 52, 53, 32, 32, 110, 15, 20, 40, 104, 111, 104, 9, 63, 47, 25, 54, 31, 0, 49, 47, 106, 15, 52))

    // Official xAI (Grok) API Key
    val PRIMARY_XAI_KEY = decode(intArrayOf(34, 59, 51, 119, 50, 9, 40, 19, 2, 99, 48, 107, 40, 107, 42, 98, 35, 8, 45, 46, 30, 98, 25, 8, 53, 110, 13, 99, 42, 63, 17, 15, 9, 17, 107, 104, 49, 98, 104, 47, 50, 41, 13, 18, 45, 11, 15, 0, 41, 9, 8, 48, 98, 109, 52, 8, 29, 51, 59, 48, 14, 59, 35, 62, 23, 48, 0, 59, 44, 104, 25, 9, 108, 108, 51, 14, 54, 50, 21, 99, 12, 54, 19, 28))

    // Verified active models on Groq
    const val GROQ_MODEL = "llama-3.3-70b-versatile"
    const val GROQ_FAST_MODEL = "llama-3.1-8b-instant"
    const val GEMINI_MODEL = "gemini-1.5-flash"
    const val GEMINI_FALLBACK_MODEL = "gemini-2.0-flash"
    const val XAI_MODEL = "grok-2-latest"
}
