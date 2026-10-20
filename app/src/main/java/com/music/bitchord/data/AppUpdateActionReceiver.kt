package com.music.bitchord.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Handles actions from ongoing update notifications (e.g. Cancel download).
 */
class AppUpdateActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CANCEL_DOWNLOAD) {
            AppUpdateChecker.cancelDownload()
            AppUpdateNotificationHelper.dismiss(context)
        }
    }

    companion object {
        const val ACTION_CANCEL_DOWNLOAD = "com.music.bitchord.action.CANCEL_UPDATE_DOWNLOAD"
    }
}
