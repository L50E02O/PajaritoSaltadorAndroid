package com.pajaritosaltador.game

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Efectos de sonido cortos del juego (puntos, poderes, bonus).
 * Usa [ToneGenerator] para no incluir archivos raw adicionales; respeta el toggle de SFX.
 */
class SoundEffectsPlayer(
    private val isSfxEnabled: () -> Boolean
) {

    private val toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 88)
    } catch (_: Exception) {
        null
    }

    private fun playIfEnabled(block: (ToneGenerator) -> Unit) {
        if (!isSfxEnabled()) return
        val tg = toneGenerator ?: return
        try {
            block(tg)
        } catch (_: Exception) {}
    }

    fun playScorePoint() {
        playIfEnabled { it.startTone(ToneGenerator.TONE_PROP_BEEP, 85) }
    }

    fun playCollectibleBonus() {
        playIfEnabled { it.startTone(ToneGenerator.TONE_PROP_ACK, 110) }
    }

    fun playPowerActivated() {
        playIfEnabled { tg ->
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 140)
        }
    }

    fun playPipeDestroyed() {
        playIfEnabled { tg ->
            tg.startTone(ToneGenerator.TONE_PROP_NACK, 130)
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (_: Exception) {}
    }
}
