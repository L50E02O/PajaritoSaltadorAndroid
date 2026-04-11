package com.pajaritosaltador.game

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias para el modulo Physics.
 *
 * Verifica las operaciones fundamentales de fisica del juego:
 * gravedad, salto y limitacion de velocidad.
 *
 * Estrategia de prueba:
 * - Se verifica que la gravedad aumente la velocidad proporcionalmente al deltaTime.
 * - Se verifica que el salto establezca una velocidad negativa (hacia arriba).
 * - Se verifica que clampVelocity limite en ambas direcciones.
 */
class PhysicsTest {

    // =====================================================================
    // GRAVEDAD
    // =====================================================================

    @Test
    fun `applyGravity - aumenta la velocidad del objeto proporcionalmente`() {
        val obj = GameObject(velocity = 0f)
        val gravity = 1000f
        val deltaTime = 0.016f // ~60 FPS

        Physics.applyGravity(obj, gravity, deltaTime)

        assertEquals(
            "La velocidad debe ser gravity * deltaTime = 16",
            16f, obj.velocity, 0.01f
        )
    }

    @Test
    fun `applyGravity - acumula velocidad en multiples frames`() {
        val obj = GameObject(velocity = 0f)
        val gravity = 1000f

        // Simular 3 frames a 60 FPS
        repeat(3) {
            Physics.applyGravity(obj, gravity, 0.016f)
        }

        assertEquals(
            "Despues de 3 frames, la velocidad debe ser ~48",
            48f, obj.velocity, 0.1f
        )
    }

    @Test
    fun `applyGravity - no modifica otras propiedades del objeto`() {
        val obj = GameObject(x = 10f, y = 20f, width = 30f, height = 40f, velocity = 5f, rotation = 0.5f)

        Physics.applyGravity(obj, 1000f, 0.016f)

        assertEquals("X no debe cambiar", 10f, obj.x)
        assertEquals("Y no debe cambiar", 20f, obj.y)
        assertEquals("Width no debe cambiar", 30f, obj.width)
        assertEquals("Height no debe cambiar", 40f, obj.height)
        assertEquals("Rotation no debe cambiar", 0.5f, obj.rotation)
    }

    // =====================================================================
    // SALTO
    // =====================================================================

    @Test
    fun `applyJump - establece velocidad negativa (hacia arriba)`() {
        val obj = GameObject(velocity = 100f)
        val jumpForce = 250f

        Physics.applyJump(obj, jumpForce)

        assertEquals(
            "La velocidad debe ser -jumpForce (negativa = hacia arriba)",
            -250f, obj.velocity
        )
    }

    @Test
    fun `applyJump - reemplaza la velocidad actual completamente`() {
        val obj = GameObject(velocity = 500f)

        Physics.applyJump(obj, 200f)

        assertEquals(
            "El salto debe reemplazar la velocidad, no sumarla",
            -200f, obj.velocity
        )
    }

    @Test
    fun `applyJump - funciona incluso si ya tiene velocidad negativa`() {
        val obj = GameObject(velocity = -100f)

        Physics.applyJump(obj, 250f)

        assertEquals(
            "Debe establecer -250 sin importar la velocidad previa",
            -250f, obj.velocity
        )
    }

    // =====================================================================
    // LIMITACION DE VELOCIDAD
    // =====================================================================

    @Test
    fun `clampVelocity - no modifica velocidad dentro del rango`() {
        val obj = GameObject(velocity = 100f)

        Physics.clampVelocity(obj, 400f)

        assertEquals(
            "La velocidad dentro del rango no debe modificarse",
            100f, obj.velocity
        )
    }

    @Test
    fun `clampVelocity - limita velocidad positiva excesiva`() {
        val obj = GameObject(velocity = 600f)

        Physics.clampVelocity(obj, 400f)

        assertEquals(
            "La velocidad debe limitarse al maximo positivo",
            400f, obj.velocity
        )
    }

    @Test
    fun `clampVelocity - limita velocidad negativa excesiva`() {
        val obj = GameObject(velocity = -600f)

        Physics.clampVelocity(obj, 400f)

        assertEquals(
            "La velocidad debe limitarse al maximo negativo",
            -400f, obj.velocity
        )
    }

    @Test
    fun `clampVelocity - velocidad exactamente en el limite no cambia`() {
        val obj = GameObject(velocity = 400f)

        Physics.clampVelocity(obj, 400f)

        assertEquals("La velocidad en el limite exacto no debe cambiar", 400f, obj.velocity)
    }

    @Test
    fun `clampVelocity - velocidad cero no se modifica`() {
        val obj = GameObject(velocity = 0f)

        Physics.clampVelocity(obj, 400f)

        assertEquals("La velocidad cero no debe modificarse", 0f, obj.velocity)
    }
}
