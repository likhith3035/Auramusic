package com.music.bitchord.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.music.bitchord.ui.replay.Fonts
import com.music.bitchord.ui.replay.drawArtwork
import com.music.bitchord.ui.replay.drawBackdrop
import com.music.bitchord.ui.replay.drawLogo
import com.music.bitchord.ui.replay.ellipsised
import com.music.bitchord.ui.replay.loadBitmap
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The picked lines as one picture, drawn to a bitmap you can send.
 *
 * ## Why this is drawn rather than screenshotted
 *
 * The same reason the Replay is. A screenshot of the lyrics panel carries the
 * scrubber, the glow falloff, whichever lines happened to be on screen and the
 * status bar of that particular phone, so two people sharing the same verse
 * would send two different-looking pictures. A card laid out here is the verse,
 * the sleeve and this app's mark, and nothing else — and it is only as tall as
 * that verse needs, which holds it inside 1080×1920 (9:16): the shape a story
 * or a chat head expects. Keeping it there is the player's job, by refusing
 * more words at the pick rather than by shrinking them here.
 *
 * ## Why it isn't Compose
 *
 * `Bitmap` + `android.graphics.Canvas`, exactly as
 * [com.music.bitchord.ui.replay.ReplayPoster] does it, and for the same
 * reasons written up there: Compose offscreen on minSdk 26 is a cascade of
 * hardware bitmaps and layers that fails differently per device, while a
 * software canvas draws identically from API 26 up. The backdrop, the logo, the
 * fonts and the sleeve are shared with the Replay by importing them rather than
 * by copying, so the two cards cannot drift apart.
 *
 * ## Fitting the type
 *
 * A passage can be one line or twenty, so both the size and the height are
 * chosen rather than fixed: the layout is retried down a fixed ladder of sizes
 * until the rows stop overflowing, and the card is then grown to exactly the
 * room those rows take. The bottom rung of that ladder is a floor rather than a
 * last resort — no card ever sets type smaller than it to squeeze a passage
 * in, so a passage too long for the frame lengthens the card instead of being
 * cut, or made small enough to hold a phone at arm's length to read.
 */
internal enum class ShareCardTheme {
    DYNAMIC,
    DARK_OLED,
    VIBRANT,
}

internal suspend fun renderLyricsShareCard(
    context: Context,
    card: LyricsShareRequest,
    /** Pad out to the full 1080×1920 frame, the passage centred in the room. */
    story: Boolean = false,
    theme: ShareCardTheme = ShareCardTheme.DYNAMIC,
): Bitmap = withContext(Dispatchers.Default) {
    val type = Fonts(context)
    val cover = card.artworkUrl?.let { loadBitmap(context, it) }

    val hasLyrics = card.lines.isNotEmpty()
    val headTop = HEADER_BOTTOM + GAP_ABOVE_BODY
    val frameRoom = MAX_CARD_H - FOOTER_THUMB - FOOTER_PAD - GAP_BELOW_BODY - headTop
    val plan = if (hasLyrics) fittestPlan(type, card.lines, frameRoom) else null

    val spare = if (story) {
        val contentH = plan?.content ?: 0f
        (frameRoom - contentH).coerceAtLeast(0f)
    } else 0f

    val bodyTop = headTop + spare / 2
    val bodyBottom = bodyTop + (plan?.content ?: 0f)
    val cardH = if (!hasLyrics) {
        if (story) MAX_CARD_H else 1350f
    } else {
        bodyBottom + spare / 2 + GAP_BELOW_BODY + FOOTER_THUMB + FOOTER_PAD
    }

    val bitmap = Bitmap.createBitmap(CARD_W, cardH.roundToInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    when (theme) {
        ShareCardTheme.DYNAMIC -> drawBackdrop(canvas, cover, 0f, cardH)
        ShareCardTheme.DARK_OLED -> {
            canvas.drawColor(0xFF0C0C0E.toInt())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val cx = CARD_W / 2f
            val cy = cardH * 0.45f
            val radius = CARD_W * 0.85f
            paint.shader = RadialGradient(
                cx,
                cy,
                radius,
                intArrayOf(0x33FFFFFF, 0x00000000),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
        }
        ShareCardTheme.VIBRANT -> drawBackdrop(canvas, cover, 50f, cardH)
    }

    // ── Header ──────────────────────────────────────────────────────────────
    val brand = type.heading(46f, 0xE6FFFFFF.toInt()).apply {
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(BRAND, CARD_W - CARD_MARGIN, HEADER_BASELINE, brand)
    drawLogo(
        canvas,
        context,
        CARD_W - CARD_MARGIN - brand.measureText(BRAND) - LOGO_GAP,
        HEADER_BASELINE,
    )

    if (hasLyrics && plan != null) {
        // ── The passage ─────────────────────────────────────────────────────
        var y = bodyTop
        for (row in plan.rows) {
            val baseline = y + row.gapBefore - row.paint.ascent() +
                if (row.isGap) row.paint.textSize * GAP_MARKER_OFFSET else 0f
            val x = if (row.centered) CARD_W / 2f else CARD_MARGIN
            canvas.drawText(row.text, x, baseline, row.paint)
            y += row.gapBefore + row.height
        }

        // ── Footer ──────────────────────────────────────────────────────────
        val thumbY = cardH - FOOTER_PAD - FOOTER_THUMB
        drawArtwork(canvas, cover, card.song.title, CARD_MARGIN, thumbY, FOOTER_THUMB, false)
        val textX = CARD_MARGIN + FOOTER_THUMB + 34f
        val room = CARD_W - CARD_MARGIN - textX
        val title = type.body(50f, Color.WHITE, bold = true)
        canvas.drawText(ellipsised(card.song.title, title, room), textX, thumbY + 74f, title)
        val artist = type.body(42f, 0x99FFFFFF.toInt())
        canvas.drawText(ellipsised(card.song.artist, artist, room), textX, thumbY + 132f, artist)
    } else {
        // ── Track Story View (Album Art + Title + Artist + Waveform) ─────────
        val artSize = 620f
        val artX = (CARD_W - artSize) / 2f
        val artY = (cardH - artSize) / 2f - 90f
        drawArtwork(canvas, cover, card.song.title, artX, artY, artSize, false)

        val titleY = artY + artSize + 85f
        val titlePaint = type.heading(58f, Color.WHITE).apply { textAlign = Paint.Align.CENTER }
        val titleText = ellipsised(card.song.title, titlePaint, CARD_W - CARD_MARGIN * 2)
        canvas.drawText(titleText, CARD_W / 2f, titleY, titlePaint)

        val artistY = titleY + 65f
        val artistPaint = type.body(44f, 0xCCFFFFFF.toInt()).apply { textAlign = Paint.Align.CENTER }
        val artistText = ellipsised(card.song.artist, artistPaint, CARD_W - CARD_MARGIN * 2)
        canvas.drawText(artistText, CARD_W / 2f, artistY, artistPaint)

        val waveY = artistY + 55f
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x80FFFFFF.toInt()
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 7f
        }
        val barHeights = floatArrayOf(18f, 32f, 48f, 26f, 60f, 38f, 52f, 30f, 65f, 42f, 24f, 45f, 58f, 34f, 20f)
        val totalWaveW = barHeights.size * 22f
        val waveStartX = (CARD_W - totalWaveW) / 2f
        barHeights.forEachIndexed { i, h ->
            val bx = waveStartX + i * 22f
            canvas.drawLine(bx, waveY + (65f - h) / 2f, bx, waveY + (65f + h) / 2f, wavePaint)
        }
    }

    bitmap
}

// ── Fitting ────────────────────────────────────────────────────────────────

private class Row(
    val text: String,
    val paint: Paint,
    val height: Float,
    val gapBefore: Float,
    /** Drawn centred rather than off the left edge — the gap marker. */
    val centered: Boolean = false,
    val isGap: Boolean = false,
)

/** The rows at one size, with everything needed to place them. */
private class Plan(val rows: List<Row>, val ascent: Float) {
    /** Distance from the top of the first line to the bottom of the last. */
    val content: Float
        get() = ascent + rows.fold(0f) { total, row -> total + row.gapBefore + row.height }
}

/**
 * The largest size on [FIT_LADDER] whose rows still fit in [room], or the
 * bottom rung when none of them do.
 *
 * The bottom rung is a floor rather than a last resort: under it the type stops
 * being worth sharing, so the frame grows to hold the words instead of the
 * words shrinking to hold the frame. That the player budgets characters at the
 * pick is what keeps this branch from ever being reached in practice.
 */
private fun fittestPlan(
    type: Fonts,
    lines: List<LyricsShareLine>,
    room: Float,
): Plan {
    for (size in FIT_LADDER) {
        val plan = planAt(type, lines, size)
        if (plan.content <= room) return plan
    }
    return planAt(type, lines, FIT_LADDER.last())
}

/**
 * The rows at one [size], set in the face the lyrics panel draws with.
 *
 * SF Pro Display Heavy with the panel's tight tracking and its line advance,
 * carried over as the same ratios the panel's 34sp headline uses — so the card
 * reads as a still of that panel rather than as a different app quoting it.
 * Compose's tracking is absolute and `Paint`'s is a fraction of the size, hence
 * the divisions: -0.7 over 34 for the words, -0.7 over 20 for the smaller line
 * the panel hangs underneath them.
 */
private fun planAt(type: Fonts, lines: List<LyricsShareLine>, size: Float): Plan {
    val primary = type.heading(size, Color.WHITE).apply { letterSpacing = TRACKING }
    val subSize = (size * SUB_RATIO).coerceAtLeast(SUB_FLOOR)
    val secondary = type.heading(subSize, SUB_COLOR).apply { letterSpacing = SUB_TRACKING }

    val rows = mutableListOf<Row>()
    lines.forEachIndexed { index, line ->
        if (line.isGap) {
            // The skipped lines, as "(...)" — the conventional editorial mark
            // for an omission. Hung off the left edge with the words, smaller
            // and a dim grey. Keep its row compact so the marker follows the
            // preceding lyric closely; the regular gap on the next lyric line
            // provides the separation below it.
            val marker = type.body(size * 0.65f, 0x66FFFFFF.toInt())
            val markerHeight = marker.descent() - marker.ascent()
            rows += Row("(...)", marker, markerHeight, 0f, isGap = true)
            return@forEachIndexed
        }
        var firstOfLine = true
        fun add(text: String, paint: Paint, height: Float) {
            // One verse line is one paragraph: its first row carries the space
            // above it and its wraps sit tight underneath, so a phrase reads as
            // a block rather than as a list of separate sentences.
            val gap = when {
                rows.isEmpty() -> 0f
                firstOfLine && index > 0 -> size * 0.55f
                else -> 0f
            }
            rows += Row(text, paint, height, gap)
            firstOfLine = false
        }
        wrap(line.text, primary, CARD_CONTENT_W).forEach { add(it, primary, size * LEAD) }
        val sub = line.subText
        if (!sub.isNullOrBlank() && sub != line.text) {
            wrap(sub, secondary, CARD_CONTENT_W).forEach { add(it, secondary, subSize * SUB_LEAD) }
        }
    }
    return Plan(rows, -primary.ascent())
}

// ── Text ───────────────────────────────────────────────────────────────────

/**
 * Greedy wrap at [width], breaking wherever the line has to.
 *
 * Not a plain word-wrap: Japanese, Chinese and Korean lyrics carry no spaces at
 * all, and a wrapper that only split on spaces would hand the whole verse to
 * `drawText` and let it run off the card. `breakText` finds the character that
 * fits, and only then is the break walked back to a space — so scripts that keep
 * their words apart lose nothing, and scripts that don't still get several lines.
 */
private fun wrap(text: String, paint: Paint, width: Float): List<String> {
    val source = text.trim()
    if (source.isEmpty()) return listOf("")

    val out = mutableListOf<String>()
    var rest = source
    while (rest.isNotEmpty()) {
        val fit = paint.breakText(rest, true, width, null)
        if (fit <= 0) {
            out += rest
            break
        }
        // Walk back to the last space inside the fit, but only when something
        // still has to follow it — otherwise the fit is the whole string and
        // cutting at the space would drop the final word.
        val space = rest.lastIndexOf(' ', fit - 1)
        val take = if (space > 0 && fit < rest.length) space else fit
        if (take <= 0) {
            out += rest
            break
        }
        out += rest.substring(0, take).trimEnd()
        rest = rest.substring(take).trimStart()
    }
    return out.filter { it.isNotEmpty() }.ifEmpty { listOf("") }
}

// ── Card geometry ──────────────────────────────────────────────────────────

private const val CARD_W = 1080

/**
 * 9:16 — a ceiling on the height, not the height itself. The card is grown to
 * its passage and only ever comes out this tall when the passage demands it.
 */
private const val MAX_CARD_H = 1920f

private const val CARD_MARGIN = 72f
private const val CARD_CONTENT_W = CARD_W - CARD_MARGIN * 2

private const val LOGO_GAP = 20f
private const val BRAND = "AuraMusic"

/** Baseline of the mark at the top right, and just under it where the gap starts. */
private const val HEADER_BASELINE = 132f
private const val HEADER_BOTTOM = 170f

/** Air kept between the fixed furniture and the passage, both ends alike. */
private const val GAP_ABOVE_BODY = 20f
private const val GAP_BELOW_BODY = 20f

/** The sleeve square in the footer, and the air kept under it. */
private const val FOOTER_THUMB = 160f
private const val FOOTER_PAD = 96f

/** Smallest the translation line is allowed to get before it stops being legible. */
private const val SUB_FLOOR = 24f

/** Small visual correction so the omission marker sits below the line above. */
private const val GAP_MARKER_OFFSET = 0.18f

/**
 * The sizes tried, largest first.
 *
 * A ladder rather than a search: a dozen candidates is nothing to lay out, and
 * stepping by a visible amount keeps two cards with adjacent line counts from
 * coming out nearly the same size and reading as a mistake.
 *
 * The last rung doubles as the floor — see [fittestPlan] for why the frame is
 * what gives way under it rather than the type.
 */
private val FIT_LADDER = floatArrayOf(96f, 88f, 80f, 72f, 64f, 56f, 50f, 44f, 38f, 32f)

// ── The panel's type, as ratios ────────────────────────────────────────────
// Every number here is one the lyrics panel uses, divided by the size it uses
// it at so it holds at whatever size the ladder lands on.

/** Tracking of the panel's headline: -0.7sp against its 34sp. */
private const val TRACKING = -0.7f / 34f

/** The same -0.7sp, against the 20sp it sets the line underneath in. */
private const val SUB_TRACKING = -0.7f / 20f

/** Line advance: 41sp over 34sp for the words, 25sp over 20sp for the sub. */
private const val LEAD = 41f / 34f
private const val SUB_LEAD = 25f / 20f

/** The sub-line against the words above it, and the white it is drawn in. */
private const val SUB_RATIO = 20f / 34f
private val SUB_COLOR = 0xD9FFFFFF.toInt()
