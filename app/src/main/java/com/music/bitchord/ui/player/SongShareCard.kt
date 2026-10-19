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
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.music.bitchord.R
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.replay.Fonts
import com.music.bitchord.ui.replay.drawArtwork
import com.music.bitchord.ui.replay.drawBackdrop
import com.music.bitchord.ui.replay.ellipsised
import com.music.bitchord.ui.replay.loadBitmap
import com.music.bitchord.ui.replay.paletteOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

/**
 * Visual mode for the share card: full song presentation or lyrics snippet.
 * Preserved for backward compatibility.
 */
enum class SongShareMode {
    SONG,
    LYRICS,
}

/**
 * Card layout and visual presentation template.
 */
enum class SongShareTemplate {
    GLASS_STORY,
    VINYL_RECORD,
    RETRO_CASSETTE,
    EDITORIAL_LYRICS,
    BENTO_SQUARE,
}

/**
 * Aspect ratio of the generated share card.
 */
enum class SongShareAspectRatio {
    STORY_9_16,
    SQUARE_1_1,
}

/**
 * Visualizer animation style drawn on the share card.
 */
enum class SongShareVisualizer {
    BARS,
    SINE_WAVE,
    PULSE_DOTS,
    OFF,
}

/**
 * Color theme for the share card background.
 */
enum class SongSharePalette {
    DYNAMIC,
    VIBRANT,
    OBSIDIAN,
    AURA_CYAN,
    SUNSET_GOLD,
}

private const val CARD_W = 1080
private const val MARGIN_X = 96f

/**
 * Renders a full high-resolution Story/Square Card for sharing on Instagram, WhatsApp, Snapchat, etc.
 */
suspend fun renderSongShareCard(
    context: Context,
    song: Song,
    template: SongShareTemplate = SongShareTemplate.GLASS_STORY,
    palette: SongSharePalette = SongSharePalette.DYNAMIC,
    aspectRatio: SongShareAspectRatio = SongShareAspectRatio.STORY_9_16,
    visualizer: SongShareVisualizer = SongShareVisualizer.BARS,
    lyricsLines: List<String> = emptyList(),
): Bitmap = withContext(Dispatchers.Default) {
    val cardW = CARD_W
    val cardH = if (aspectRatio == SongShareAspectRatio.SQUARE_1_1) 1080 else 1920
    val type = Fonts(context)
    val cover = song.thumbnailUrl?.let { loadBitmap(context, it) }

    val bitmap = Bitmap.createBitmap(cardW, cardH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 1. Draw Background according to selected palette
    drawBackground(canvas, cover, palette, cardW.toFloat(), cardH.toFloat())

    // 2. Draw Content based on selected template
    when (template) {
        SongShareTemplate.GLASS_STORY -> {
            drawGlassCardContent(canvas, context, type, song, cover, cardW.toFloat(), cardH.toFloat(), visualizer)
        }
        SongShareTemplate.VINYL_RECORD -> {
            drawVinylTemplate(canvas, context, type, song, cover, cardW.toFloat(), cardH.toFloat(), visualizer)
        }
        SongShareTemplate.RETRO_CASSETTE -> {
            drawCassetteTemplate(canvas, context, type, song, cover, cardW.toFloat(), cardH.toFloat(), visualizer)
        }
        SongShareTemplate.EDITORIAL_LYRICS -> {
            val lines = lyricsLines.ifEmpty { listOf(song.title, "by ${song.artist}") }
            drawLyricsTemplate(canvas, context, type, song, cover, lines, cardW.toFloat(), cardH.toFloat())
        }
        SongShareTemplate.BENTO_SQUARE -> {
            drawBentoTemplate(canvas, context, type, song, cover, cardW.toFloat(), cardH.toFloat(), visualizer)
        }
    }

    // 3. Draw Bottom Brand Footer (Optically Centered Frosted Pill Badge)
    drawBrandFooter(canvas, context, type, cardW.toFloat(), cardH.toFloat())

    bitmap
}

/**
 * Overload for backward compatibility with previous SongShareMode callers.
 */
suspend fun renderSongShareCard(
    context: Context,
    song: Song,
    mode: SongShareMode,
    palette: SongSharePalette = SongSharePalette.DYNAMIC,
    lyricsLines: List<String> = emptyList(),
): Bitmap = renderSongShareCard(
    context = context,
    song = song,
    template = if (mode == SongShareMode.LYRICS && lyricsLines.isNotEmpty()) SongShareTemplate.EDITORIAL_LYRICS else SongShareTemplate.GLASS_STORY,
    palette = palette,
    aspectRatio = SongShareAspectRatio.STORY_9_16,
    visualizer = SongShareVisualizer.BARS,
    lyricsLines = lyricsLines,
)

private fun drawBackground(
    canvas: Canvas,
    cover: Bitmap?,
    palette: SongSharePalette,
    cardW: Float,
    cardH: Float,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    when (palette) {
        SongSharePalette.DYNAMIC -> {
            val colors = paletteOf(cover)
            val dominant = colors.firstOrNull() ?: 0xFFB71C1C.toInt()
            canvas.drawColor(dominant)

            // Deep gradient to dark at the bottom for readability
            paint.shader = LinearGradient(
                0f, 0f, 0f, cardH,
                intArrayOf(0x33000000, 0x00000000, 0x99000000.toInt()),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, cardW, cardH, paint)
            paint.shader = null
        }

        SongSharePalette.VIBRANT -> {
            drawBackdrop(canvas, cover, 0f, cardH)
        }

        SongSharePalette.OBSIDIAN -> {
            canvas.drawColor(0xFF090A0E.toInt())
            val colors = paletteOf(cover)
            val tint = colors.firstOrNull() ?: 0xFF818CF8.toInt()

            val cx = cardW / 2f
            val cy = cardH * 0.42f
            val radius = cardW * 0.85f
            paint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(
                    ColorUtils.setAlphaComponent(tint, 140),
                    ColorUtils.setAlphaComponent(tint, 35),
                    0x00000000,
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
        }

        SongSharePalette.AURA_CYAN -> {
            canvas.drawColor(0xFF060914.toInt())
            // Top-left cyan halo
            paint.shader = RadialGradient(
                cardW * 0.25f, cardH * 0.30f, cardW * 0.9f,
                intArrayOf(0x9900E5FF.toInt(), 0x3300E5FF.toInt(), 0x00000000),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cardW * 0.25f, cardH * 0.30f, cardW * 0.9f, paint)

            // Bottom-right purple ambient glow
            paint.shader = RadialGradient(
                cardW * 0.8f, cardH * 0.65f, cardW * 0.85f,
                intArrayOf(0x88A855F7.toInt(), 0x22A855F7.toInt(), 0x00000000),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cardW * 0.8f, cardH * 0.65f, cardW * 0.85f, paint)
            paint.shader = null
        }

        SongSharePalette.SUNSET_GOLD -> {
            canvas.drawColor(0xFF140807.toInt())
            // Top-center gold glow
            paint.shader = RadialGradient(
                cardW * 0.35f, cardH * 0.32f, cardW * 0.85f,
                intArrayOf(0x99FFA000.toInt(), 0x33FFA000.toInt(), 0x00000000),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cardW * 0.35f, cardH * 0.32f, cardW * 0.85f, paint)

            // Bottom-center crimson ambient
            paint.shader = RadialGradient(
                cardW * 0.75f, cardH * 0.66f, cardW * 0.85f,
                intArrayOf(0x88E53935.toInt(), 0x22E53935.toInt(), 0x00000000),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cardW * 0.75f, cardH * 0.66f, cardW * 0.85f, paint)
            paint.shader = null
        }
    }
}

private fun drawGlassCardContent(
    canvas: Canvas,
    context: Context,
    type: Fonts,
    song: Song,
    cover: Bitmap?,
    cardW: Float,
    cardH: Float,
    visualizer: SongShareVisualizer,
) {
    val isStory = cardH > 1200f
    val cardLeft = if (isStory) MARGIN_X else 80f
    val cardRight = cardW - cardLeft
    val cardTop = if (isStory) 280f else 80f
    val cardBottom = if (isStory) 1620f else cardH - 180f
    val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
    val cardRadius = 44f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    // Dark glass card fill with drop shadow
    paint.color = 0x24000000
    canvas.drawRoundRect(RectF(cardLeft, cardTop + 14f, cardRight, cardBottom + 14f), cardRadius, cardRadius, paint)
    paint.color = 0x28FFFFFF
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, paint)

    // Border stroke
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2.5f
    paint.color = 0x26FFFFFF
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, paint)
    paint.style = Paint.Style.FILL

    // Cover Art
    val artPadding = if (isStory) 52f else 40f
    val artX = cardLeft + artPadding
    val artY = cardTop + artPadding
    val artSize = if (isStory) (cardRight - cardLeft) - (artPadding * 2) else 440f

    drawArtwork(canvas, cover, song.title, artX, artY, artSize, false)

    // Song Title
    val titleY = artY + artSize + (if (isStory) 100f else 64f)
    val titlePaint = type.heading(if (isStory) 54f else 42f, Color.WHITE)
    val maxTextW = if (isStory) artSize else (cardRight - artX - 30f)
    val titleText = ellipsised(song.title, titlePaint, maxTextW)
    canvas.drawText(titleText, artX, titleY, titlePaint)

    // Artist
    val artistY = titleY + (if (isStory) 64f else 48f)
    val artistPaint = type.label(if (isStory) 36f else 28f, 0xD9FFFFFF.toInt())
    val artistText = ellipsised(song.artist, artistPaint, maxTextW)
    canvas.drawText(artistText, artX, artistY, artistPaint)

    // Soundwave visualizer
    val waveY = artistY + (if (isStory) 50f else 36f)
    drawVisualizer(canvas, visualizer, artX, waveY + 28f, maxTextW)
}

private fun drawVinylTemplate(
    canvas: Canvas,
    context: Context,
    type: Fonts,
    song: Song,
    cover: Bitmap?,
    cardW: Float,
    cardH: Float,
    visualizer: SongShareVisualizer,
) {
    val isStory = cardH > 1200f
    val cy = if (isStory) cardH * 0.40f else cardH * 0.42f
    val discRadius = if (isStory) 340f else 250f
    val discCx = cardW * 0.60f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 1. Vinyl Record Disc (Slipping out to the right)
    // Disc body
    paint.color = 0xFF111215.toInt()
    canvas.drawCircle(discCx, cy, discRadius, paint)

    // Vinyl Grooves (concentric circular tracks)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.3f
    val grooveRatios = floatArrayOf(0.94f, 0.88f, 0.82f, 0.74f, 0.66f, 0.58f)
    grooveRatios.forEach { ratio ->
        paint.color = 0x1AFFFFFF
        canvas.drawCircle(discCx, cy, discRadius * ratio, paint)
    }

    // Specular light reflection on vinyl
    paint.shader = RadialGradient(
        discCx, cy, discRadius,
        intArrayOf(0x00FFFFFF, 0x22FFFFFF, 0x00FFFFFF),
        floatArrayOf(0.4f, 0.7f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawCircle(discCx, cy, discRadius, paint)
    paint.shader = null
    paint.style = Paint.Style.FILL

    // Center Album Art Label on vinyl
    val labelRadius = discRadius * 0.35f
    val labelBounds = RectF(discCx - labelRadius, cy - labelRadius, discCx + labelRadius, cy + labelRadius)
    if (cover != null) {
        val matrix = Matrix().apply {
            val scale = (labelRadius * 2) / minOf(cover.width, cover.height).toFloat()
            setScale(scale, scale)
            postTranslate(
                discCx - (cover.width * scale) / 2f,
                cy - (cover.height * scale) / 2f,
            )
        }
        paint.shader = BitmapShader(cover, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(matrix)
        }
        canvas.drawCircle(discCx, cy, labelRadius, paint)
        paint.shader = null
    } else {
        paint.color = 0xFF333333.toInt()
        canvas.drawCircle(discCx, cy, labelRadius, paint)
    }

    // Spindle Center Hole
    paint.color = 0xFF050505.toInt()
    canvas.drawCircle(discCx, cy, 18f, paint)

    // 2. Album Jacket / Sleeve (Front overlapping)
    val sleeveSize = if (isStory) 540f else 430f
    val sleeveLeft = cardW * 0.08f
    val sleeveTop = cy - sleeveSize / 2f

    // Sleeve shadow
    paint.color = 0x55000000
    canvas.drawRoundRect(
        RectF(sleeveLeft + 10f, sleeveTop + 14f, sleeveLeft + sleeveSize + 10f, sleeveTop + sleeveSize + 14f),
        24f, 24f, paint,
    )
    drawArtwork(canvas, cover, song.title, sleeveLeft, sleeveTop, sleeveSize, false)

    // 3. Typography Below
    val textTop = cy + sleeveSize / 2f + (if (isStory) 90f else 44f)
    val textLeft = sleeveLeft + 8f
    val maxTextW = cardW - (textLeft * 2)

    // "NOW SPINNING" badge
    val badgePaint = type.label(22f, 0xCC00E5FF.toInt())
    canvas.drawText("NOW SPINNING • AURA VINYL", textLeft, textTop, badgePaint)

    // Title
    val titleY = textTop + (if (isStory) 68f else 50f)
    val titlePaint = type.heading(if (isStory) 52f else 40f, Color.WHITE)
    canvas.drawText(ellipsised(song.title, titlePaint, maxTextW), textLeft, titleY, titlePaint)

    // Artist
    val artistY = titleY + (if (isStory) 60f else 44f)
    val artistPaint = type.label(if (isStory) 34f else 26f, 0xD9FFFFFF.toInt())
    canvas.drawText(ellipsised(song.artist, artistPaint, maxTextW), textLeft, artistY, artistPaint)

    // Visualizer
    val waveY = artistY + (if (isStory) 48f else 32f)
    drawVisualizer(canvas, visualizer, textLeft, waveY + 25f, maxTextW)
}

private fun drawCassetteTemplate(
    canvas: Canvas,
    context: Context,
    type: Fonts,
    song: Song,
    cover: Bitmap?,
    cardW: Float,
    cardH: Float,
    visualizer: SongShareVisualizer,
) {
    val isStory = cardH > 1200f
    val cy = if (isStory) cardH * 0.42f else cardH * 0.44f
    val cassetteW = cardW - 140f
    val cassetteH = if (isStory) 560f else 460f
    val left = (cardW - cassetteW) / 2f
    val top = cy - cassetteH / 2f
    val right = left + cassetteW
    val bottom = top + cassetteH

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 1. Cassette Shell (Smoky translucent body with border)
    paint.color = 0x40000000.toInt()
    canvas.drawRoundRect(RectF(left, top, right, bottom), 32f, 32f, paint)

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3f
    paint.color = 0x33FFFFFF
    canvas.drawRoundRect(RectF(left, top, right, bottom), 32f, 32f, paint)
    paint.style = Paint.Style.FILL

    // Screws in 4 corners
    val screwInset = 28f
    val screwR = 8f
    paint.color = 0x55FFFFFF
    val corners = listOf(
        Pair(left + screwInset, top + screwInset),
        Pair(right - screwInset, top + screwInset),
        Pair(left + screwInset, bottom - screwInset),
        Pair(right - screwInset, bottom - screwInset),
    )
    corners.forEach { (sx, sy) ->
        canvas.drawCircle(sx, sy, screwR, paint)
    }

    // 2. Center Mixtape Label Sticker
    val labelMarginH = 40f
    val labelTop = top + 44f
    val labelH = cassetteH * 0.50f
    val labelRect = RectF(left + labelMarginH, labelTop, right - labelMarginH, labelTop + labelH)

    paint.color = 0xD9F3F4F6.toInt()
    canvas.drawRoundRect(labelRect, 16f, 16f, paint)

    // Header strip on sticker: SIDE A • STEREO
    val labelHeaderPaint = type.label(20f, 0xFF4B5563.toInt())
    canvas.drawText("SIDE A  •  STEREO HI-FI", left + labelMarginH + 24f, labelTop + 36f, labelHeaderPaint)

    // Song Title & Artist stamped on sticker
    val labelTitlePaint = type.heading(34f, 0xFF111827.toInt())
    val maxLabelTextW = labelRect.width() - 48f
    canvas.drawText(ellipsised(song.title, labelTitlePaint, maxLabelTextW), left + labelMarginH + 24f, labelTop + 86f, labelTitlePaint)

    val labelArtistPaint = type.body(26f, 0xFF4B5563.toInt(), bold = true)
    canvas.drawText(ellipsised(song.artist, labelArtistPaint, maxLabelTextW), left + labelMarginH + 24f, labelTop + 130f, labelArtistPaint)

    // 3. Tape Window & Spools in bottom half
    val windowW = cassetteW * 0.65f
    val windowH = cassetteH * 0.28f
    val windowLeft = (cardW - windowW) / 2f
    val windowTop = labelTop + labelH + 16f
    val windowRect = RectF(windowLeft, windowTop, windowLeft + windowW, windowTop + windowH)

    // Dark tape window
    paint.color = 0x55000000
    canvas.drawRoundRect(windowRect, 12f, 12f, paint)

    // Spools (Left and Right gear hubs)
    val spoolR = windowH * 0.36f
    val spoolY = windowTop + windowH / 2f
    val spoolX1 = windowLeft + 70f
    val spoolX2 = windowLeft + windowW - 70f

    paint.color = 0xCCFFFFFF.toInt()
    canvas.drawCircle(spoolX1, spoolY, spoolR, paint)
    canvas.drawCircle(spoolX2, spoolY, spoolR, paint)

    // Inner spool hole
    paint.color = 0xFF1A1C22.toInt()
    canvas.drawCircle(spoolX1, spoolY, spoolR * 0.45f, paint)
    canvas.drawCircle(spoolX2, spoolY, spoolR * 0.45f, paint)

    // Visualizer inside tape window
    drawVisualizer(canvas, visualizer, spoolX1 + spoolR + 15f, spoolY, spoolX2 - spoolX1 - (spoolR * 2) - 30f)

    // 4. Large Digital Metadata Display below cassette
    val textY = bottom + (if (isStory) 100f else 54f)
    val titlePaint = type.heading(if (isStory) 52f else 40f, Color.WHITE)
    val maxTextW = cardW - 160f
    canvas.drawText(ellipsised(song.title, titlePaint, maxTextW), 80f, textY, titlePaint)

    val artistPaint = type.label(if (isStory) 34f else 26f, 0xD9FFFFFF.toInt())
    canvas.drawText(ellipsised(song.artist, artistPaint, maxTextW), 80f, textY + (if (isStory) 58f else 44f), artistPaint)
}

private fun drawLyricsTemplate(
    canvas: Canvas,
    context: Context,
    type: Fonts,
    song: Song,
    cover: Bitmap?,
    lines: List<String>,
    cardW: Float,
    cardH: Float,
) {
    val isStory = cardH > 1200f
    val cardLeft = if (isStory) MARGIN_X else 80f
    val cardRight = cardW - cardLeft
    val cardTop = if (isStory) 240f else 80f
    val cardBottom = if (isStory) 1640f else cardH - 180f
    val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
    val cardRadius = 44f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = 0x33000000
    canvas.drawRoundRect(cardRect, cardRadius, cardRadius, paint)

    // Mini header: cover art (130x130) + Title + Artist
    val miniArtSize = if (isStory) 130f else 100f
    val headerX = cardLeft + 50f
    val headerY = cardTop + 50f
    drawArtwork(canvas, cover, song.title, headerX, headerY, miniArtSize, false)

    val metaX = headerX + miniArtSize + 28f
    val maxMetaW = cardRight - 50f - metaX

    val titlePaint = type.heading(if (isStory) 36f else 28f, Color.WHITE)
    canvas.drawText(ellipsised(song.title, titlePaint, maxMetaW), metaX, headerY + (miniArtSize * 0.40f), titlePaint)

    val artistPaint = type.label(if (isStory) 28f else 22f, 0xB3FFFFFF.toInt())
    canvas.drawText(ellipsised(song.artist, artistPaint, maxMetaW), metaX, headerY + (miniArtSize * 0.82f), artistPaint)

    // Giant opening quotation mark
    val quotePaint = type.heading(if (isStory) 110f else 80f, 0x4D00E5FF.toInt())
    val quoteY = headerY + miniArtSize + (if (isStory) 90f else 64f)
    canvas.drawText("“", headerX, quoteY, quotePaint)

    // Lyrics lines in display type
    val lyricsTop = quoteY + 30f
    val lyricPaint = type.heading(if (isStory) 54f else 38f, Color.WHITE)
    val maxLyricW = (cardRight - cardLeft) - 100f

    var currentY = lyricsTop
    val displayLines = lines.take(if (isStory) 6 else 4)
    val lineSpacing = if (isStory) 82f else 54f
    for (line in displayLines) {
        val trimmed = ellipsised(line, lyricPaint, maxLyricW)
        canvas.drawText(trimmed, headerX, currentY, lyricPaint)
        currentY += lineSpacing
    }
}

private fun drawBentoTemplate(
    canvas: Canvas,
    context: Context,
    type: Fonts,
    song: Song,
    cover: Bitmap?,
    cardW: Float,
    cardH: Float,
    visualizer: SongShareVisualizer,
) {
    val isStory = cardH > 1200f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    if (isStory) {
        // Bento Box 1: Album Art Hero Tile (Top)
        val artSize = cardW - 192f
        val artX = 96f
        val artY = 260f
        drawArtwork(canvas, cover, song.title, artX, artY, artSize, false)

        // Bento Box 2: Metadata Tile (Middle)
        val metaTop = artY + artSize + 28f
        val metaH = 220f
        val metaRect = RectF(artX, metaTop, artX + artSize, metaTop + metaH)
        paint.color = 0x2A000000
        canvas.drawRoundRect(metaRect, 32f, 32f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = 0x24FFFFFF
        canvas.drawRoundRect(metaRect, 32f, 32f, paint)
        paint.style = Paint.Style.FILL

        val titlePaint = type.heading(48f, Color.WHITE)
        val maxW = artSize - 60f
        canvas.drawText(ellipsised(song.title, titlePaint, maxW), artX + 30f, metaTop + 80f, titlePaint)

        val artistPaint = type.label(32f, 0xD9FFFFFF.toInt())
        canvas.drawText(ellipsised(song.artist, artistPaint, maxW), artX + 30f, metaTop + 140f, artistPaint)

        // Bento Box 3: Audio Spectrum Tile (Bottom)
        val specTop = metaTop + metaH + 24f
        val specH = 160f
        val specRect = RectF(artX, specTop, artX + artSize, specTop + specH)
        paint.color = 0x2A000000
        canvas.drawRoundRect(specRect, 32f, 32f, paint)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(specRect, 32f, 32f, paint)
        paint.style = Paint.Style.FILL

        drawVisualizer(canvas, visualizer, artX + 36f, specTop + specH / 2f, maxW)
    } else {
        // 1:1 Square Bento Layout
        val left = 80f
        val top = 80f
        val artSize = 440f
        drawArtwork(canvas, cover, song.title, left, top, artSize, false)

        // Right Column: Info & Audio tiles
        val rightLeft = left + artSize + 28f
        val rightW = cardW - rightLeft - 80f

        // Tile A: Song Info
        val infoH = 240f
        val infoRect = RectF(rightLeft, top, rightLeft + rightW, top + infoH)
        paint.color = 0x2A000000
        canvas.drawRoundRect(infoRect, 28f, 28f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = 0x24FFFFFF
        canvas.drawRoundRect(infoRect, 28f, 28f, paint)
        paint.style = Paint.Style.FILL

        val titlePaint = type.heading(36f, Color.WHITE)
        canvas.drawText(ellipsised(song.title, titlePaint, rightW - 40f), rightLeft + 24f, top + 74f, titlePaint)

        val artistPaint = type.label(26f, 0xD9FFFFFF.toInt())
        canvas.drawText(ellipsised(song.artist, artistPaint, rightW - 40f), rightLeft + 24f, top + 130f, artistPaint)

        // Tile B: Visualizer Box
        val waveTop = top + infoH + 20f
        val waveH = artSize - infoH - 20f
        val waveRect = RectF(rightLeft, waveTop, rightLeft + rightW, waveTop + waveH)
        paint.color = 0x2A000000
        canvas.drawRoundRect(waveRect, 28f, 28f, paint)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(waveRect, 28f, 28f, paint)
        paint.style = Paint.Style.FILL

        drawVisualizer(canvas, visualizer, rightLeft + 28f, waveTop + waveH / 2f, rightW - 56f)
    }
}

private fun drawVisualizer(
    canvas: Canvas,
    visualizer: SongShareVisualizer,
    startX: Float,
    centerY: Float,
    availableWidth: Float,
) {
    when (visualizer) {
        SongShareVisualizer.OFF -> Unit
        SongShareVisualizer.BARS -> {
            val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xAAFFFFFF.toInt()
                strokeCap = Paint.Cap.ROUND
                strokeWidth = 6.5f
            }
            val barHeights = floatArrayOf(
                16f, 28f, 44f, 62f, 38f, 54f, 75f, 48f, 32f, 68f, 85f, 52f, 36f, 60f, 42f, 22f, 34f,
            )
            val barGap = 20f
            barHeights.forEachIndexed { i, h ->
                val bx = startX + (i * barGap)
                if (bx <= startX + availableWidth) {
                    canvas.drawLine(bx, centerY - h / 2f, bx, centerY + h / 2f, wavePaint)
                }
            }
        }
        SongShareVisualizer.SINE_WAVE -> {
            val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xDD00E5FF.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 4.5f
                strokeCap = Paint.Cap.ROUND
            }
            val path = android.graphics.Path()
            val totalPoints = 30
            val maxW = minOf(availableWidth, 340f)
            val step = maxW / totalPoints
            for (i in 0..totalPoints) {
                val x = startX + i * step
                val sinVal = sin(i * 0.40) * sin(i * 0.18)
                val y = centerY + (sinVal * 26f).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            canvas.drawPath(path, wavePaint)
        }
        SongShareVisualizer.PULSE_DOTS -> {
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xCCFFFFFF.toInt()
            }
            val dotR = 4f
            val dotGap = 22f
            val dotPattern = intArrayOf(2, 4, 6, 8, 7, 5, 8, 9, 6, 4, 7, 5, 3)
            dotPattern.forEachIndexed { col, numDots ->
                val colX = startX + col * dotGap
                if (colX <= startX + availableWidth) {
                    val startDotY = centerY - (numDots - 1) * 7f
                    for (d in 0 until numDots) {
                        val dotY = startDotY + d * 14f
                        canvas.drawCircle(colX, dotY, dotR, dotPaint)
                    }
                }
            }
        }
    }
}

/**
 * Renders an optically-centered frosted pill brand badge:
 * [ (Aura Waves) AuraMusic ]
 * Perfect baseline and visual centering, fixing the misaligned raw vector issue.
 */
private fun drawBrandFooter(
    canvas: Canvas,
    context: Context,
    type: Fonts,
    cardW: Float,
    cardH: Float,
) {
    val footerCenterY = cardH - 105f
    val brandPaint = type.heading(28f, Color.WHITE).apply {
        letterSpacing = 0.04f
    }
    val byPaint = type.label(21f, 0xB3FFFFFF.toInt()).apply {
        letterSpacing = 0.02f
    }
    val brandText = "AuraMusic"
    val byText = " by Likki"
    val brandWidth = brandPaint.measureText(brandText)
    val byWidth = byPaint.measureText(byText)
    val textWidth = brandWidth + byWidth
    val fontMetrics = brandPaint.fontMetrics

    // Authentic Aura Logo aspect ratio (730 x 484)
    val logoH = 30f
    val logoW = logoH * (730f / 484f) // ~45f
    val pillPaddingH = 26f
    val pillPaddingV = 13f
    val iconGap = 16f

    val pillWidth = pillPaddingH * 2 + logoW + iconGap + textWidth
    val pillHeight = logoH + pillPaddingV * 2 // ~56f
    val pillRadius = pillHeight / 2f

    val pillLeft = (cardW - pillWidth) / 2f
    val pillTop = footerCenterY - pillHeight / 2f
    val pillRect = RectF(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight)

    // Frosted Pill Backdrop
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x3D000000.toInt()
    }
    canvas.drawRoundRect(pillRect, pillRadius, pillRadius, bgPaint)

    // Frosted Pill Border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.2f
        color = 0x33FFFFFF
    }
    canvas.drawRoundRect(pillRect, pillRadius, pillRadius, borderPaint)

    // Logo vertically centered in pill
    val logoLeft = pillLeft + pillPaddingH
    val logoTop = pillTop + pillPaddingV
    val logoDrawable = runCatching {
        ResourcesCompat.getDrawable(context.resources, R.drawable.ic_logo, null)
    }.getOrNull()
    if (logoDrawable != null) {
        logoDrawable.setTint(0xFFFFFFFF.toInt())
        logoDrawable.setBounds(logoLeft.toInt(), logoTop.toInt(), (logoLeft + logoW).toInt(), (logoTop + logoH).toInt())
        logoDrawable.draw(canvas)
    }

    // Text vertically centered: baseline = center - (ascent + descent) / 2f
    val textBaseline = footerCenterY - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText(brandText, logoLeft + logoW + iconGap, textBaseline, brandPaint)
    canvas.drawText(byText, logoLeft + logoW + iconGap + brandWidth, textBaseline, byPaint)
}
