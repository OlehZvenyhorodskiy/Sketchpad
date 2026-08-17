package com.example.shared.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface CanvasCommand {
    fun execute(page: PageEntity): PageEntity
    fun undo(page: PageEntity): PageEntity
}

@Serializable
data class AddStrokeCommand(
    val layerId: String,
    val stroke: StrokeEntity
) : CanvasCommand {
    override fun execute(page: PageEntity): PageEntity {
        return page.withUpdatedLayer(layerId) { layer ->
            layer.copy(strokes = layer.strokes + stroke)
        }
    }

    override fun undo(page: PageEntity): PageEntity {
        return page.withUpdatedLayer(layerId) { layer ->
            layer.copy(strokes = layer.strokes.filterNot { it.id == stroke.id })
        }
    }
}

@Serializable
data class EraseStrokesCommand(
    val layerId: String,
    val removedStrokes: List<StrokeEntity>,
    val addedSplitStrokes: List<StrokeEntity>
) : CanvasCommand {
    override fun execute(page: PageEntity): PageEntity {
        val removedIds = removedStrokes.map { it.id }.toSet()
        return page.withUpdatedLayer(layerId) { layer ->
            val remaining = layer.strokes.filterNot { it.id in removedIds }
            layer.copy(strokes = remaining + addedSplitStrokes)
        }
    }

    override fun undo(page: PageEntity): PageEntity {
        val addedIds = addedSplitStrokes.map { it.id }.toSet()
        return page.withUpdatedLayer(layerId) { layer ->
            val remaining = layer.strokes.filterNot { it.id in addedIds }
            layer.copy(strokes = remaining + removedStrokes)
        }
    }
}

@Serializable
data class InsertShapeCommand(
    val layerId: String,
    val shape: ShapeEntity
) : CanvasCommand {
    override fun execute(page: PageEntity): PageEntity {
        return page.withUpdatedLayer(layerId) { layer ->
            layer.copy(shapes = layer.shapes + shape)
        }
    }

    override fun undo(page: PageEntity): PageEntity {
        return page.withUpdatedLayer(layerId) { layer ->
            layer.copy(shapes = layer.shapes.filterNot { it.id == shape.id })
        }
    }
}

@Serializable
data class MoveElementsCommand(
    val layerId: String,
    val strokeDisplacements: Map<String, List<StrokePoint>>,
    val shapeDisplacements: Map<String, Pair<Float, Float>>,
    val dx: Float,
    val dy: Float
) : CanvasCommand {
    override fun execute(page: PageEntity): PageEntity {
        return page.withUpdatedLayer(layerId) { layer ->
            val updatedStrokes = layer.strokes.map { stroke ->
                if (stroke.id in strokeDisplacements.keys) {
                    stroke.copy(points = stroke.points.map { pt ->
                        pt.copy(x = pt.x + dx, y = pt.y + dy)
                    })
                } else stroke
            }
            val updatedShapes = layer.shapes.map { shape ->
                if (shape.id in shapeDisplacements.keys) {
                    shape.copy(x = shape.x + dx, y = shape.y + dy)
                } else shape
            }
            layer.copy(strokes = updatedStrokes, shapes = updatedShapes)
        }
    }

    override fun undo(page: PageEntity): PageEntity {
        return page.withUpdatedLayer(layerId) { layer ->
            val updatedStrokes = layer.strokes.map { stroke ->
                if (stroke.id in strokeDisplacements.keys) {
                    stroke.copy(points = stroke.points.map { pt ->
                        pt.copy(x = pt.x - dx, y = pt.y - dy)
                    })
                } else stroke
            }
            val updatedShapes = layer.shapes.map { shape ->
                if (shape.id in shapeDisplacements.keys) {
                    shape.copy(x = shape.x - dx, y = shape.y - dy)
                } else shape
            }
            layer.copy(strokes = updatedStrokes, shapes = updatedShapes)
        }
    }
}
