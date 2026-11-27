package com.music.bitchord.data.telemetry

import com.music.bitchord.data.DebugLog as Log
import com.music.bitchord.data.Http
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Lightweight, privacy-first daily heartbeat client for AuraMusic.
 * Pings https://auramusic-stats.onrender.com/api/heartbeat once per 24 hours.
 * Transmits zero personal data, zero listening history, and zero location info.
 */
object AuraTelemetryClient {

    private const val TAG = "AuraTelemetry"
    private const val HEARTBEAT_ENDPOINT = "https://auramusic-stats.onrender.com/api/heartbeat"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var storageDir: File? = null
    private var cachedInstallId: String? = null
    private var lastPingDate: String? = null

    /**
     * Initialize telemetry with a writable storage directory.
     */
    fun init(directory: File) {
        storageDir = directory
        loadStoredState()
    }

    /**
     * Send an anonymous daily heartbeat ping if not already sent today.
     * Runs in a background IO coroutine with silent error suppression.
     */
    fun ping(
        version: String,
        platform: String = "Android",
        osVersion: String = System.getProperty("os.name") ?: "Unknown",
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (lastPingDate == today) {
            return
        }

        val installId = cachedInstallId ?: getOrCreateInstallId()

        scope.launch {
            try {
                val jsonPayload = buildJsonObject {
                    put("installId", installId)
                    put("version", version)
                    put("platform", platform)
                    put("osVersion", osVersion)
                }.toString()

                val request = Request.Builder()
                    .url(HEARTBEAT_ENDPOINT)
                    .header("User-Agent", "AuraMusic/$version ($platform)")
                    .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                Http.client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        lastPingDate = today
                        saveStoredState(today)
                    } else {
                        Log.w(TAG, "Heartbeat failed with HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Heartbeat skipped: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    private fun loadStoredState() {
        val dir = storageDir ?: return
        try {
            val idFile = File(dir, "aura_install_id.txt")
            if (idFile.exists()) {
                cachedInstallId = idFile.readText().trim()
            }
            val dateFile = File(dir, "aura_last_ping.txt")
            if (dateFile.exists()) {
                lastPingDate = dateFile.readText().trim()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load telemetry state: ${e.message}")
        }
    }

    fun getOrCreateInstallId(): String {
        cachedInstallId?.let { return it }
        val id = UUID.randomUUID().toString()
        cachedInstallId = id
        val dir = storageDir
        if (dir != null && dir.exists()) {
            try {
                File(dir, "aura_install_id.txt").writeText(id)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist install ID: ${e.message}")
            }
        }
        return id
    }

    fun getLastPingDate(): String? = lastPingDate

    private fun saveStoredState(today: String) {
        val dir = storageDir ?: return
        try {
            File(dir, "aura_last_ping.txt").writeText(today)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save last ping date: ${e.message}")
        }
    }
}

