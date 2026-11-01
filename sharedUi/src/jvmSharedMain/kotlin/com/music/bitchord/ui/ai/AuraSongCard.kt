package com.music.bitchord.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.ai.AuraAiService
import com.music.bitchord.data.model.Song
import kotlinx.coroutines.launch

/**
 * Modern song card embedded within Aura AI chat messages.
 * Adapts dynamically to BitChord's theme and provides on-demand resolution for recommended tracks.
 */
@Composable
fun AuraSongCard(
    song: Song,
    reason: String? = null,
    isPlaying: Boolean = false,
    onPlay: (Song) -> Unit,
    onQueue: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var isResolving by remember { mutableStateOf(false) }

    fun playWithResolve() {
        if (isResolving) return
        if (song.videoId.startsWith("search:")) {
            isResolving = true
            scope.launch {
                val resolved = AuraAiService.resolveSingleSong(song)
                isResolving = false
                onPlay(resolved)
            }
        } else {
            onPlay(song)
        }
    }

    fun queueWithResolve() {
        if (isResolving) return
        if (song.videoId.startsWith("search:")) {
            isResolving = true
            scope.launch {
                val resolved = AuraAiService.resolveSingleSong(song)
                isResolving = false
                onQueue(resolved)
            }
        } else {
            onQueue(song)
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { playWithResolve() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Artwork / Thumbnail with Play overlay
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (!song.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp),
                    )
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "▶",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Metadata: Title, Artist, Reason badge
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Match Rate & Reason Badge
                val matchRateRegex = Regex("""(\d+)%\s*Match""", RegexOption.IGNORE_CASE)
                val matchMatch = reason?.let { matchRateRegex.find(it) }
                val extractedPercent = matchMatch?.groupValues?.get(1)?.toIntOrNull()
                val cleanReason = if (matchMatch != null) {
                    reason.replace(matchMatch.value, "").trim().removePrefix("·").removePrefix("-").trim()
                } else {
                    reason
                }

                if (extractedPercent != null || !cleanReason.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (extractedPercent != null) {
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                ),
                            ) {
                                Text(
                                    text = "$extractedPercent% MATCH",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }

                        if (!cleanReason.isNullOrBlank()) {
                            if (extractedPercent != null) Spacer(Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outline,
                                ),
                            ) {
                                Text(
                                    text = cleanReason,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Actions: Queue and Play
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { queueWithResolve() },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = "Queue track",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPlaying) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                        )
                        .border(
                            width = 0.5.dp,
                            color = if (isPlaying) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        )
                        .clickable(enabled = !isResolving) { playWithResolve() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isResolving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = if (isPlaying) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause song" else "Play song",
                            tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
