package com.pajaritosaltador.game

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
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
    private lateinit var powerUpContainer: LinearLayout
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

    // Servicio de musica
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

        bindViews()
        setupGameView()
        setupButtons()
        observeViewModel()

        val intent = Intent(this, MusicService::class.java)
        bindService(intent, musicConnection, Context.BIND_AUTO_CREATE)
    }

    private fun bindViews() {
        gameView = findViewById(R.id.gameView)
        startScreen = findViewById(R.id.startScreen)
        gameOverScreen = findViewById(R.id.gameOverScreen)
        powerUpContainer = findViewById(R.id.powerUpContainer)
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
        }

        gameView.onHighScoreUpdate = { hs ->
            highScoreText.text = getString(R.string.high_score, hs)
            viewModel.updateHighScore(hs)
        }

        gameView.onGameOver = { score ->
            gameOverScreen.visibility = View.VISIBLE
            finalScoreText.text = getString(R.string.score, score)
            powerUpContainer.visibility = View.GONE
            pauseButton.visibility = View.GONE
            viewModel.updateGameState(GameLogic.GameState.GAME_OVER)
            musicService?.pauseMusic()
        }

        gameView.onGameStart = {
            startScreen.visibility = View.GONE
            gameOverScreen.visibility = View.GONE
            pauseScreen.visibility = View.GONE
            powerUpContainer.visibility = View.VISIBLE
            pauseButton.visibility = View.VISIBLE
            scoreText.text = "0"
            viewModel.updateGameState(GameLogic.GameState.PLAYING)
            updateAllPowerUpUI()
            if (viewModel.musicEnabled.value == true) {
                musicService?.restart()
            }
        }

        gameView.onPowerUpStateChanged = {
            updateAllPowerUpUI()
        }

        gameView.onPauseChanged = { paused ->
            if (paused) {
                pauseScreen.visibility = View.VISIBLE
                powerUpContainer.visibility = View.GONE
                viewModel.updateGameState(GameLogic.GameState.PAUSED)
                musicService?.pauseMusic()
            } else {
                pauseScreen.visibility = View.GONE
                powerUpContainer.visibility = View.VISIBLE
                viewModel.updateGameState(GameLogic.GameState.PLAYING)
                if (viewModel.musicEnabled.value == true) {
                    musicService?.play()
                }
            }
        }
    }

    private fun setupButtons() {
        startButton.setOnClickListener { gameView.startGame() }
        restartButton.setOnClickListener { gameView.startGame() }

        btnInvincibility.setOnClickListener { gameView.activateInvincibility() }
        btnSpeedX2.setOnClickListener { gameView.activateSpeedX2() }
        btnBreakPipe.setOnClickListener { gameView.destroyNearestPipe() }

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
            powerUpContainer.visibility = View.GONE
            startScreen.visibility = View.VISIBLE
            viewModel.updateGameState(GameLogic.GameState.START)
            musicService?.pauseMusic()
        }
    }

    private fun observeViewModel() {
        highScoreText.text = getString(R.string.high_score, viewModel.getSavedHighScore())

        viewModel.musicEnabled.observe(this) { enabled ->
            if (enabled) musicService?.play() else musicService?.pauseMusic()
        }
    }

    /**
     * Actualiza la UI de los tres poderes: arcos, textos de cooldown y alpha de botones
     */
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

    /**
     * Muestra el dialogo de ajustes con opciones de musica y sonido
     */
    private fun showSettingsDialog() {
        val musicEnabled = viewModel.musicEnabled.value ?: true
        val sfxEnabled = viewModel.sfxEnabled.value ?: true

        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val switchMusic = dialogView.findViewById<Switch>(R.id.switchMusic)
        val switchSfx = dialogView.findViewById<Switch>(R.id.switchSfx)

        switchMusic.isChecked = musicEnabled
        switchSfx.isChecked = sfxEnabled

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
        if (musicBound) {
            unbindService(musicConnection)
            musicBound = false
        }
        super.onDestroy()
    }
}
