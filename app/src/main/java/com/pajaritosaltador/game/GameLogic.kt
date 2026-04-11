package com.pajaritosaltador.game

/**
 * Logica principal del juego.
 * Gestiona el estado, pajaro, tuberias, coleccionables y poderes.
 */
class GameLogic(
    private val viewportWidth: Float,
    private val viewportHeight: Float
) {

    enum class GameState {
        START, PLAYING, PAUSED, GAME_OVER
    }

    var state = GameState.START
        private set

    var score = 0
        private set

    var highScore = 0

    // Pajaro
    val bird = GameObject(
        x = viewportWidth * 0.2f,
        y = viewportHeight / 2,
        width = viewportWidth * 0.1f,
        height = viewportWidth * 0.075f,
        velocity = 0f,
        rotation = 0f
    )

    var birdWingPhase = 0f
    var birdIsDying = false
    var birdDeathAnimationTime = 0f

    // Tuberias
    val pipes = mutableListOf<Pipe>()
    val pipeWidth = viewportWidth * 0.15f
    val pipeGap = viewportHeight * 0.25f
    private var basePipeSpeed = viewportWidth * 0.375f
    var pipeSpeed = basePipeSpeed
    private var pipeIdCounter = 0

    // Coleccionables
    val collectibles = mutableListOf<Collectible>()
    private var collectibleSpawnChance = 0.35f

    // Fisica (proporcional al viewport)
    private val baseGravity = viewportHeight * 1.67f
    private val baseJumpForce = viewportHeight * 0.42f
    private val baseMaxVelocity = viewportHeight * 0.67f
    var gravity = baseGravity
    var jumpForce = baseJumpForce
    var maxVelocity = baseMaxVelocity

    // Dificultad progresiva
    private var difficultyLevel = 0

    // Sistema de poderes
    val powerUpManager = PowerUpManager()

    // Multiplicador de velocidad (modo rapido, antes x2)
    val speedMultiplier: Float
        get() = if (powerUpManager.speedX2.isActive) 1.50f else 1f

    /** Tubos que se dibujan cayendo tras romper tuberia (no colisionan). */
    val pipeTumbleAnimations = mutableListOf<PipeTumblePair>()

    // Scroll del fondo
    var backgroundScrollX = 0f

    // Timers
    private var pipeSpawnTimer = 0f
    private var pipeSpawnInterval = 1.5f

    // Callbacks
    var onScoreChanged: ((Int) -> Unit)? = null
    var onHighScoreChanged: ((Int) -> Unit)? = null
    var onGameOver: ((Int) -> Unit)? = null
    var onDifficultyIncrease: ((Int) -> Unit)? = null
    var onCollectiblePickup: (() -> Unit)? = null
    var onPipeDestroyed: (() -> Unit)? = null
    var onPauseChanged: ((Boolean) -> Unit)? = null

    /**
     * Inicia el juego
     */
    fun startGame() {
        state = GameState.PLAYING
        score = 0

        bird.x = viewportWidth * 0.2f
        bird.y = viewportHeight / 2
        bird.velocity = 0f
        bird.rotation = 0f
        birdWingPhase = 0f
        birdIsDying = false
        birdDeathAnimationTime = 0f

        pipes.clear()
        collectibles.clear()
        pipeTumbleAnimations.clear()

        gravity = baseGravity
        jumpForce = baseJumpForce
        pipeSpeed = basePipeSpeed
        difficultyLevel = 0
        backgroundScrollX = 0f

        powerUpManager.reset()

        pipeSpawnTimer = 0f
        pipeIdCounter = 0

        onScoreChanged?.invoke(score)
    }

    /**
     * Actualiza el juego un frame
     */
    fun update(deltaTime: Float, shouldJump: Boolean) {
        updatePipeTumbleAnimations(deltaTime)
        if (state != GameState.PLAYING) return

        val effectiveDelta = deltaTime * speedMultiplier

        updateBird(deltaTime, shouldJump)
        updatePipes(effectiveDelta)
        updateCollectibles(effectiveDelta)
        powerUpManager.update(effectiveDelta)
        checkCollisions()

        backgroundScrollX += pipeSpeed * effectiveDelta * 0.3f
        if (backgroundScrollX > viewportWidth) {
            backgroundScrollX -= viewportWidth
        }
    }

    /**
     * Actualiza la fisica y animacion del pajaro
     */
    private fun updateBird(deltaTime: Float, shouldJump: Boolean) {
        if (birdIsDying) {
            birdDeathAnimationTime += deltaTime
            Physics.applyGravity(bird, gravity * 1.5f, deltaTime)
            Physics.clampVelocity(bird, maxVelocity * 1.5f)
            bird.y += bird.velocity * deltaTime

            val targetRotation = Math.PI.toFloat()
            bird.rotation += (targetRotation - bird.rotation) * 0.15f
            birdWingPhase = 0f

            if (bird.y + bird.height >= viewportHeight || birdDeathAnimationTime > 2f) {
                bird.y = (viewportHeight - bird.height).coerceAtMost(bird.y)
            }
            return
        }

        if (shouldJump) {
            Physics.applyJump(bird, jumpForce)
            birdWingPhase = 0f
        }

        Physics.applyGravity(bird, gravity, deltaTime)
        Physics.clampVelocity(bird, maxVelocity)
        bird.y += bird.velocity * deltaTime

        val targetRotation = (bird.velocity * 0.002f).coerceAtMost(Math.PI.toFloat() / 2)
        bird.rotation += (targetRotation - bird.rotation) * 0.1f

        val wingSpeed = if (bird.velocity < 0) 15f else 8f
        birdWingPhase += deltaTime * wingSpeed
        if (birdWingPhase > Math.PI.toFloat() * 2) {
            birdWingPhase -= Math.PI.toFloat() * 2
        }

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
     * Actualiza posicion de tuberias y genera nuevas
     */
    private fun updatePipes(effectiveDelta: Float) {
        val speed = pipeSpeed * effectiveDelta
        for (i in pipes.indices) {
            pipes[i].x -= speed
        }

        pipes.removeAll { it.x + pipeWidth < -50f }

        pipeSpawnTimer += effectiveDelta
        if (pipeSpawnTimer >= pipeSpawnInterval) {
            spawnPipe()
            pipeSpawnTimer = 0f
        }

        for (i in pipes.indices) {
            val pipe = pipes[i]
            if (!pipe.passed && pipe.x + pipeWidth < bird.x) {
                pipe.passed = true
                var pairPassed = false
                for (j in pipes.indices) {
                    val other = pipes[j]
                    if (j != i && other.pairId == pipe.pairId && other.passed) {
                        pairPassed = true
                        break
                    }
                }
                if (pairPassed) {
                    score++
                    onScoreChanged?.invoke(score)

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
     * Genera un nuevo par de tuberias con posibilidad de coleccionable
     */
    private fun spawnPipe() {
        val minGapY = viewportHeight * 0.15f
        val maxGapY = viewportHeight - pipeGap - minGapY
        val gapY = (Math.random() * (maxGapY - minGapY)).toFloat() + minGapY

        val currentPairId = pipeIdCounter++

        pipes.add(Pipe(
            x = viewportWidth,
            y = 0f,
            width = pipeWidth,
            height = gapY,
            passed = false,
            pairId = currentPairId
        ))

        pipes.add(Pipe(
            x = viewportWidth,
            y = gapY + pipeGap,
            width = pipeWidth,
            height = viewportHeight - (gapY + pipeGap),
            passed = false,
            pairId = currentPairId
        ))

        if (Math.random() < collectibleSpawnChance) {
            val collectibleY = gapY + pipeGap / 2
            val collectibleX = viewportWidth + pipeWidth / 2 + pipeWidth * 1.5f
            collectibles.add(Collectible(
                x = collectibleX,
                y = collectibleY,
                radius = viewportWidth * 0.03f
            ))
        }
    }

    /**
     * Actualiza posicion y animacion de coleccionables
     */
    private fun updateCollectibles(effectiveDelta: Float) {
        val speed = pipeSpeed * effectiveDelta
        for (i in collectibles.indices) {
            val c = collectibles[i]
            c.x -= speed
            c.animationPhase += effectiveDelta * 4f
        }
        collectibles.removeAll { it.collected || it.x < -50f }
    }

    /**
     * Verifica colisiones del pajaro con tuberias y coleccionables
     */
    private fun checkCollisions() {
        if (birdIsDying) return

        val birdRect = CollisionUtils.createHitbox(bird)

        if (!powerUpManager.invincibility.isActive) {
            for (pipe in pipes) {
                val pipeRect = CollisionUtils.pipeToRect(pipe)
                if (CollisionUtils.checkRectCollision(birdRect, pipeRect)) {
                    startDeathAnimation()
                    gameOver()
                    return
                }
            }
        }

        val iterator = collectibles.iterator()
        while (iterator.hasNext()) {
            val c = iterator.next()
            if (!c.collected && CollisionUtils.checkCircleRectCollision(c.x, c.y, c.radius, birdRect)) {
                c.collected = true
                powerUpManager.reduceAllCooldowns(0.15f)
                onCollectiblePickup?.invoke()
            }
        }
    }

    /**
     * Destruye la tuberia (par) mas cercana al frente del pajaro
     */
    fun destroyNearestPipe(): Boolean {
        val nearest = pipes
            .filter { it.x + it.width > bird.x }
            .minByOrNull { it.x } ?: return false
        val nearestPairId = nearest.pairId

        if (!powerUpManager.activate(powerUpManager.breakPipe)) return false

        val pairPipes = pipes.filter { it.pairId == nearestPairId }
        val top = pairPipes.find { it.y == 0f }
        val bottom = pairPipes.find { it.y != 0f }

        if (top != null && bottom != null) {
            val baseVx = -pipeSpeed
            pipeTumbleAnimations.add(
                PipeTumblePair(
                    top = PipeTumblePiece(
                        x = top.x,
                        y = top.y,
                        width = top.width,
                        height = top.height,
                        isTopPipe = true,
                        velX = baseVx - viewportWidth * 0.14f,
                        velY = -viewportHeight * 0.28f,
                        rotVelDegPerSec = 520f
                    ),
                    bottom = PipeTumblePiece(
                        x = bottom.x,
                        y = bottom.y,
                        width = bottom.width,
                        height = bottom.height,
                        isTopPipe = false,
                        velX = baseVx - viewportWidth * 0.11f,
                        velY = viewportHeight * 0.18f,
                        rotVelDegPerSec = -460f
                    )
                )
            )
        }

        pipes.removeAll { it.pairId == nearestPairId }
        onPipeDestroyed?.invoke()
        return true
    }

    private fun updatePipeTumbleAnimations(deltaTime: Float) {
        if (pipeTumbleAnimations.isEmpty()) return
        val gravity = viewportHeight * 2.4f
        val iter = pipeTumbleAnimations.iterator()
        while (iter.hasNext()) {
            val pair = iter.next()
            pair.age += deltaTime
            for (piece in arrayOf(pair.top, pair.bottom)) {
                piece.velY += gravity * deltaTime
                piece.x += piece.velX * deltaTime
                piece.y += piece.velY * deltaTime
                piece.rotationDeg += piece.rotVelDegPerSec * deltaTime
            }
            val outOfView = pair.top.x + pair.top.width < -120f ||
                pair.bottom.x + pair.bottom.width < -120f ||
                pair.top.y > viewportHeight + 120f ||
                pair.age > 1.4f
            if (outOfView) iter.remove()
        }
    }

    /**
     * Activa la invencibilidad
     */
    fun activateInvincibility(): Boolean {
        return powerUpManager.activate(powerUpManager.invincibility)
    }

    /**
     * Activa el modo X2
     */
    fun activateSpeedX2(): Boolean {
        return powerUpManager.activate(powerUpManager.speedX2)
    }

    /**
     * Pausa el juego si esta en estado PLAYING
     */
    fun pauseGame() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED
            onPauseChanged?.invoke(true)
        }
    }

    /**
     * Reanuda el juego si esta en estado PAUSED
     */
    fun resumeGame() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING
            onPauseChanged?.invoke(false)
        }
    }

    /**
     * Vuelve al estado inicial (menu principal)
     */
    fun returnToStart() {
        state = GameState.START
        pipes.clear()
        collectibles.clear()
        pipeTumbleAnimations.clear()
        powerUpManager.reset()
    }

    private fun startDeathAnimation() {
        if (birdIsDying) return
        birdIsDying = true
        birdDeathAnimationTime = 0f
        bird.velocity = bird.velocity.coerceAtLeast(200f)
    }

    private fun gameOver() {
        if (state != GameState.PLAYING) return
        state = GameState.GAME_OVER

        if (score > highScore) {
            highScore = score
            onHighScoreChanged?.invoke(highScore)
        }
        onGameOver?.invoke(score)
    }

    private fun increaseDifficulty() {
        gravity = baseGravity * (1f + difficultyLevel * 0.1f)
        pipeSpeed = basePipeSpeed * (1f + difficultyLevel * 0.15f)
        pipeSpawnInterval = (1.5f / (1f + difficultyLevel * 0.1f)).coerceAtLeast(0.8f)
        onDifficultyIncrease?.invoke(difficultyLevel)
    }
}
