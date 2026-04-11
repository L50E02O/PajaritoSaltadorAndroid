package com.pajaritosaltador.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader

/**
 * Dibuja tuberias estilo retro en un [Canvas] (API View), sin asignar gradientes por tubo y por frame.
 * Los gradientes se reutilizan mientras el ancho del cuerpo o de la cabeza no cambie.
 */
object RetroPipeDrawer {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
    }

    private var cachedBodyW = -1f
    private var cachedBodyGradient: LinearGradient? = null
    private var cachedCapW = -1f
    private var cachedCapGradient: LinearGradient? = null

    private val bodyColors = intArrayOf(
        Color.parseColor("#225101"),
        Color.parseColor("#53D501"),
        Color.parseColor("#328A02")
    )
    private val bodyStops = floatArrayOf(0f, 0.35f, 1f)

    private fun bodyGradient(width: Float): LinearGradient {
        if (width != cachedBodyW) {
            cachedBodyW = width
            cachedBodyGradient = LinearGradient(
                0f, 0f, width, 0f,
                bodyColors, bodyStops,
                Shader.TileMode.CLAMP
            )
        }
        return cachedBodyGradient!!
    }

    private fun capGradient(width: Float): LinearGradient {
        if (width != cachedCapW) {
            cachedCapW = width
            cachedCapGradient = LinearGradient(
                0f, 0f, width, 0f,
                bodyColors, bodyStops,
                Shader.TileMode.CLAMP
            )
        }
        return cachedCapGradient!!
    }

    fun draw(canvas: Canvas, pipe: Pipe, strokeWidth: Float, viewportWidth: Float) {
        drawInternal(
            canvas = canvas,
            x = pipe.x,
            y = pipe.y,
            width = pipe.width,
            height = pipe.height,
            isTopPipe = pipe.y == 0f,
            strokeWidth = strokeWidth,
            viewportWidth = viewportWidth
        )
    }

    fun draw(canvas: Canvas, display: PipeDisplay, strokeWidth: Float, viewportWidth: Float) {
        drawInternal(
            canvas = canvas,
            x = display.x,
            y = display.y,
            width = display.width,
            height = display.height,
            isTopPipe = display.isTopPipe,
            strokeWidth = strokeWidth,
            viewportWidth = viewportWidth
        )
    }

    /**
     * Dibuja un segmento de tuberia con rotacion (animacion de vuelco).
     */
    fun drawTumblePiece(
        canvas: Canvas,
        piece: PipeTumblePiece,
        strokeWidth: Float,
        viewportWidth: Float
    ) {
        val px = piece.x + piece.width * 0.5f
        val py = if (piece.isTopPipe) piece.y + piece.height else piece.y
        canvas.save()
        canvas.translate(px, py)
        canvas.rotate(piece.rotationDeg)
        canvas.translate(-px, -py)
        drawInternal(
            canvas = canvas,
            x = piece.x,
            y = piece.y,
            width = piece.width,
            height = piece.height,
            isTopPipe = piece.isTopPipe,
            strokeWidth = strokeWidth,
            viewportWidth = viewportWidth
        )
        canvas.restore()
    }

    private fun drawInternal(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        isTopPipe: Boolean,
        strokeWidth: Float,
        viewportWidth: Float
    ) {
        if (x + width < -20f || x > viewportWidth + 20f) return

        val capHeight = width * 0.28f
        val capOverhang = width * 0.12f
        val capFullWidth = width + capOverhang * 2f
        val capLeft = x - capOverhang

        canvas.save()
        canvas.translate(x, y)
        fillPaint.shader = bodyGradient(width)
        canvas.drawRect(0f, 0f, width, height, fillPaint)
        fillPaint.shader = null

        strokePaint.color = Color.BLACK
        strokePaint.strokeWidth = strokeWidth
        canvas.drawRect(0f, 0f, width, height, strokePaint)
        canvas.restore()

        val capTop: Float
        val capBottom: Float
        if (isTopPipe) {
            capTop = y + height - capHeight
            capBottom = y + height
        } else {
            capTop = y
            capBottom = y + capHeight
        }

        canvas.save()
        canvas.translate(capLeft, capTop)
        fillPaint.shader = capGradient(capFullWidth)
        canvas.drawRect(0f, 0f, capFullWidth, capBottom - capTop, fillPaint)
        fillPaint.shader = null
        strokePaint.strokeWidth = strokeWidth
        canvas.drawRect(0f, 0f, capFullWidth, capBottom - capTop, strokePaint)
        canvas.restore()

        linePaint.strokeWidth = strokeWidth
        val lineY = if (isTopPipe) capTop else capBottom
        canvas.drawLine(capLeft, lineY, capLeft + capFullWidth, lineY, linePaint)
    }
}
