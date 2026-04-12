package com.pajaritosaltador.game

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

/**
 * Activity principal del juego.
 * Conecta la vista (GameView), el ViewModel (GameViewModel) y el servicio de musica.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: GameViewModel
    private lateinit var gameView: GameView

    private lateinit var startScreen: LinearLayout
    private lateinit var gameOverScreen: LinearLayout
    private lateinit var powerBarRoot: FrameLayout
    private lateinit var powerUpContainer: LinearLayout
    private lateinit var powerBarDragHandle: ImageButton
    private lateinit var startButton: Button
    private lateinit var restartButton: Button
    private lateinit var scoreText: TextView
    private lateinit var highScoreText: TextView
    private lateinit var finalScoreText: TextView
    private lateinit var settingsButton: ImageButton
    private lateinit var pauseButton: ImageButton
    private lateinit var pauseScreen: LinearLayout
    private lateinit var resumeButton: Button
    private lateinit var quitButton: Button

    private lateinit var soundEffects: SoundEffectsPlayer

    private var lastScoreForSfx: Int = 0

    // Botones de poderes
    private lateinit var btnInvincibility: ImageButton
    private lateinit var btnSpeedX2: ImageButton
    private lateinit var btnBreakPipe: ImageButton
    private lateinit var arcInvincibility: CooldownArcView
    private lateinit var arcSpeedX2: CooldownArcView
    private lateinit var arcBreakPipe: CooldownArcView
    private lateinit var txtCdInvincibility: TextView
    private lateinit var txtCdSpeedX2: TextView
    private lateinit var txtCdBreakPipe: TextView

    private var musicService: MusicService? = null
    private var musicBound = false

    private val musicConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            musicBound = true
            if (viewModel.musicEnabled.value == true) {
                musicService?.play()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicBound = false
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[GameViewModel::class.java]
        soundEffects = SoundEffectsPlayer { viewModel.sfxEnabled.value != false }

        bindViews()
        setupPowerBarDrag()
        setupGameView()
        setupButtons()
        observeViewModel()

        syncPowerBarUiForState(gameView.gameLogic.state)
        positionPowerBarFromPrefs()

        val intent = Intent(this, MusicService::class.java)
        bindService(intent, musicConnection, Context.BIND_AUTO_CREATE)
    }

    private fun bindViews() {
        gameView = findViewById(R.id.gameView)
        startScreen = findViewById(R.id.startScreen)
        gameOverScreen = findViewById(R.id.gameOverScreen)
        powerBarRoot = findViewById(R.id.powerBarRoot)
        powerUpContainer = findViewById(R.id.powerUpContainer)
        powerBarDragHandle = findViewById(R.id.powerBarDragHandle)
        startButton = findViewById(R.id.startButton)
        restartButton = findViewById(R.id.restartButton)
        scoreText = findViewById(R.id.scoreText)
        highScoreText = findViewById(R.id.highScoreText)
        finalScoreText = findViewById(R.id.finalScoreText)
        settingsButton = findViewById(R.id.settingsButton)
        pauseButton = findViewById(R.id.pauseButton)
        pauseScreen = findViewById(R.id.pauseScreen)
        resumeButton = findViewById(R.id.resumeButton)
        quitButton = findViewById(R.id.quitButton)

        btnInvincibility = findViewById(R.id.btnInvincibility)
        btnSpeedX2 = findViewById(R.id.btnSpeedX2)
        btnBreakPipe = findViewById(R.id.btnBreakPipe)
        arcInvincibility = findViewById(R.id.arcInvincibility)
        arcSpeedX2 = findViewById(R.id.arcSpeedX2)
        arcBreakPipe = findViewById(R.id.arcBreakPipe)
        txtCdInvincibility = findViewById(R.id.txtCdInvincibility)
        txtCdSpeedX2 = findViewById(R.id.txtCdSpeedX2)
        txtCdBreakPipe = findViewById(R.id.txtCdBreakPipe)
    }

    private fun setupGameView() {
        gameView.gameLogic.highScore = viewModel.getSavedHighScore()

        gameView.onScoreUpdate = { score ->
            scoreText.text = score.toString()
            viewModel.updateScore(score)
            if (gameView.gameLogic.state == GameLogic.GameState.PLAYING &&
                score > lastScoreForSfx
            ) {
                soundEffects.playScorePoint()
            }
            lastScoreForSfx = score
        }

        gameView.onHighScoreUpdate = { hs ->
            highScoreText.text = getString(R.string.high_score, hs)
            viewModel.updateHighScore(hs)
        }

        gameView.onGameOver = { score ->
            gameOverScreen.visibility = View.VISIBLE
            finalScoreText.text = getString(R.string.score, score)
            pauseButton.visibility = View.GONE
            viewModel.updateGameState(GameLogic.GameState.GAME_OVER)
            musicService?.pauseMusic()
            syncPowerBarUiForState(GameLogic.GameState.GAME_OVER)
        }

        gameView.onGameStart = {
            startScreen.visibility = View.GONE
            gameOverScreen.visibility = View.GONE
            pauseScreen.visibility = View.GONE
            pauseButton.visibility = View.VISIBLE
            scoreText.text = "0"
            lastScoreForSfx = 0
            viewModel.updateGameState(GameLogic.GameState.PLAYING)
            updateAllPowerUpUI()
            syncPowerBarUiForState(GameLogic.GameState.PLAYING)
            if (viewModel.musicEnabled.value == true) {
                musicService?.restart()
            }
        }

        gameView.onPowerUpStateChanged = {
            updateAllPowerUpUI()
        }

        gameView.onCollectiblePicked = {
            soundEffects.playCollectibleBonus()
        }

        gameView.onPauseChanged = { paused ->
            if (paused) {
                pauseScreen.visibility = View.VISIBLE
                viewModel.updateGameState(GameLogic.GameState.PAUSED)
                musicService?.pauseMusic()
                syncPowerBarUiForState(GameLogic.GameState.PAUSED)
            } else {
                pauseScreen.visibility = View.GONE
                viewModel.updateGameState(GameLogic.GameState.PLAYING)
                if (viewModel.musicEnabled.value == true) {
                    musicService?.play()
                }
                syncPowerBarUiForState(GameLogic.GameState.PLAYING)
            }
        }
    }

    private fun setupButtons() {
        startButton.setOnClickListener { gameView.startGame() }
        restartButton.setOnClickListener { gameView.startGame() }

        btnInvincibility.setOnClickListener {
            if (gameView.activateInvincibility()) soundEffects.playPowerActivated()
        }
        btnSpeedX2.setOnClickListener {
            if (gameView.activateSpeedX2()) soundEffects.playPowerActivated()
        }
        btnBreakPipe.setOnClickListener {
            if (gameView.destroyNearestPipe()) soundEffects.playPipeDestroyed()
        }

        settingsButton.setOnClickListener { showSettingsDialog() }

        pauseButton.setOnClickListener {
            gameView.pauseGame()
        }

        resumeButton.setOnClickListener {
            gameView.resumeGame()
        }

        quitButton.setOnClickListener {
            gameView.gameLogic.returnToStart()
            pauseScreen.visibility = View.GONE
            pauseButton.visibility = View.GONE
            startScreen.visibility = View.VISIBLE
            viewModel.updateGameState(GameLogic.GameState.START)
            musicService?.pauseMusic()
            syncPowerBarUiForState(GameLogic.GameState.START)
        }
    }

    private fun observeViewModel() {
        highScoreText.text = getString(R.string.high_score, viewModel.getSavedHighScore())

        viewModel.musicEnabled.observe(this) { enabled ->
            if (enabled) musicService?.play() else musicService?.pauseMusic()
        }
    }

    private fun updateAllPowerUpUI() {
        val pm = gameView.gameLogic.powerUpManager

        updateSinglePowerUI(pm.invincibility, btnInvincibility, arcInvincibility, txtCdInvincibility)
        updateSinglePowerUI(pm.speedX2, btnSpeedX2, arcSpeedX2, txtCdSpeedX2)
        updateSinglePowerUI(pm.breakPipe, btnBreakPipe, arcBreakPipe, txtCdBreakPipe)
    }

    private fun updateSinglePowerUI(
        power: PowerUp,
        button: ImageButton,
        arc: CooldownArcView,
        cdText: TextView
    ) {
        when {
            power.isActive -> {
                arc.setProgress(power.activeFraction, true)
                button.alpha = 1f
                button.isEnabled = false
                cdText.visibility = View.GONE
            }
            power.isOnCooldown -> {
                arc.setProgress(power.cooldownFraction, false)
                button.alpha = 0.5f
                button.isEnabled = false
                val seconds = power.cooldownTimer.toInt()
                if (seconds > 0) {
                    cdText.text = "${seconds}s"
                    cdText.visibility = View.VISIBLE
                } else {
                    cdText.visibility = View.GONE
                }
            }
            else -> {
                arc.setProgress(0f, false)
                button.alpha = 1f
                button.isEnabled = true
                cdText.visibility = View.GONE
            }
        }
    }

    private fun setPowerButtonsEnabled(enabled: Boolean) {
        updateAllPowerUpUI()
        if (!enabled) {
            for (b in listOf(btnInvincibility, btnSpeedX2, btnBreakPipe)) {
                b.isEnabled = false
                b.alpha = 0.45f
            }
        }
    }

    private fun syncPowerBarUiForState(state: GameLogic.GameState) {
        when (state) {
            GameLogic.GameState.START -> {
                powerBarRoot.visibility = View.VISIBLE
                powerUpContainer.visibility = View.VISIBLE
                setPowerButtonsEnabled(false)
                positionPowerBarFromPrefs()
                updateAllPowerUpUI()
            }
            GameLogic.GameState.PLAYING -> {
                powerBarRoot.visibility = View.VISIBLE
                powerUpContainer.visibility = View.VISIBLE
                setPowerButtonsEnabled(true)
            }
            GameLogic.GameState.PAUSED -> {
                powerBarRoot.visibility = View.GONE
            }
            GameLogic.GameState.GAME_OVER -> {
                powerBarRoot.visibility = View.GONE
            }
        }
    }

    private fun positionPowerBarFromPrefs() {
        powerBarRoot.post {
            val rw = powerBarRoot.width
            val rh = powerBarRoot.height
            if (rw <= 0 || rh <= 0) return@post
            powerUpContainer.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val cw = powerUpContainer.measuredWidth
            val ch = powerUpContainer.measuredHeight
            val cx = viewModel.getPowerBarCenterX()
            val cy = viewModel.getPowerBarCenterY()
            val lp = powerUpContainer.layoutParams as FrameLayout.LayoutParams
            lp.gravity = Gravity.TOP or Gravity.START
            lp.leftMargin = (rw * cx - cw / 2f).toInt().coerceIn(0, (rw - cw).coerceAtLeast(0))
            lp.topMargin = (rh * cy - ch / 2f).toInt().coerceIn(0, (rh - ch).coerceAtLeast(0))
            powerUpContainer.layoutParams = lp
        }
    }

    private fun setupPowerBarDrag() {
        var lastRawX = 0f
        var lastRawY = 0f
        powerBarDragHandle.setOnTouchListener { _, event ->
            val root = powerBarRoot
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastRawX
                    val dy = event.rawY - lastRawY
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    val rw = root.width
                    val rh = root.height
                    if (rw <= 0 || rh <= 0) return@setOnTouchListener true
                    val lp = powerUpContainer.layoutParams as FrameLayout.LayoutParams
                    var left = lp.leftMargin + dx.toInt()
                    var top = lp.topMargin + dy.toInt()
                    val cw = powerUpContainer.width.coerceAtLeast(powerUpContainer.measuredWidth)
                    val ch = powerUpContainer.height.coerceAtLeast(powerUpContainer.measuredHeight)
                    left = left.coerceIn(0, (rw - cw).coerceAtLeast(0))
                    top = top.coerceIn(0, (rh - ch).coerceAtLeast(0))
                    lp.gravity = Gravity.TOP or Gravity.START
                    lp.leftMargin = left
                    lp.topMargin = top
                    powerUpContainer.layoutParams = lp
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    savePowerBarPositionFromLayout()
                    true
                }
                else -> false
            }
        }
    }

    private fun savePowerBarPositionFromLayout() {
        val rw = powerBarRoot.width
        val rh = powerBarRoot.height
        if (rw <= 0 || rh <= 0) return
        val cx = (powerUpContainer.left + powerUpContainer.width / 2f) / rw
        val cy = (powerUpContainer.top + powerUpContainer.height / 2f) / rh
        viewModel.setPowerBarCenter(cx, cy)
    }

    private fun showSettingsDialog() {
        val musicEnabled = viewModel.musicEnabled.value ?: true
        val sfxEnabled = viewModel.sfxEnabled.value ?: true

        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val switchMusic = dialogView.findViewById<Switch>(R.id.switchMusic)
        val switchSfx = dialogView.findViewById<Switch>(R.id.switchSfx)
        val textVersion = dialogView.findViewById<TextView>(R.id.textVersionInfo)

        switchMusic.isChecked = musicEnabled
        switchSfx.isChecked = sfxEnabled
        textVersion.text = getString(
            R.string.settings_version_line,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )

        AlertDialog.Builder(this, R.style.SettingsDialog)
            .setTitle(R.string.settings_title)
            .setView(dialogView)
            .setPositiveButton(R.string.settings_ok) { dialog, _ ->
                if (switchMusic.isChecked != musicEnabled) viewModel.toggleMusic()
                if (switchSfx.isChecked != sfxEnabled) viewModel.toggleSfx()
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
        positionPowerBarFromPrefs()
        val state = gameView.gameLogic.state
        if (viewModel.musicEnabled.value == true && state == GameLogic.GameState.PLAYING) {
            musicService?.play()
        }
    }

    override fun onPause() {
        super.onPause()
        if (gameView.gameLogic.state == GameLogic.GameState.PLAYING) {
            gameView.pauseGame()
        }
        gameView.pause()
        musicService?.pauseMusic()
    }

    override fun onDestroy() {
        soundEffects.release()
        if (musicBound) {
            unbindService(musicConnection)
            musicBound = false
        }
        super.onDestroy()
    }
}
