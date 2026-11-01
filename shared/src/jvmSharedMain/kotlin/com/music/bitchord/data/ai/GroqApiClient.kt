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
private data class GroqMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Double = 0.7,
)

@Serializable
private data class GroqChoiceMessage(
    val role: String? = null,
    val content: String? = null,
)

@Serializable
private data class GroqChoice(
    val message: GroqChoiceMessage? = null,
)

@Serializable
private data class GroqResponse(
    val choices: List<GroqChoice> = emptyList(),
)

@Serializable
private data class GroqWhisperResponse(
    val text: String? = null,
)

object GroqApiClient {
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
        model: String = AuraAiConfig.GROQ_MODEL,
    ): Result<String> = runCatching {
        val modelsToTry = listOf(model, AuraAiConfig.GROQ_FAST_MODEL, "llama-3.3-70b-versatile", "llama-3.1-8b-instant").distinct()
        var lastError: Throwable? = null

        for (targetModel in modelsToTry) {
            try {
                val messages = mutableListOf<GroqMessage>()
                messages.add(GroqMessage(role = "system", content = systemPrompt))
                for ((role, text) in history) {
                    messages.add(GroqMessage(role = if (role == "user") "user" else "assistant", content = text))
                }
                messages.add(GroqMessage(role = "user", content = userPrompt))

                val bodyPayload = GroqRequest(
                    model = targetModel,
                    messages = messages,
                )

                val response = client.post("https://api.groq.com/openai/v1/chat/completions") {
                    header("Authorization", "Bearer $apiKey")
                    header("User-Agent", "AuraMusic/1.0 (Android; Mobile)")
                    contentType(ContentType.Application.Json)
                    setBody(bodyPayload)
                }

                val rawText = response.bodyAsText()
                if (response.status.value !in 200..299) {
                    throw IllegalStateException("Groq API error HTTP ${response.status.value}: $rawText")
                }

                val parsed = json.decodeFromString<GroqResponse>(rawText)
                val replyContent = parsed.choices.firstOrNull()?.message?.content
                if (!replyContent.isNullOrBlank()) {
                    return@runCatching replyContent
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        throw (lastError ?: IllegalStateException("All Groq models failed"))
    }

    suspend fun transcribeAudio(
        apiKey: String,
        wavBytes: ByteArray,
    ): Result<String> = runCatching {
        val boundary = "---------------------------${System.currentTimeMillis()}"
        val byteStream = java.io.ByteArrayOutputStream()
        val writer = java.io.PrintWriter(java.io.OutputStreamWriter(byteStream, java.nio.charset.StandardCharsets.UTF_8), true)

        writer.append("--$boundary\r\n")
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"ambient.wav\"\r\n")
        writer.append("Content-Type: audio/wav\r\n\r\n")
        writer.flush()
        byteStream.write(wavBytes)
        byteStream.flush()
        writer.append("\r\n")

        writer.append("--$boundary\r\n")
        writer.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n")
        writer.append("whisper-large-v3\r\n")

        writer.append("--$boundary\r\n")
        writer.append("Content-Disposition: form-data; name=\"response_format\"\r\n\r\n")
        writer.append("json\r\n")

        writer.append("--$boundary--\r\n")
        writer.flush()

        val payload = byteStream.toByteArray()

        val response = client.post("https://api.groq.com/openai/v1/audio/transcriptions") {
            header("Authorization", "Bearer $apiKey")
            header("User-Agent", "AuraMusic/1.0 (Android; Mobile)")
            header("Content-Type", "multipart/form-data; boundary=$boundary")
            setBody(payload)
        }

        val rawText = response.bodyAsText()
        if (response.status.value !in 200..299) {
            throw IllegalStateException("Groq Whisper API error HTTP ${response.status.value}: $rawText")
        }

        val parsed = json.decodeFromString<GroqWhisperResponse>(rawText)
        parsed.text?.trim().orEmpty()
    }
}
