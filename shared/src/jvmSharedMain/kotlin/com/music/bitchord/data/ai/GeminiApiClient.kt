package com.music.bitchord.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class GeminiPart(
    val text: String,
)

@Serializable
private data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>,
)

@Serializable
private data class GeminiSystemInstruction(
    val parts: List<GeminiPart>,
)

@Serializable
private data class GeminiRequest(
    val systemInstruction: GeminiSystemInstruction? = null,
    val contents: List<GeminiContent>,
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent? = null,
)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

object GeminiApiClient {
    private val json = Json { ignoreUnknownKeys = true }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
        }
    }

    suspend fun chat(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        history: List<Pair<String, String>> = emptyList(),
        model: String = AuraAiConfig.GEMINI_MODEL,
    ): Result<String> = runCatching {
        val modelsToTry = listOf(
            model,
            AuraAiConfig.GEMINI_FALLBACK_MODEL,
            "gemini-1.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-pro",
        ).distinct()

        var lastError: Throwable? = null

        val contents = mutableListOf<GeminiContent>()
        for ((role, text) in history) {
            contents.add(
                GeminiContent(
                    role = if (role == "user") "user" else "model",
                    parts = listOf(GeminiPart(text = text)),
                )
            )
        }
        contents.add(
            GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = userPrompt)),
            )
        )

        val request = GeminiRequest(
            systemInstruction = GeminiSystemInstruction(parts = listOf(GeminiPart(text = systemPrompt))),
            contents = contents,
        )

        for (targetModel in modelsToTry) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$apiKey"
                val response = client.post(endpoint) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                val rawText = response.bodyAsText()
                if (response.status.value !in 200..299) {
                    throw IllegalStateException("Gemini API HTTP ${response.status.value}: $rawText")
                }

                val parsed = json.decodeFromString<GeminiResponse>(rawText)
                val reply = parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!reply.isNullOrBlank()) {
                    return@runCatching reply
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        throw (lastError ?: IllegalStateException("All Gemini models failed"))
    }
}
