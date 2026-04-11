package com.pajaritosaltador.game

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias para CollisionUtils.
 *
 * Estas pruebas verifican la correcta deteccion de colisiones AABB (rectangulo-rectangulo)
 * y circulo-rectangulo, que son la base del sistema de colisiones del juego.
 *
 * Estrategia de prueba:
 * - Se prueban los casos limite: sin colision, colision parcial, contencion total.
 * - Se prueban colisiones desde las 4 direcciones.
 * - Se verifica que los margenes del hitbox funcionen correctamente.
 */
class CollisionUtilsTest {

    // =====================================================================
    // PRUEBAS DE COLISION RECTANGULO-RECTANGULO (AABB)
    // =====================================================================

    @Test
    fun `checkRectCollision - dos rectangulos separados horizontalmente no colisionan`() {
        val rect1 = Rect(0f, 0f, 10f, 10f)
        val rect2 = Rect(20f, 0f, 10f, 10f)

        assertFalse(
            "Rectangulos separados por 10 unidades en X no deben colisionar",
            CollisionUtils.checkRectCollision(rect1, rect2)
        )
    }

    @Test
    fun `checkRectCollision - dos rectangulos separados verticalmente no colisionan`() {
        val rect1 = Rect(0f, 0f, 10f, 10f)
        val rect2 = Rect(0f, 20f, 10f, 10f)

        assertFalse(
            "Rectangulos separados por 10 unidades en Y no deben colisionar",
            CollisionUtils.checkRectCollision(rect1, rect2)
        )
    }

    @Test
    fun `checkRectCollision - dos rectangulos superpuestos parcialmente colisionan`() {
        val rect1 = Rect(0f, 0f, 10f, 10f)
        val rect2 = Rect(5f, 5f, 10f, 10f)

        assertTrue(
            "Rectangulos que se solapan 5x5 unidades deben colisionar",
            CollisionUtils.checkRectCollision(rect1, rect2)
        )
    }

    @Test
    fun `checkRectCollision - rectangulo contenido completamente dentro de otro colisiona`() {
        val outer = Rect(0f, 0f, 100f, 100f)
        val inner = Rect(25f, 25f, 50f, 50f)

        assertTrue(
            "Un rectangulo dentro de otro debe reportar colision",
            CollisionUtils.checkRectCollision(outer, inner)
        )
    }

    @Test
    fun `checkRectCollision - rectangulos que se tocan en el borde exacto no colisionan`() {
        val rect1 = Rect(0f, 0f, 10f, 10f)
        val rect2 = Rect(10f, 0f, 10f, 10f)

        assertFalse(
            "Rectangulos que solo comparten un borde (sin solapamiento) no deben colisionar",
            CollisionUtils.checkRectCollision(rect1, rect2)
        )
    }

    @Test
    fun `checkRectCollision - colision simetrica funciona en ambas direcciones`() {
        val rect1 = Rect(0f, 0f, 15f, 15f)
        val rect2 = Rect(10f, 10f, 15f, 15f)

        val resultAB = CollisionUtils.checkRectCollision(rect1, rect2)
        val resultBA = CollisionUtils.checkRectCollision(rect2, rect1)

        assertEquals(
            "La deteccion de colision debe ser simetrica (A vs B == B vs A)",
            resultAB, resultBA
        )
        assertTrue("Ambos deben colisionar", resultAB)
    }

    // =====================================================================
    // PRUEBAS DE COLISION CIRCULO-RECTANGULO
    // =====================================================================

    @Test
    fun `checkCircleRectCollision - circulo lejos del rectangulo no colisiona`() {
        val rect = Rect(0f, 0f, 10f, 10f)

        assertFalse(
            "Un circulo a 50 unidades de distancia no debe colisionar",
            CollisionUtils.checkCircleRectCollision(50f, 50f, 5f, rect)
        )
    }

    @Test
    fun `checkCircleRectCollision - circulo centrado dentro del rectangulo colisiona`() {
        val rect = Rect(0f, 0f, 20f, 20f)

        assertTrue(
            "Un circulo en el centro del rectangulo debe colisionar",
            CollisionUtils.checkCircleRectCollision(10f, 10f, 5f, rect)
        )
    }

    @Test
    fun `checkCircleRectCollision - circulo tocando el borde del rectangulo colisiona`() {
        val rect = Rect(10f, 0f, 10f, 10f)

        assertTrue(
            "Un circulo cuyo borde toca el rectangulo debe colisionar",
            CollisionUtils.checkCircleRectCollision(7f, 5f, 3.1f, rect)
        )
    }

    @Test
    fun `checkCircleRectCollision - circulo justo fuera del borde no colisiona`() {
        val rect = Rect(10f, 0f, 10f, 10f)

        assertFalse(
            "Un circulo cuyo borde no alcanza el rectangulo no debe colisionar",
            CollisionUtils.checkCircleRectCollision(7f, 5f, 2.9f, rect)
        )
    }

    @Test
    fun `checkCircleRectCollision - colision en esquina del rectangulo`() {
        val rect = Rect(0f, 0f, 10f, 10f)

        assertTrue(
            "Un circulo que toca la esquina del rectangulo debe colisionar",
            CollisionUtils.checkCircleRectCollision(11f, 11f, 2f, rect)
        )
    }

    // =====================================================================
    // PRUEBAS DE HITBOX Y CONVERSION
    // =====================================================================

    @Test
    fun `createHitbox - genera rectangulo con margenes correctos`() {
        val obj = GameObject(x = 10f, y = 20f, width = 40f, height = 30f)
        val hitbox = CollisionUtils.createHitbox(obj, marginX = 5f, marginY = 3f)

        assertEquals("X debe incluir margen", 15f, hitbox.x)
        assertEquals("Y debe incluir margen", 23f, hitbox.y)
        assertEquals("Width debe reducirse por doble margen", 30f, hitbox.width)
        assertEquals("Height debe reducirse por doble margen", 24f, hitbox.height)
    }

    @Test
    fun `createHitbox - margenes por defecto son 4 unidades`() {
        val obj = GameObject(x = 0f, y = 0f, width = 20f, height = 20f)
        val hitbox = CollisionUtils.createHitbox(obj)

        assertEquals("Margen X por defecto es 4", 4f, hitbox.x)
        assertEquals("Margen Y por defecto es 4", 4f, hitbox.y)
        assertEquals("Width con margen por defecto", 12f, hitbox.width)
        assertEquals("Height con margen por defecto", 12f, hitbox.height)
    }

    @Test
    fun `pipeToRect - convierte Pipe a Rect correctamente`() {
        val pipe = Pipe(x = 100f, y = 0f, width = 54f, height = 200f, passed = false, pairId = 1)
        val rect = CollisionUtils.pipeToRect(pipe)

        assertEquals(100f, rect.x)
        assertEquals(0f, rect.y)
        assertEquals(54f, rect.width)
        assertEquals(200f, rect.height)
    }
}
