package com.pajaritosaltador.game

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias para la data class PowerUp.
 *
 * Verifica las propiedades computadas (isReady, isOnCooldown, fracciones)
 * que son criticas para la correcta visualizacion de la UI de poderes.
 *
 * Estrategia de prueba:
 * - Se crean instancias de PowerUp con diferentes estados.
 * - Se verifican las propiedades derivadas en cada combinacion de estado.
 */
class PowerUpDataTest {

    // =====================================================================
    // PROPIEDADES DE ESTADO
    // =====================================================================

    @Test
    fun `isReady - true cuando no esta activo ni en cooldown`() {
        val power = PowerUp(name = "Test", cooldown = 10000L)

        assertTrue("Debe estar listo por defecto", power.isReady)
    }

    @Test
    fun `isReady - false cuando esta activo`() {
        val power = PowerUp(name = "Test", cooldown = 10000L, duration = 5000L,
            isActive = true, activeTimer = 5f)

        assertFalse("No debe estar listo si esta activo", power.isReady)
    }

    @Test
    fun `isReady - false cuando esta en cooldown`() {
        val power = PowerUp(name = "Test", cooldown = 10000L, cooldownTimer = 5f)

        assertFalse("No debe estar listo si esta en cooldown", power.isReady)
    }

    @Test
    fun `isOnCooldown - true cuando tiene cooldownTimer mayor a 0 y no esta activo`() {
        val power = PowerUp(name = "Test", cooldown = 10000L, cooldownTimer = 5f)

        assertTrue("Debe estar en cooldown", power.isOnCooldown)
    }

    @Test
    fun `isOnCooldown - false cuando esta activo aunque tenga cooldownTimer`() {
        val power = PowerUp(name = "Test", cooldown = 10000L,
            isActive = true, cooldownTimer = 5f)

        assertFalse(
            "No debe reportar cooldown si esta activo (el cooldown es posterior)",
            power.isOnCooldown
        )
    }

    // =====================================================================
    // FRACCIONES PARA UI
    // =====================================================================

    @Test
    fun `cooldownFraction - retorna valor correcto proporcional al cooldown total`() {
        val power = PowerUp(name = "Test", cooldown = 20000L, cooldownTimer = 10f)

        assertEquals(
            "Con 10s de 20s totales, la fraccion debe ser 0.5",
            0.5f, power.cooldownFraction, 0.01f
        )
    }

    @Test
    fun `cooldownFraction - retorna 0 cuando no hay cooldown`() {
        val power = PowerUp(name = "Test", cooldown = 20000L, cooldownTimer = 0f)

        assertEquals(0f, power.cooldownFraction, 0.001f)
    }

    @Test
    fun `cooldownFraction - se limita a 1 como maximo`() {
        val power = PowerUp(name = "Test", cooldown = 10000L, cooldownTimer = 15f)

        assertEquals(
            "La fraccion no debe superar 1.0",
            1f, power.cooldownFraction, 0.001f
        )
    }

    @Test
    fun `activeFraction - retorna valor correcto proporcional a la duracion total`() {
        val power = PowerUp(name = "Test", cooldown = 10000L, duration = 10000L,
            isActive = true, activeTimer = 5f)

        assertEquals(
            "Con 5s de 10s totales, la fraccion debe ser 0.5",
            0.5f, power.activeFraction, 0.01f
        )
    }

    @Test
    fun `activeFraction - retorna 0 para poderes instantaneos (duration 0)`() {
        val power = PowerUp(name = "Test", cooldown = 10000L, duration = 0L)

        assertEquals(
            "Poderes instantaneos deben tener fraccion activa 0",
            0f, power.activeFraction, 0.001f
        )
    }

    @Test
    fun `activeFraction - retorna 1 cuando acaba de activarse`() {
        val power = PowerUp(name = "Test", cooldown = 10000L, duration = 5000L,
            isActive = true, activeTimer = 5f)

        assertEquals(
            "Al inicio de la activacion, la fraccion debe ser 1.0",
            1f, power.activeFraction, 0.01f
        )
    }
}
