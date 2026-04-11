package com.pajaritosaltador.game

/**
 * Modulo de fisica del juego
 */
object Physics {

    /**
     * Aplica gravedad a un objeto
     */
    fun applyGravity(obj: GameObject, gravity: Float, deltaTime: Float) {
        obj.velocity += gravity * deltaTime
    }

    /**
     * Aplica fuerza de salto
     */
    fun applyJump(obj: GameObject, jumpForce: Float) {
        obj.velocity = -jumpForce
    }

    /**
     * Limita la velocidad maxima
     */
    fun clampVelocity(obj: GameObject, maxVelocity: Float) {
        obj.velocity = obj.velocity.coerceIn(-maxVelocity, maxVelocity)
    }
}

/**
 * Clase base para objetos del juego con fisica
 */
data class GameObject(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f,
    var velocity: Float = 0f,
    var rotation: Float = 0f
)

/**
 * Rectangulo para colisiones
 */
data class Rect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * Representa un tubo
 */
data class Pipe(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var passed: Boolean,
    var pairId: Int = 0
)

/**
 * Representa un coleccionable que reduce cooldowns
 */
data class Collectible(
    var x: Float,
    var y: Float,
    var radius: Float = 12f,
    var collected: Boolean = false,
    var animationPhase: Float = 0f
)

/**
 * Modelo de datos para un poder del jugador.
 * Facilita que el sistema de reduccion de CDR simplemente reste milisegundos a lastUsed.
 */
data class PowerUp(
    val name: String,
    val duration: Long = 0,
    val cooldown: Long,
    var lastUsed: Long = 0,
    var isActive: Boolean = false,
    var activeTimer: Float = 0f,
    var cooldownTimer: Float = 0f
) {
    val isOnCooldown: Boolean get() = cooldownTimer > 0f && !isActive
    val isReady: Boolean get() = !isActive && cooldownTimer <= 0f

    /**
     * Fraccion de cooldown restante (0.0 a 1.0) para el arco de progreso visual
     */
    val cooldownFraction: Float
        get() = if (cooldown > 0) (cooldownTimer / (cooldown / 1000f)).coerceIn(0f, 1f) else 0f

    /**
     * Fraccion de duracion restante (0.0 a 1.0)
     */
    val activeFraction: Float
        get() = if (duration > 0) (activeTimer / (duration / 1000f)).coerceIn(0f, 1f) else 0f
}
