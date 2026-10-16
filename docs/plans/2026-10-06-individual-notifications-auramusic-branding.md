# Individual Release Notifications & AuraMusic Branding Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Deliver individual notifications for each new music release with full AuraMusic branding (AuraMusic logo, signature cyan color, app name header, and individual song artwork/deep link) instead of bundling multiple tracks into a single inbox card.

**Architecture:** Refactor `NewReleaseNotificationHelper.kt` to iterate across detected releases and post individual rich notifications keyed by each release's unique ID. Replace the status-bar silhouette in `ic_notification_logo.xml` with the authentic AuraMusic harmonic wave mark and configure explicit color tinting (`#00E5FF`). Recompile and install the APK to the connected device to clear cached resources.

**Tech Stack:** Kotlin, Android NotificationCompat (Channels, BigPictureStyle, SubText), Vector Drawable, Gradle, Android SDK.

---

### Task 1: Update `ic_notification_logo.xml` with Authentic AuraMusic Wave Silhouette

**Files:**
- Modify: `app/src/main/res/drawable/ic_notification_logo.xml`

**Details:**
Ensure the vector matches the exact 3-wave harmonic crest-trough shape of AuraMusic, scaled crisply for 24x24dp notification status bar rendering.

---

### Task 2: Refactor `NewReleaseNotificationHelper.kt` to Post Individual Notifications

**Files:**
- Modify: `app/src/main/java/com/music/bitchord/data/newrelease/NewReleaseNotificationHelper.kt`

**Details:**
1. Remove the `releases.size > 1` InboxStyle multi-song bundling logic.
2. In `notifyNewReleases`, loop through `releases.take(5)` and call `buildSingleNotification` for each release.
3. Post each notification to `manager.notify(release.id.hashCode(), notification)`.
4. In `buildSingleNotification`:
   - Set `.setSubText(context.getString(R.string.app_name))` ("AuraMusic").
   - Set `.setColor(0xFF00E5FF.toInt())` (Aura cyan brand color).
   - Set `.setContentTitle(release.artistName)`.
   - Set `.setContentText(if (release.releaseType != null) "${release.releaseType}: ${release.title}" else release.title)`.
   - Attach album artwork bitmap as large icon and BigPictureStyle.
   - Deep link to `release.browseId` or `release.videoId`.

---

### Task 3: Compile and Assemble Debug APK

**Commands:**
- `.\gradlew.bat assembleDevDebug`
- Ensure 0 errors and newly generated resources are packaged into `app-dev-debug.apk`.

---

### Task 4: Install APK to Connected Device & Verify

**Commands:**
- `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "app\build\outputs\apk\dev\debug\app-dev-debug.apk"`
- Trigger test notification or launch app to confirm individual notifications with the new AuraMusic logo and branding.
