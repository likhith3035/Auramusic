package com.music.bitchord.ui.screens.settings

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.music.bitchord.data.telemetry.TelemetryStats
import com.music.bitchord.ui.replay.cacheForSharing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object TelemetryCardRenderer {

    private const val CARD_WIDTH = 1080
    private const val CARD_HEIGHT = 1350

    suspend fun renderMilestoneCard(
        stats: TelemetryStats,
    ): Bitmap = withContext(Dispatchers.Default) {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Deep Obsidian Background with Radial Ambient Glows
        canvas.drawColor(0xFF09090C.toInt())

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Top right crimson-rose aura
        glowPaint.shader = RadialGradient(
            CARD_WIDTH * 0.85f,
            CARD_HEIGHT * 0.15f,
            550f,
            intArrayOf(0x40E11D48, 0x15E11D48, 0x00000000),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(CARD_WIDTH * 0.85f, CARD_HEIGHT * 0.15f, 550f, glowPaint)

        // Bottom left blue-indigo aura
        glowPaint.shader = RadialGradient(
            CARD_WIDTH * 0.15f,
            CARD_HEIGHT * 0.85f,
            600f,
            intArrayOf(0x356366F1, 0x106366F1, 0x00000000),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(CARD_WIDTH * 0.15f, CARD_HEIGHT * 0.85f, 600f, glowPaint)
        glowPaint.shader = null

        // 2. Main Framed Panel
        val margin = 54f
        val panelRect = RectF(margin, margin, CARD_WIDTH - margin, CARD_HEIGHT - margin)
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF141419.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(panelRect, 48f, 48f, panelPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF27272A.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(panelRect, 48f, 48f, borderPaint)

        // 3. Header Section
        val contentLeft = margin + 50f
        val contentRight = CARD_WIDTH - margin - 50f
        var currentY = margin + 74f

        // Glowing Live Dot
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF10B981.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(contentLeft + 10f, currentY, 10f, dotPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF10B981.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("LIVE NETWORK TELEMETRY", contentLeft + 32f, currentY + 8f, headerTextPaint)

        // Right-aligned verification tag
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFA1A1AA.toInt()
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("RENDER CLOUD VERIFIED", contentRight, currentY + 8f, tagPaint)

        // Divider
        currentY += 40f
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF27272A.toInt()
            strokeWidth = 2f
        }
        canvas.drawLine(contentLeft, currentY, contentRight, currentY, divPaint)

        // 4. Hero Title
        currentY += 60f
        val appTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("AuraMusic Telemetry Milestone", contentLeft, currentY, appTitlePaint)

        currentY += 34f
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFA1A1AA.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Real-time telemetry report for founder & open-source community", contentLeft, currentY, subtitlePaint)

        // 5. Two Hero Metric Bento Boxes
        currentY += 50f
        val boxWidth = (contentRight - contentLeft - 30f) / 2f
        val boxHeight = 220f

        // Box 1: Active Today
        val box1Rect = RectF(contentLeft, currentY, contentLeft + boxWidth, currentY + boxHeight)
        val boxBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1B1B22.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(box1Rect, 28f, 28f, boxBgPaint)

        val box1Stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE11D48.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
            alpha = 140
        }
        canvas.drawRoundRect(box1Rect, 28f, 28f, box1Stroke)

        val boxLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFA1A1AA.toInt()
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        canvas.drawText("ACTIVE TODAY", box1Rect.left + 28f, box1Rect.top + 46f, boxLabelPaint)

        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 76f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("${stats.activeToday}", box1Rect.left + 28f, box1Rect.top + 130f, numPaint)

        val subNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE11D48.toInt()
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Unique Devices (DAU)", box1Rect.left + 28f, box1Rect.top + 175f, subNumPaint)

        // Box 2: All-Time Installs
        val box2Rect = RectF(contentLeft + boxWidth + 30f, currentY, contentRight, currentY + boxHeight)
        canvas.drawRoundRect(box2Rect, 28f, 28f, boxBgPaint)

        val box2Stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF06B6D4.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
            alpha = 140
        }
        canvas.drawRoundRect(box2Rect, 28f, 28f, box2Stroke)

        canvas.drawText("ALL-TIME DEVICES", box2Rect.left + 28f, box2Rect.top + 46f, boxLabelPaint)
        canvas.drawText("${stats.allTimeDevices}", box2Rect.left + 28f, box2Rect.top + 130f, numPaint)

        val subNum2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF06B6D4.toInt()
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Recorded Installations", box2Rect.left + 28f, box2Rect.top + 175f, subNum2Paint)

        // 6. 14-Day Activity Chart Panel
        currentY += boxHeight + 40f
        val chartBoxHeight = 310f
        val chartRect = RectF(contentLeft, currentY, contentRight, currentY + chartBoxHeight)
        canvas.drawRoundRect(chartRect, 28f, 28f, boxBgPaint)
        canvas.drawRoundRect(chartRect, 28f, 28f, borderPaint)

        canvas.drawText("14-DAY LISTENER TREND", chartRect.left + 30f, chartRect.top + 46f, boxLabelPaint)

        val latencyText = "Latency: ${stats.latencyMs} ms"
        val latencyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF10B981.toInt()
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(latencyText, chartRect.right - 30f, chartRect.top + 46f, latencyPaint)

        // Draw Spline Curve
        val history = stats.dailyHistory
        if (history.isNotEmpty()) {
            val graphLeft = chartRect.left + 35f
            val graphRight = chartRect.right - 35f
            val graphBottom = chartRect.bottom - 45f
            val graphTop = chartRect.top + 80f
            val graphH = graphBottom - graphTop
            val maxCount = history.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

            val points = history.mapIndexed { idx, item ->
                val x = if (history.size > 1) graphLeft + (idx.toFloat() / (history.size - 1)) * (graphRight - graphLeft) else (graphLeft + graphRight) / 2f
                val yRatio = (item.count.toFloat() / maxCount).coerceIn(0.08f, 1f)
                val y = graphBottom - (yRatio * graphH * 0.85f)
                x to y
            }

            val splinePath = Path().apply {
                moveTo(points[0].first, points[0].second)
                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx1 = p0.first + (p1.first - p0.first) / 2f
                    val cy1 = p0.second
                    val cx2 = p0.first + (p1.first - p0.first) / 2f
                    val cy2 = p1.second
                    cubicTo(cx1, cy1, cx2, cy2, p1.first, p1.second)
                }
            }

            // Fill gradient under curve
            val fillPath = Path().apply {
                addPath(splinePath)
                lineTo(points.last().first, graphBottom)
                lineTo(points.first().first, graphBottom)
                close()
            }

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    graphTop,
                    0f,
                    graphBottom,
                    intArrayOf(0x40E11D48, 0x05E11D48),
                    null,
                    Shader.TileMode.CLAMP,
                )
                style = Paint.Style.FILL
            }
            canvas.drawPath(fillPath, fillPaint)

            // Stroke curve
            val curveStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    graphLeft,
                    0f,
                    graphRight,
                    0f,
                    intArrayOf(0xFFE11D48.toInt(), 0xFFF43F5E.toInt(), 0xFFFB7185.toInt()),
                    null,
                    Shader.TileMode.CLAMP,
                )
                style = Paint.Style.STROKE
                strokeWidth = 6f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(splinePath, curveStroke)

            // Dots
            val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                style = Paint.Style.FILL
            }
            for (p in points) {
                canvas.drawCircle(p.first, p.second, 7f, pointPaint)
            }

            // Dates along bottom
            val dateLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF71717A.toInt()
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val firstDate = history.first().date.takeLast(5)
            val lastDate = history.last().date.takeLast(5)
            canvas.drawText(firstDate, graphLeft, chartRect.bottom - 16f, dateLabelPaint)
            dateLabelPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(lastDate, graphRight, chartRect.bottom - 16f, dateLabelPaint)
        }

        // 7. Footer Section
        currentY += chartBoxHeight + 50f
        val currentDate = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())

        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("AuraMusic · Bit-Perfect & Living Ambient Audio", contentLeft, currentY, footerTextPaint)

        currentY += 30f
        val footerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF71717A.toInt()
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("auramusic-stats.onrender.com · Generated $currentDate", contentLeft, currentY, footerSubPaint)

        val privTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF52525B.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("100% Anonymous · Zero Listening Telemetry", contentRight, currentY, privTagPaint)

        bitmap
    }

    suspend fun shareMilestoneCard(
        context: Context,
        stats: TelemetryStats,
    ) {
        val bitmap = renderMilestoneCard(stats)
        val uri = cacheForSharing(context, bitmap, "telemetry_milestone.png") ?: return

        withContext(Dispatchers.Main) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(null, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "AuraMusic Telemetry Milestone: ${stats.activeToday} active users today, ${stats.allTimeDevices} total devices recorded! https://github.com/likhith3035/Auramusic",
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share AuraMusic Telemetry Milestone").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        }
    }
}
