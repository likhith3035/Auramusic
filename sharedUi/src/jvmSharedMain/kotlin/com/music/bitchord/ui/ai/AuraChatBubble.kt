package com.music.bitchord.ui.ai

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.ai.AuraActionRef
import com.music.bitchord.data.ai.AuraChatMessage
import com.music.bitchord.data.ai.ChatSender
import com.music.bitchord.data.model.Song

/**
 * Message bubble for Aura AI chat conversations.
 * Themed to BitChord design language (Dark, Light, Nothing OS modes).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuraChatBubble(
    message: AuraChatMessage,
    currentPlayingSong: Song?,
    isPlaying: Boolean,
    onPlaySong: (Song) -> Unit,
    onQueueSong: (Song) -> Unit,
    onPlayMix: (List<Song>) -> Unit,
    onQueueMix: (List<Song>) -> Unit,
    onActionClick: (AuraActionRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = message.sender == ChatSender.USER
    val uriHandler = LocalUriHandler.current

    val openUrlSafely: (String) -> Unit = { rawUrl ->
        runCatching {
            val url = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                "https://$rawUrl"
            } else rawUrl
            uriHandler.openUri(url)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (isUser) {
            // User message bubble — uses high contrast primary theme container
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 4.dp,
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.widthIn(max = 290.dp),
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        letterSpacing = (-0.15).sp,
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        } else {
            // Aura AI message bubble — clean surfaceVariant card with outline
            Surface(
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp,
                ),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.widthIn(max = 340.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Aura Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp),
                    ) {
                        AuraOrbIcon(
                            size = 18.dp,
                            isThinking = false,
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = "AURA",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // Markdown-like parsed text with precision tracking
                    val formattedText = formatMarkdown(
                        rawText = message.text,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        codeBg = MaterialTheme.colorScheme.surface,
                        codeColor = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = formattedText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 21.sp,
                            letterSpacing = (-0.15).sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    // Behavioral Profile Stat Card (if present)
                    val statCard = message.statCard
                    if (statCard != null) {
                        Spacer(Modifier.height(10.dp))
                        AuraStatCard(profile = statCard)
                    }

                    // Embedded Songs: Single SongCard or MixCard Carousel
                    if (message.resolvedSongs.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        if (message.resolvedSongs.size == 1) {
                            val song = message.resolvedSongs.first()
                            val reason = message.songReasons[song.videoId]
                            val isThisSongPlaying = isPlaying && currentPlayingSong?.videoId == song.videoId
                            AuraSongCard(
                                song = song,
                                reason = reason,
                                isPlaying = isThisSongPlaying,
                                onPlay = onPlaySong,
                                onQueue = onQueueSong,
                            )
                        } else {
                            AuraMixCard(
                                songs = message.resolvedSongs,
                                reasons = message.songReasons,
                                onPlaySong = onPlaySong,
                                onPlayAll = onPlayMix,
                                onQueueAll = onQueueMix,
                            )
                        }
                    }

                    // Social Links for Kami Likhith if mentioned
                    val textLower = message.text.lowercase()
                    if ("kami likhith" in textLower || "likhith" in textLower || "developer" in textLower) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            DeveloperSocialPill(
                                label = "GitHub @likhith3035",
                                onClick = { openUrlSafely("https://github.com/likhith3035") },
                            )
                            DeveloperSocialPill(
                                label = "LinkedIn",
                                onClick = { openUrlSafely("https://linkedin.com/in/likhith-kami") },
                            )
                            DeveloperSocialPill(
                                label = "Instagram",
                                onClick = { openUrlSafely("https://www.instagram.com/lucky__likhith") },
                            )
                        }
                    }

                    // Action Chips (e.g. Open EQ, Start Party, View Replay)
                    if (message.actionChips.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            message.actionChips.forEach { action ->
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(percent = 50))
                                        .clickable { onActionClick(action) },
                                ) {
                                    Text(
                                        text = action.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
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

@Composable
private fun DeveloperSocialPill(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/**
 * Comprehensive in-chat markdown formatter.
 * Handles bold text, inline code spans, clean bullet glyphs, header sanitization,
 * and markdown table normalization with dynamic theme color adaptation.
 */
private fun formatMarkdown(
    rawText: String,
    textColor: Color,
    codeBg: Color,
    codeColor: Color,
): AnnotatedString {
    // 1. Preprocess lines: normalize bullets, headers, and sanitize tables
    val cleanedLines = rawText.lines().mapNotNull { rawLine ->
        val trimmed = rawLine.trim()
        when {
            // Filter out table separators like |---|---|
            trimmed.startsWith("|") && trimmed.contains("---") -> null
            // Convert table rows | a | b | into clean bullet items
            trimmed.startsWith("|") && trimmed.endsWith("|") -> {
                val cells = trimmed.trim('|').split("|").map { it.trim() }.filter { it.isNotBlank() }
                if (cells.isNotEmpty()) "• " + cells.joinToString(" · ") else null
            }
            // Normalize headers: ### Title -> **Title**
            trimmed.startsWith("###") -> "**" + trimmed.removePrefix("###").trim() + "**"
            trimmed.startsWith("##") -> "**" + trimmed.removePrefix("##").trim() + "**"
            trimmed.startsWith("#") -> "**" + trimmed.removePrefix("#").trim() + "**"
            // Normalize bullets: * text or - text -> • text
            trimmed.startsWith("* ") -> "• " + trimmed.removePrefix("* ").trim()
            trimmed.startsWith("- ") -> "• " + trimmed.removePrefix("- ").trim()
            else -> rawLine
        }
    }
    val preprocessed = cleanedLines.joinToString("\n")

    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < preprocessed.length) {
        // Check for bold **
        if (preprocessed.startsWith("**", i)) {
            val end = preprocessed.indexOf("**", i + 2)
            if (end != -1) {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor))
                builder.append(preprocessed.substring(i + 2, end))
                builder.pop()
                i = end + 2
                continue
            }
        }
        // Check for inline code `
        if (preprocessed[i] == '`') {
            val end = preprocessed.indexOf('`', i + 1)
            if (end != -1) {
                builder.pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = codeColor,
                        background = codeBg,
                    ),
                )
                builder.append(preprocessed.substring(i + 1, end))
                builder.pop()
                i = end + 1
                continue
            }
        }
        builder.append(preprocessed[i])
        i++
    }
    return builder.toAnnotatedString()
}
