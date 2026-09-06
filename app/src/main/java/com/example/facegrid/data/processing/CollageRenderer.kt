package com.example.facegrid.data.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.ceil
import kotlin.math.max
import com.example.facegrid.domain.model.IdentityResult
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip

object CollageRenderer {
    private const val WIDTH = 1080
    private const val HEIGHT = 1920
    private const val MARGIN = 48f
    private const val GAP = 24f
    private val CANVAS_BACKGROUND = Color.rgb(230, 230, 200)
    private val TITLE_COLOR = Color.rgb(30, 29, 28)
    private val SUBTITLE_COLOR = Color.rgb(110, 103, 96)
    private val TILE_BACKGROUND = Color.rgb(231, 224, 215)
    private const val DETAIL_TEXT_COLOR = 0xFFE9E2DA.toInt()

    fun render(identities: List<IdentityResult>): Bitmap {
        val output = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(output)
        canvas.drawColor(CANVAS_BACKGROUND)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TITLE_COLOR
            textSize = 64f
            typeface =
                android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SUBTITLE_COLOR
            textSize = 30f
        }
        canvas.drawText("FaceGrid", MARGIN, 92f, titlePaint)
        canvas.drawText(
            "${identities.size} people · ${identities.sumOf { it.appearanceCount }} appearances",
            MARGIN,
            138f,
            subtitlePaint
        )
        if (identities.isEmpty()) return output
        val columns = if (identities.size == 1) 1 else 2
        val rows = ceil(identities.size / columns.toDouble()).toInt()
        val gridTop = 190f
        val gridBottom = HEIGHT - MARGIN
        val tileWidth = (WIDTH - MARGIN * 2 - GAP * (columns - 1)) / columns
        val tileHeight = (gridBottom - gridTop - GAP * (rows - 1)) / rows
        identities.forEachIndexed { index, identity ->
            val column = index % columns
            val row = index / columns
            val left = MARGIN + column * (tileWidth + GAP)
            val top = gridTop + row * (tileHeight + GAP)
            drawTile(canvas, identity, RectF(left, top, left + tileWidth, top + tileHeight))
        }
        return output
    }

    private fun drawTile(canvas: Canvas, identity: IdentityResult, tile: RectF) {
        val tilePath = Path().apply { addRoundRect(tile, 32f, 32f, Path.Direction.CW) }
        canvas.withClip(tilePath) {
            val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TILE_BACKGROUND }
            drawRect(tile, background)
            val source = squareCrop(
                identity.representative.bitmap,
                identity.representative.boundingBox,
                1.45f
            )
            drawBitmap(source, null, tile, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            source.recycle()
            val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.LinearGradient(
                    tile.left,
                    tile.bottom - tile.height() * 0.45f,
                    tile.left,
                    tile.bottom,
                    Color.TRANSPARENT,
                    0xCC000000.toInt(),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            drawRect(tile, shade)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = max(26f, tile.width() * 0.075f)
            typeface =
                android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = DETAIL_TEXT_COLOR
            textSize = max(20f, tile.width() * 0.052f)
        }
        canvas.drawText("Person ${identity.id}", tile.left + 24f, tile.bottom - 62f, labelPaint)
        canvas.drawText(
            "${identity.appearanceCount} appearance${if (identity.appearanceCount == 1) "" else "s"}",
            tile.left + 24f,
            tile.bottom - 28f,
            detailPaint
        )
    }
}
