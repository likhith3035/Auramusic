package com.music.bitchord.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class XAiMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class XAiRequest(
    val model: String,
    val messages: List<XAiMessage>,
    val temperature: Double = 0.7,
)

@Serializable
private data class XAiChoiceMessage(
    val role: String? = null,
    val content: String? = null,
)

@Serializable
private data class XAiChoice(
    val message: XAiChoiceMessage? = null,
)

@Serializable
private data class XAiResponse(
    val choices: List<XAiChoice> = emptyList(),
)

object XAiApiClient {
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
        model: String = AuraAiConfig.XAI_MODEL,
    ): Result<String> = runCatching {
        val messages = mutableListOf<XAiMessage>()
        messages.add(XAiMessage(role = "system", content = systemPrompt))
        for ((role, text) in history) {
            messages.add(XAiMessage(role = if (role == "user") "user" else "assistant", content = text))
        }
        messages.add(XAiMessage(role = "user", content = userPrompt))

        val bodyPayload = XAiRequest(
            model = model,
            messages = messages,
        )

        val response = client.post("https://api.x.ai/v1/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            header("User-Agent", "AuraMusic/1.0 (Android; Mobile)")
            contentType(ContentType.Application.Json)
            setBody(bodyPayload)
        }

        val rawText = response.bodyAsText()
        if (response.status.value !in 200..299) {
            throw IllegalStateException("xAI API error HTTP ${response.status.value}: $rawText")
        }

        val parsed = json.decodeFromString<XAiResponse>(rawText)
        val replyContent = parsed.choices.firstOrNull()?.message?.content
            ?: error("No choices returned from xAI API: $rawText")

        replyContent
    }
}
