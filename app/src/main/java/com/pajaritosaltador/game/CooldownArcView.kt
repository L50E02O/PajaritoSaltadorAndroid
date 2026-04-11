package com.pajaritosaltador.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Vista personalizada que dibuja un arco de progreso circular sobre un boton de poder.
 * Muestra el progreso del cooldown (arco gris) o la duracion activa (arco dorado).
 */
class CooldownArcView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var fraction = 0f
    private var isActiveState = false

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }

    private val bgArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.argb(60, 255, 255, 255)
    }

    private val rectF = RectF()

    /**
     * Actualiza el estado visual del arco.
     * @param newFraction Porcentaje de 0.0 a 1.0
     * @param active Si el poder esta activo (dorado) o en cooldown (gris)
     */
    fun setProgress(newFraction: Float, active: Boolean) {
        fraction = newFraction.coerceIn(0f, 1f)
        isActiveState = active
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (fraction <= 0f) return

        val padding = arcPaint.strokeWidth / 2 + 4f
        rectF.set(padding, padding, width - padding, height - padding)

        canvas.drawArc(rectF, 0f, 360f, false, bgArcPaint)

        arcPaint.color = if (isActiveState) {
            Color.parseColor("#FFD700")
        } else {
            Color.parseColor("#B0BEC5")
        }

        val sweepAngle = 360f * fraction
        canvas.drawArc(rectF, -90f, sweepAngle, false, arcPaint)
    }
}
