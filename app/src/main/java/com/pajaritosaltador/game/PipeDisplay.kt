package com.pajaritosaltador.game

import androidx.compose.ui.unit.Dp

/**
 * Estado de tuberia para la capa de presentacion (MVVM).
 * Las coordenadas son las mismas unidades de viewport que [Pipe] (logicas, no px de pantalla).
 */
data class PipeDisplay(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val isTopPipe: Boolean
)

/**
 * Modelo de tuberia con medidas en Dp para composicion UI (previews, menus).
 * La conversion desde el viewport debe hacerse con la escala adecuada en la capa Composable.
 */
data class PipeUiModel(
    val x: Dp,
    val y: Dp,
    val width: Dp,
    val height: Dp,
    val isTopPipe: Boolean
)
