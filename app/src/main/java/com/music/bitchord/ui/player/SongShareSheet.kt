package com.music.bitchord.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
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
 * Interactive popup for sharing songs and stories across social apps with ultra-customizable preview options.
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

    var template by remember(song) { mutableStateOf(SongShareTemplate.GLASS_STORY) }
    var palette by remember(song) { mutableStateOf(SongSharePalette.DYNAMIC) }
    var aspectRatio by remember(song) { mutableStateOf(SongShareAspectRatio.STORY_9_16) }
    var visualizer by remember(song) { mutableStateOf(SongShareVisualizer.BARS) }

    var bitmap by remember(song, template, palette, aspectRatio, visualizer) { mutableStateOf<Bitmap?>(null) }
    var rendering by remember(song, template, palette, aspectRatio, visualizer) { mutableStateOf(true) }
    var cachedUri by remember(song, template, palette, aspectRatio, visualizer) { mutableStateOf<Uri?>(null) }
    var isSaved by remember(song, template, palette, aspectRatio, visualizer) { mutableStateOf(false) }

    LaunchedEffect(song, template, palette, aspectRatio, visualizer, lyricsLines) {
        rendering = true
        val bm = runCatching {
            renderSongShareCard(
                context = context,
                song = song,
                template = template,
                palette = palette,
                aspectRatio = aspectRatio,
                visualizer = visualizer,
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
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

            // ── 1. Live Card Preview (Adaptive 9:16 or 1:1) ──────────────────────
            val isSquare = aspectRatio == SongShareAspectRatio.SQUARE_1_1
            val previewModifier = Modifier
                .height(if (isSquare) 190.dp else 205.dp)
                .aspectRatio(if (isSquare) 1f else 9f / 16f)
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

            // ── 2. Card Template Selector [ Glass | Vinyl | Cassette | Lyrics | Bento ] ──
            val templateScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(templateScrollState)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF22252C))
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SegmentedPill(
                    label = stringResource(R.string.share_template_glass),
                    selected = template == SongShareTemplate.GLASS_STORY,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        template = SongShareTemplate.GLASS_STORY
                    },
                )
                SegmentedPill(
                    label = stringResource(R.string.share_template_vinyl),
                    selected = template == SongShareTemplate.VINYL_RECORD,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        template = SongShareTemplate.VINYL_RECORD
                    },
                )
                SegmentedPill(
                    label = stringResource(R.string.share_template_cassette),
                    selected = template == SongShareTemplate.RETRO_CASSETTE,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        template = SongShareTemplate.RETRO_CASSETTE
                    },
                )
                SegmentedPill(
                    label = stringResource(R.string.share_template_lyrics),
                    selected = template == SongShareTemplate.EDITORIAL_LYRICS,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        if (lyricsLines.isEmpty()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.share_lyrics_unavailable),
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            template = SongShareTemplate.EDITORIAL_LYRICS
                        }
                    },
                )
                SegmentedPill(
                    label = stringResource(R.string.share_template_bento),
                    selected = template == SongShareTemplate.BENTO_SQUARE,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        template = SongShareTemplate.BENTO_SQUARE
                    },
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── 3. Aspect Ratio & Palette Row ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Aspect Ratio Selector: [ 9:16 | 1:1 ]
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF22252C))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiniTogglePill(
                        label = stringResource(R.string.share_aspect_story),
                        selected = aspectRatio == SongShareAspectRatio.STORY_9_16,
                        onClick = {
                            haptics.play(Haptic.Tap)
                            aspectRatio = SongShareAspectRatio.STORY_9_16
                        },
                    )
                    MiniTogglePill(
                        label = stringResource(R.string.share_aspect_square),
                        selected = aspectRatio == SongShareAspectRatio.SQUARE_1_1,
                        onClick = {
                            haptics.play(Haptic.Tap)
                            aspectRatio = SongShareAspectRatio.SQUARE_1_1
                        },
                    )
                }

                // Visualizer Style Pills: [ Bars | Wave | Pulse | Off ]
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF22252C))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiniTogglePill(
                        label = stringResource(R.string.share_visualizer_bars),
                        selected = visualizer == SongShareVisualizer.BARS,
                        onClick = {
                            haptics.play(Haptic.Tap)
                            visualizer = SongShareVisualizer.BARS
                        },
                    )
                    MiniTogglePill(
                        label = stringResource(R.string.share_visualizer_sine),
                        selected = visualizer == SongShareVisualizer.SINE_WAVE,
                        onClick = {
                            haptics.play(Haptic.Tap)
                            visualizer = SongShareVisualizer.SINE_WAVE
                        },
                    )
                    MiniTogglePill(
                        label = stringResource(R.string.share_visualizer_pulse),
                        selected = visualizer == SongShareVisualizer.PULSE_DOTS,
                        onClick = {
                            haptics.play(Haptic.Tap)
                            visualizer = SongShareVisualizer.PULSE_DOTS
                        },
                    )
                    MiniTogglePill(
                        label = stringResource(R.string.share_visualizer_off),
                        selected = visualizer == SongShareVisualizer.OFF,
                        onClick = {
                            haptics.play(Haptic.Tap)
                            visualizer = SongShareVisualizer.OFF
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 4. Color Palette Swatches (5 Colors) ─────────────────────────────
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
                    brush = SolidColor(Color(0xFF0F1117)),
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
                PaletteCircle(
                    brush = Brush.linearGradient(listOf(Color(0xFFFFA000), Color(0xFFE53935))),
                    selected = palette == SongSharePalette.SUNSET_GOLD,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        palette = SongSharePalette.SUNSET_GOLD
                    },
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── 5. Social Action Buttons (Authentic Brand Icons & "Snap Song" Integration) ──
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

                // WhatsApp (Authentic Official WhatsApp Logo)
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

                // Snapchat (Authentic Ghostface Chillah Icon & Snap Song Camera/Story Target)
                SocialShareAction(
                    icon = SnapchatIcon,
                    label = stringResource(R.string.share_snapchat),
                    background = SolidColor(Color(0xFFFFFC00)),
                    iconTint = Color.Unspecified,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        scope.launch {
                            val uri = cachedUri ?: bitmap?.let { cacheForSharing(context, it, SONG_SHARE_FILE) }
                            if (uri != null) {
                                shareToSnapchat(context, uri, song)
                            }
                        }
                    },
                )

                // Instagram Stories (Authentic Camera Glyph on Sunset Gradient)
                SocialShareAction(
                    icon = InstagramIcon,
                    label = stringResource(R.string.share_instagram_stories),
                    background = Brush.linearGradient(
                        listOf(
                            Color(0xFF833AB4),
                            Color(0xFFFD1D1D),
                            Color(0xFFFCB045),
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

                // Threads (Authentic Meta Threads Swirl Icon)
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
                                Toast.makeText(context, context.getString(R.string.share_saved_to_gallery), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                )
            }
        }
    }
}

// ── Segmented Control Pills & Palette Circle ────────────────────────────────

@Composable
private fun SegmentedPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = if (selected) Color.White else Color.White.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun MiniTogglePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
            color = if (selected) Color.White else Color.White.copy(alpha = 0.62f),
        )
    }
}

@Composable
private fun PaletteCircle(
    brush: Brush,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(targetValue = if (selected) 1.15f else 1f, label = "paletteScale")
    Box(
        modifier = Modifier
            .size((28 * scale).dp)
            .clip(CircleShape)
            .border(
                width = if (selected) 2.5.dp else 0.dp,
                color = if (selected) Color.White else Color.Transparent,
                shape = CircleShape,
            )
            .padding(if (selected) 3.dp else 0.dp)
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
                modifier = Modifier.size(24.dp),
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

/**
 * Shares to Snapchat as a "Snap Song" story/camera preview rather than a direct chat message.
 * Targets Snapchat's LandingPageActivity / Story Intent so it loads directly into the camera preview.
 */
private fun shareToSnapchat(context: Context, uri: Uri, song: Song): Boolean {
    val songUrl = "https://music.youtube.com/watch?v=${song.videoId}"
    val caption = "${song.title} · ${song.artist}"

    // 1. Copy song link to clipboard so user can 1-tap attach via Snapchat's paperclip
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(song.title, songUrl))

    // 2. Guide user to select 'My Story' at the top
    Toast.makeText(context, "Select 'My Story' at the top in Snapchat 📸", Toast.LENGTH_LONG).show()

    // 3. Grant URI permission to Snapchat
    context.grantUriPermission("com.snapchat.android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

    // 4. Send image to Snapchat
    return try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra("attachmentUrl", songUrl)
            putExtra("content_url", songUrl)
            putExtra("captionText", caption)
            putExtra(Intent.EXTRA_TEXT, "$caption\n$songUrl")
            setPackage("com.snapchat.android")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(sendIntent)
        true
    } catch (_: Exception) {
        shareToSystem(context, uri, song)
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
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_song_title)))
}

// ── Authentic Official Brand Vector Icons (Simple Icons SVG Specifications) ──

/**
 * Official Instagram camera glyph.
 */
private const val INSTAGRAM_SVG_PATH =
    "M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"

private val InstagramIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "instagram",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(INSTAGRAM_SVG_PATH),
        fill = SolidColor(Color.White),
    ).build()
}

/**
 * Official WhatsApp speech-bubble and telephone handset glyph.
 */
private const val WHATSAPP_SVG_PATH =
    "M17.472 14.382c-.297-.149-1.758-.868-2.031-.967-.272-.099-.47-.149-.669.149-.198.297-.768.967-.941 1.165-.173.198-.347.223-.644.074-.297-.149-1.255-.462-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.297-.347.446-.521.151-.172.2-.296.3-.495.099-.198.05-.372-.025-.521-.075-.148-.669-1.611-.916-2.206-.242-.579-.487-.501-.669-.51l-.57-.01c-.198 0-.52.074-.792.372s-1.04 1.016-1.04 2.479 1.065 2.876 1.213 3.074c.149.198 2.096 3.2 5.077 4.487.709.306 1.263.489 1.694.626.712.226 1.36.194 1.872.118.571-.085 1.758-.719 2.006-1.413.248-.695.248-1.29.173-1.414-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.371l-.361-.214-3.732.978.996-3.635-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.41 0 0 5.41 0 12.05c0 2.122.556 4.185 1.613 6.002L0 24l6.155-1.612a11.83 11.83 0 005.893 1.589h.005c6.639 0 12.05-5.41 12.05-12.05 0-3.219-1.252-6.25-3.526-8.524"

private val WhatsAppIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "whatsapp",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(WHATSAPP_SVG_PATH),
        fill = SolidColor(Color.White),
    ).build()
}

/**
 * Official Snapchat Ghost (Ghostface Chillah) with authentic wavy body contour and black outline.
 */
private const val SNAPCHAT_SVG_PATH =
    "M12.166 3c.796 0 3.495.223 4.769 3.073c.426.959.324 2.589.24 3.898l-.002.047c-.011.146-.018.278-.024.41a.6.6 0 0 0 .322.072c.241-.013.53-.096.831-.241a.8.8 0 0 1 .373-.084c.146 0 .289.023.409.072c.361.12.59.385.59.674q.019.542-.975.939c-.58.232-1.229.434-1.748.711c-.554.296-.862.827-.856 1.396c.007.697 1.258 2.658 3.882 3.123c.385.068.681.334.78.705c.105.39-.033.805-.347 1.056c-2.029 1.62-4.07 2.373-6.19 2.373s-4.161-.753-6.19-2.373c-.314-.251-.452-.666-.347-1.056c.099-.371.395-.637.78-.705c2.624-.465 3.875-2.426 3.882-3.123c.006-.569-.302-1.1-.856-1.396c-.52-.277-1.168-.479-1.748-.711q-.994-.397-.975-.939c0-.289.229-.554.59-.674c.12-.049.263-.072.409-.072a.8.8 0 0 1 .373.084c.3.145.59.228.831.241a.6.6 0 0 0 .322-.072l-.024-.457c-.084-1.309-.186-2.939.24-3.898C8.671 3.223 11.37 3 12.166 3z"

private val SnapchatIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "snapchat",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(SNAPCHAT_SVG_PATH),
        fill = SolidColor(Color.White),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.15f,
    ).build()
}

/**
 * Official Meta Threads spiral @ glyph.
 */
private const val THREADS_SVG_PATH =
    "M12.186 24h-.007c-3.581-.024-6.334-1.205-8.184-3.509C2.35 18.44 1.5 15.586 1.472 12.01v-.017c.03-3.579.879-6.43 2.525-8.482C5.845 1.205 8.6.024 12.18 0h.014c2.746.02 5.043.725 6.826 2.098c1.677 1.29 2.858 3.13 3.509 5.467l-2.04.569c-1.104-3.96-3.898-5.984-8.304-6.015c-2.91.022-5.11.936-6.54 2.717C4.307 6.504 3.616 9.07 3.59 12.002c.027 2.92.717 5.48 2.062 7.153c1.43 1.78 3.63 2.693 6.545 2.718c2.316-.017 4.144-.57 5.438-1.646c1.332-1.11 2.016-2.822 2.036-5.097c-.018-2.319-.715-4.045-2.072-5.137c-1.282-1.037-3.04-1.536-5.226-1.488c-2.457.054-4.325.77-5.545 2.132c-.93 1.042-1.408 2.47-1.422 4.242h1.996c.01-.986.307-1.78.883-2.422c.866-.97 2.227-1.43 4.047-1.468c1.67-.037 2.96.327 3.829 1.083c.83.72 1.258 1.77 1.272 3.125c-.015 1.345-.395 2.378-1.131 2.985c-.71.587-1.77.89-3.15.901-2.016-.016-3.414-.653-4.148-1.895c-.435-.733-.66-1.688-.667-2.842h-1.996c.01 1.558.334 2.88 1.002 3.966c1.01 1.636 2.784 2.49 5.27 2.541h.01c2.193-.016 3.843-.524 5.045-1.554c1.458-1.246 2.215-3.19 2.247-5.787c-.03-2.473-.787-4.402-2.25-5.742c-1.503-1.378-3.69-2.095-6.495-2.132C7.306 4.966 4.67 7.03 4.654 11.986h-2.01c.02-3.692 1.07-6.587 3.12-8.608c1.88-1.844 4.54-2.825 7.91-2.917h.034c3.398.093 6.07 1.077 7.95 2.943c2.05 2.025 3.104 4.92 3.132 8.618c-.04 3.407-.99 6.068-2.83 7.915c-2.16 2.146-5.32 3.255-9.405 3.298Z"

private val ThreadsIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "threads",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(THREADS_SVG_PATH),
        fill = SolidColor(Color.White),
    ).build()
}
