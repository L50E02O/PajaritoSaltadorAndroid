package com.pajaritosaltador.game

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para PowerUpManager.
 *
 * Valida el ciclo de vida completo de los poderes:
 * activacion -> duracion activa -> cooldown -> listo de nuevo.
 *
 * Estrategia de prueba:
 * - Se verifica que cada poder inicie en estado "listo".
 * - Se prueba la activacion exitosa y el rechazo cuando no esta listo.
 * - Se simula el paso del tiempo para verificar transiciones de estado.
 * - Se prueba la reduccion de cooldowns por coleccionables.
 * - Se prueba el reset completo.
 */
class PowerUpManagerTest {

    private lateinit var manager: PowerUpManager

    @Before
    fun setUp() {
        manager = PowerUpManager()
    }

    // =====================================================================
    // ESTADO INICIAL
    // =====================================================================

    @Test
    fun `estado inicial - todos los poderes estan listos para usar`() {
        manager.allPowerUps.forEach { power ->
            assertTrue(
                "${power.name} debe estar listo al inicio",
                power.isReady
            )
            assertFalse("${power.name} no debe estar activo al inicio", power.isActive)
            assertFalse("${power.name} no debe estar en cooldown al inicio", power.isOnCooldown)
        }
    }

    @Test
    fun `estado inicial - hay exactamente 3 poderes registrados`() {
        assertEquals(
            "Deben existir 3 poderes: invencibilidad, velocidad x2, romper tuberia",
            3, manager.allPowerUps.size
        )
    }

    @Test
    fun `estado inicial - invencibilidad tiene 5s de duracion y 25s de cooldown`() {
        assertEquals(5000L, manager.invincibility.duration)
        assertEquals(25000L, manager.invincibility.cooldown)
    }

    @Test
    fun `estado inicial - modo x2 tiene 30s de duracion y 25s de cooldown`() {
        assertEquals(30000L, manager.speedX2.duration)
        assertEquals(25000L, manager.speedX2.cooldown)
    }

    @Test
    fun `estado inicial - romper tuberia es instantaneo con 25s de cooldown`() {
        assertEquals(0L, manager.breakPipe.duration)
        assertEquals(25000L, manager.breakPipe.cooldown)
    }

    // =====================================================================
    // ACTIVACION DE PODERES
    // =====================================================================

    @Test
    fun `activar invencibilidad - se activa correctamente cuando esta listo`() {
        val result = manager.activate(manager.invincibility)

        assertTrue("La activacion debe retornar true", result)
        assertTrue("Debe estar activo despues de activar", manager.invincibility.isActive)
        assertEquals(
            "El timer activo debe ser 5 segundos (5000ms / 1000)",
            5f, manager.invincibility.activeTimer
        )
    }

    @Test
    fun `activar invencibilidad - no se puede activar dos veces seguidas`() {
        manager.activate(manager.invincibility)
        val secondAttempt = manager.activate(manager.invincibility)

        assertFalse(
            "No debe poder activarse mientras ya esta activo",
            secondAttempt
        )
    }

    @Test
    fun `activar romper tuberia - es instantaneo y entra en cooldown inmediatamente`() {
        val result = manager.activate(manager.breakPipe)

        assertTrue("La activacion debe retornar true", result)
        assertFalse(
            "Romper tuberia no debe quedar activo (es instantaneo, duration=0)",
            manager.breakPipe.isActive
        )
        assertTrue(
            "Debe entrar en cooldown inmediatamente",
            manager.breakPipe.isOnCooldown
        )
        assertEquals(
            "El cooldown debe ser 25 segundos",
            25f, manager.breakPipe.cooldownTimer
        )
    }

    @Test
    fun `activar poder en cooldown - retorna false`() {
        manager.activate(manager.breakPipe)
        val secondAttempt = manager.activate(manager.breakPipe)

        assertFalse(
            "No debe poder activarse durante el cooldown",
            secondAttempt
        )
    }

    // =====================================================================
    // ACTUALIZACION DE TEMPORIZADORES
    // =====================================================================

    @Test
    fun `update - el timer activo de invencibilidad disminuye con el tiempo`() {
        manager.activate(manager.invincibility)
        val initialTimer = manager.invincibility.activeTimer

        manager.update(1f)

        assertEquals(
            "Despues de 1 segundo, el timer debe reducirse en 1",
            initialTimer - 1f, manager.invincibility.activeTimer, 0.01f
        )
    }

    @Test
    fun `update - invencibilidad se desactiva y entra en cooldown al expirar`() {
        manager.activate(manager.invincibility)

        // Simular 5 segundos (duracion completa)
        manager.update(5.1f)

        assertFalse(
            "Debe desactivarse despues de que expire la duracion",
            manager.invincibility.isActive
        )
        assertTrue(
            "Debe entrar en cooldown despues de desactivarse",
            manager.invincibility.isOnCooldown
        )
    }

    @Test
    fun `update - el cooldown disminuye con el tiempo`() {
        manager.activate(manager.breakPipe)
        val initialCooldown = manager.breakPipe.cooldownTimer

        manager.update(5f)

        assertEquals(
            "Despues de 5 segundos, el cooldown debe reducirse en 5",
            initialCooldown - 5f, manager.breakPipe.cooldownTimer, 0.01f
        )
    }

    @Test
    fun `update - poder vuelve a estar listo cuando el cooldown llega a 0`() {
        manager.activate(manager.breakPipe)

        // Simular 25 segundos (cooldown completo)
        manager.update(25.1f)

        assertTrue(
            "El poder debe estar listo despues de que expire el cooldown",
            manager.breakPipe.isReady
        )
        assertFalse("No debe estar en cooldown", manager.breakPipe.isOnCooldown)
    }

    @Test
    fun `update - modo x2 dura 30 segundos completos`() {
        manager.activate(manager.speedX2)

        // Simular 29 segundos (aun activo)
        manager.update(29f)
        assertTrue("Debe seguir activo a los 29 segundos", manager.speedX2.isActive)

        // Simular 2 segundos mas (total 31, ya expiro)
        manager.update(2f)
        assertFalse("Debe desactivarse despues de 30 segundos", manager.speedX2.isActive)
    }

    // =====================================================================
    // REDUCCION DE COOLDOWNS (COLECCIONABLES)
    // =====================================================================

    @Test
    fun `reduceAllCooldowns - reduce el 15 porciento del cooldown total de cada poder`() {
        manager.activate(manager.breakPipe)
        val cooldownBefore = manager.breakPipe.cooldownTimer

        manager.reduceAllCooldowns(0.15f)

        val expectedReduction = (25000f / 1000f) * 0.15f
        assertEquals(
            "El cooldown debe reducirse en 15% del total (3.75 segundos)",
            cooldownBefore - expectedReduction,
            manager.breakPipe.cooldownTimer,
            0.01f
        )
    }

    @Test
    fun `reduceAllCooldowns - reduce todos los poderes en cooldown simultaneamente`() {
        // Activar los 3 poderes para que entren en cooldown
        manager.activate(manager.invincibility)
        manager.activate(manager.breakPipe)
        manager.update(5.1f) // Invencibilidad expira y entra en cooldown

        val cdInvBefore = manager.invincibility.cooldownTimer
        val cdBreakBefore = manager.breakPipe.cooldownTimer

        manager.reduceAllCooldowns(0.15f)

        assertTrue(
            "El cooldown de invencibilidad debe haberse reducido",
            manager.invincibility.cooldownTimer < cdInvBefore
        )
        assertTrue(
            "El cooldown de romper tuberia debe haberse reducido",
            manager.breakPipe.cooldownTimer < cdBreakBefore
        )
    }

    @Test
    fun `reduceAllCooldowns - no reduce por debajo de 0`() {
        manager.activate(manager.breakPipe)
        manager.update(24f) // Queda ~1 segundo de cooldown

        // Reducir 100% (forzar a 0)
        manager.reduceAllCooldowns(1f)

        assertTrue(
            "El cooldown no debe ser negativo",
            manager.breakPipe.cooldownTimer >= 0f
        )
    }

    @Test
    fun `reduceAllCooldowns - no afecta poderes que no estan en cooldown`() {
        val timerBefore = manager.invincibility.cooldownTimer

        manager.reduceAllCooldowns(0.15f)

        assertEquals(
            "Un poder sin cooldown activo no debe verse afectado",
            timerBefore, manager.invincibility.cooldownTimer, 0.001f
        )
    }

    // =====================================================================
    // RESET
    // =====================================================================

    @Test
    fun `reset - todos los poderes vuelven a su estado inicial`() {
        manager.activate(manager.invincibility)
        manager.activate(manager.breakPipe)
        manager.update(2f)

        manager.reset()

        manager.allPowerUps.forEach { power ->
            assertFalse("${power.name} no debe estar activo despues del reset", power.isActive)
            assertEquals("${power.name} activeTimer debe ser 0", 0f, power.activeTimer)
            assertEquals("${power.name} cooldownTimer debe ser 0", 0f, power.cooldownTimer)
            assertTrue("${power.name} debe estar listo despues del reset", power.isReady)
        }
    }

    // =====================================================================
    // FRACCIONES PARA UI (ARCOS DE PROGRESO)
    // =====================================================================

    @Test
    fun `cooldownFraction - retorna 1 cuando el cooldown acaba de empezar`() {
        manager.activate(manager.breakPipe)

        assertEquals(
            "La fraccion debe ser 1.0 al inicio del cooldown",
            1f, manager.breakPipe.cooldownFraction, 0.01f
        )
    }

    @Test
    fun `cooldownFraction - retorna 0 cuando no hay cooldown`() {
        assertEquals(
            "La fraccion debe ser 0 cuando no hay cooldown",
            0f, manager.breakPipe.cooldownFraction, 0.01f
        )
    }

    @Test
    fun `activeFraction - retorna 1 cuando el poder acaba de activarse`() {
        manager.activate(manager.invincibility)

        assertEquals(
            "La fraccion activa debe ser 1.0 al inicio",
            1f, manager.invincibility.activeFraction, 0.01f
        )
    }

    @Test
    fun `activeFraction - disminuye con el tiempo`() {
        manager.activate(manager.invincibility)
        manager.update(2.5f) // Mitad de la duracion (5s)

        assertEquals(
            "A la mitad de la duracion, la fraccion debe ser ~0.5",
            0.5f, manager.invincibility.activeFraction, 0.05f
        )
    }
}
