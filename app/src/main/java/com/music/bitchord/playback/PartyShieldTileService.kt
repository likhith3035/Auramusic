package com.music.bitchord.playback

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.music.bitchord.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Android Quick Settings Tile for toggling Party Shield directly from
 * the system notification shade / quick settings panel without opening the app.
 */
@RequiresApi(Build.VERSION_CODES.N)
class PartyShieldTileService : TileService() {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + Job())
    private var observeJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(AppSettings.ignoreAudioFocus.value)
        observeJob?.cancel()
        observeJob = scope.launch {
            AppSettings.ignoreAudioFocus.collect { enabled ->
                updateTileState(enabled)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        observeJob?.cancel()
        observeJob = null
    }

    override fun onClick() {
        super.onClick()
        val newState = !AppSettings.ignoreAudioFocus.value
        AppSettings.setIgnoreAudioFocus(newState)
        updateTileState(newState)
    }

    private fun updateTileState(enabled: Boolean) {
        val currentTile = qsTile ?: return
        currentTile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            currentTile.subtitle = if (enabled) "Active" else "Off"
        }
        currentTile.updateTile()
    }
}
