package com.music.bitchord.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.music.bitchord.MainActivity
import com.music.bitchord.R
import java.io.File
import java.util.Locale

/**
 * Handles creation and updates of Android system notifications during update downloads.
 * Allows background downloads to show active progress in the system shade and instant installation.
 */
object AppUpdateNotificationHelper {

    private const val CHANNEL_ID = "app_updates"
    private const val NOTIFICATION_ID = 90210

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Software Updates",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows progress and completion of AuraMusic app updates."
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun showProgress(
        context: Context,
        version: String,
        percent: Int,
        bytesRead: Long,
        totalBytes: Long,
        bytesPerSec: Long,
    ) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        ensureChannel(context)

        val cancelIntent = Intent(context, AppUpdateActionReceiver::class.java).apply {
            action = AppUpdateActionReceiver.ACTION_CANCEL_DOWNLOAD
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val speedText = if (bytesPerSec > 0) " • ${formatSpeed(bytesPerSec)}" else ""
        val bytesText = if (totalBytes > 0) {
            "${formatBytes(bytesRead)} / ${formatBytes(totalBytes)}$speedText"
        } else {
            "${formatBytes(bytesRead)} downloaded$speedText"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle("Downloading AuraMusic v$version ($percent%)")
            .setContentText(bytesText)
            .setSubText("$percent%")
            .setProgress(100, percent.coerceIn(0, 100), totalBytes <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.cancel),
                cancelPendingIntent,
            )
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun showReady(context: Context, version: String, file: File) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        ensureChannel(context)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)
            .addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK,
            )

        val installPendingIntent = PendingIntent.getActivity(
            context,
            2,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle("AuraMusic v$version Ready to Install")
            .setContentText("Download complete. Tap to install now.")
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(installPendingIntent)
            .addAction(
                R.drawable.ic_notification_logo,
                context.getString(R.string.install_now),
                installPendingIntent,
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun dismiss(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return ""
        val kb = bytesPerSec / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB/s", mb)
            else -> String.format(Locale.US, "%.0f KB/s", kb)
        }
    }
}
