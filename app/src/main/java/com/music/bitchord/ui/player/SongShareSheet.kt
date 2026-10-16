package com.music.bitchord.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.replay.cacheForSharing
import com.music.bitchord.ui.replay.saveToGallery
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

private const val SONG_SHARE_FILE = "auramusic_share.png"

/**
 * Spotify-style interactive popup for sharing songs and lyrics stories across social apps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongShareSheet(
    song: Song,
    hazeState: HazeState? = null,
    lyricsLines: List<String> = emptyList(),
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var mode by remember(song) { mutableStateOf(SongShareMode.SONG) }
    var palette by remember(song) { mutableStateOf(SongSharePalette.DYNAMIC) }
    var bitmap by remember(song, mode, palette) { mutableStateOf<Bitmap?>(null) }
    var rendering by remember(song, mode, palette) { mutableStateOf(true) }
    var cachedUri by remember(song, mode, palette) { mutableStateOf<Uri?>(null) }
    var isSaved by remember(song, mode, palette) { mutableStateOf(false) }

    LaunchedEffect(song, mode, palette, lyricsLines) {
        rendering = true
        val bm = runCatching {
            renderSongShareCard(
                context = context,
                song = song,
                mode = mode,
                palette = palette,
                lyricsLines = lyricsLines,
            )
        }.getOrNull()
        bitmap = bm
        if (bm != null) {
            cachedUri = cacheForSharing(context, bm, SONG_SHARE_FILE)
        }
        rendering = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF14171E),
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Row: Title & Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.share_song_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color.White,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // ── 1. Live Card Preview (Compact 210dp height to ensure ZERO scroll on any mobile screen) ──
            val previewModifier = Modifier
                .height(210.dp)
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(14.dp))

            if (rendering || bitmap == null) {
                Box(
                    modifier = previewModifier
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF00E5FF),
                    )
                }
            } else {
                bitmap?.let { bm ->
                    Image(
                        bitmap = bm.asImageBitmap(),
                        contentDescription = song.title,
                        modifier = previewModifier,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 2. [ Song | Lyrics ] Segmented Toggle Pill ────────────────────────
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF22252C))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SegmentedPill(
                    label = stringResource(R.string.share_mode_song),
                    selected = mode == SongShareMode.SONG,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        mode = SongShareMode.SONG
                    },
                )
                SegmentedPill(
                    label = stringResource(R.string.share_mode_lyrics),
                    selected = mode == SongShareMode.LYRICS,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        if (lyricsLines.isEmpty()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.share_lyrics_unavailable),
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            mode = SongShareMode.LYRICS
                        }
                    },
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── 3. Color Palette Swatches (Compact 28dp) ────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PaletteCircle(
                    brush = SolidColor(Color(0xFFE53935)),
                    selected = palette == SongSharePalette.DYNAMIC,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        palette = SongSharePalette.DYNAMIC
                    },
                )
                PaletteCircle(
                    brush = Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFF7C4DFF))),
                    selected = palette == SongSharePalette.VIBRANT,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        palette = SongSharePalette.VIBRANT
                    },
                )
                PaletteCircle(
                    brush = SolidColor(Color(0xFF14171E)),
                    selected = palette == SongSharePalette.OBSIDIAN,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        palette = SongSharePalette.OBSIDIAN
                    },
                )
                PaletteCircle(
                    brush = Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFA855F7))),
                    selected = palette == SongSharePalette.AURA_CYAN,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        palette = SongSharePalette.AURA_CYAN
                    },
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── 4. Social Action Buttons (Horizontal Row with 46dp action icons) ──
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Copy Link
                SocialShareAction(
                    icon = Icons.Rounded.Link,
                    label = stringResource(R.string.share_copy_link),
                    background = SolidColor(Color(0xFF2C2F36)),
                    iconTint = Color.White,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        copySongLink(context, song)
                    },
                )

                // WhatsApp
                SocialShareAction(
                    icon = WhatsAppIcon,
                    label = stringResource(R.string.share_whatsapp),
                    background = SolidColor(Color(0xFF25D366)),
                    iconTint = Color.White,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        scope.launch {
                            val uri = cachedUri ?: bitmap?.let { cacheForSharing(context, it, SONG_SHARE_FILE) }
                            if (uri != null) {
                                shareToWhatsApp(context, uri, song)
                            }
                        }
                    },
                )

                // Snapchat
                SocialShareAction(
                    icon = SnapchatIcon,
                    label = stringResource(R.string.share_snapchat),
                    background = SolidColor(Color(0xFFFFFC00)),
                    iconTint = Color.Black,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        scope.launch {
                            val uri = cachedUri ?: bitmap?.let { cacheForSharing(context, it, SONG_SHARE_FILE) }
                            if (uri != null) {
                                shareToSnapchat(context, uri)
                            }
                        }
                    },
                )

                // Instagram Stories
                SocialShareAction(
                    icon = InstagramIcon,
                    label = stringResource(R.string.share_instagram_stories),
                    background = Brush.linearGradient(
                        listOf(
                            Color(0xFFF58529),
                            Color(0xFFDD2A7B),
                            Color(0xFF8134AF),
                            Color(0xFF515BD4),
                        ),
                    ),
                    iconTint = Color.White,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        scope.launch {
                            val uri = cachedUri ?: bitmap?.let { cacheForSharing(context, it, SONG_SHARE_FILE) }
                            if (uri != null) {
                                shareToInstagramStory(context, uri, song)
                            }
                        }
                    },
                )

                // Threads
                SocialShareAction(
                    icon = ThreadsIcon,
                    label = stringResource(R.string.share_threads),
                    background = SolidColor(Color(0xFF101010)),
                    iconTint = Color.White,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        scope.launch {
                            val uri = cachedUri ?: bitmap?.let { cacheForSharing(context, it, SONG_SHARE_FILE) }
                            if (uri != null) {
                                shareToThreads(context, uri, song)
                            }
                        }
                    },
                )

                // More (System share)
                SocialShareAction(
                    icon = Icons.Rounded.Share,
                    label = stringResource(R.string.share_more),
                    background = SolidColor(Color(0xFF2C2F36)),
                    iconTint = Color.White,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        scope.launch {
                            val uri = cachedUri ?: bitmap?.let { cacheForSharing(context, it, SONG_SHARE_FILE) }
                            shareToSystem(context, uri, song)
                        }
                    },
                )

                // Save to Gallery
                SocialShareAction(
                    icon = if (isSaved) Icons.Rounded.Check else Icons.Rounded.Download,
                    label = if (isSaved) stringResource(R.string.share_saved_to_gallery) else stringResource(R.string.share_save_image),
                    background = SolidColor(Color(0xFF2C2F36)),
                    iconTint = if (isSaved) Color(0xFF00E5FF) else Color.White,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        scope.launch {
                            bitmap?.let { bm ->
                                saveToGallery(context, bm, "auramusic-${song.videoId}")
                                isSaved = true
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.share_saved_to_gallery),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    },
                )
            }
        }
    }
}

// ── Components ──────────────────────────────────────────────────────────────

@Composable
private fun SegmentedPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = if (selected) Color.Black else Color.White.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun PaletteCircle(
    brush: Brush,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderModifier = if (selected) {
        Modifier.border(2.dp, Color.White, CircleShape)
    } else {
        Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
    }

    Box(
        modifier = Modifier
            .size(28.dp)
            .then(borderModifier)
            .padding(2.5.dp)
            .clip(CircleShape)
            .background(brush)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun SocialShareAction(
    icon: ImageVector,
    label: String,
    background: Brush,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(58.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(background)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = Color.White.copy(alpha = 0.88f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Native Intent Handlers ──────────────────────────────────────────────────

private fun copySongLink(context: Context, song: Song) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val url = "https://music.youtube.com/watch?v=${song.videoId}"
    val clip = ClipData.newPlainText(song.title, url)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, context.getString(R.string.share_link_copied), Toast.LENGTH_SHORT).show()
}

private fun shareToInstagramStory(context: Context, uri: Uri, song: Song): Boolean {
    val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
        setDataAndType(uri, "image/png")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra("interactive_asset_uri", uri)
        putExtra("source_application", context.packageName)
        putExtra("content_url", "https://music.youtube.com/watch?v=${song.videoId}")
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
                putExtra(Intent.EXTRA_TEXT, "${song.title} - ${song.artist}\nhttps://music.youtube.com/watch?v=${song.videoId}")
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.grantUriPermission("com.instagram.android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(sendIntent)
            true
        } catch (_: Exception) {
            shareToSystem(context, uri, song)
            false
        }
    }
}

private fun shareToWhatsApp(context: Context, uri: Uri, song: Song): Boolean {
    val pkgs = listOf("com.whatsapp", "com.whatsapp.w4b")
    val caption = "${song.title} · ${song.artist}\nhttps://music.youtube.com/watch?v=${song.videoId}"
    for (pkg in pkgs) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                setPackage(pkg)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
            return true
        } catch (_: Exception) {
            // Check next package
        }
    }
    shareToSystem(context, uri, song)
    return false
}

private fun shareToSnapchat(context: Context, uri: Uri): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            setPackage("com.snapchat.android")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.grantUriPermission("com.snapchat.android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        shareToSystem(context, uri, null)
        false
    }
}

private fun shareToThreads(context: Context, uri: Uri, song: Song): Boolean {
    val caption = "Listening to ${song.title} by ${song.artist} on AuraMusic 🎵\nhttps://music.youtube.com/watch?v=${song.videoId}"
    return try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            setPackage("com.instagram.barcelona")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.grantUriPermission("com.instagram.barcelona", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        shareToSystem(context, uri, song)
        false
    }
}

private fun shareToSystem(context: Context, uri: Uri?, song: Song?) {
    val url = song?.let { "https://music.youtube.com/watch?v=${it.videoId}" } ?: ""
    val text = if (song != null) "${song.title} · ${song.artist}\n$url" else url

    val intent = Intent(Intent.ACTION_SEND).apply {
        if (uri != null) {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }
    context.startActivity(Intent.createChooser(intent, song?.title))
}

// ── Brand Vector Icons ──────────────────────────────────────────────────────

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

private val SnapchatIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "snapchat",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 3.5f)
            curveTo(8.5f, 3.5f, 6.5f, 6f, 6.5f, 9.5f)
            curveTo(6.5f, 10.5f, 6.2f, 11.2f, 5.5f, 11.5f)
            curveTo(4.8f, 11.8f, 4.2f, 12.2f, 4.2f, 12.8f)
            curveTo(4.2f, 13.5f, 5.2f, 13.8f, 6.5f, 13.8f)
            curveTo(6.8f, 14.8f, 7.5f, 15.5f, 8.5f, 16f)
            curveTo(7.8f, 16.5f, 6f, 17f, 5f, 18.5f)
            curveTo(6.5f, 19f, 9f, 18.5f, 12f, 18.5f)
            curveTo(15f, 18.5f, 17.5f, 19f, 19f, 18.5f)
            curveTo(18f, 17f, 16.2f, 16.5f, 15.5f, 16f)
            curveTo(16.5f, 15.5f, 17.2f, 14.8f, 17.5f, 13.8f)
            curveTo(18.8f, 13.8f, 19.8f, 13.5f, 19.8f, 12.8f)
            curveTo(19.8f, 12.2f, 19.2f, 11.8f, 18.5f, 11.5f)
            curveTo(17.8f, 11.2f, 17.5f, 10.5f, 17.5f, 9.5f)
            curveTo(17.5f, 6f, 15.5f, 3.5f, 12f, 3.5f)
            close()
        }
    }.build()
}

private val ThreadsIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "threads",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(16.5f, 8.5f)
            curveTo(15.2f, 6.8f, 13.6f, 6f, 11.8f, 6f)
            curveTo(8.5f, 6f, 6f, 8.6f, 6f, 12f)
            curveTo(6f, 15.4f, 8.5f, 18f, 11.8f, 18f)
            curveTo(14.5f, 18f, 16.5f, 16.5f, 17.2f, 14.2f)
            moveTo(12f, 9.5f)
            curveTo(10.5f, 9.5f, 9.5f, 10.5f, 9.5f, 12f)
            curveTo(9.5f, 13.5f, 10.5f, 14.5f, 12f, 14.5f)
            curveTo(13.8f, 14.5f, 14.5f, 13.2f, 14.5f, 11.5f)
            curveTo(14.5f, 8.5f, 12.8f, 8f, 11.5f, 8f)
        }
    }.build()
}
