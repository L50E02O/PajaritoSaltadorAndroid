package com.pajaritosaltador.game

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.*

/**
 * Vista del juego con renderizado en Canvas
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {
    
    private val holder: SurfaceHolder = getHolder()
    private var gameThread: GameThread? = null
    private var isRunning = false
    
    // Viewport virtual (lógica del juego)
    private val viewportWidth = 400f
    private val viewportHeight = 600f
    
    // Lógica del juego
    private lateinit var gameLogic: GameLogic
    
    // Paint objects
    private val birdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val pipePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val pipeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    private val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val shieldStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
    }
    
    // Colores
    private val skyBlueLight = Color.parseColor("#87CEEB")
    private val skyBlueMedium = Color.parseColor("#98D8E8")
    private val skyBlueDark = Color.parseColor("#B0E0E6")
    private val birdYellow = Color.parseColor("#FFD700")
    private val birdOrange = Color.parseColor("#FF8C00")
    private val pipeGreen = Color.parseColor("#228B22")
    private val pipeGreenDark = Color.parseColor("#006400")
    private val pipeGreenLight = Color.parseColor("#32CD32")
    private val shieldGold = Color.parseColor("#FFD700")
    
    // Input
    private var jumpRequested = false
    private var lastTouchTime = 0L
    private val touchCooldown = 100L
    
    // Callbacks
    private var onScoreUpdate: ((Int) -> Unit)? = null
    private var onHighScoreUpdate: ((Int) -> Unit)? = null
    private var onGameOver: ((Int) -> Unit)? = null
    private var onGameStart: (() -> Unit)? = null
    private var onAbilityUpdate: ((String, Float) -> Unit)? = null
    
    // SharedPreferences para high score
    private val prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
    
    init {
        holder.addCallback(this)
        isFocusable = true
        
        // Inicializar lógica del juego
        val savedHighScore = prefs.getInt("high_score", 0)
        gameLogic = GameLogic(viewportWidth, viewportHeight)
        gameLogic.highScore = savedHighScore
        setupGameCallbacks()
    }
    
    /**
     * Configura los callbacks del juego
     */
    private fun setupGameCallbacks() {
        gameLogic.onScoreChanged = { score ->
            onScoreUpdate?.invoke(score)
        }
        
        gameLogic.onHighScoreChanged = { highScore ->
            prefs.edit().putInt("high_score", highScore).apply()
            onHighScoreUpdate?.invoke(highScore)
        }
        
        gameLogic.onGameOver = { score ->
            onGameOver?.invoke(score)
        }
        
        gameLogic.onDifficultyIncrease = { level ->
            // Notificación de aumento de dificultad (opcional)
        }
    }
    
    /**
     * Configura los callbacks de UI
     */
    fun setupUI(
        onScoreUpdate: (Int) -> Unit,
        onHighScoreUpdate: (Int) -> Unit,
        onGameOver: (Int) -> Unit,
        onGameStart: () -> Unit,
        onAbilityUpdate: (String, Float) -> Unit
    ) {
        this.onScoreUpdate = onScoreUpdate
        this.onHighScoreUpdate = onHighScoreUpdate
        this.onGameOver = onGameOver
        this.onGameStart = onGameStart
        this.onAbilityUpdate = onAbilityUpdate
    }
    
    /**
     * Inicia el juego
     */
    fun startGame() {
        gameLogic.startGame()
        onGameStart?.invoke()
        updateAbilityUI()
    }
    
    /**
     * Activa la habilidad
     */
    fun activateAbility() {
        if (gameLogic.activateInvulnerability()) {
            updateAbilityUI()
        }
    }
    
    /**
     * Obtiene el high score
     */
    fun getHighScore(): Int {
        return prefs.getInt("high_score", 0)
    }
    
    /**
     * Actualiza la UI de la habilidad
     */
    private fun updateAbilityUI() {
        val ability = gameLogic.invulnerability
        val text = when {
            ability.active -> context.getString(R.string.ability_active)
            ability.cooldownTimer > 0 -> context.getString(R.string.ability_cooldown)
            else -> context.getString(R.string.ability_button)
        }
        onAbilityUpdate?.invoke(text, ability.cooldownTimer)
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        resume()
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // El viewport se mantiene constante, solo se escala el renderizado
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val now = System.currentTimeMillis()
                if (now - lastTouchTime < touchCooldown) {
                    return true
                }
                lastTouchTime = now
                
                // No procesar si es un botón
                if (event.y < 100 || event.y > height - 200) {
                    return super.onTouchEvent(event)
                }
                
                if (gameLogic.state == GameLogic.GameState.PLAYING) {
                    jumpRequested = true
                } else if (gameLogic.state == GameLogic.GameState.START) {
                    startGame()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    /**
     * Resume el juego
     */
    fun resume() {
        isRunning = true
        gameThread = GameThread()
        gameThread?.start()
    }
    
    /**
     * Pausa el juego
     */
    fun pause() {
        isRunning = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        gameThread = null
    }
    
    /**
     * Thread del juego
     */
    private inner class GameThread : Thread() {
        private val targetFPS = 60
        private val targetTime = 1000 / targetFPS
        
        override fun run() {
            var lastTime = System.currentTimeMillis()
            
            while (isRunning) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = (currentTime - lastTime) / 1000f
                lastTime = currentTime
                
                // Limitar deltaTime para evitar saltos grandes
                val clampedDelta = deltaTime.coerceIn(0f, 0.1f)
                
                // Actualizar juego
                val shouldJump = jumpRequested
                jumpRequested = false
                
                gameLogic.update(clampedDelta, shouldJump)
                updateAbilityUI()
                
                // Renderizar
                var canvas: Canvas? = null
                try {
                    canvas = holder.lockCanvas()
                    if (canvas != null) {
                        render(canvas)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    canvas?.let {
                        try {
                            holder.unlockCanvasAndPost(it)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                // Control de FPS
                val sleepTime = targetTime - (System.currentTimeMillis() - currentTime)
                if (sleepTime > 0) {
                    sleep(sleepTime)
                }
            }
        }
    }
    
    /**
     * Renderiza el juego
     */
    private fun render(canvas: Canvas) {
        // Calcular escalado para mantener aspect ratio
        val screenWidth = width.toFloat()
        val screenHeight = height.toFloat()
        val scaleX = screenWidth / viewportWidth
        val scaleY = screenHeight / viewportHeight
        val scale = min(scaleX, scaleY)
        
        val scaledWidth = viewportWidth * scale
        val scaledHeight = viewportHeight * scale
        val offsetX = (screenWidth - scaledWidth) / 2
        val offsetY = (screenHeight - scaledHeight) / 2
        
        // Limpiar canvas
        canvas.drawColor(skyBlueLight)
        
        // Guardar estado
        canvas.save()
        
        // Aplicar transformación
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        
        // Dibujar fondo
        drawBackground(canvas)
        
        // Dibujar tubos
        gameLogic.pipes.forEach { pipe ->
            drawPipe(canvas, pipe)
        }
        
        // Dibujar pájaro
        drawBird(canvas)
        
        // Restaurar estado
        canvas.restore()
    }
    
    /**
     * Dibuja el fondo
     */
    private fun drawBackground(canvas: Canvas) {
        val gradient = LinearGradient(
            0f, 0f, 0f, viewportHeight,
            intArrayOf(skyBlueLight, skyBlueMedium, skyBlueDark),
            null,
            Shader.TileMode.CLAMP
        )
        val paint = Paint().apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, viewportWidth, viewportHeight, paint)
        
        // Nubes simples
        drawCloud(canvas, 80f, 100f)
        drawCloud(canvas, 250f, 150f)
        drawCloud(canvas, 150f, 250f)
    }
    
    /**
     * Dibuja una nube
     */
    private fun drawCloud(canvas: Canvas, x: Float, y: Float) {
        val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(153, 255, 255, 255) // 60% opacidad
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x, y, 20f, cloudPaint)
        canvas.drawCircle(x + 25f, y, 25f, cloudPaint)
        canvas.drawCircle(x + 50f, y, 20f, cloudPaint)
    }
    
    /**
     * Dibuja el pájaro
     */
    private fun drawBird(canvas: Canvas) {
        val bird = gameLogic.bird
        val isInvulnerable = gameLogic.invulnerability.active && !gameLogic.birdIsDying
        
        canvas.save()
        canvas.translate(bird.x + bird.width / 2, bird.y + bird.height / 2)
        canvas.rotate(Math.toDegrees(bird.rotation.toDouble()).toFloat())
        
        // Escudo si está invulnerable
        if (isInvulnerable) {
            val shieldRadius = bird.width / 2 + 15f
            val shieldGradient = RadialGradient(
                0f, 0f, shieldRadius,
                intArrayOf(
                    Color.argb(204, 255, 215, 0),
                    Color.argb(102, 255, 215, 0),
                    Color.argb(0, 255, 215, 0)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            shieldPaint.shader = shieldGradient
            canvas.drawCircle(0f, 0f, shieldRadius, shieldPaint)
            
            shieldStrokePaint.color = shieldGold
            canvas.drawCircle(0f, 0f, bird.width / 2 + 10f, shieldStrokePaint)
        }
        
        // Efectos de muerte
        if (gameLogic.birdIsDying) {
            val blinkPhase = ((gameLogic.birdDeathAnimationTime * 10).toInt() % 20) / 10f
            if (blinkPhase < 1) {
                val redPaint = Paint().apply {
                    color = Color.argb(76, 255, 0, 0) // 30% rojo
                }
                canvas.drawRect(-bird.width / 2 - 5, -bird.height / 2 - 5,
                    bird.width / 2 + 5, bird.height / 2 + 5, redPaint)
            }
        }
        
        // Cuerpo del pájaro
        val bodyColor = if (gameLogic.birdIsDying) Color.parseColor("#CCAA00") else birdYellow
        birdPaint.color = bodyColor
        canvas.drawCircle(0f, 0f, 12f, birdPaint)
        
        // Ojo
        if (!gameLogic.birdIsDying) {
            birdPaint.color = Color.BLACK
            canvas.drawCircle(5f, -3f, 3f, birdPaint)
        } else {
            // Ojo cerrado
            val eyePaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawLine(2f, -3f, 8f, -3f, eyePaint)
        }
        
        // Pico
        val beakColor = if (gameLogic.birdIsDying) Color.parseColor("#CC6600") else birdOrange
        birdPaint.color = beakColor
        val beakPath = Path().apply {
            moveTo(12f, 0f)
            lineTo(20f, -3f)
            lineTo(20f, 3f)
            close()
        }
        canvas.drawPath(beakPath, birdPaint)
        
        // Alas
        if (!gameLogic.birdIsDying && gameLogic.birdWingPhase > 0) {
            val wingAngle = sin(gameLogic.birdWingPhase) * 0.5f
            val wingColor = if (gameLogic.birdIsDying) Color.parseColor("#CC8800") else birdOrange
            
            // Ala izquierda
            canvas.save()
            canvas.translate(-5f, 5f)
            canvas.rotate(Math.toDegrees((-0.3 + wingAngle).toDouble()).toFloat())
            birdPaint.color = wingColor
            val wingRect = RectF(-8f, -5f, 8f, 5f)
            canvas.drawOval(wingRect, birdPaint)
            canvas.restore()
        } else if (gameLogic.birdIsDying) {
            // Alas caídas
            canvas.save()
            canvas.translate(-5f, 5f)
            canvas.rotate(28.6f) // 0.5 radianes
            birdPaint.color = Color.parseColor("#CC8800")
            val wingRect = RectF(-8f, -5f, 8f, 5f)
            canvas.drawOval(wingRect, birdPaint)
            canvas.restore()
        }
        
        canvas.restore()
    }
    
    /**
     * Dibuja un tubo
     */
    private fun drawPipe(canvas: Canvas, pipe: Pipe) {
        if (pipe.x + pipe.width < -50f || pipe.x > viewportWidth + 50f) {
            return
        }
        
        // Tubo principal
        pipePaint.color = pipeGreen
        canvas.drawRect(pipe.x, pipe.y, pipe.x + pipe.width, pipe.y + pipe.height, pipePaint)
        
        // Borde oscuro
        pipeStrokePaint.color = pipeGreenDark
        pipeStrokePaint.strokeWidth = 4f
        canvas.drawRect(pipe.x, pipe.y, pipe.x + pipe.width, pipe.y + pipe.height, pipeStrokePaint)
        
        // Borde claro interno
        pipeStrokePaint.color = pipeGreenLight
        pipeStrokePaint.strokeWidth = 2f
        canvas.drawRect(pipe.x + 2, pipe.y + 2,
            pipe.x + pipe.width - 2, pipe.y + pipe.height - 2, pipeStrokePaint)
    }
}

