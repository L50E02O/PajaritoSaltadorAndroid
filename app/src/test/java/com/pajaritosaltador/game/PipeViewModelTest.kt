package com.pajaritosaltador.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PipeViewModelTest {

    @Test
    fun syncFromGameLogic_marca_isTopPipe_correctamente() {
        val vm = PipeViewModel()
        val pipes = listOf(
            Pipe(0f, 0f, 54f, 200f, false, 0),
            Pipe(0f, 400f, 54f, 240f, false, 0)
        )

        vm.syncFromGameLogic(pipes)

        val out = vm.pipes.value
        assertEquals(2, out.size)
        assertTrue(out[0].isTopPipe)
        assertTrue(!out[1].isTopPipe)
    }

    @Test
    fun syncFromGameLogic_lista_vacia() {
        val vm = PipeViewModel()
        vm.syncFromGameLogic(emptyList())
        assertTrue(vm.pipes.value.isEmpty())
    }
}
