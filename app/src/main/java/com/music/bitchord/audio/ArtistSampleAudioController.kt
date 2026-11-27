package com.music.bitchord.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.music.bitchord.data.taste.MusicTasteRepository
import com.music.bitchord.data.taste.TasteArtistPick
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Lightweight disposable sample player for 1-tap creator previews during Onboarding
 * and Settings Taste editing, kept completely separate from main queue state.
 */
class ArtistSampleAudioController(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var fetchJob: Job? = null
    private var autoStopJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    var playingKey by mutableStateOf<String?>(null)
        private set

    var loadingKey by mutableStateOf<String?>(null)
        private set

    fun togglePreview(
        artist: TasteArtistPick,
        scope: kotlinx.coroutines.CoroutineScope,
    ) {
        val key = "${artist.browseId}:${artist.name}"
        if (playingKey == key || loadingKey == key) {
            stop()
            return
        }
        stop()
        loadingKey = key
        fetchJob = scope.launch {
            val preview = MusicTasteRepository.loadArtistPreviewTrack(artist).getOrNull()
            if (loadingKey != key) return@launch
            if (preview == null) {
                loadingKey = null
                return@launch
            }
            startMediaPlayer(key, preview.streamUrl, preview.headers, scope)
        }
    }

    private fun startMediaPlayer(
        key: String,
        url: String,
        headers: Map<String, String>,
        scope: kotlinx.coroutines.CoroutineScope,
    ) {
        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(context, Uri.parse(url), headers)
                setOnPreparedListener { ready ->
                    if (loadingKey != key) {
                        ready.release()
                        return@setOnPreparedListener
                    }
                    loadingKey = null
                    playingKey = key
                    ready.start()
                    autoStopJob = scope.launch {
                        delay(15_000L)
                        if (playingKey == key) stop()
                    }
                }
                setOnCompletionListener {
                    mainHandler.post { if (playingKey == key) stop() }
                }
                setOnErrorListener { _, _, _ ->
                    mainHandler.post {
                        if (loadingKey == key || playingKey == key) stop()
                    }
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        }.onFailure {
            loadingKey = null
            playingKey = null
        }
    }

    fun stop() {
        fetchJob?.cancel()
        fetchJob = null
        autoStopJob?.cancel()
        autoStopJob = null
        loadingKey = null
        playingKey = null
        mediaPlayer?.let { mp ->
            runCatching {
                if (mp.isPlaying) mp.stop()
            }
            runCatching { mp.release() }
        }
        mediaPlayer = null
    }
}

@Composable
fun rememberArtistSampleController(): ArtistSampleAudioController {
    val context = LocalContext.current.applicationContext
    val controller = remember(context) { ArtistSampleAudioController(context) }
    DisposableEffect(controller) {
        onDispose { controller.stop() }
    }
    return controller
}
