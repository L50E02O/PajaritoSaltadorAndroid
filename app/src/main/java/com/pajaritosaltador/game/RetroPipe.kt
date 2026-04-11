package com.pajaritosaltador.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val PipeGreenDark = Color(0xFF225101)
private val PipeGreenBright = Color(0xFF53D501)
private val PipeGreenBase = Color(0xFF328A02)

/**
 * Tuberia retro con volumen simulado (degradado horizontal) y cabeza mas ancha.
 * El [modifier] debe reservar ancho extra horizontal para la cabeza (ver [totalWidth]).
 *
 * @param width Ancho del cuerpo cilindrico.
 * @param height Altura total del composable (cuerpo + zona de cabeza).
 * @param isTopPipe Si es tuberia colgante (cabeza abajo) o desde el suelo (cabeza arriba).
 */
@Composable
fun RetroPipe(
    width: Dp,
    height: Dp,
    isTopPipe: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val headOutset = with(density) { (width.toPx() * 0.12f).toDp() }
    val headHeight = with(density) { (width.toPx() * 0.28f).toDp() }
    val totalWidth = width + headOutset * 2

    Canvas(modifier = modifier.size(totalWidth, height)) {
        val strokePx = 2.dp.toPx()
        val stroke = Stroke(width = strokePx)
        val headOutsetPx = headOutset.toPx()
        val headHeightPx = headHeight.toPx()
        val bodyLeft = headOutsetPx
        val bodyWidthPx = width.toPx()
        val bodyHeightPx = size.height

        val pipeBrush = Brush.linearGradient(
            colorStops = arrayOf(
                0f to PipeGreenDark,
                0.35f to PipeGreenBright,
                1f to PipeGreenBase
            ),
            start = Offset(bodyLeft, 0f),
            end = Offset(bodyLeft + bodyWidthPx, 0f)
        )

        drawRect(
            brush = pipeBrush,
            topLeft = Offset(bodyLeft, 0f),
            size = Size(bodyWidthPx, bodyHeightPx)
        )
        drawRect(
            color = Color.Black,
            topLeft = Offset(bodyLeft, 0f),
            size = Size(bodyWidthPx, bodyHeightPx),
            style = stroke
        )

        val headY = if (isTopPipe) bodyHeightPx - headHeightPx else 0f
        val headW = bodyWidthPx + headOutsetPx * 2

        val capBrush = Brush.linearGradient(
            colorStops = arrayOf(
                0f to PipeGreenDark,
                0.35f to PipeGreenBright,
                1f to PipeGreenBase
            ),
            start = Offset(0f, 0f),
            end = Offset(headW, 0f)
        )

        drawRect(
            brush = capBrush,
            topLeft = Offset(0f, headY),
            size = Size(headW, headHeightPx)
        )
        drawRect(
            color = Color.Black,
            topLeft = Offset(0f, headY),
            size = Size(headW, headHeightPx),
            style = stroke
        )

        val lineY = if (isTopPipe) headY else headY + headHeightPx
        drawLine(
            color = Color.Black,
            start = Offset(0f, lineY),
            end = Offset(headW, lineY),
            strokeWidth = strokePx
        )
    }
}
