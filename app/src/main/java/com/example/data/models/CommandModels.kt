package com.example.data.models

import java.util.UUID

/**
 * Command Pattern для Undo/Redo.
 * Замінює full-page snapshots на легковагові команди.
 */
sealed interface CanvasCommand {
    val id: String
    val timestamp: Long
    val description: String

    fun execute(page: PageEntity): PageEntity
    fun undo(page: PageEntity): PageEntity
}

data class AddStrokeCommand(
    override val id: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis(),
    override val description: String = "Додати штрих",
    val stroke: StrokeEntity,
    val layerId: String
) : CanvasCommand {
    override fun execute(page: PageEntity): PageEntity {
        val layers = page.layers.map { l ->
            if (l.id == layerId) l.copy(strokes = l.strokes + stroke) else l
        }
        return page.copy(layers = layers)
    }
    override fun undo(page: PageEntity): PageEntity {
        val layers = page.layers.map { l ->
            if (l.id == layerId) l.copy(strokes = l.strokes - stroke) else l
        }
        return page.copy(layers = layers)
    }
}

data class EraseStrokeCommand(
    override val id: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis(),
    override val description: String = "Стерти штрих",
    val erasedStrokes: List<StrokeEntity>,
    val layerId: String
) : CanvasCommand {
    override fun execute(page: PageEntity): PageEntity {
        val layers = page.layers.map { l ->
            if (l.id == layerId) l.copy(strokes = l.strokes - erasedStrokes.toSet()) else l
        }
        return page.copy(layers = layers)
    }
    override fun undo(page: PageEntity): PageEntity {
        val layers = page.layers.map { l ->
            if (l.id == layerId) l.copy(strokes = l.strokes + erasedStrokes) else l
        }
        return page.copy(layers = layers)
    }
}

data class AddShapeCommand(
    override val id: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis(),
    override val description: String = "Додати фігуру",
    val shape: ShapeEntity,
    val layerId: String
) : CanvasCommand {
    override fun execute(page: PageEntity): PageEntity {
        val layers = page.layers.map { l ->
            if (l.id == layerId) l.copy(shapes = l.shapes + shape) else l
        }
        return page.copy(layers = layers)
    }
    override fun undo(page: PageEntity): PageEntity {
        val layers = page.layers.map { l ->
            if (l.id == layerId) l.copy(shapes = l.shapes - shape) else l
        }
        return page.copy(layers = layers)
    }
}

data class AddLayerCommand(
    override val id: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis(),
    override val description: String = "Додати шар",
    val layer: LayerEntity
) : CanvasCommand {
    override fun execute(page: PageEntity): PageEntity =
        page.copy(layers = page.layers + layer, activeLayerId = layer.id)
    override fun undo(page: PageEntity): PageEntity =
        page.copy(layers = page.layers - layer)
}

data class MoveElementCommand(
    override val id: String = UUID.randomUUID().toString(),
    override val timestamp: Long = System.currentTimeMillis(),
    override val description: String = "Перемістити",
    val layerId: String,
    val strokeId: String?,
    val shapeId: String?,
    val deltaX: Float,
    val deltaY: Float
) : CanvasCommand {
    override fun execute(page: PageEntity): PageEntity = applyDelta(page, deltaX, deltaY)
    override fun undo(page: PageEntity): PageEntity = applyDelta(page, -deltaX, -deltaY)

    private fun applyDelta(page: PageEntity, dx: Float, dy: Float): PageEntity {
        val layers = page.layers.map { l ->
            if (l.id != layerId) return@map l
            l.copy(
                strokes = l.strokes.map { s ->
                    if (s.id == strokeId) s.copy(points = s.points.map { p -> p.copy(x = p.x + dx, y = p.y + dy) })
                    else s
                },
                shapes = l.shapes.map { s ->
                    if (s.id == shapeId) s.copy(x = s.x + dx, y = s.y + dy) else s
                }
            )
        }
        return page.copy(layers = layers)
    }
}
