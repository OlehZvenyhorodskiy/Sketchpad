package com.example.data.models

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.util.UUID

enum class PageSizePreset {
    UNLIMITED,
    A4_VERTICAL,
    A4_HORIZONTAL,
    RATIO_16_9_VERTICAL,
    RATIO_16_9_HORIZONTAL,
    LETTER_11X85,
    CUSTOM
}

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
    TEXT
}

enum class EraserMode {
    OBJECT,
    PIXEL
}

enum class SelectionMode {
    SINGLE,
    LASSO
}

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

@JsonClass(generateAdapter = true)
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
    val tilt: Float = 0f,
    val timestampMs: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class HslaColor(
    val hue: Float,        // 0..360
    val saturation: Float, // 0..1
    val lightness: Float,  // 0..1
    val alpha: Float = 1.0f  // 0..1
) {
    fun toColor(): Color {
        return Color.hsl(
            hue = hue.coerceIn(0f, 360f),
            saturation = saturation.coerceIn(0f, 1f),
            lightness = lightness.coerceIn(0f, 1f),
            alpha = alpha.coerceIn(0f, 1f)
        )
    }

    fun toArgbInt(): Int {
        val c = toColor()
        val a = (c.alpha * 255).toInt() and 0xFF
        val r = (c.red * 255).toInt() and 0xFF
        val g = (c.green * 255).toInt() and 0xFF
        val b = (c.blue * 255).toInt() and 0xFF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    companion object {
        val BLACK = HslaColor(0f, 0f, 0f, 1f)
        val WHITE = HslaColor(0f, 0f, 1f, 1f)
        val BLUE = HslaColor(210f, 0.8f, 0.5f, 1f)
        val RED = HslaColor(0f, 0.8f, 0.5f, 1f)
        val GREEN = HslaColor(120f, 0.8f, 0.4f, 1f)
        val YELLOW = HslaColor(50f, 0.9f, 0.5f, 1f)
        val PURPLE = HslaColor(270f, 0.8f, 0.5f, 1f)

        fun fromArgb(argb: Int): HslaColor {
            val a = ((argb shr 24) and 0xFF) / 255f
            val r = ((argb shr 16) and 0xFF) / 255f
            val g = ((argb shr 8) and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f

            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min

            val l = (max + min) / 2f
            var h = 0f
            var s = 0f

            if (delta != 0f) {
                s = if (l < 0.5f) delta / (max + min) else delta / (2f - max - min)
                h = when (max) {
                    r -> ((g - b) / delta) + (if (g < b) 6 else 0)
                    g -> ((b - r) / delta) + 2
                    else -> ((r - g) / delta) + 4
                } * 60f
            }

            return HslaColor(h, s, l, a)
        }
    }
}

@JsonClass(generateAdapter = true)
data class StrokeEntity(
    val id: String = UUID.randomUUID().toString(),
    val tool: ToolType,
    val colorHsla: HslaColor,
    val baseWidth: Float, // 1..22
    val points: List<StrokePoint>,
    val snappedToRuler: Boolean = false,
    /** Natural stroke tips stay round; pixel-erased cut edges are rendered flat. */
    val startCapRound: Boolean = true,
    val endCapRound: Boolean = true,
    /**
     * The coordinate grid a stroke was started on. This is explicit instead of inferred from
     * rectangle overlap, so handwriting on a graph keeps travelling with that graph.
     * Null preserves the behaviour of notes created before graph attachments existed.
     */
    val parentChartId: String? = null
)

@JsonClass(generateAdapter = true)
data class ShapeEntity(
    val id: String = UUID.randomUUID().toString(),
    val shapeType: ShapeType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f,
    val fillColor: Int = 0x336366F1, // semi-transparent
    val strokeColor: Int = 0xFF6366F1.toInt(),
    val strokeWidth: Float = 3f
)

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
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

@JsonClass(generateAdapter = true)
data class EraserMark(
    val id: String = UUID.randomUUID().toString(),
    val points: List<StrokePoint>,
    val width: Float,
    /**
     * Strokes which existed when this mask was created. Keeping this list makes erasing behave
     * like a raster paint app: a later stroke drawn over an erased area remains visible.
     * Empty means "all strokes" for notes saved by older app versions.
     */
    val affectedStrokeIds: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
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
    val backgroundColor: Int = 0,
    val rotation: Float = 0f,
    /**
     * Position of (0, 0) inside the chart frame, in unscaled canvas pixels. Older charts use a
     * negative value and are migrated visually to their centre. Keeping it independent of the
     * frame means resizing reveals new cells without snapping the axes between them.
     */
    val originOffsetX: Float = -1f,
    val originOffsetY: Float = -1f
)

/**
 * A Cartesian plane has one physical unit size. Older documents stored independent X/Y values,
 * which stretched cells into rectangles. Prefer the smaller valid legacy value: that normalises
 * the grid without unexpectedly clipping more of a saved chart.
 */
fun ChartElementEntity.squarePixelsPerUnit(): Float {
    val x = pixelsPerUnitX.takeIf { it.isFinite() && it > 0f }
    val y = pixelsPerUnitY.takeIf { it.isFinite() && it > 0f }
    return when {
        x != null && y != null -> minOf(x, y)
        x != null -> x
        y != null -> y
        else -> gridStep.takeIf { it.isFinite() && it > 0f }?.coerceAtLeast(8f) ?: 20f
    }
}

/**
 * Resizing changes the visible frame, never the underlying coordinate plane. The global origin
 * therefore stays at the same canvas point while its local offset is recalculated for the frame.
 */
fun ChartElementEntity.resizeFramePreservingOrigin(
    newX: Float,
    newY: Float,
    newWidth: Float,
    newHeight: Float
): ChartElementEntity {
    val unitPixels = squarePixelsPerUnit()
    val localOriginX = originOffsetX.takeIf { it >= 0f } ?: width / 2f
    val localOriginY = originOffsetY.takeIf { it >= 0f } ?: height / 2f
    val globalOriginX = x + localOriginX
    val globalOriginY = y + localOriginY
    return copy(
        x = newX,
        y = newY,
        width = newWidth,
        height = newHeight,
        pixelsPerUnitX = unitPixels,
        pixelsPerUnitY = unitPixels,
        originOffsetX = globalOriginX - newX,
        originOffsetY = globalOriginY - newY
    )
}

/** Normalises legacy charts even when their frame is merely moved. */
fun ChartElementEntity.withSquareGrid(): ChartElementEntity {
    val unitPixels = squarePixelsPerUnit()
    return if (pixelsPerUnitX == unitPixels && pixelsPerUnitY == unitPixels) this
    else copy(pixelsPerUnitX = unitPixels, pixelsPerUnitY = unitPixels)
}

/**
 * Strokes created on current builds carry an explicit chart ID. For notes created before that
 * field existed, accept only strokes whose every recorded point lies inside the chart frame;
 * overlap alone is too broad and makes unrelated handwriting travel with a graph.
 */
fun StrokeEntity.isAttachedToChart(chart: ChartElementEntity): Boolean {
    if (parentChartId == chart.id) return true
    if (parentChartId != null || points.isEmpty()) return false
    val right = chart.x + chart.width
    val bottom = chart.y + chart.height
    return points.all { point ->
        point.x in chart.x..right && point.y in chart.y..bottom
    }
}

@JsonClass(generateAdapter = true)
data class SyncMarker(
    val timestampInAudioMs: Long,
    val timestampInWritingMs: Long,
    val posX: Float = 0f,
    val posY: Float = 0f
)

@Entity(tableName = "audio_recordings")
@JsonClass(generateAdapter = true)
data class AudioRecordingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val canvasId: String,
    val filePath: String,
    val name: String = "",
    val durationMs: Long,
    val recordedAt: Long = System.currentTimeMillis(),
    val syncMarkers: List<SyncMarker> = emptyList()
) {
    fun formattedDuration(): String {
        val totalSec = durationMs / 1000
        return String.format(java.util.Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
    }

    fun formattedDate(): String {
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(recordedAt))
    }

    fun displayName(): String = name.ifBlank { "Запис ${formattedDate()}" }
}

enum class BlendMode {
    NORMAL, MULTIPLY, SCREEN, OVERLAY
}

@JsonClass(generateAdapter = true)
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

@Entity(tableName = "pages")
@JsonClass(generateAdapter = true)
data class PageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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
        if (layers.isEmpty()) {
            return listOf(LayerEntity(
                id = "default", name = "Шар 1",
                strokes = strokes, shapes = shapes,
                textBlocks = textBlocks, images = images, charts = charts
            ))
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

    fun withAddedImage(image: ImageElementEntity): PageEntity {
        val targetLayerId = activeLayerId ?: "default"
        return withUpdatedLayer(targetLayerId) { layer ->
            layer.copy(images = layer.images + image)
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

@Entity(tableName = "canvases")
data class CanvasEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val pageSizePreset: PageSizePreset = PageSizePreset.UNLIMITED,
    val customWidth: Float? = null,
    val customHeight: Float? = null,
    val backgroundColor: Int = 0xFFFFFFFF.toInt(), // white default
    val backgroundPattern: BackgroundPattern = BackgroundPattern.BLANK,
    val driveFileId: String? = null,
    val folderName: String? = null
)
