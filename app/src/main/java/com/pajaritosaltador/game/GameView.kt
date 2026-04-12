package com.pajaritosaltador.game

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.*

/**
 * Vista del juego con renderizado en Canvas.
 * Usa un viewport virtual de 9:16 y escala a cualquier pantalla.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private val surfaceHolderRef: SurfaceHolder = getHolder()
    private var gameThread: GameThread? = null
    private var isRunning = false

    private val viewportWidth = 360f
    private val viewportHeight = 640f

    lateinit var gameLogic: GameLogic
        private set

    // Paints reutilizables
    private val birdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shieldStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val collectiblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val collectibleGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val bgPaint = Paint()
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    // Paints cacheados para evitar allocations en el game loop
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(40, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(70, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val x2GlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(45, 255, 120, 40)
        style = Paint.Style.FILL
    }
    private val deathRedPaint = Paint().apply { color = Color.argb(76, 255, 0, 0) }
    private val eyeStrokePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val beakPath = Path()
    private val wingRect = RectF()

    // Gradiente de fondo cacheado (se recrea solo si cambia el viewport)
    private var cachedBgGradient: LinearGradient? = null
    private var cachedViewportH = 0f

    // Colores
    private val skyTop = Color.parseColor("#87CEEB")
    private val skyBottom = Color.parseColor("#E0F7FA")
    private val birdYellow = Color.parseColor("#FFD700")
    private val birdOrange = Color.parseColor("#FF8C00")
    private val shieldGold = Color.parseColor("#FFD700")
    private val collectibleCyan = Color.parseColor("#00E5FF")
    private val collectiblePurple = Color.parseColor("#AA00FF")
    private val groundBrown = Color.parseColor("#8D6E63")
    private val groundGreen = Color.parseColor("#66BB6A")

    // Nubes: base horizontal en [0,1], parallax distinto por capa (bucle continuo sin reset global)
    private data class Cloud(val baseX: Float, val y: Float, val scale: Float, val parallax: Float)
    private val clouds = listOf(
        Cloud(0.05f, 0.10f, 1.0f, 0.32f),
        Cloud(0.38f, 0.14f, 0.85f, 0.26f),
        Cloud(0.72f, 0.09f, 1.25f, 0.38f),
        Cloud(0.18f, 0.22f, 0.75f, 0.22f),
        Cloud(0.55f, 0.20f, 1.1f, 0.30f),
        Cloud(0.88f, 0.16f, 0.95f, 0.34f),
        Cloud(0.28f, 0.28f, 0.65f, 0.18f)
    )

    // Input
    private var jumpRequested = false
    private var lastTouchTime = 0L
    private val touchCooldown = 80L

    // Callbacks al Activity
    var onScoreUpdate: ((Int) -> Unit)? = null
    var onHighScoreUpdate: ((Int) -> Unit)? = null
    var onGameOver: ((Int) -> Unit)? = null
    var onGameStart: (() -> Unit)? = null
    var onPowerUpStateChanged: (() -> Unit)? = null
    var onPauseChanged: ((Boolean) -> Unit)? = null

    private val uiHandler = Handler(Looper.getMainLooper())

    init {
        surfaceHolderRef.addCallback(this)
        isFocusable = true
        gameLogic = GameLogic(viewportWidth, viewportHeight)
        setupGameCallbacks()
    }

    private fun setupGameCallbacks() {
        gameLogic.onScoreChanged = { score ->
            uiHandler.post { onScoreUpdate?.invoke(score) }
        }
        gameLogic.onHighScoreChanged = { hs ->
            uiHandler.post { onHighScoreUpdate?.invoke(hs) }
        }
        gameLogic.onGameOver = { score ->
            uiHandler.post { onGameOver?.invoke(score) }
        }
        gameLogic.onCollectiblePickup = {
            uiHandler.post { onPowerUpStateChanged?.invoke() }
        }
        gameLogic.onPipeDestroyed = {
            uiHandler.post { onPowerUpStateChanged?.invoke() }
        }
        gameLogic.onPauseChanged = { paused ->
            uiHandler.post { onPauseChanged?.invoke(paused) }
        }
    }

    fun startGame() {
        gameLogic.birdIsDying = false
        gameLogic.birdDeathAnimationTime = 0f
        gameLogic.startGame()
        uiHandler.post { onGameStart?.invoke() }
    }

    fun activateInvincibility(): Boolean {
        val result = gameLogic.activateInvincibility()
        if (result) uiHandler.post { onPowerUpStateChanged?.invoke() }
        return result
    }

    fun activateSpeedX2(): Boolean {
        val result = gameLogic.activateSpeedX2()
        if (result) uiHandler.post { onPowerUpStateChanged?.invoke() }
        return result
    }

    fun destroyNearestPipe(): Boolean {
        val result = gameLogic.destroyNearestPipe()
        if (result) uiHandler.post { onPowerUpStateChanged?.invoke() }
        return result
    }

    fun pauseGame() {
        gameLogic.pauseGame()
    }

    fun resumeGame() {
        gameLogic.resumeGame()
    }

    override fun surfaceCreated(holder: SurfaceHolder) { resume() }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) { pause() }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameLogic.state == GameLogic.GameState.GAME_OVER) return false
        if (gameLogic.state == GameLogic.GameState.PAUSED) return false

        if (event.action == MotionEvent.ACTION_DOWN) {
            val now = System.currentTimeMillis()
            if (now - lastTouchTime < touchCooldown) return true
            lastTouchTime = now

            if (gameLogic.state == GameLogic.GameState.PLAYING) {
                jumpRequested = true
            } else if (gameLogic.state == GameLogic.GameState.START) {
                startGame()
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    fun resume() {
        if (isRunning) return
        isRunning = true
        gameThread = GameThread()
        gameThread?.start()
    }

    fun pause() {
        if (!isRunning) return
        isRunning = false
        try {
            gameThread?.join(150)
        } catch (_: InterruptedException) {}
        gameThread = null
    }

    private inner class GameThread : Thread("GameThread") {
        private val targetFPS = 60
        private val targetTime = 1000L / targetFPS
        private var frameCount = 0

        override fun run() {
            var lastTime = System.currentTimeMillis()

            while (isRunning) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = ((currentTime - lastTime) / 1000f).coerceIn(0f, 0.05f)
                lastTime = currentTime

                val shouldJump = jumpRequested
                jumpRequested = false

                try {
                    gameLogic.update(deltaTime, shouldJump)
                    frameCount++
                    if (frameCount % 6 == 0) {
                        uiHandler.post { onPowerUpStateChanged?.invoke() }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GameView", "Error en update", e)
                }

                var canvas: Canvas? = null
                try {
                    canvas = surfaceHolderRef.lockCanvas()
                    if (canvas != null) render(canvas)
                } catch (e: Exception) {
                    android.util.Log.e("GameView", "Error al renderizar", e)
                } finally {
                    canvas?.let {
                        try { surfaceHolderRef.unlockCanvasAndPost(it) }
                        catch (_: Exception) {}
                    }
                }

                val sleepTime = targetTime - (System.currentTimeMillis() - currentTime)
                if (sleepTime > 0) sleep(sleepTime)
            }
        }
    }

    private fun render(canvas: Canvas) {
        val screenW = width.toFloat()
        val screenH = height.toFloat()
        val scaleX = screenW / viewportWidth
        val scaleY = screenH / viewportHeight
        // max: rellena toda la pantalla (sin bandas). El tubo superior (y=0) llega al borde superior.
        // min: letterbox; deja franjas y el juego no ocupa el alto completo.
        val scale = max(scaleX, scaleY)

        val scaledW = viewportWidth * scale
        val scaledH = viewportHeight * scale
        val offsetX = (screenW - scaledW) / 2
        val offsetY = (screenH - scaledH) / 2

        canvas.drawColor(skyTop)
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        drawBackground(canvas)
        drawGround(canvas)

        val strokeViewport = 2f * context.resources.displayMetrics.density / scale

        val pipes = gameLogic.pipes
        for (i in pipes.indices) {
            RetroPipeDrawer.draw(canvas, pipes[i], strokeViewport, viewportWidth)
        }
        val tumbles = gameLogic.pipeTumbleAnimations
        for (i in tumbles.indices) {
            val pair = tumbles[i]
            RetroPipeDrawer.drawTumblePiece(canvas, pair.top, strokeViewport, viewportWidth)
            RetroPipeDrawer.drawTumblePiece(canvas, pair.bottom, strokeViewport, viewportWidth)
        }
        val collectibles = gameLogic.collectibles
        for (i in collectibles.indices) {
            drawCollectible(canvas, collectibles[i])
        }
        drawBird(canvas)

        canvas.restore()
    }

    private fun drawBackground(canvas: Canvas) {
        if (cachedBgGradient == null || cachedViewportH != viewportHeight) {
            cachedBgGradient = LinearGradient(
                0f, 0f, 0f, viewportHeight * 0.85f,
                skyTop, skyBottom, Shader.TileMode.CLAMP
            )
            cachedViewportH = viewportHeight
        }
        bgPaint.shader = cachedBgGradient
        canvas.drawRect(0f, 0f, viewportWidth, viewportHeight, bgPaint)
        bgPaint.shader = null

        val scroll = gameLogic.backgroundScrollX
        val period = viewportWidth + 200f
        for (i in clouds.indices) {
            val cloud = clouds[i]
            val drift = scroll * cloud.parallax + cloud.baseX * period
            val wrapped = ((drift % period) + period) % period
            val cx = wrapped - 50f
            drawCloud(canvas, cx, cloud.y * viewportHeight, cloud.scale)
        }
    }

    private fun drawCloud(canvas: Canvas, x: Float, y: Float, scale: Float) {
        val s = 18f * scale
        canvas.drawCircle(x, y, s, cloudPaint)
        canvas.drawCircle(x + s * 1.4f, y, s * 1.25f, cloudPaint)
        canvas.drawCircle(x + s * 2.8f, y, s, cloudPaint)
    }

    private fun drawGround(canvas: Canvas) {
        val groundTop = viewportHeight * 0.92f
        groundPaint.color = groundGreen
        canvas.drawRect(0f, groundTop, viewportWidth, groundTop + 6f, groundPaint)
        groundPaint.color = groundBrown
        canvas.drawRect(0f, groundTop + 6f, viewportWidth, viewportHeight, groundPaint)
    }

    private fun drawBird(canvas: Canvas) {
        val bird = gameLogic.bird
        val isInvincible = gameLogic.powerUpManager.invincibility.isActive && !gameLogic.birdIsDying
        val isX2 = gameLogic.powerUpManager.speedX2.isActive && !gameLogic.birdIsDying

        canvas.save()
        canvas.translate(bird.x + bird.width / 2, bird.y + bird.height / 2)
        canvas.rotate(Math.toDegrees(bird.rotation.toDouble()).toFloat())

        val bodyRadius = bird.width * 0.3f

        if (isInvincible) {
            val shieldRadius = bodyRadius + bird.width * 0.35f
            val shieldGradient = RadialGradient(
                0f, 0f, shieldRadius,
                intArrayOf(
                    Color.argb(180, 255, 215, 0),
                    Color.argb(80, 255, 215, 0),
                    Color.argb(0, 255, 215, 0)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            shieldPaint.shader = shieldGradient
            canvas.drawCircle(0f, 0f, shieldRadius, shieldPaint)
            shieldPaint.shader = null

            shieldStrokePaint.color = shieldGold
            canvas.drawCircle(0f, 0f, bodyRadius + bird.width * 0.25f, shieldStrokePaint)
        }

        if (isX2) {
            canvas.drawCircle(0f, 0f, bodyRadius + bird.width * 0.2f, x2GlowPaint)
        }

        if (gameLogic.birdIsDying) {
            val blinkPhase = ((gameLogic.birdDeathAnimationTime * 10).toInt() % 2)
            if (blinkPhase == 0) {
                canvas.drawCircle(0f, 0f, bodyRadius + 4f, deathRedPaint)
            }
        }

        val bodyColor = if (gameLogic.birdIsDying) Color.parseColor("#CCAA00") else birdYellow
        birdPaint.color = bodyColor
        canvas.drawCircle(0f, 0f, bodyRadius, birdPaint)

        if (!gameLogic.birdIsDying) {
            birdPaint.color = Color.WHITE
            canvas.drawCircle(bodyRadius * 0.35f, -bodyRadius * 0.25f, bodyRadius * 0.28f, birdPaint)
            birdPaint.color = Color.BLACK
            canvas.drawCircle(bodyRadius * 0.42f, -bodyRadius * 0.22f, bodyRadius * 0.14f, birdPaint)
        } else {
            canvas.drawLine(-bodyRadius * 0.1f, -bodyRadius * 0.25f,
                bodyRadius * 0.4f, -bodyRadius * 0.25f, eyeStrokePaint)
        }

        birdPaint.color = if (gameLogic.birdIsDying) Color.parseColor("#CC6600") else birdOrange
        beakPath.rewind()
        beakPath.moveTo(bodyRadius, 0f)
        beakPath.lineTo(bodyRadius + bird.width * 0.2f, -bodyRadius * 0.25f)
        beakPath.lineTo(bodyRadius + bird.width * 0.2f, bodyRadius * 0.25f)
        beakPath.close()
        canvas.drawPath(beakPath, birdPaint)

        wingRect.set(-bodyRadius * 0.6f, -bodyRadius * 0.35f, bodyRadius * 0.6f, bodyRadius * 0.35f)
        if (!gameLogic.birdIsDying && gameLogic.birdWingPhase > 0) {
            val wingAngle = sin(gameLogic.birdWingPhase) * 0.5f
            canvas.save()
            canvas.translate(-bodyRadius * 0.3f, bodyRadius * 0.4f)
            canvas.rotate(Math.toDegrees((-0.3 + wingAngle).toDouble()).toFloat())
            birdPaint.color = birdOrange
            canvas.drawOval(wingRect, birdPaint)
            canvas.restore()
        } else if (gameLogic.birdIsDying) {
            canvas.save()
            canvas.translate(-bodyRadius * 0.3f, bodyRadius * 0.4f)
            canvas.rotate(28.6f)
            birdPaint.color = Color.parseColor("#CC8800")
            canvas.drawOval(wingRect, birdPaint)
            canvas.restore()
        }

        canvas.restore()
    }

    private fun drawCollectible(canvas: Canvas, c: Collectible) {
        if (c.collected) return

        val pulse = 1f + sin(c.animationPhase) * 0.15f
        val radius = c.radius * pulse

        val glowGradient = RadialGradient(
            c.x, c.y, radius * 2.5f,
            intArrayOf(Color.argb(80, 0, 229, 255), Color.argb(0, 0, 229, 255)),
            null, Shader.TileMode.CLAMP
        )
        collectibleGlowPaint.shader = glowGradient
        canvas.drawCircle(c.x, c.y, radius * 2.5f, collectibleGlowPaint)
        collectibleGlowPaint.shader = null

        val gradient = RadialGradient(
            c.x - radius * 0.3f, c.y - radius * 0.3f, radius * 1.2f,
            collectibleCyan, collectiblePurple, Shader.TileMode.CLAMP
        )
        collectiblePaint.shader = gradient
        canvas.drawCircle(c.x, c.y, radius, collectiblePaint)
        collectiblePaint.shader = null

        canvas.drawCircle(c.x - radius * 0.2f, c.y - radius * 0.2f, radius * 0.2f, starPaint)
    }
}
