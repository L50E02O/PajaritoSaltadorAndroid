package com.pajaritosaltador.game

import org.junit.Assert.assertEquals
import org.junit.Test

class PowerBarGeometryTest {

    @Test
    fun clampCenter_limita_valores_extremos() {
        val (x, y) = PowerBarGeometry.clampCenter(0f, 0f)
        assertEquals(PowerBarGeometry.MIN_X, x, 0.001f)
        assertEquals(PowerBarGeometry.MIN_Y, y, 0.001f)
    }

    @Test
    fun clampCenter_mantiene_valores_centralos() {
        val (x, y) = PowerBarGeometry.clampCenter(0.5f, 0.5f)
        assertEquals(0.5f, x, 0.001f)
        assertEquals(0.5f, y, 0.001f)
    }

    @Test
    fun centerFromLegacyPosition_mapea_tres_casos() {
        val bottom = PowerBarGeometry.centerFromLegacyPosition("BOTTOM")
        assertEquals(0.5f, bottom.first, 0.001f)
        assertEquals(0.90f, bottom.second, 0.001f)

        val left = PowerBarGeometry.centerFromLegacyPosition("LEFT")
        assertEquals(0.12f, left.first, 0.001f)

        val right = PowerBarGeometry.centerFromLegacyPosition("RIGHT")
        assertEquals(0.88f, right.first, 0.001f)
    }
}
