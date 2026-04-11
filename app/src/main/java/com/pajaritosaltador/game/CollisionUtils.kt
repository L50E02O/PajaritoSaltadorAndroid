package com.pajaritosaltador.game

/**
 * Utilidad separada para deteccion de colisiones.
 * Centraliza toda la logica de colision AABB y colision circular.
 */
object CollisionUtils {

    /**
     * Verifica si dos rectangulos colisionan usando AABB (Axis-Aligned Bounding Box)
     */
    fun checkRectCollision(rect1: Rect, rect2: Rect): Boolean {
        return rect1.x < rect2.x + rect2.width &&
               rect1.x + rect1.width > rect2.x &&
               rect1.y < rect2.y + rect2.height &&
               rect1.y + rect1.height > rect2.y
    }

    /**
     * Verifica si un circulo colisiona con un rectangulo.
     * Usado para coleccionables (circulares) contra el pajaro (rectangular).
     */
    fun checkCircleRectCollision(
        circleX: Float,
        circleY: Float,
        circleRadius: Float,
        rect: Rect
    ): Boolean {
        val closestX = circleX.coerceIn(rect.x, rect.x + rect.width)
        val closestY = circleY.coerceIn(rect.y, rect.y + rect.height)
        val dx = circleX - closestX
        val dy = circleY - closestY
        return (dx * dx + dy * dy) <= (circleRadius * circleRadius)
    }

    /**
     * Crea un Rect con margen de tolerancia para colisiones mas justas
     */
    fun createHitbox(obj: GameObject, marginX: Float = 4f, marginY: Float = 4f): Rect {
        return Rect(
            obj.x + marginX,
            obj.y + marginY,
            obj.width - marginX * 2,
            obj.height - marginY * 2
        )
    }

    /**
     * Crea un Rect a partir de un Pipe
     */
    fun pipeToRect(pipe: Pipe): Rect {
        return Rect(pipe.x, pipe.y, pipe.width, pipe.height)
    }
}
