package com.music.bitchord.ui.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.R
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.replay.ShareAction
import com.music.bitchord.ui.replay.cacheForSharing
import com.music.bitchord.ui.replay.saveToGallery
import com.music.bitchord.ui.replay.sendIntent
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

/**
 * The picked lines, drawn, with Instagram Story, WhatsApp, and gallery sharing options.
 *
 * Built on [PlayerDrawer] rather than on a `ModalBottomSheet` for the same reason
 * as every other sheet in this package: it is inside the player, which is itself
 * already a modal sheet, and nesting the two leaves the inner one with nothing
 * to be modal over. It also means this drawer gets the player's own frosted
 * material, the drag-to-dismiss gesture and the scrim fade for free.
 *
 * The render happens on a background dispatcher and the preview is a plain
 * `Image` of the result — no second layout pass, no offscreen Compose, and the
 * identical pixels that the Save and Share buttons then write to disk.
 */
@Composable
internal fun AndroidLyricsShareSheet(
    hazeState: HazeState,
    request: LyricsShareRequest,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    var image by remember(request) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(request) { mutableStateOf(false) }
    var saved by remember(request) { mutableStateOf(false) }
    // Defaults to 9:16 frame for stories; can be toggled to compact card
    var story by remember(request) { mutableStateOf(true) }
    var theme by remember(request) { mutableStateOf(ShareCardTheme.DYNAMIC) }

    LaunchedEffect(request, story, theme) {
        image = null
        failed = false
        saved = false
        image = runCatching { renderLyricsShareCard(context, request, story, theme) }
            .onFailure { failed = true }
            .getOrNull()
    }

    PlayerDrawer(
        hazeState = hazeState,
        title = stringResource(R.string.lyrics_share_title),
        onDismiss = onDismiss,
        modifier = modifier,
        titleGap = 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val bitmap = image
            val ratio = bitmap?.let { it.width.toFloat() / it.height } ?: (9f / 16f)
            val preview = Modifier
                .fillMaxWidth((0.68f * ratio).coerceIn(0.34f, 0.88f))
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(16.dp))

            if (bitmap == null) {
                Box(
                    modifier = preview
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            } else {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.your_lyrics_card),
                    modifier = preview,
                )
            }

            // Theme and Frame format selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemePill(
                        label = stringResource(R.string.theme_dynamic),
                        selected = theme == ShareCardTheme.DYNAMIC,
                        onClick = {
                            haptics.play(Haptic.Tap)
                            theme = ShareCardTheme.DYNAMIC
                        },
                    )
                    ThemePill(
                        label = stringResource(R.string.theme_oled),
                        selected = theme == ShareCardTheme.DARK_OLED,
                        onClick = {
                            haptics.play(Haptic.Tap)
                            theme = ShareCardTheme.DARK_OLED
                        },
                    )
                    ThemePill(
                        label = stringResource(R.string.theme_vibrant),
                        selected = theme == ShareCardTheme.VIBRANT,
                        onClick = {
                            haptics.play(Haptic.Tap)
                            theme = ShareCardTheme.VIBRANT
                        },
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (story) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f))
                        .clickable {
                            haptics.play(Haptic.Tap)
                            story = !story
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (story) "9:16" else "Card",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = if (story) MaterialTheme.colorScheme.onPrimary else Color.White,
                    )
                }
            }

            if (failed) {
                Text(
                    text = stringResource(R.string.couldnt_draw_picture),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            // Direct Social Action Row: Instagram Story & WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BrandedShareButton(
                    label = stringResource(R.string.share_instagram_story),
                    icon = InstagramIcon,
                    background = Brush.horizontalGradient(
                        listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045)),
                    ),
                    textColor = Color.White,
                    enabled = image != null,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val drawn = image ?: return@BrandedShareButton
                        haptics.play(Haptic.Tap)
                        scope.launch {
                            val uri = cacheForSharing(context, drawn, LYRICS_FILE_NAME) ?: return@launch
                            val launched = shareToInstagramStory(context, uri)
                            if (!launched) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.app_not_installed, "Instagram"),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                context.startActivity(
                                    Intent.createChooser(
                                        sendIntent(uri),
                                        context.getString(R.string.share_your_lyrics),
                                    ),
                                )
                            }
                        }
                    },
                )

                BrandedShareButton(
                    label = stringResource(R.string.share_whatsapp),
                    icon = WhatsAppIcon,
                    background = Brush.horizontalGradient(
                        listOf(Color(0xFF25D366), Color(0xFF128C7E)),
                    ),
                    textColor = Color.White,
                    enabled = image != null,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val drawn = image ?: return@BrandedShareButton
                        haptics.play(Haptic.Tap)
                        scope.launch {
                            val uri = cacheForSharing(context, drawn, LYRICS_FILE_NAME) ?: return@launch
                            val launched = shareToWhatsApp(context, uri)
                            if (!launched) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.app_not_installed, "WhatsApp"),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                context.startActivity(
                                    Intent.createChooser(
                                        sendIntent(uri),
                                        context.getString(R.string.share_your_lyrics),
                                    ),
                                )
                            }
                        }
                    },
                )
            }

            // Secondary Action Row: Save to Gallery & More Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ShareAction(
                    label = stringResource(R.string.save),
                    icon = Icons.Rounded.Download,
                    accent = false,
                    enabled = image != null,
                    modifier = Modifier.weight(1f),
                    saved = saved,
                    savedLabel = stringResource(R.string.saved_bang),
                    onClick = {
                        val drawn = image
                        if (drawn != null) {
                            haptics.play(Haptic.Tap)
                            scope.launch {
                                saved = saveToGallery(
                                    context,
                                    drawn,
                                    request.song.title,
                                    LYRICS_FILE_PREFIX,
                                )
                            }
                        }
                    },
                )
                ShareAction(
                    label = stringResource(R.string.share_more),
                    icon = Icons.Rounded.IosShare,
                    accent = true,
                    enabled = image != null,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val drawn = image
                        if (drawn != null) {
                            haptics.play(Haptic.Tap)
                            scope.launch {
                                val uri = cacheForSharing(context, drawn, LYRICS_FILE_NAME)
                                if (uri == null) {
                                    failed = true
                                    saved = false
                                } else {
                                    context.startActivity(
                                        Intent.createChooser(
                                            sendIntent(uri),
                                            context.getString(R.string.share_your_lyrics),
                                        ),
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f)
    val text = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.8f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = text,
        )
    }
}

@Composable
private fun BrandedShareButton(
    label: String,
    icon: ImageVector,
    background: Brush,
    textColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun shareToInstagramStory(context: Context, uri: Uri): Boolean {
    val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
        setDataAndType(uri, "image/png")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra("interactive_asset_uri", uri)
        putExtra("source_application", context.packageName)
    }
    context.grantUriPermission("com.instagram.android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.grantUriPermission("com.instagram.android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(sendIntent)
            true
        } catch (_: Exception) {
            false
        }
    }
}

private fun shareToWhatsApp(context: Context, uri: Uri): Boolean {
    val pkgs = listOf("com.whatsapp", "com.whatsapp.w4b")
    for (pkg in pkgs) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage(pkg)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
            return true
        } catch (_: Exception) {
            // Try next package
        }
    }
    return false
}

private val InstagramIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "instagram",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(7f, 2f)
            lineTo(17f, 2f)
            quadTo(22f, 2f, 22f, 7f)
            lineTo(22f, 17f)
            quadTo(22f, 22f, 17f, 22f)
            lineTo(7f, 22f)
            quadTo(2f, 22f, 2f, 17f)
            lineTo(2f, 7f)
            quadTo(2f, 2f, 7f, 2f)
            close()
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
        ) {
            moveTo(12f, 8f)
            arcToRelative(4f, 4f, 0f, true, true, 0f, 8f)
            arcToRelative(4f, 4f, 0f, true, true, 0f, -8f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(17.5f, 6.5f)
            arcToRelative(1f, 1f, 0f, true, true, 0f, 2f)
            arcToRelative(1f, 1f, 0f, true, true, 0f, -2f)
            close()
        }
    }.build()
}

private val WhatsAppIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "whatsapp",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 3f)
            arcTo(9f, 9f, 0f, false, false, 4.2f, 16.5f)
            lineTo(3f, 21f)
            lineTo(7.7f, 19.8f)
            arcTo(9f, 9f, 0f, true, false, 12f, 3f)
            close()
        }
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(9.5f, 8.5f)
            lineTo(10.5f, 10.5f)
            lineTo(9.5f, 11.5f)
            arcTo(6.5f, 6.5f, 0f, false, false, 12.5f, 14.5f)
            lineTo(13.5f, 13.5f)
            lineTo(15.5f, 14.5f)
            lineTo(15f, 16.5f)
            arcTo(1.5f, 1.5f, 0f, false, true, 13.5f, 17.5f)
            arcTo(9f, 9f, 0f, false, true, 6.5f, 10.5f)
            arcTo(1.5f, 1.5f, 0f, false, true, 7.5f, 9f)
            close()
        }
    }.build()
}

/**
 * Two names, deliberately not the Replay's.
 *
 * The cache name so a replay share and a lyrics share cannot overwrite each
 * other while the receiving app is still reading the first, and the gallery
 * prefix so a saved card in the pictures folder says which feature produced it.
 */
private const val LYRICS_FILE_NAME = "lyrics.png"
private const val LYRICS_FILE_PREFIX = "auramusic-lyrics"
