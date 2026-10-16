package com.music.bitchord.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.replay.Fonts
import com.music.bitchord.ui.replay.drawArtwork
import com.music.bitchord.ui.replay.drawBackdrop
import com.music.bitchord.ui.replay.drawLogo
import com.music.bitchord.ui.replay.ellipsised
import com.music.bitchord.ui.replay.loadBitmap
import com.music.bitchord.ui.replay.paletteOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Visual mode for the share card: full song presentation or lyrics snippet.
 */
enum class SongShareMode {
    SONG,
    LYRICS,
}

/**
 * Color theme for the share card background.
 */
enum class SongSharePalette {
    DYNAMIC,
    VIBRANT,
    OBSIDIAN,
    AURA_CYAN,
}

private const val CARD_W = 1080
private const val CARD_H = 1920
private const val MARGIN_X = 96f

/**
 * Renders a full 1080×1920 9:16 Story Card for sharing on Instagram, WhatsApp, Snapchat, etc.
 */
suspend fun renderSongShareCard(
    context: Context,
    song: Song,
    mode: SongShareMode = SongShareMode.SONG,
    palette: SongSharePalette = SongSharePalette.DYNAMIC,
    lyricsLines: List<String> = emptyList(),
): Bitmap = withContext(Dispatchers.Default) {
    val type = Fonts(context)
    val cover = song.thumbnailUrl?.let { loadBitmap(context, it) }

    val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 1. Draw Background according to selected palette
    drawBackground(canvas, cover, palette)

    // 2. Draw Content based on Mode
    if (mode == SongShareMode.SONG || lyricsLines.isEmpty()) {
        drawSongCardContent(canvas, context, type, song, cover)
    } else {
        drawLyricsCardContent(canvas, context, type, song, cover, lyricsLines)
    }

    // 3. Draw Bottom Brand Footer
    drawFooter(canvas, context, type)

    bitmap
}

private fun drawBackground(
    canvas: Canvas,
    cover: Bitmap?,
    palette: SongSharePalette,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    when (palette) {
        SongSharePalette.DYNAMIC -> {
            val colors = paletteOf(cover)
            val dominant = colors.firstOrNull() ?: 0xFFB71C1C.toInt()
            canvas.drawColor(dominant)

            // Deep gradient to dark at the bottom for readability
            paint.shader = LinearGradient(
                0f, 0f, 0f, CARD_H.toFloat(),
                intArrayOf(0x33000000, 0x00000000, 0x99000000.toInt()),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, CARD_W.toFloat(), CARD_H.toFloat(), paint)
            paint.shader = null
        }

        SongSharePalette.VIBRANT -> {
            drawBackdrop(canvas, cover, 0f, CARD_H.toFloat())
        }

        SongSharePalette.OBSIDIAN -> {
            canvas.drawColor(0xFF0C0D12.toInt())
            val colors = paletteOf(cover)
            val tint = colors.firstOrNull() ?: 0xFF818CF8.toInt()

            val cx = CARD_W / 2f
            val cy = CARD_H * 0.44f
            val radius = CARD_W * 0.85f
            paint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(
                    ColorUtils.setAlphaComponent(tint, 140),
                    ColorUtils.setAlphaComponent(tint, 40),
                    0x00000000,
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
        }

        SongSharePalette.AURA_CYAN -> {
            canvas.drawColor(0xFF070B18.toInt())
            // Top-left cyan halo
            paint.shader = RadialGradient(
                CARD_W * 0.25f, CARD_H * 0.32f, CARD_W * 0.9f,
                intArrayOf(0x9900E5FF.toInt(), 0x3300E5FF.toInt(), 0x00000000),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(CARD_W * 0.25f, CARD_H * 0.32f, CARD_W * 0.9f, paint)

            // Bottom-right purple ambient glow
            paint.shader = RadialGradient(
                CARD_W * 0.8f, CARD_H * 0.65f, CARD_W * 0.85f,
                intArrayOf(0x88A855F7.toInt(), 0x22A855F7.toInt(), 0x00000000),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(CARD_W * 0.8f, CARD_H * 0.65f, CARD_W * 0.85f, paint)
            paint.shader = null
        }
    }
}

private fun drawSongCardContent(
    canvas: Canvas,
    context: Context,
    type: Fonts,
    song: Song,
    cover: Bitmap?,
) {
    // Inner elevated card container
    val cardLeft = MARGIN_X
    val cardRight = CARD_W - MARGIN_X
    val cardTop = 320f
    val cardBottom = 1600f
    val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
    val cardRadius = 48f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Dark glass card fill with subtle drop shadow
    paint.color = 0x24000000
    canvas.drawRoundRect(RectF(cardLeft, cardTop + 14f, cardRight, cardBottom + 14f), cardRadius, cardRadius, paint)
    paint.color = 0x2DFFFFFF
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, paint)

    // Large Cover Art inside card
    val artPadding = 56f
    val artX = cardLeft + artPadding
    val artY = cardTop + artPadding
    val artSize = (cardRight - cardLeft) - (artPadding * 2)

    drawArtwork(canvas, cover, song.title, artX, artY, artSize, false)

    // Song Title
    val titleY = artY + artSize + 110f
    val titlePaint = type.heading(56f, Color.WHITE)
    val maxTextW = artSize
    val titleText = ellipsised(song.title, titlePaint, maxTextW)
    canvas.drawText(titleText, artX, titleY, titlePaint)

    // Artist
    val artistY = titleY + 68f
    val artistPaint = type.label(38f, 0xD9FFFFFF.toInt())
    val artistText = ellipsised(song.artist, artistPaint, maxTextW)
    canvas.drawText(artistText, artX, artistY, artistPaint)

    // Soundwave visualizer bars
    val waveY = artistY + 54f
    val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x8AFFFFFF.toInt()
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 6.5f
    }
    val barHeights = floatArrayOf(
        16f, 28f, 44f, 62f, 38f, 54f, 75f, 48f, 32f, 68f, 85f, 52f, 36f, 60f, 42f, 22f, 34f,
    )
    val barGap = 20f
    val totalW = (barHeights.size - 1) * barGap
    barHeights.forEachIndexed { i, h ->
        val bx = artX + (i * barGap)
        val cy = waveY + 45f
        canvas.drawLine(bx, cy - h / 2f, bx, cy + h / 2f, wavePaint)
    }
}

private fun drawLyricsCardContent(
    canvas: Canvas,
    context: Context,
    type: Fonts,
    song: Song,
    cover: Bitmap?,
    lines: List<String>,
) {
    val cardLeft = MARGIN_X
    val cardRight = CARD_W - MARGIN_X
    val cardTop = 280f
    val cardBottom = 1620f
    val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
    val cardRadius = 48f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = 0x33000000
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, paint)

    // Mini header: cover art (140x140) + Title + Artist
    val miniArtSize = 140f
    val headerX = cardLeft + 54f
    val headerY = cardTop + 54f
    drawArtwork(canvas, cover, song.title, headerX, headerY, miniArtSize, false)

    val metaX = headerX + miniArtSize + 32f
    val maxMetaW = cardRight - 54f - metaX

    val titlePaint = type.heading(38f, Color.WHITE)
    canvas.drawText(ellipsised(song.title, titlePaint, maxMetaW), metaX, headerY + 55f, titlePaint)

    val artistPaint = type.label(28f, 0xB3FFFFFF.toInt())
    canvas.drawText(ellipsised(song.artist, artistPaint, maxMetaW), metaX, headerY + 105f, artistPaint)

    // Lyrics lines in prominent display type
    val lyricsTop = headerY + miniArtSize + 110f
    val lyricPaint = type.heading(58f, Color.WHITE)
    val maxLyricW = (cardRight - cardLeft) - 108f

    var currentY = lyricsTop
    val displayLines = lines.take(6)
    for (line in displayLines) {
        val trimmed = ellipsised(line, lyricPaint, maxLyricW)
        canvas.drawText(trimmed, cardLeft + 54f, currentY, lyricPaint)
        currentY += 88f
    }
}

private fun drawFooter(
    canvas: Canvas,
    context: Context,
    type: Fonts,
) {
    val footerY = 1720f
    val brandPaint = type.heading(38f, 0xE6FFFFFF.toInt()).apply {
        textAlign = Paint.Align.LEFT
    }
    // Logo mark + AuraMusic by Likki wordmark
    val startX = MARGIN_X + 24f
    drawLogo(canvas, context, startX + 66f, footerY)
    canvas.drawText("AuraMusic by Likki", startX + 86f, footerY, brandPaint)
}

