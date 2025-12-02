package com.pajaritosaltador.game

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var gameView: GameView
    private lateinit var startScreen: LinearLayout
    private lateinit var gameOverScreen: LinearLayout
    private lateinit var startButton: Button
    private lateinit var restartButton: Button
    private lateinit var scoreText: TextView
    private lateinit var highScoreText: TextView
    private lateinit var finalScoreText: TextView
    private lateinit var abilityButton: Button
    private lateinit var abilityCooldownText: TextView
    private lateinit var abilityContainer: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Inicializar vistas
        gameView = findViewById(R.id.gameView)
        startScreen = findViewById(R.id.startScreen)
        gameOverScreen = findViewById(R.id.gameOverScreen)
        startButton = findViewById(R.id.startButton)
        restartButton = findViewById(R.id.restartButton)
        scoreText = findViewById(R.id.scoreText)
        highScoreText = findViewById(R.id.highScoreText)
        finalScoreText = findViewById(R.id.finalScoreText)
        abilityButton = findViewById(R.id.abilityButton)
        abilityCooldownText = findViewById(R.id.abilityCooldownText)
        abilityContainer = findViewById(R.id.abilityContainer)
        
        // Configurar GameView
        gameView.setupUI(
            onScoreUpdate = { score ->
                scoreText.text = score.toString()
            },
            onHighScoreUpdate = { highScore ->
                highScoreText.text = getString(R.string.high_score, highScore)
            },
            onGameOver = { score ->
                gameOverScreen.visibility = android.view.View.VISIBLE
                finalScoreText.text = getString(R.string.score, score)
                abilityContainer.visibility = android.view.View.GONE
            },
            onGameStart = {
                startScreen.visibility = android.view.View.GONE
                gameOverScreen.visibility = android.view.View.GONE
                abilityContainer.visibility = android.view.View.VISIBLE
            },
            onAbilityUpdate = { text, cooldown ->
                abilityButton.text = text
                if (cooldown > 0) {
                    abilityCooldownText.text = "${cooldown.toInt()}s"
                    abilityCooldownText.visibility = android.view.View.VISIBLE
                } else {
                    abilityCooldownText.visibility = android.view.View.GONE
                }
            }
        )
        
        // Botones
        startButton.setOnClickListener {
            gameView.startGame()
        }
        
        restartButton.setOnClickListener {
            gameView.startGame()
        }
        
        abilityButton.setOnClickListener {
            gameView.activateAbility()
        }
        
        // Cargar high score inicial
        val highScore = gameView.getHighScore()
        highScoreText.text = getString(R.string.high_score, highScore)
    }
    
    override fun onResume() {
        super.onResume()
        gameView.resume()
    }
    
    override fun onPause() {
        super.onPause()
        gameView.pause()
    }
}

