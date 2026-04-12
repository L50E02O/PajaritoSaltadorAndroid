package com.pajaritosaltador.game

/**
 * Normaliza el centro de la barra de poderes en coordenadas relativas (0-1) al contenedor.
 * Usado al persistir y al arrastrar para mantener la barra dentro de la pantalla.
 */
object PowerBarGeometry {

    const val MIN_X = 0.08f
    const val MAX_X = 0.92f
    const val MIN_Y = 0.10f
    const val MAX_Y = 0.92f

    fun clampCenter(x: Float, y: Float): Pair<Float, Float> {
        return x.coerceIn(MIN_X, MAX_X) to y.coerceIn(MIN_Y, MAX_Y)
    }

    /**
     * Convierte la posicion legada (radio) a centro aproximado si aun no hay coords guardadas.
     */
    fun centerFromLegacyPosition(name: String): Pair<Float, Float> {
        return when (name) {
            "LEFT" -> 0.12f to 0.50f
            "RIGHT" -> 0.88f to 0.50f
            else -> 0.50f to 0.90f
        }
    }
}
