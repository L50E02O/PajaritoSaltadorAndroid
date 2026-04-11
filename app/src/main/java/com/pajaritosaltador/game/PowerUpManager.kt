package com.pajaritosaltador.game

/**
 * Gestor centralizado de poderes.
 * Controla temporizadores de cooldown (CDR) y duraciones de efectos de forma independiente.
 */
class PowerUpManager {

    val invincibility = PowerUp(
        name = "Invencibilidad",
        duration = 5000L,
        cooldown = 25000L
    )

    val speedX2 = PowerUp(
        name = "Modo X2",
        duration = 30000L,
        cooldown = 25000L
    )

    val breakPipe = PowerUp(
        name = "Romper Tuberia",
        duration = 0L,
        cooldown = 25000L
    )

    val allPowerUps: List<PowerUp> get() = listOf(invincibility, speedX2, breakPipe)

    /**
     * Actualiza todos los temporizadores de poderes
     */
    fun update(deltaTime: Float) {
        allPowerUps.forEach { power ->
            if (power.cooldownTimer > 0f && !power.isActive) {
                power.cooldownTimer -= deltaTime
                if (power.cooldownTimer <= 0f) {
                    power.cooldownTimer = 0f
                }
            }

            if (power.isActive && power.duration > 0) {
                power.activeTimer -= deltaTime
                if (power.activeTimer <= 0f) {
                    deactivate(power)
                }
            }
        }
    }

    /**
     * Intenta activar un poder. Retorna true si se activo exitosamente.
     */
    fun activate(power: PowerUp): Boolean {
        if (!power.isReady) return false

        power.lastUsed = System.currentTimeMillis()

        if (power.duration > 0) {
            power.isActive = true
            power.activeTimer = power.duration / 1000f
        } else {
            power.isActive = false
            power.cooldownTimer = power.cooldown / 1000f
        }
        return true
    }

    /**
     * Desactiva un poder y comienza su cooldown
     */
    private fun deactivate(power: PowerUp) {
        power.isActive = false
        power.activeTimer = 0f
        power.cooldownTimer = power.cooldown / 1000f
    }

    /**
     * Reduce todos los cooldowns actuales en un porcentaje dado.
     * Llamado al recoger un coleccionable.
     */
    fun reduceAllCooldowns(fraction: Float = 0.15f) {
        allPowerUps.forEach { power ->
            if (power.cooldownTimer > 0f) {
                val reduction = (power.cooldown / 1000f) * fraction
                power.cooldownTimer = (power.cooldownTimer - reduction).coerceAtLeast(0f)
            }
        }
    }

    /**
     * Resetea todos los poderes a su estado inicial
     */
    fun reset() {
        allPowerUps.forEach { power ->
            power.isActive = false
            power.activeTimer = 0f
            power.cooldownTimer = 0f
            power.lastUsed = 0
        }
    }
}
