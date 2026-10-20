package com.music.bitchord.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.music.bitchord.BuildConfig
import com.music.bitchord.R
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.utils.containSheetGestures
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import java.util.Locale

/** Backward-compatible legacy metrics for other dialogs in this package. */
internal val ALERT_WIDTH = 270.dp
internal val ALERT_CORNER = 14.dp
internal val ACTION_HEIGHT = 44.dp

/** The dim behind the modal overlay. */
internal val SCRIM_COLOR = Color.Black.copy(alpha = 0.45f)

private val MODAL_CORNER = 24.dp
private val NOTES_MAX_HEIGHT = 200.dp

/**
 * Modern glassmorphic in-app update popup for AuraMusic.
 *
 * Replaces the cramped iOS-style alert with a high-fidelity, spacious modal:
 * - Glowing hero header with version progression pill (vCurrent -> vLatest).
 * - Styled card container for release notes with clean Markdown rendering.
 * - Live animated download progress bar with real-time percentage and byte counters.
 * - Modern tactile pill action buttons.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun UpdateAvailableDialog(
    version: String,
    notes: String?,
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onSkipVersion: (() -> Unit)? = null,
    isMandatory: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val state by AppUpdateChecker.download.collectAsStateWithLifecycle()
    val cardShape = RoundedCornerShape(MODAL_CORNER)

    Box(
        modifier = modifier
            .fillMaxSize()
            .containSheetGestures()
            .background(SCRIM_COLOR)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { if (!isMandatory) onDismiss() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 320.dp, max = 360.dp)
                .fillMaxWidth(0.92f)
                .clip(cardShape)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.06f),
                        ),
                    ),
                    shape = cardShape,
                )
                .then(
                    if (reduceDynamicBlur) {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    } else {
                        Modifier
                            .optimizedHazeEffect(
                                state = hazeState,
                                style = HazeMaterials.regular(MaterialTheme.colorScheme.surface),
                            )
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                    },
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                )
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 1. Hero Badge Icon
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isMandatory) {
                                listOf(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
                                )
                            } else {
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                                )
                            },
                        ),
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isMandatory) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        },
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when {
                        isMandatory -> Icons.Rounded.Warning
                        state is AppUpdateChecker.DownloadState.Ready -> Icons.Rounded.CheckCircle
                        else -> Icons.Rounded.RocketLaunch
                    },
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (isMandatory) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(14.dp))

            // 2. Title
            Text(
                text = if (isMandatory) "Critical Update Required" else stringResource(R.string.software_update),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = if (isMandatory) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            // 3. Version Progression Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isMandatory) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        },
                    )
                    .border(
                        width = 0.5.dp,
                        color = if (isMandatory) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        },
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (isMandatory) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "v$version",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = if (isMandatory) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isMandatory) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = if (isMandatory) "REQUIRED" else "LATEST",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = if (isMandatory) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // 4. "What's New" Changelog Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.whats_new).uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp, max = NOTES_MAX_HEIGHT)
                        .verticalScroll(rememberScrollState()),
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                        if (!notes.isNullOrBlank()) {
                            RichText(style = RichTextStyle.Default) {
                                Markdown(notes)
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "• Performance and responsiveness enhancements",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                )
                                Text(
                                    text = "• Audio engine improvements & stream fixes",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                )
                                Text(
                                    text = "• General bug fixes and stability updates",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                }
            }

            // 5. Download Progress / Ready / Failed State Indicators
            val downloading = state as? AppUpdateChecker.DownloadState.Downloading
            if (downloading != null) {
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    val animatedFraction by animateFloatAsState(
                        targetValue = downloading.fraction.coerceIn(0f, 1f),
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "progressFraction",
                    )
                    val percent = (downloading.fraction * 100).toInt().coerceIn(0, 100)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Downloading update…",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    ) {
                        if (animatedFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedFraction)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary,
                                            ),
                                        ),
                                    ),
                            )
                        }
                    }

                    if (downloading.totalBytes > 0L || downloading.bytesRead > 0L) {
                        Spacer(Modifier.height(6.dp))
                        val speedText = if (downloading.bytesPerSecond > 0L) {
                            val etaSec = if (downloading.totalBytes > downloading.bytesRead) {
                                (downloading.totalBytes - downloading.bytesRead) / downloading.bytesPerSecond
                            } else 0L
                            val etaStr = if (etaSec > 0L) " • ~${etaSec}s left" else ""
                            " • ${formatSpeed(downloading.bytesPerSecond)}$etaStr"
                        } else ""

                        val bytesText = if (downloading.totalBytes > 0L) {
                            "${formatBytes(downloading.bytesRead)} / ${formatBytes(downloading.totalBytes)}$speedText"
                        } else {
                            "${formatBytes(downloading.bytesRead)} downloaded$speedText"
                        }
                        Text(
                            text = bytesText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            } else if (state is AppUpdateChecker.DownloadState.Ready) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                        .border(0.5.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF10B981),
                    )
                    Text(
                        text = "Package ready to install",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = Color(0xFF10B981),
                    )
                }
            } else if (state is AppUpdateChecker.DownloadState.Failed) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .border(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = (state as AppUpdateChecker.DownloadState.Failed).message,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // 6. Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (state) {
                    is AppUpdateChecker.DownloadState.Downloading -> {
                        if (!isMandatory) {
                            OutlinedButton(
                                onClick = onCancelDownload,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = CircleShape,
                            ) {
                                Text(
                                    text = stringResource(R.string.cancel),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                )
                            }
                        }
                    }
                    is AppUpdateChecker.DownloadState.Ready -> {
                        Button(
                            onClick = onInstall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.install_now),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                ),
                            )
                        }
                        if (!isMandatory) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.later),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                    is AppUpdateChecker.DownloadState.Failed -> {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.try_again),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                ),
                            )
                        }
                        TextButton(
                            onClick = onOpenReleasePage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.open_releases_page),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                    }
                    else -> {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.download_now),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                ),
                            )
                        }
                        if (!isMandatory) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.remind_me_later),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }

                if (state !is AppUpdateChecker.DownloadState.Downloading && state !is AppUpdateChecker.DownloadState.Failed) {
                    TextButton(
                        onClick = onOpenReleasePage,
                        modifier = Modifier.height(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.open_releases_page),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }

                    if (!isMandatory && onSkipVersion != null) {
                        TextButton(
                            onClick = onSkipVersion,
                            modifier = Modifier.height(28.dp),
                        ) {
                            Text(
                                text = "Skip this version",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Formats download transfer rate into human-readable speed strings (e.g. 3.2 MB/s). */
private fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return ""
    val kb = bytesPerSec / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB/s", mb)
        else -> String.format(Locale.US, "%.0f KB/s", kb)
    }
}

/** Formats byte counts into human-readable strings (e.g. 18.4 MB). */
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

/** Legacy alert row kept for backward compatibility with dialogs in the same package. */
@Composable
internal fun AlertAction(
    label: String,
    emphasised: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ACTION_HEIGHT)
            .background(
                if (pressed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f) else Color.Transparent,
            )
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                fontWeight = if (emphasised) FontWeight.W600 else FontWeight.W400,
            ),
            color = (if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                .copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

/** Legacy hairline separator kept for backward compatibility. */
@Composable
internal fun AlertRule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
    )
}
