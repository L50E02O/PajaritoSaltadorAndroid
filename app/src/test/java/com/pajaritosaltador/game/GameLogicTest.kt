package com.pajaritosaltador.game

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias para GameLogic.
 *
 * Verifica la logica central del juego: estados, generacion de tuberias,
 * puntuacion, poderes, coleccionables, pausa y destruccion de tuberias.
 *
 * Estrategia de prueba:
 * - Se usa un viewport fijo de 360x640 (igual que en produccion).
 * - Se simula el paso del tiempo con update() para probar comportamiento real.
 * - Se verifican transiciones de estado y callbacks.
 */
class GameLogicTest {

    private lateinit var game: GameLogic

    @Before
    fun setUp() {
        game = GameLogic(360f, 640f)
    }

    // =====================================================================
    // ESTADO INICIAL Y TRANSICIONES
    // =====================================================================

    @Test
    fun `estado inicial - el juego empieza en estado START`() {
        assertEquals(
            "El estado inicial debe ser START",
            GameLogic.GameState.START, game.state
        )
    }

    @Test
    fun `estado inicial - el puntaje empieza en 0`() {
        assertEquals("El puntaje inicial debe ser 0", 0, game.score)
    }

    @Test
    fun `estado inicial - no hay tuberias ni coleccionables`() {
        assertTrue("No debe haber tuberias al inicio", game.pipes.isEmpty())
        assertTrue("No debe haber coleccionables al inicio", game.collectibles.isEmpty())
    }

    @Test
    fun `startGame - cambia el estado a PLAYING`() {
        game.startGame()

        assertEquals(
            "Despues de startGame, el estado debe ser PLAYING",
            GameLogic.GameState.PLAYING, game.state
        )
    }

    @Test
    fun `startGame - resetea el puntaje a 0`() {
        game.startGame()
        // Simular que se gano un punto manualmente
        game.update(2f, false) // Generar tuberias
        game.startGame() // Reiniciar

        assertEquals("El puntaje debe resetearse a 0", 0, game.score)
    }

    @Test
    fun `startGame - limpia tuberias y coleccionables`() {
        game.startGame()
        game.update(2f, false) // Generar tuberias
        assertTrue("Debe haber tuberias despues de jugar", game.pipes.isNotEmpty())

        game.startGame()
        assertTrue("Las tuberias deben limpiarse al reiniciar", game.pipes.isEmpty())
        assertTrue("Los coleccionables deben limpiarse al reiniciar", game.collectibles.isEmpty())
    }

    @Test
    fun `startGame - resetea los poderes`() {
        game.startGame()
        game.powerUpManager.activate(game.powerUpManager.breakPipe)
        assertTrue("breakPipe debe estar en cooldown", game.powerUpManager.breakPipe.isOnCooldown)

        game.startGame()
        assertTrue(
            "Todos los poderes deben estar listos despues de reiniciar",
            game.powerUpManager.breakPipe.isReady
        )
    }

    // =====================================================================
    // FISICA DEL PAJARO
    // =====================================================================

    @Test
    fun `update - la gravedad hace caer al pajaro`() {
        game.startGame()
        val initialY = game.bird.y

        game.update(0.1f, false)

        assertTrue(
            "El pajaro debe caer (Y aumenta) por la gravedad",
            game.bird.y > initialY
        )
    }

    @Test
    fun `update - el salto hace subir al pajaro`() {
        game.startGame()
        // Dejar caer un poco
        game.update(0.1f, false)
        val yBeforeJump = game.bird.y

        // Saltar
        game.update(0.016f, true)

        assertTrue(
            "La velocidad del pajaro debe ser negativa (hacia arriba) despues de saltar",
            game.bird.velocity < 0
        )
    }

    @Test
    fun `update - el pajaro no puede subir por encima del viewport`() {
        game.startGame()

        // Forzar muchos saltos para intentar salir por arriba
        repeat(20) {
            game.update(0.016f, true)
        }

        assertTrue(
            "El pajaro no debe tener Y negativo (limite superior)",
            game.bird.y >= 0f
        )
    }

    @Test
    fun `update - backgroundScrollX avanza de forma continua sin reinicio brusco`() {
        game.startGame()
        var prev = game.backgroundScrollX
        repeat(400) {
            game.update(1f / 60f, false)
            assertTrue(
                "El scroll de fondo debe aumentar monotonicamente",
                game.backgroundScrollX >= prev - 0.001f
            )
            prev = game.backgroundScrollX
        }
    }

    // =====================================================================
    // GENERACION DE TUBERIAS
    // =====================================================================

    @Test
    fun `update - genera tuberias despues del intervalo de spawn`() {
        game.startGame()

        // Simular 2 segundos (el intervalo de spawn es 1.5s)
        game.update(2f, false)

        assertTrue(
            "Deben generarse tuberias despues de 2 segundos",
            game.pipes.isNotEmpty()
        )
    }

    @Test
    fun `update - las tuberias se generan en pares (superior e inferior)`() {
        game.startGame()
        game.update(2f, false)

        assertTrue(
            "Las tuberias deben generarse en pares (numero par)",
            game.pipes.size % 2 == 0
        )
    }

    @Test
    fun `update - las tuberias tienen el mismo pairId por par`() {
        game.startGame()
        game.update(2f, false)

        if (game.pipes.size >= 2) {
            val firstPairId = game.pipes[0].pairId
            val matchingPipes = game.pipes.filter { it.pairId == firstPairId }
            assertEquals(
                "Cada par de tuberias debe tener exactamente 2 elementos con el mismo pairId",
                2, matchingPipes.size
            )
        }
    }

    @Test
    fun `update - las tuberias se mueven hacia la izquierda`() {
        game.startGame()
        val dt = 1f / 60f
        repeat(100) {
            game.update(dt, true)
        }

        assertTrue("Debe haberse generado al menos un par", game.pipes.size >= 2)
        val initialXs = game.pipes.map { it.x }
        game.update(dt, true)
        assertEquals(game.pipes.size, initialXs.size)
        for (i in game.pipes.indices) {
            assertTrue(
                "La tuberia $i debe moverse hacia la izquierda",
                game.pipes[i].x < initialXs[i]
            )
        }
    }

    // =====================================================================
    // SISTEMA DE PAUSA
    // =====================================================================

    @Test
    fun `pauseGame - cambia el estado a PAUSED desde PLAYING`() {
        game.startGame()
        game.pauseGame()

        assertEquals(
            "El estado debe ser PAUSED despues de pausar",
            GameLogic.GameState.PAUSED, game.state
        )
    }

    @Test
    fun `pauseGame - no hace nada si no esta jugando`() {
        game.pauseGame()

        assertEquals(
            "Pausar desde START no debe cambiar el estado",
            GameLogic.GameState.START, game.state
        )
    }

    @Test
    fun `resumeGame - cambia el estado de PAUSED a PLAYING`() {
        game.startGame()
        game.pauseGame()
        game.resumeGame()

        assertEquals(
            "El estado debe ser PLAYING despues de reanudar",
            GameLogic.GameState.PLAYING, game.state
        )
    }

    @Test
    fun `update - no procesa logica mientras esta pausado`() {
        game.startGame()
        game.update(2f, false) // Generar tuberias
        game.pauseGame()

        val pipesBeforePause = game.pipes.map { it.x }
        game.update(1f, false)
        val pipesAfterPause = game.pipes.map { it.x }

        assertEquals(
            "Las tuberias no deben moverse durante la pausa",
            pipesBeforePause, pipesAfterPause
        )
    }

    @Test
    fun `returnToStart - vuelve al estado START y limpia todo`() {
        game.startGame()
        game.update(2f, false)
        game.pauseGame()
        game.returnToStart()

        assertEquals("El estado debe ser START", GameLogic.GameState.START, game.state)
        assertTrue("Las tuberias deben limpiarse", game.pipes.isEmpty())
        assertTrue("Los coleccionables deben limpiarse", game.collectibles.isEmpty())
    }

    // =====================================================================
    // DESTRUCCION DE TUBERIAS
    // =====================================================================

    @Test
    fun `destroyNearestPipe - destruye el par de tuberias mas cercano`() {
        game.startGame()
        game.update(2f, false) // Generar tuberias
        val pipesCountBefore = game.pipes.size

        val result = game.destroyNearestPipe()

        assertTrue("destroyNearestPipe debe retornar true", result)
        assertEquals(
            "Deben eliminarse exactamente 2 tuberias (un par)",
            pipesCountBefore - 2, game.pipes.size
        )
    }

    @Test
    fun `destroyNearestPipe - entra en cooldown despues de usar`() {
        game.startGame()
        game.update(2f, false)
        game.destroyNearestPipe()

        assertTrue(
            "breakPipe debe estar en cooldown despues de usar",
            game.powerUpManager.breakPipe.isOnCooldown
        )
    }

    @Test
    fun `destroyNearestPipe - no se puede usar dos veces seguidas`() {
        game.startGame()
        game.update(2f, false)
        game.destroyNearestPipe()

        val secondAttempt = game.destroyNearestPipe()

        assertFalse(
            "No debe poder destruir tuberias durante el cooldown",
            secondAttempt
        )
    }

    // =====================================================================
    // MULTIPLICADOR DE VELOCIDAD (MODO X2)
    // =====================================================================

    @Test
    fun `speedMultiplier - es 1 cuando modo speed no esta activo`() {
        assertEquals(
            "El multiplicador debe ser 1.0 por defecto",
            1f, game.speedMultiplier
        )
    }

    @Test
    fun `speedMultiplier - es 1_50 cuando modo rapido esta activo`() {
        game.startGame()
        assertTrue(
            "El poder velocidad debe activarse en partida",
            game.activateSpeedX2()
        )

        assertEquals(
            "El multiplicador debe ser 1.50 con modo rapido activo",
            1.50f,
            game.speedMultiplier,
            0.001f
        )
    }

    // =====================================================================
    // CALLBACKS
    // =====================================================================

    @Test
    fun `callback onPauseChanged - se invoca al pausar y reanudar`() {
        var pauseCallbackValues = mutableListOf<Boolean>()
        game.onPauseChanged = { paused -> pauseCallbackValues.add(paused) }

        game.startGame()
        game.pauseGame()
        game.resumeGame()

        assertEquals(
            "Debe invocarse 2 veces: una al pausar (true) y otra al reanudar (false)",
            listOf(true, false), pauseCallbackValues
        )
    }

    @Test
    fun `callback onScoreChanged - se invoca al iniciar con puntaje 0`() {
        var lastScore = -1
        game.onScoreChanged = { score -> lastScore = score }

        game.startGame()

        assertEquals(
            "El callback debe recibir 0 al iniciar",
            0, lastScore
        )
    }
}
