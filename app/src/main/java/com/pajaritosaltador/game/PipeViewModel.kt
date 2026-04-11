package com.pajaritosaltador.game

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel de tuberias: expone la lista sincronizada desde [GameLogic] para MVVM.
 * La logica de movimiento sigue en [GameLogic]; aqui solo se refleja el estado para la UI.
 */
class PipeViewModel : ViewModel() {

    private val _pipes = MutableStateFlow<List<PipeDisplay>>(emptyList())
    val pipes: StateFlow<List<PipeDisplay>> = _pipes.asStateFlow()

    /**
     * Actualiza el estado observable a partir de los [Pipe] del juego.
     * Debe llamarse desde el hilo del game loop antes de dibujar el frame.
     */
    fun syncFromGameLogic(source: List<Pipe>) {
        if (source.isEmpty()) {
            _pipes.value = emptyList()
            return
        }
        val mapped = ArrayList<PipeDisplay>(source.size)
        for (p in source) {
            mapped.add(
                PipeDisplay(
                    x = p.x,
                    y = p.y,
                    width = p.width,
                    height = p.height,
                    isTopPipe = p.y == 0f
                )
            )
        }
        _pipes.value = mapped
    }
}
