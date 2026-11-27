package com.music.bitchord.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast

/**
 * Receives the result of a [PackageInstaller] session commit.
 *
 * The session-based PackageInstaller API is the modern, reliable way to
 * install sideloaded APKs: it surfaces a system-managed confirmation dialog
 * on every phone and ROM, whereas the older `ACTION_VIEW` intent is silently
 * swallowed by many OEMs (Xiaomi, Samsung One UI, Huawei EMUI, etc.).
 *
 * On success, Android kills the running process and relaunches the new
 * version, so the [STATUS_SUCCESS] branch is only reached on rare devices
 * that keep the old process alive.
 *
 * On user confirmation needed ([STATUS_PENDING_USER_ACTION]), Android
 * packages the system installer activity in [EXTRA_INTENT]; we simply
 * start it.
 */
class AppInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {

            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // The system needs the user to confirm the install.
                val confirmIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirmIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                // Install succeeded — the app will restart momentarily.
                AppUpdateChecker.onInstallSuccess()
            }

            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_ABORTED,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                val userMessage = when (status) {
                    PackageInstaller.STATUS_FAILURE_ABORTED ->
                        "Update cancelled"
                    PackageInstaller.STATUS_FAILURE_BLOCKED ->
                        "Install blocked — please allow AuraMusic to install updates in Settings"
                    PackageInstaller.STATUS_FAILURE_CONFLICT ->
                        "Signing key mismatch — please uninstall the old version first"
                    PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                        "This update is not compatible with your device"
                    PackageInstaller.STATUS_FAILURE_STORAGE ->
                        "Not enough storage space to install the update"
                    PackageInstaller.STATUS_FAILURE_INVALID ->
                        "The downloaded update file is corrupted"
                    else ->
                        message ?: "Install failed (error $status)"
                }
                AppUpdateChecker.onInstallFailed(userMessage)
                if (status != PackageInstaller.STATUS_FAILURE_ABORTED) {
                    Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.music.bitchord.action.INSTALL_STATUS"
    }
}
