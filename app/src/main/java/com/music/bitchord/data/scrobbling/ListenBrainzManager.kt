package com.music.bitchord.data.scrobbling

import com.music.bitchord.data.DebugLog as Log
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.music.bitchord.data.Http

object ListenBrainzManager {
    private const val TAG = "ListenBrainzManager"
    private const val API_URL = "https://api.listenbrainz.org/1/submit-listens"

    suspend fun submitPlayingNow(
        token: String,
        song: Song?,
        positionMs: Long,
        durationMsOverride: Long? = null,
        primaryArtistOnly: Boolean = false,
    ): Boolean {
        if (token.isBlank() || song == null) return false
        return withContext(Dispatchers.IO) {
            try {
                val durationMs = durationMsOverride ?: parseDurationMs(song.durationText)
                val releaseName = song.albumName.orEmpty()
                val artist = if (primaryArtistOnly) song.artist.primaryArtist() else song.artist
                val bodyJson = buildJsonObject {
                    put("listen_type", "playing_now")
                    putJsonArray("payload") {
                        addJsonObject {
                            putJsonObject("track_metadata") {
                                put("artist_name", artist)
                                put("track_name", song.title)
                                if (releaseName.isNotBlank()) put("release_name", releaseName)
                                putJsonObject("additional_info") {
                                    // The API rejects a zero/negative duration_ms, and it is
                                    // optional — so only send it when it is actually known.
                                    if (durationMs > 0) put("duration_ms", durationMs)
                                    put("position_ms", positionMs)
                                    put("submission_client", "BitChord")
                                }
                            }
                        }
                    }
                }.toString()
                Log.d(TAG, "submitPlayingNow: $bodyJson")
                val body = bodyJson.toRequestBody("application/json".toMediaType())
                val request =
                    Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "Token $token")
                        .build()

                Http.client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        Log.d(TAG, "playing_now submitted for ${song.title}")
                        true
                    } else {
                        val bodyText = try { resp.body.string() } catch (_: Exception) { "" }
                        Log.w(TAG, "playing_now submit failed: ${resp.code} - $bodyText")
                        false
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "submitPlayingNow failed", ex)
                false
            }
        }
    }

    suspend fun submitFinished(
        token: String,
        song: Song?,
        startMs: Long,
        endMs: Long,
        durationMsOverride: Long? = null,
        primaryArtistOnly: Boolean = false,
    ): Boolean {
        if (token.isBlank() || song == null) return false
        return withContext(Dispatchers.IO) {
            try {
                val durationMs = durationMsOverride ?: parseDurationMs(song.durationText)
                val releaseName = song.albumName.orEmpty()
                var listenedAtStart = startMs / 1000L
                val MIN_LISTEN_TS = 1033430400L
                if (listenedAtStart < MIN_LISTEN_TS) {
                    listenedAtStart = System.currentTimeMillis() / 1000L
                }
                val artist = if (primaryArtistOnly) song.artist.primaryArtist() else song.artist
                val bodyJson = buildJsonObject {
                    put("listen_type", "single")
                    putJsonArray("payload") {
                        addJsonObject {
                            put("listened_at", listenedAtStart)
                            putJsonObject("track_metadata") {
                                put("artist_name", artist)
                                put("track_name", song.title)
                                if (releaseName.isNotBlank()) put("release_name", releaseName)
                                putJsonObject("additional_info") {
                                    if (durationMs > 0) put("duration_ms", durationMs)
                                    put("start_ms", startMs)
                                    put("end_ms", endMs)
                                    put("submission_client", "BitChord")
                                }
                            }
                        }
                    }
                }.toString()
                Log.d(TAG, "submitFinished: $bodyJson")
                val body = bodyJson.toRequestBody("application/json".toMediaType())
                val request =
                    Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "Token $token")
                        .build()

                Http.client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        Log.d(TAG, "finished listen submitted for ${song.title}")
                        true
                    } else {
                        val bodyText = try { resp.body.string() } catch (_: Exception) { "" }
                        Log.w(TAG, "finished listen submit failed: ${resp.code} - $bodyText")
                        false
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "submitFinished failed", ex)
                false
            }
        }
    }

    private fun parseDurationMs(text: String?): Long {
        if (text == null) return 0L
        val parts = text.split(":")
        if (parts.size != 2) return 0L
        val minutes = parts[0].toLongOrNull() ?: return 0L
        val seconds = parts[1].toLongOrNull() ?: return 0L
        return (minutes * 60 + seconds) * 1000
    }
}
