package com.music.bitchord.data.newrelease

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.bitchord.MainActivity
import com.music.bitchord.R
import com.music.bitchord.data.model.NOTIFICATION_ART_PX
import com.music.bitchord.data.model.artworkAt

/**
 * Handles creation and display of system notifications for newly detected music releases.
 */
object NewReleaseNotificationHelper {

    const val CHANNEL_ID = "new_releases"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.new_releases_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.new_releases_channel_desc)
            enableLights(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    suspend fun notifyNewReleases(context: Context, releases: List<NewRelease>) {
        if (releases.isEmpty()) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // Push each release as its own individual notification so user gets separate alerts per song
        releases.take(5).forEach { release ->
            val notification = buildSingleNotification(context, release)
            runCatching {
                manager.notify(release.id.hashCode(), notification)
            }
        }
    }

    private suspend fun buildSingleNotification(
        context: Context,
        release: NewRelease,
    ): android.app.Notification {
        val bitmap = release.thumbnailUrl?.let { loadBitmap(context, it) }

        val deepLinkUri = when {
            release.browseId != null -> Uri.parse("https://music.youtube.com/browse/${release.browseId}")
            release.videoId != null -> Uri.parse("https://music.youtube.com/watch?v=${release.videoId}")
            else -> null
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            if (deepLinkUri != null) {
                action = Intent.ACTION_VIEW
                data = deepLinkUri
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            release.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val appName = context.getString(R.string.app_name)
        val title = context.getString(R.string.new_release_notification_title, release.artistName)
        val text = if (!release.releaseType.isNullOrBlank()) {
            "${release.releaseType}: ${release.title}"
        } else {
            release.title
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setColor(0xFF00E5FF.toInt()) // AuraMusic signature cyan
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(appName)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .setBigContentTitle(title)
                    .setSummaryText(text),
            )
        }

        return builder.build()
    }

    private suspend fun loadBitmap(context: Context, url: String): Bitmap? {
        val px = NOTIFICATION_ART_PX
        val targetUrl = url.artworkAt(px) ?: url
        val request = ImageRequest.Builder(context)
            .data(targetUrl)
            .size(px)
            .allowHardware(false)
            .build()
        val result = runCatching {
            SingletonImageLoader.get(context).execute(request)
        }.getOrNull()
        return (result as? SuccessResult)?.image?.toBitmap()
    }
}
