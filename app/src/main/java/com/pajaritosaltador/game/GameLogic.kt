package com.pajaritosaltador.game

/**
 * Lógica principal del juego
 */
class GameLogic(
    private val viewportWidth: Float,
    private val viewportHeight: Float
) {
    
    enum class GameState {
        START, PLAYING, GAME_OVER
    }
    
    var state = GameState.START
        private set
    
    var score = 0
        private set
    
    var highScore = 0
    
    // Pájaro
    val bird = GameObject(
        x = 100f,
        y = viewportHeight / 2,
        width = 40f,
        height = 30f,
        velocity = 0f,
        rotation = 0f
    )
    
    var birdWingPhase = 0f
    var birdIsDying = false
    var birdDeathAnimationTime = 0f
    
    // Tubos
    val pipes = mutableListOf<Pipe>()
    val pipeWidth = 60f
    val pipeGap = 150f
    var pipeSpeed = 150f
    
    // Física
    var gravity = 1000f
    var jumpForce = 250f
    var maxVelocity = 400f
    
    // Dificultad progresiva
    private val baseGravity = 1000f
    private val baseJumpForce = 250f
    private val basePipeSpeed = 150f
    private val basePipeGap = 150f
    private var difficultyLevel = 0
    
    // Sistema de habilidades
    data class Ability(
        var active: Boolean = false,
        var duration: Float = 3f,
        var cooldown: Float = 15f,
        var cooldownTimer: Float = 0f,
        var activeTimer: Float = 0f
    )
    
    val invulnerability = Ability()
    
    // Timers
    private var pipeSpawnTimer = 0f
    private var pipeSpawnInterval = 1.5f
    
    // Callbacks
    var onScoreChanged: ((Int) -> Unit)? = null
    var onHighScoreChanged: ((Int) -> Unit)? = null
    var onGameOver: ((Int) -> Unit)? = null
    var onDifficultyIncrease: ((Int) -> Unit)? = null
    
    /**
     * Inicia el juego
     */
    fun startGame() {
        state = GameState.PLAYING
        score = 0
        
        // Resetear pájaro
        bird.x = 100f
        bird.y = viewportHeight / 2
        bird.velocity = 0f
        bird.rotation = 0f
        birdWingPhase = 0f
        birdIsDying = false
        birdDeathAnimationTime = 0f
        
        // Limpiar tubos
        pipes.clear()
        
        // Resetear física
        gravity = baseGravity
        jumpForce = baseJumpForce
        pipeSpeed = basePipeSpeed
        difficultyLevel = 0
        
        // Resetear habilidad
        invulnerability.active = false
        invulnerability.cooldownTimer = 0f
        invulnerability.activeTimer = 0f
        
        pipeSpawnTimer = 0f
        
        onScoreChanged?.invoke(score)
    }
    
    /**
     * Actualiza el juego
     */
    fun update(deltaTime: Float, shouldJump: Boolean) {
        if (state != GameState.PLAYING) return
        
        updateBird(deltaTime, shouldJump)
        updatePipes(deltaTime)
        updateAbilities(deltaTime)
        checkCollisions()
    }
    
    /**
     * Actualiza el pájaro
     */
    private fun updateBird(deltaTime: Float, shouldJump: Boolean) {
        if (birdIsDying) {
            birdDeathAnimationTime += deltaTime
            
            // Gravedad aumentada
            Physics.applyGravity(bird, gravity * 1.5f, deltaTime)
            Physics.clampVelocity(bird, maxVelocity * 1.5f)
            
            // Rotación hacia abajo
            val targetRotation = Math.PI.toFloat()
            bird.rotation += (targetRotation - bird.rotation) * 0.15f
            
            birdWingPhase = 0f
            
            // Si toca el suelo
            if (bird.y + bird.height >= viewportHeight || birdDeathAnimationTime > 2f) {
                bird.y = (viewportHeight - bird.height).coerceAtMost(bird.y)
                return
            }
            return
        }
        
        // Salto
        if (shouldJump) {
            Physics.applyJump(bird, jumpForce)
            birdWingPhase = 0f
        }
        
        // Gravedad
        Physics.applyGravity(bird, gravity, deltaTime)
        Physics.clampVelocity(bird, maxVelocity)
        
        // Rotación basada en velocidad
        val targetRotation = (bird.velocity * 0.002f).coerceAtMost(Math.PI.toFloat() / 2)
        bird.rotation += (targetRotation - bird.rotation) * 0.1f
        
        // Animación de alas
        val wingSpeed = if (bird.velocity < 0) 15f else 8f
        birdWingPhase += deltaTime * wingSpeed
        if (birdWingPhase > Math.PI.toFloat() * 2) {
            birdWingPhase -= Math.PI.toFloat() * 2
        }
        
        // Límites
        if (bird.y < 0) {
            bird.y = 0f
            bird.velocity = 0f
        }
        if (bird.y + bird.height > viewportHeight) {
            bird.y = viewportHeight - bird.height
            startDeathAnimation()
            gameOver()
        }
    }
    
    /**
     * Actualiza los tubos
     */
    private fun updatePipes(deltaTime: Float) {
        // Mover tubos
        pipes.forEach { pipe ->
            pipe.x -= pipeSpeed * deltaTime
        }
        
        // Eliminar tubos fuera de pantalla
        pipes.removeAll { it.x + pipeWidth < -50f || it.x > viewportWidth + 50f }
        
        // Generar nuevos tubos
        pipeSpawnTimer += deltaTime
        if (pipeSpawnTimer >= pipeSpawnInterval) {
            spawnPipe()
            pipeSpawnTimer = 0f
        }
        
        // Verificar si el pájaro pasó un tubo
        pipes.forEach { pipe ->
            if (!pipe.passed && pipe.x + pipeWidth < bird.x) {
                pipe.passed = true
                // Solo contar cuando pasan ambos tubos del par
                val pair = pipes.filter { 
                    Math.abs(it.x - pipe.x) < 10f && it.passed 
                }
                if (pair.size == 2) {
                    val oldScore = score
                    score++
                    onScoreChanged?.invoke(score)
                    
                    // Verificar dificultad (cada 25 puntos)
                    val newLevel = score / 25
                    if (newLevel > difficultyLevel) {
                        difficultyLevel = newLevel
                        increaseDifficulty()
                    }
                }
            }
        }
    }
    
    /**
     * Genera un nuevo par de tubos
     */
    private fun spawnPipe() {
        val gapY = (Math.random() * (viewportHeight - pipeGap - 200)).toFloat() + 100f
        
        // Tubo superior
        pipes.add(Pipe(
            x = viewportWidth,
            y = 0f,
            width = pipeWidth,
            height = gapY,
            passed = false
        ))
        
        // Tubo inferior
        pipes.add(Pipe(
            x = viewportWidth,
            y = gapY + pipeGap,
            width = pipeWidth,
            height = viewportHeight - (gapY + pipeGap),
            passed = false
        ))
    }
    
    /**
     * Verifica colisiones
     */
    private fun checkCollisions() {
        if (invulnerability.active || birdIsDying) return
        
        val birdRect = Rect(bird.x, bird.y, bird.width, bird.height)
        
        pipes.forEach { pipe ->
            val pipeRect = Rect(pipe.x, pipe.y, pipe.width, pipe.height)
            if (Physics.checkCollision(birdRect, pipeRect)) {
                startDeathAnimation()
                gameOver()
            }
        }
    }
    
    /**
     * Inicia animación de muerte
     */
    private fun startDeathAnimation() {
        if (birdIsDying) return
        birdIsDying = true
        birdDeathAnimationTime = 0f
        bird.velocity = bird.velocity.coerceAtLeast(200f)
    }
    
    /**
     * Game Over
     */
    private fun gameOver() {
        if (state != GameState.PLAYING) return
        
        state = GameState.GAME_OVER
        
        if (score > highScore) {
            highScore = score
            onHighScoreChanged?.invoke(highScore)
        }
        
        onGameOver?.invoke(score)
    }
    
    /**
     * Actualiza habilidades
     */
    private fun updateAbilities(deltaTime: Float) {
        val ability = invulnerability
        
        // Actualizar cooldown
        if (ability.cooldownTimer > 0) {
            ability.cooldownTimer -= deltaTime
            if (ability.cooldownTimer <= 0) {
                ability.cooldownTimer = 0f
            }
        }
        
        // Actualizar duración activa
        if (ability.active) {
            ability.activeTimer -= deltaTime
            if (ability.activeTimer <= 0) {
                ability.active = false
                ability.activeTimer = 0f
                ability.cooldownTimer = ability.cooldown
            }
        }
    }
    
    /**
     * Activa la habilidad de invulnerabilidad
     */
    fun activateInvulnerability(): Boolean {
        val ability = invulnerability
        
        if (ability.active || ability.cooldownTimer > 0) {
            return false
        }
        
        ability.active = true
        ability.activeTimer = ability.duration
        ability.cooldownTimer = 0f
        
        return true
    }
    
    /**
     * Aumenta la dificultad
     */
    private fun increaseDifficulty() {
        gravity = baseGravity * (1f + difficultyLevel * 0.1f)
        pipeSpeed = basePipeSpeed * (1f + difficultyLevel * 0.15f)
        pipeSpawnInterval = (basePipeGap / pipeSpeed).coerceAtMost(1.2f)
        onDifficultyIncrease?.invoke(difficultyLevel)
    }
}

/**
 * Representa un tubo
 */
data class Pipe(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var passed: Boolean
)

