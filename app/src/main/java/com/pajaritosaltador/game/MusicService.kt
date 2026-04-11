package com.pajaritosaltador.game

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder

/**
 * Servicio de musica de fondo en loop.
 * Sobrevive rotaciones de pantalla y minimizacion.
 * Se controla desde el Activity via bind/unbind y metodos pause/resume.
 */
class MusicService : Service() {

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.lofi_background)?.apply {
                isLooping = true
                setVolume(0.4f, 0.4f)
                isPrepared = true
            }
        } catch (e: Exception) {
            android.util.Log.w("MusicService", "No se pudo cargar la musica de fondo", e)
        }
    }

    /**
     * Inicia o reanuda la reproduccion
     */
    fun play() {
        try {
            if (isPrepared && mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            android.util.Log.w("MusicService", "Error al reproducir", e)
        }
    }

    /**
     * Pausa la reproduccion sin liberar recursos
     */
    fun pauseMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            android.util.Log.w("MusicService", "Error al pausar", e)
        }
    }

    /**
     * Reinicia la reproduccion desde el inicio
     */
    fun restart() {
        try {
            if (isPrepared) {
                mediaPlayer?.seekTo(0)
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            android.util.Log.w("MusicService", "Error al reiniciar", e)
        }
    }

    /**
     * Ajusta el volumen (0.0 a 1.0)
     */
    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(v, v)
        } catch (_: Exception) {}
    }

    /**
     * Indica si la musica esta sonando actualmente
     */
    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }

    override fun onDestroy() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPrepared = false
        super.onDestroy()
    }
}
