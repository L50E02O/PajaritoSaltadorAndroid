package com.pajaritosaltador.game

/**
 * Módulo de física del juego
 */
object Physics {
    
    /**
     * Aplica gravedad a un objeto
     */
    fun applyGravity(object_: GameObject, gravity: Float, deltaTime: Float) {
        object_.velocity += gravity * deltaTime
    }
    
    /**
     * Aplica fuerza de salto
     */
    fun applyJump(object_: GameObject, jumpForce: Float) {
        object_.velocity = -jumpForce
    }
    
    /**
     * Limita la velocidad máxima
     */
    fun clampVelocity(object_: GameObject, maxVelocity: Float) {
        if (object_.velocity > maxVelocity) {
            object_.velocity = maxVelocity
        } else if (object_.velocity < -maxVelocity) {
            object_.velocity = -maxVelocity
        }
    }
    
    /**
     * Verifica si dos rectángulos colisionan (AABB)
     */
    fun checkCollision(rect1: Rect, rect2: Rect): Boolean {
        return rect1.x < rect2.x + rect2.width &&
               rect1.x + rect1.width > rect2.x &&
               rect1.y < rect2.y + rect2.height &&
               rect1.y + rect1.height > rect2.y
    }
}

/**
 * Clase base para objetos del juego con física
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
 * Rectángulo para colisiones
 */
data class Rect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

