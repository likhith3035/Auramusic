package com.music.bitchord.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbDownOffAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.ui.theme.ArtworkPalette

/**
 * Rich track header displaying album art, song title, and artist at the head of the action sheet.
 */
@Composable
fun AuraTrackHeader(
    song: Song,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val artShape = RoundedCornerShape(12.dp).continuous()
        AsyncImage(
            model = song.artworkAt(ROW_ART_PX),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(54.dp)
                .clip(artShape)
                .thumbnailBorder(artShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ExplicitSongTitle(
                song = song,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                ),
                color = Color.White,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.70f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = Color.White.copy(alpha = 0.60f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Rich browse collection header for albums, playlists, and artists.
 */
@Composable
fun AuraBrowseHeader(
    target: BrowseTarget,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isArtist = target.type == BrowseType.ARTIST
    val artShape = if (isArtist) CircleShape else RoundedCornerShape(12.dp).continuous()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (target.thumbnailUrl != null) {
            AsyncImage(
                model = target.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(artShape)
                    .thumbnailBorder(artShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(14.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = target.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = target.subtitle.ifEmpty {
                    when (target.type) {
                        BrowseType.ALBUM -> "Album"
                        BrowseType.PLAYLIST -> "Playlist"
                        BrowseType.ARTIST -> "Artist"
                        else -> "Collection"
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.70f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = Color.White.copy(alpha = 0.60f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * 1-Tap quick action pills row across the top of the sheet (Like, Dislike, Download, Radio, Share).
 */
@Composable
fun AuraQuickActionsBar(
    liked: Boolean,
    disliked: Boolean,
    downloaded: Boolean,
    onToggleLike: () -> Unit,
    onToggleDislike: (() -> Unit)?,
    onDownload: (() -> Unit)?,
    onStartRadio: (() -> Unit)?,
    onShare: (() -> Unit)?,
    palette: ArtworkPalette? = null,
    modifier: Modifier = Modifier,
) {
    val accentColor = palette?.accent ?: Color(0xFF00F5D4)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Favorite / Like Pill
        val favAnimatedColor by animateColorAsState(
            targetValue = if (liked) accentColor else Color.White.copy(alpha = 0.85f),
            animationSpec = tween(durationMillis = 200),
            label = "favoriteColor",
        )
        AuraQuickActionPill(
            icon = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            label = if (liked) "Liked" else "Like",
            iconTint = favAnimatedColor,
            onClick = onToggleLike,
            modifier = Modifier.weight(1f),
        )

        // Dislike Pill (when available)
        if (onToggleDislike != null) {
            val dislikeColor = if (disliked) accentColor else Color.White.copy(alpha = 0.85f)
            AuraQuickActionPill(
                icon = if (disliked) Icons.Rounded.ThumbDown else Icons.Rounded.ThumbDownOffAlt,
                label = "Dislike",
                iconTint = dislikeColor,
                onClick = onToggleDislike,
                modifier = Modifier.weight(1f),
            )
        }

        // Download Pill (when available)
        if (onDownload != null) {
            val downloadColor = if (downloaded) Color(0xFF00E676) else Color.White.copy(alpha = 0.85f)
            AuraQuickActionPill(
                icon = if (downloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                label = if (downloaded) "Saved" else "Download",
                iconTint = downloadColor,
                onClick = onDownload,
                modifier = Modifier.weight(1f),
            )
        }

        // Radio Pill (when available)
        if (onStartRadio != null) {
            AuraQuickActionPill(
                icon = Icons.Rounded.Radio,
                label = "Radio",
                iconTint = Color.White.copy(alpha = 0.85f),
                onClick = onStartRadio,
                modifier = Modifier.weight(1f),
            )
        }

        // Share Pill (when available)
        if (onShare != null) {
            AuraQuickActionPill(
                icon = Icons.Rounded.Share,
                label = "Share",
                iconTint = Color.White.copy(alpha = 0.85f),
                onClick = onShare,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AuraQuickActionPill(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(14.dp).continuous()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bgColor = if (pressed) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.07f)

    Column(
        modifier = modifier
            .clip(pillShape)
            .background(bgColor)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = Color.White.copy(alpha = 0.80f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Highlighted Aura Superpower card (AI Companion, Neural DJ, Vocal Stem Karaoke).
 * Uses clean typographic badges and official Material icons with zero emojis.
 */
@Composable
fun AuraSuperpowerCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(16.dp).continuous()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val bgAlpha = if (pressed) 0.22f else 0.12f

    Surface(
        shape = cardShape,
        color = accentColor.copy(alpha = bgAlpha),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(cardShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        ),
                        color = Color.White,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp).continuous(),
                        color = accentColor.copy(alpha = 0.20f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.50f)),
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                            ),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.70f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Standard touch-friendly action row with 50dp minimum height and clean Material 3 typography.
 */
@Composable
fun AuraActionRow(
    icon: ImageVector?,
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    description: String? = null,
    iconTint: Color = Color.White.copy(alpha = 0.85f),
    labelColor: Color = Color.White,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val alpha = if (enabled) 1f else 0.40f
    val rowShape = RoundedCornerShape(12.dp).continuous()
    val bgColor = if (pressed && enabled) Color.White.copy(alpha = 0.08f) else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .clip(rowShape)
            .background(bgColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .heightIn(min = 50.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(26.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint.copy(alpha = alpha),
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = labelColor.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.55f * alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * Hairline separator line between action groups.
 */
@Composable
fun AuraActionDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.08f)),
    )
}

/**
 * Category section title.
 */
@Composable
fun AuraSectionHeading(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        ),
        color = Color.White.copy(alpha = 0.45f),
        modifier = modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 4.dp),
    )
}
