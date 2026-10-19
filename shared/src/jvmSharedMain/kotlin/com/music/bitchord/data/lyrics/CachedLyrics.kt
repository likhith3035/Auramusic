package com.music.bitchord.data.lyrics

import kotlinx.serialization.Serializable

@Serializable
data class CachedLyricSyllable(
    val startMs: Long,
    val endMs: Long,
    val startChar: Int,
    val endChar: Int,
)

@Serializable
data class CachedLyricWord(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val syllables: List<CachedLyricSyllable> = emptyList(),
)

@Serializable
data class CachedLyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<CachedLyricWord> = emptyList(),
    val sungUntilMs: Long? = null,
    val background: CachedLyricLine? = null,
    val alignment: String = "Start",
)

@Serializable
data class CachedLyricsData(
    val version: Int = 1,
    val sourceName: String,
    val lines: List<CachedLyricLine>,
)

fun LyricSyllable.toCached(): CachedLyricSyllable =
    CachedLyricSyllable(startMs, endMs, startChar, endChar)

fun CachedLyricSyllable.toSyllable(): LyricSyllable =
    LyricSyllable(startMs, endMs, startChar, endChar)

fun LyricWord.toCached(): CachedLyricWord =
    CachedLyricWord(startMs, endMs, text, syllables.map { it.toCached() })

fun CachedLyricWord.toWord(): LyricWord =
    LyricWord(startMs, endMs, text, syllables.map { it.toSyllable() })

fun LyricLine.toCached(): CachedLyricLine = CachedLyricLine(
    timeMs = timeMs,
    text = text,
    words = words.map { it.toCached() },
    sungUntilMs = sungUntilMs,
    background = background?.toCached(),
    alignment = alignment.name,
)

fun CachedLyricLine.toLyricLine(): LyricLine = LyricLine(
    timeMs = timeMs,
    text = text,
    words = words.map { it.toWord() },
    sungUntilMs = sungUntilMs,
    background = background?.toLyricLine(),
    alignment = if (alignment == "End") LyricAlignment.End else LyricAlignment.Start,
)

fun LyricsRepository.Result.toCached(): CachedLyricsData = CachedLyricsData(
    sourceName = source.name,
    lines = lines.map { it.toCached() },
)

fun CachedLyricsData.toResult(): LyricsRepository.Result? {
    val src = runCatching { LyricsSource.valueOf(sourceName) }.getOrNull() ?: LyricsSource.LRCLIB
    return LyricsRepository.Result(
        source = src,
        lines = lines.map { it.toLyricLine() },
    )
}
