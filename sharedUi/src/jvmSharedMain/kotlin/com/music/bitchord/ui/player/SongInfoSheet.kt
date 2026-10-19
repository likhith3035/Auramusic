package com.music.bitchord.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.SongDetailsResolver
import com.music.bitchord.data.model.ArtistRef
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.SongDetails
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.theme.ArtworkPalette
import com.music.bitchord.ui.theme.rememberArtworkPalette

/**
 * Beautiful, pro-grade Song Info & Credits sheet.
 *
 * Displays:
 * - High-res cover with dynamic ambient glow from artwork palette
 * - Song Title, Primary Artist & Album
 * - Lossless / Audio stream quality badge
 * - Quick Action Studio (Album, Share Link, Artist Radio)
 * - Metadata Spec Grid (Release Year, Duration, Quality, Source)
 * - Contributing Singers & Artists with interactive profile chips
 * - Editorial description / Behind the Track
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SongInfoContent(
    song: Song,
    onClose: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onCopyLink: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    palette: ArtworkPalette? = null,
    scrollable: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val effectivePalette = palette ?: rememberArtworkPalette(song.thumbnailUrl, artPx = CARD_ART_PX)
    val palette = effectivePalette
    var details by remember(song.videoId) { mutableStateOf<SongDetails?>(null) }
    var isLoading by remember(song.videoId) { mutableStateOf(true) }

    LaunchedEffect(song.videoId) {
        isLoading = true
        details = SongDetailsResolver.resolve(song)
        isLoading = false
    }

    val contentModifier = if (scrollable) {
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    } else {
        modifier.fillMaxWidth()
    }

    Column(
        modifier = contentModifier
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Top navigation bar with Close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Song Details & Credits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Hero Card with dynamic artwork ambient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            palette.elevated,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = palette.accent.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // High-res album art
                AsyncImage(
                    model = song.artworkAt(544),
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Audio Quality Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = palette.accent.copy(alpha = 0.18f),
                        modifier = Modifier.padding(bottom = 6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HighQuality,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (song.localUri != null) "ORIGINAL / LOSSLESS" else "HI-RES AUDIO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = palette.accent,
                            )
                        }
                    }

                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            val id = details?.song?.artistId ?: song.artistId
                            if (!id.isNullOrBlank()) onOpenArtist(id)
                        },
                    )

                    val albumName = song.albumName
                    if (!albumName.isNullOrBlank()) {
                        Text(
                            text = albumName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                val id = details?.song?.albumId ?: song.albumId
                                if (!id.isNullOrBlank()) onOpenAlbum(id)
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Quick Action Studio Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val albumId = details?.albumId ?: song.albumId
            if (!albumId.isNullOrBlank()) {
                QuickActionButton(
                    icon = Icons.Rounded.Album,
                    label = "Album",
                    paletteAccent = palette.accent,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenAlbum(albumId) },
                )
            }

            if (onCopyLink != null) {
                QuickActionButton(
                    icon = Icons.Rounded.Link,
                    label = "Link",
                    paletteAccent = palette.accent,
                    modifier = Modifier.weight(1f),
                    onClick = onCopyLink,
                )
            }

            if (onShare != null) {
                QuickActionButton(
                    icon = Icons.Rounded.Share,
                    label = "Share",
                    paletteAccent = palette.accent,
                    modifier = Modifier.weight(1f),
                    onClick = onShare,
                )
            }

            if (song.videoId.isNotBlank()) {
                val uriHandler = LocalUriHandler.current
                QuickActionButton(
                    icon = Icons.AutoMirrored.Rounded.OpenInNew,
                    label = "YouTube",
                    paletteAccent = Color(0xFFFF0033),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        try {
                            uriHandler.openUri("https://www.youtube.com/watch?v=${song.videoId}")
                        } catch (_: Throwable) {
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Section: Track Specifications Grid
        Text(
            text = "Track Specifications",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SpecTile(
                icon = Icons.Rounded.CalendarToday,
                title = "Release Year",
                value = details?.releaseDateOrYear ?: "Catalogue Release",
                paletteAccent = palette.accent,
                modifier = Modifier.weight(1f),
            )
            SpecTile(
                icon = Icons.Rounded.Schedule,
                title = "Duration",
                value = song.durationText ?: "Unknown",
                paletteAccent = palette.accent,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SpecTile(
                icon = Icons.Rounded.GraphicEq,
                title = "Audio Quality",
                value = details?.audioQuality ?: "256 kbps (High)",
                paletteAccent = palette.accent,
                modifier = Modifier.weight(1f),
            )
            SpecTile(
                icon = Icons.Rounded.Album,
                title = "Stream Source",
                value = details?.streamSource ?: "YouTube Music",
                paletteAccent = palette.accent,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(22.dp))

        // Section: Contributing Singers & Artists
        val artists = details?.artists.orEmpty().ifEmpty {
            if (song.artist.isNotBlank()) listOf(ArtistRef(song.artist, song.artistId)) else emptyList()
        }

        if (artists.isNotEmpty()) {
            Text(
                text = "Singers & Contributors",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 10.dp),
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                artists.forEach { artistRef ->
                    ArtistChip(
                        artist = artistRef,
                        accent = palette.accent,
                        onClick = {
                            val browseId = artistRef.browseId
                            if (!browseId.isNullOrBlank()) {
                                onOpenArtist(browseId)
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
        }

        // Section: Behind the Track / Editorial Synopsis (if available)
        val description = details?.description
        if (!description.isNullOrBlank()) {
            Text(
                text = "Behind the Track",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        if (isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = palette.accent,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Loading complete metadata...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    paletteAccent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = paletteAccent,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SpecTile(
    icon: ImageVector,
    title: String,
    value: String,
    paletteAccent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = paletteAccent,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ArtistChip(
    artist: ArtistRef,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
        modifier = Modifier.clickable { onClick() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
