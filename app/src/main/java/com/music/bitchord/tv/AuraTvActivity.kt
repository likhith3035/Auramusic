package com.music.bitchord.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * AuraMusic Android TV Native Leanback Host Activity.
 * Provides full hardware acceleration, D-pad remote mapping, audio focus management,
 * and local offline asset streaming for Android TV & Google TV.
 */
class AuraTvActivity : Activity(), AudioManager.OnAudioFocusChangeListener {

    private lateinit var webView: WebView
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    class AndroidHostBridge(private val activity: Activity) {
        @JavascriptInterface
        fun exitApp() {
            activity.runOnUiThread {
                activity.finish()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep TV screen awake during audio playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Immersive full-screen mode for 10-foot living room experience
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        requestTvAudioFocus()

        webView = WebView(this).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            addJavascriptInterface(AndroidHostBridge(this@AuraTvActivity), "AndroidHost")
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
        }

        setContentView(webView)
        webView.loadUrl("file:///android_asset/tvApp/index.html")
        webView.requestFocus()
    }

    private fun requestTvAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttrs)
                .setOnAudioFocusChangeListener(this)
                .build()
            focusRequest?.let { audioManager?.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                webView.evaluateJavascript("if (window.AuraPlayer) window.AuraPlayer.pause();", null)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Audio focus restored
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                webView.evaluateJavascript(
                    "if (window.AuraApp && typeof window.AuraApp.handleBack === 'function') { window.AuraApp.handleBack(); } else { false; }",
                ) { result ->
                    if (result == "false" || result == null) {
                        finish()
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                webView.evaluateJavascript("if (window.AuraPlayer) window.AuraPlayer.togglePlay();", null)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                webView.evaluateJavascript("if (window.AuraPlayer) window.AuraPlayer.play();", null)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_STOP -> {
                webView.evaluateJavascript("if (window.AuraPlayer) window.AuraPlayer.pause();", null)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                webView.evaluateJavascript("if (window.AuraPlayer) window.AuraPlayer.nextTrack();", null)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                webView.evaluateJavascript("if (window.AuraPlayer) window.AuraPlayer.prevTrack();", null)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                webView.evaluateJavascript("if (window.AuraPlayer) window.AuraPlayer.seekTo(window.AuraPlayer.getCurrentTime() + 10);", null)
                return true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                webView.evaluateJavascript("if (window.AuraPlayer) window.AuraPlayer.seekTo(Math.max(0, window.AuraPlayer.getCurrentTime() - 10));", null)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(this)
        }
        webView.destroy()
    }
}
