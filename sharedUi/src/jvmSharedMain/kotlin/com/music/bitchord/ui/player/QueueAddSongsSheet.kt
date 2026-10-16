package com.music.bitchord.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.components.ExplicitSongTitle
import com.music.bitchord.ui.components.thumbnailBorder
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Interactive in-player sheet allowing users to search, browse, and add songs
 * directly to the Up Next play queue without interrupting current playback.
 */
@Composable
fun QueueAddSongsSheet(
    hazeState: HazeState,
    onAddToQueue: (Song) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    val addedVideoIds = remember { mutableStateListOf<String>() }

    val suggestionChips = remember {
        listOf("Top Hits", "Trending", "Chill", "Lo-Fi", "Pop", "Rock", "Hip-Hop", "Acoustic")
    }

    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun executeSearch(searchQuery: String) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            // Default to top hits so the user always has music to pick
            searchJob?.cancel()
            searchJob = scope.launch {
                isLoading = true
                val res = YtMusicRepository.search("Top Hits", SearchFilter.SONGS)
                if (res.isSuccess) {
                    val tracks = res.getOrNull().orEmpty().mapNotNull { item ->
                        when (item) {
                            is SearchResult.Track -> item.song
                            is SearchResult.TopTrack -> item.song
                            else -> null
                        }
                    }
                    results = tracks
                }
                isLoading = false
            }
            return
        }

        searchJob?.cancel()
        searchJob = scope.launch {
            isLoading = true
            delay(280) // Debounce
            val res = YtMusicRepository.search(q, SearchFilter.SONGS)
            if (res.isSuccess) {
                val tracks = res.getOrNull().orEmpty().mapNotNull { item ->
                    when (item) {
                        is SearchResult.Track -> item.song
                        is SearchResult.TopTrack -> item.song
                        else -> null
                    }
                }
                results = tracks
            }
            isLoading = false
        }
    }

    // Load initial recommendations on launch
    LaunchedEffect(Unit) {
        executeSearch("")
    }

    PlayerDrawer(
        hazeState = hazeState,
        title = "Add Songs to Queue",
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        // Search Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search songs or artists...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = Color.White.copy(alpha = 0.40f),
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        executeSearch(it)
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    cursorBrush = SolidColor(Color(0xFF00D2FF)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        executeSearch(query)
                    }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable {
                            query = ""
                            executeSearch("")
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Quick Suggestion Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestionChips.forEach { chip ->
                val isSelected = query.equals(chip, ignoreCase = true)
                val chipBg by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF00D2FF).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                    label = "chip_bg",
                )
                val chipBorder by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF00D2FF).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.10f),
                    label = "chip_border",
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(chipBg)
                        .border(1.dp, chipBorder, RoundedCornerShape(percent = 50))
                        .clickable {
                            query = chip
                            executeSearch(chip)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF00D2FF) else Color.White.copy(alpha = 0.8f),
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Results Section Header & Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (query.isBlank()) "Recommended to Queue" else "Search Results",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.65f),
                ),
                modifier = Modifier.weight(1f),
            )
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF00D2FF),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Song Rows
        if (results.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (query.isBlank()) "Loading suggestions..." else "No songs found for '$query'",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.45f),
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                results.take(15).forEach { song ->
                    val isAdded = addedVideoIds.contains(song.videoId)
                    QueueAddSongRow(
                        song = song,
                        isAdded = isAdded,
                        onAdd = {
                            if (!isAdded) {
                                addedVideoIds.add(song.videoId)
                                haptics.play(Haptic.Tap)
                                onAddToQueue(song)
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Individual track row inside the Add Songs sheet.
 */
@Composable
private fun QueueAddSongRow(
    song: Song,
    isAdded: Boolean,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onAdd)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = rememberRemoteArtworkUrl(song),
            contentDescription = null,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .thumbnailBorder(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            ExplicitSongTitle(
                song = song,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = Color.White.copy(alpha = 0.95f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${song.artist} • ${song.durationText ?: ""}".trimEnd(' ', '•'),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(10.dp))

        // Interactive "+ Add" / "Added ✓" button
        val buttonBg by animateColorAsState(
            targetValue = if (isAdded) Color(0xFF00D2FF).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.12f),
            label = "btn_bg",
        )
        val buttonBorder by animateColorAsState(
            targetValue = if (isAdded) Color(0xFF00D2FF).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.18f),
            label = "btn_border",
        )
        val contentColor by animateColorAsState(
            targetValue = if (isAdded) Color(0xFF00D2FF) else Color.White,
            label = "btn_content",
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(buttonBg)
                .border(1.dp, buttonBorder, RoundedCornerShape(percent = 50))
                .clickable(onClick = onAdd)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = if (isAdded) Icons.Rounded.Check else Icons.Rounded.Add,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = if (isAdded) "Added" else "Add",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                    ),
                )
            }
        }
    }
}
