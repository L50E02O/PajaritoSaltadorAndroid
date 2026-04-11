package com.pajaritosaltador.game

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * ViewModel principal del juego siguiendo patron MVVM.
 * Mantiene el estado del juego, puntaje y high score persistente.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)

    private val _score = MutableLiveData(0)
    val score: LiveData<Int> = _score

    private val _highScore = MutableLiveData(prefs.getInt("high_score", 0))
    val highScore: LiveData<Int> = _highScore

    private val _gameState = MutableLiveData(GameLogic.GameState.START)
    val gameState: LiveData<GameLogic.GameState> = _gameState

    private val _musicEnabled = MutableLiveData(prefs.getBoolean("music_enabled", true))
    val musicEnabled: LiveData<Boolean> = _musicEnabled

    private val _sfxEnabled = MutableLiveData(prefs.getBoolean("sfx_enabled", true))
    val sfxEnabled: LiveData<Boolean> = _sfxEnabled

    /**
     * Actualiza el puntaje actual
     */
    fun updateScore(newScore: Int) {
        _score.postValue(newScore)
    }

    /**
     * Actualiza el high score y lo persiste
     */
    fun updateHighScore(newHighScore: Int) {
        _highScore.postValue(newHighScore)
        prefs.edit().putInt("high_score", newHighScore).apply()
    }

    /**
     * Cambia el estado del juego
     */
    fun updateGameState(state: GameLogic.GameState) {
        _gameState.postValue(state)
    }

    /**
     * Alterna la musica de fondo
     */
    fun toggleMusic() {
        val newValue = !(_musicEnabled.value ?: true)
        _musicEnabled.postValue(newValue)
        prefs.edit().putBoolean("music_enabled", newValue).apply()
    }

    /**
     * Alterna los efectos de sonido
     */
    fun toggleSfx() {
        val newValue = !(_sfxEnabled.value ?: true)
        _sfxEnabled.postValue(newValue)
        prefs.edit().putBoolean("sfx_enabled", newValue).apply()
    }

    /**
     * Obtiene el high score guardado
     */
    fun getSavedHighScore(): Int = prefs.getInt("high_score", 0)
}
