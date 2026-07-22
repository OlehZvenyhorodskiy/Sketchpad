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
    DOTTED,
    LINED
}

enum class ToolType {
    PEN,
    PENCIL,
    INK_PEN,
    FOUNTAIN_PEN,
    MARKER,
    LASER,
    SELECTOR,
    ERASER,
    RULER
}

enum class EraserMode {
    OBJECT,
    PIXEL
}

enum class ShapeType {
    CIRCLE,
    SQUARE,
    TRIANGLE,
    ARROW,
    STAR,
    BOLD_ARROW
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
    val snappedToRuler: Boolean = false
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
    val color: Int = 0xFF1E293B.toInt(),
    val alignment: String = "LEFT"
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
    val title: String = "Графік"
)

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
    val durationMs: Long,
    val recordedAt: Long = System.currentTimeMillis(),
    val syncMarkers: List<SyncMarker> = emptyList()
)

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
    val charts: List<ChartElementEntity> = emptyList()
)

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
    val driveFileId: String? = null
)
