package com.example.shared.model

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

@Serializable
enum class PageSizePreset {
    UNLIMITED,
    A4_VERTICAL,
    A4_HORIZONTAL,
    RATIO_16_9_VERTICAL,
    RATIO_16_9_HORIZONTAL,
    LETTER_11X85,
    CUSTOM
}

@Serializable
enum class BackgroundPattern {
    BLANK,
    NONE,
    DOTTED,
    LINED,
    GRID_SQUARE,
    GRID_ISOMETRIC,
    PROTRACTOR,
    MUSIC_STAFF,
    GRAPH_MM,
    DOT_GRID,
    CORNELL_NOTES,
    KANBAN_TEMPLATE,
    ISO_3D
}

@Serializable
enum class ToolType {
    PEN,
    PENCIL,
    INK_PEN,
    FOUNTAIN_PEN,
    MARKER,
    AIRBRUSH,
    CRAYON,
    WATERCOLOR_BRUSH,
    LASER,
    POINTER,
    SELECTOR,
    ERASER,
    FILL,
    EYEDROPPER,
    RULER,
    TEXT,
    PIXEL
}

@Serializable
enum class EraserMode {
    OBJECT,
    PIXEL
}

@Serializable
enum class SelectionMode {
    SINGLE,
    LASSO
}

@Serializable
enum class ShapeType {
    CIRCLE,
    SQUARE,
    TRIANGLE,
    ARROW,
    STAR,
    BOLD_ARROW,
    HEXAGON,
    PENTAGON,
    CLOUD,
    SPEECH_BUBBLE
}

@Serializable
enum class BlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY
}

@Serializable
enum class SymmetryMode {
    NONE,
    HORIZONTAL,
    VERTICAL,
    QUAD
}

@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
    val tilt: Float = 0f,
    val azimuth: Float = 0f,
    val timestampMs: Long = System.currentTimeMillis()
)

@Serializable
data class HslaColor(
    val hue: Float,        // 0..360
    val saturation: Float, // 0..1
    val lightness: Float,  // 0..1
    val alpha: Float = 1.0f  // 0..1
) {
    fun toArgbInt(): Int {
        val h = hue.coerceIn(0f, 360f)
        val s = saturation.coerceIn(0f, 1f)
        val l = lightness.coerceIn(0f, 1f)
        val a = alpha.coerceIn(0f, 1f)

        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f

        val (rPrime, gPrime, bPrime) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((rPrime + m) * 255f).toInt().coerceIn(0, 255)
        val g = ((gPrime + m) * 255f).toInt().coerceIn(0, 255)
        val b = ((bPrime + m) * 255f).toInt().coerceIn(0, 255)
        val alphaInt = (a * 255f).toInt().coerceIn(0, 255)

        return (alphaInt shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun toHex(): String = String.format("#%08X", toArgbInt())

    companion object {
        val BLACK = HslaColor(0f, 0f, 0f, 1f)
        val WHITE = HslaColor(0f, 0f, 1f, 1f)
        val BLUE = HslaColor(210f, 0.8f, 0.5f, 1f)
        val RED = HslaColor(0f, 0.8f, 0.5f, 1f)
        val GREEN = HslaColor(120f, 0.8f, 0.4f, 1f)
        val YELLOW = HslaColor(50f, 0.9f, 0.5f, 1f)
        val PURPLE = HslaColor(270f, 0.8f, 0.5f, 1f)
        val CYAN = HslaColor(180f, 0.8f, 0.5f, 1f)
        val ORANGE = HslaColor(30f, 0.9f, 0.5f, 1f)

        fun fromArgb(argb: Int): HslaColor {
            val a = ((argb shr 24) and 0xFF) / 255f
            val r = ((argb shr 16) and 0xFF) / 255f
            val g = ((argb shr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f

            val maxVal = maxOf(r, g, b)
            val minVal = minOf(r, g, b)
            val delta = maxVal - minVal

            val l = (maxVal + minVal) / 2f
            var h = 0f
            var s = 0f

            if (delta != 0f) {
                s = if (l < 0.5f) delta / (maxVal + minVal) else delta / (2f - maxVal - minVal)
                h = when (maxVal) {
                    r -> ((g - b) / delta) + (if (g < b) 6 else 0)
                    g -> ((b - r) / delta) + 2
                    else -> ((r - g) / delta) + 4
                } * 60f
            }

            return HslaColor(h, s, l, a)
        }
    }
}

@Serializable
data class StrokeEntity(
    val id: String = UUID.randomUUID().toString(),
    val tool: ToolType,
    val colorHsla: HslaColor,
    val baseWidth: Float,
    val points: List<StrokePoint>,
    val snappedToRuler: Boolean = false,
    val startCapRound: Boolean = true,
    val endCapRound: Boolean = true
)

@Serializable
data class ShapeEntity(
    val id: String = UUID.randomUUID().toString(),
    val shapeType: ShapeType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f,
    val fillColor: Int = 0x336366F1,
    val strokeColor: Int = 0xFF6366F1.toInt(),
    val strokeWidth: Float = 3f
)

@Serializable
data class TextBlockEntity(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float = 240f,
    val height: Float = 100f,
    val fontSize: Float = 18f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val fontFamily: String = "SANS",
    val color: Int = 0xFF1E293B.toInt(),
    val alignment: String = "LEFT",
    val rotation: Float = 0f
)

@Serializable
data class ImageElementEntity(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: String,
    val x: Float,
    val y: Float,
    val width: Float = 300f,
    val height: Float = 200f,
    val rotation: Float = 0f,
    val opacity: Float = 1.0f
)

@Serializable
data class EraserMark(
    val id: String = UUID.randomUUID().toString(),
    val points: List<StrokePoint>,
    val width: Float
)

@Serializable
data class ChartElementEntity(
    val id: String = UUID.randomUUID().toString(),
    val x: Float,
    val y: Float,
    val width: Float = 360f,
    val height: Float = 240f,
    val axisRangeX: Float = 10f,
    val axisRangeY: Float = 10f,
    val gridStep: Float = 20f,
    val axisLabelsVisible: Boolean = true,
    val showAxisLabels: Boolean = true,
    val pixelsPerUnitX: Float = 0f,
    val pixelsPerUnitY: Float = 0f,
    val title: String = "Графік",
    val xMin: Float = -10f,
    val xMax: Float = 10f,
    val yMin: Float = -10f,
    val yMax: Float = 10f,
    val xStep: Float = 1f,
    val yStep: Float = 1f,
    val backgroundColor: Int = 0
)

@Serializable
enum class CodeLanguage(val displayName: String) {
    PYTHON("Python"),
    C("C"),
    CPP("C++")
}

@Serializable
data class CodeBlockEntity(
    val id: String = UUID.randomUUID().toString(),
    val x: Float = 100f,
    val y: Float = 100f,
    val width: Float = 420f,
    val height: Float = 260f,
    val language: CodeLanguage = CodeLanguage.PYTHON,
    val source: String = "print(\"Hello, Sketchpad!\")",
    val consoleOutput: String = "",
    val diagnostics: List<String> = emptyList(),
    val lastExecutedAt: Long? = null
) {
    val code: String get() = source
    val output: String get() = consoleOutput
}

@Serializable
data class SyncMarker(
    val timestampInAudioMs: Long,
    val timestampInWritingMs: Long,
    val posX: Float = 0f,
    val posY: Float = 0f
)

@Serializable
data class AudioRecordingEntity(
    val id: String = UUID.randomUUID().toString(),
    val canvasId: String = "",
    val filePath: String,
    val name: String = "",
    val durationMs: Long = 0L,
    val recordedAt: Long = System.currentTimeMillis(),
    val syncMarkers: List<SyncMarker> = emptyList()
) {
    fun formattedDuration(): String {
        val totalSec = durationMs / 1000
        return String.format("%02d:%02d", totalSec / 60, totalSec % 60)
    }
    fun displayName(): String = name.ifBlank { "Запис (${formattedDuration()})" }
}

@Serializable
data class FlashcardEntity(
    val id: String = UUID.randomUUID().toString(),
    val deckId: String = "default",
    val front: String,
    val back: String,
    val hint: String = "",
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val dueAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class LayerEntity(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Шар",
    val isVisible: Boolean = true,
    val opacity: Float = 1.0f,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val isLocked: Boolean = false,
    val strokes: List<StrokeEntity> = emptyList(),
    val eraserMarks: List<EraserMark> = emptyList(),
    val shapes: List<ShapeEntity> = emptyList(),
    val textBlocks: List<TextBlockEntity> = emptyList(),
    val images: List<ImageElementEntity> = emptyList(),
    val charts: List<ChartElementEntity> = emptyList(),
    val codeBlocks: List<CodeBlockEntity> = emptyList()
) {
    val totalElements: Int
        get() = strokes.size + shapes.size + textBlocks.size + images.size + charts.size + codeBlocks.size + eraserMarks.size
}

@Serializable
data class PageEntity(
    val id: String = UUID.randomUUID().toString(),
    val canvasId: String,
    val pageIndex: Int,
    val strokes: List<StrokeEntity> = emptyList(),
    val shapes: List<ShapeEntity> = emptyList(),
    val textBlocks: List<TextBlockEntity> = emptyList(),
    val images: List<ImageElementEntity> = emptyList(),
    val charts: List<ChartElementEntity> = emptyList(),
    val layers: List<LayerEntity> = listOf(LayerEntity(id = "default", name = "Шар 1")),
    val activeLayerId: String? = "default",
    val backgroundPattern: BackgroundPattern = BackgroundPattern.BLANK,
    val backgroundSpacing: Float = 30f,
    val backgroundLineColor: Int = 0xFFE0E0E0.toInt()
) {
    fun getEffectiveLayers(): List<LayerEntity> {
        if (layers.isEmpty() || (layers.size == 1 && layers[0].totalElements == 0 && (strokes.isNotEmpty() || shapes.isNotEmpty() || textBlocks.isNotEmpty()))) {
            return listOf(
                LayerEntity(
                    id = layers.firstOrNull()?.id ?: "default",
                    name = layers.firstOrNull()?.name ?: "Шар 1",
                    strokes = if (layers.firstOrNull()?.strokes.isNullOrEmpty()) strokes else layers[0].strokes,
                    shapes = if (layers.firstOrNull()?.shapes.isNullOrEmpty()) shapes else layers[0].shapes,
                    textBlocks = if (layers.firstOrNull()?.textBlocks.isNullOrEmpty()) textBlocks else layers[0].textBlocks,
                    images = if (layers.firstOrNull()?.images.isNullOrEmpty()) images else layers[0].images,
                    charts = if (layers.firstOrNull()?.charts.isNullOrEmpty()) charts else layers[0].charts
                )
            )
        }
        return layers
    }

    fun visibleLayersBottomUp(): List<LayerEntity> = getEffectiveLayers().filter { it.isVisible }

    fun getActiveLayer(): LayerEntity {
        val effective = getEffectiveLayers()
        return effective.find { it.id == activeLayerId } ?: effective.first()
    }

    fun withUpdatedLayer(layerId: String, transform: (LayerEntity) -> LayerEntity): PageEntity {
        val currentLayers = getEffectiveLayers()
        val updatedLayers = currentLayers.map { layer ->
            if (layer.id == layerId) transform(layer) else layer
        }
        return copy(layers = updatedLayers, activeLayerId = activeLayerId ?: layerId)
    }

    fun withAddedStroke(stroke: StrokeEntity): PageEntity {
        val targetLayerId = activeLayerId ?: "default"
        return withUpdatedLayer(targetLayerId) { layer ->
            layer.copy(strokes = layer.strokes + stroke)
        }
    }

    fun withAddedShape(shape: ShapeEntity): PageEntity {
        val targetLayerId = activeLayerId ?: "default"
        return withUpdatedLayer(targetLayerId) { layer ->
            layer.copy(shapes = layer.shapes + shape)
        }
    }

    fun withAddedChart(chart: ChartElementEntity): PageEntity {
        val targetLayerId = activeLayerId ?: "default"
        return withUpdatedLayer(targetLayerId) { layer ->
            layer.copy(charts = layer.charts + chart)
        }
    }

    fun withAddedCodeBlock(codeBlock: CodeBlockEntity): PageEntity {
        val targetLayerId = activeLayerId ?: "default"
        return withUpdatedLayer(targetLayerId) { layer ->
            layer.copy(codeBlocks = layer.codeBlocks + codeBlock)
        }
    }
}

@Serializable
data class CanvasEntity(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val pageSizePreset: PageSizePreset = PageSizePreset.UNLIMITED,
    val customWidth: Float? = null,
    val customHeight: Float? = null,
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val backgroundPattern: BackgroundPattern = BackgroundPattern.BLANK,
    val driveFileId: String? = null
)
