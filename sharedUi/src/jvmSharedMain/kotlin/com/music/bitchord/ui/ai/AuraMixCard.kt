package com.music.bitchord.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
 * Embedded mix carousel for multi-track AI recommendations.
 * Fully adapts to BitChord theme and guarantees playable resolved streams.
 */
@Composable
fun AuraMixCard(
    songs: List<Song>,
    reasons: Map<String, String>,
    onPlaySong: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onQueueAll: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (songs.isEmpty()) return

    val scope = rememberCoroutineScope()
    var resolvingSongId by remember { mutableStateOf<String?>(null) }
    var isResolvingAll by remember { mutableStateOf(false) }

    fun playSingleWithResolve(song: Song) {
        if (song.videoId.startsWith("search:")) {
            resolvingSongId = song.videoId
            scope.launch {
                val resolved = AuraAiService.resolveSingleSong(song)
                resolvingSongId = null
                onPlaySong(resolved)
            }
        } else {
            onPlaySong(song)
        }
    }

    fun playAllWithResolve() {
        val needsResolution = songs.any { it.videoId.startsWith("search:") }
        if (needsResolution) {
            isResolvingAll = true
            scope.launch {
                val resolvedList = songs.map { song ->
                    if (song.videoId.startsWith("search:")) AuraAiService.resolveSingleSong(song) else song
                }
                isResolvingAll = false
                onPlayAll(resolvedList)
            }
        } else {
            onPlayAll(songs)
        }
    }

    fun queueAllWithResolve() {
        val needsResolution = songs.any { it.videoId.startsWith("search:") }
        if (needsResolution) {
            isResolvingAll = true
            scope.launch {
                val resolvedList = songs.map { song ->
                    if (song.videoId.startsWith("search:")) AuraAiService.resolveSingleSong(song) else song
                }
                isResolvingAll = false
                onQueueAll(resolvedList)
            }
        } else {
            onQueueAll(songs)
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Mix Title and Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "⚡ Aura Smart Mix",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${songs.size} Curated Tracks",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { playAllWithResolve() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(percent = 50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp),
                        enabled = !isResolvingAll,
                    ) {
                        if (isResolvingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Spacer(Modifier.width(3.dp))
                        Text("Play All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { queueAllWithResolve() },
                        shape = RoundedCornerShape(percent = 50),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier.height(30.dp),
                        enabled = !isResolvingAll,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = "Queue All",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Horizontal Carousel of mini track cards
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                itemsIndexed(songs) { _, song ->
                    val isThisResolving = resolvingSongId == song.videoId
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier
                            .width(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isThisResolving) { playSingleWithResolve(song) },
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(118.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!song.thumbnailUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = song.thumbnailUrl,
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().height(118.dp),
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(32.dp),
                                    )
                                }

                                // Quick Play button in lower right of album art
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isThisResolving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 1.5.dp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 9.5.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            val reason = reasons[song.videoId]
                            if (!reason.isNullOrBlank()) {
                                Spacer(Modifier.height(3.dp))
                                val matchMatch = Regex("""(\d+)%\s*Match""", RegexOption.IGNORE_CASE).find(reason)
                                val percent = matchMatch?.groupValues?.get(1)?.toIntOrNull()
                                val cleanTag = if (matchMatch != null) {
                                    reason.replace(matchMatch.value, "").trim().removePrefix("·").removePrefix("-").trim()
                                } else {
                                    reason
                                }

                                if (percent != null) {
                                    Surface(
                                        shape = RoundedCornerShape(percent = 50),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.5.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        ),
                                    ) {
                                        Text(
                                            text = "$percent% MATCH",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.5.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        )
                                    }
                                } else if (cleanTag.isNotBlank()) {
                                    Text(
                                        text = cleanTag,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.5.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
