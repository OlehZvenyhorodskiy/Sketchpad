package com.example.drive

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.graphics.asAndroidPath
import com.example.core.drawing.DrawingEngine
import com.example.data.models.*
import java.io.File
import java.io.FileOutputStream

object ExportManager {

    /**
     * Експорт сторінки у SVG (векторний формат).
     */
    fun exportToSvg(page: PageEntity, outputFile: File, pageWidth: Float = 1920f, pageHeight: Float = 1080f) {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" width="$pageWidth" height="$pageHeight" viewBox="0 0 $pageWidth $pageHeight">""")
        sb.appendLine("""  <rect width="100%" height="100%" fill="white"/>""")

        page.visibleLayersBottomUp().forEach { layer ->
            sb.appendLine("""  <g opacity="${layer.opacity}" ${if (!layer.isVisible) """visibility="hidden"""" else ""}>""")

            // Strokes → <path>
            layer.strokes.forEach { stroke ->
                val pathData = buildSvgPathData(stroke.points)
                val color = stroke.colorHsla.toHexColor()
                val width = stroke.baseWidth
                val opacity = stroke.colorHsla.alpha
                sb.appendLine("""    <path d="$pathData" fill="none" stroke="$color" stroke-width="$width" stroke-opacity="$opacity" stroke-linecap="round" stroke-linejoin="round"/>""")
            }

            // Shapes → <rect>, <ellipse>, <line>
            layer.shapes.forEach { shape ->
                when (shape.shapeType) {
                    ShapeType.SQUARE -> {
                        val fill = if (shape.fillColor != 0) """fill="${colorToHex(shape.fillColor)}"""" else """fill="none""""
                        sb.appendLine("""    <rect x="${shape.x}" y="${shape.y}" width="${shape.width}" height="${shape.height}" $fill stroke="${colorToHex(shape.strokeColor)}" stroke-width="${shape.strokeWidth}"/>""")
                    }
                    ShapeType.CIRCLE -> {
                        val cx = shape.x + shape.width / 2
                        val cy = shape.y + shape.height / 2
                        sb.appendLine("""    <ellipse cx="$cx" cy="$cy" rx="${shape.width / 2}" ry="${shape.height / 2}" fill="none" stroke="${colorToHex(shape.strokeColor)}" stroke-width="${shape.strokeWidth}"/>""")
                    }
                    else -> {
                        val cx = shape.x + shape.width / 2
                        val cy = shape.y + shape.height / 2
                        sb.appendLine("""    <rect x="${shape.x}" y="${shape.y}" width="${shape.width}" height="${shape.height}" fill="none" stroke="${colorToHex(shape.strokeColor)}" stroke-width="${shape.strokeWidth}"/>""")
                    }
                }
            }

            // Text → <text>
            layer.textBlocks.forEach { text ->
                sb.appendLine("""    <text x="${text.x}" y="${text.y + text.fontSize}" font-size="${text.fontSize}" fill="${colorToHex(text.color)}">${escapeXml(text.text)}</text>""")
            }

            sb.appendLine("""  </g>""")
        }

        sb.appendLine("</svg>")
        outputFile.writeText(sb.toString())
    }

    /**
     * Експорт сторінки у PDF (векторний).
     */
    fun exportToPdf(page: PageEntity, outputFile: File, context: Context, pageWidth: Int = 1920, pageHeight: Int = 1080) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val pdfPage = pdfDocument.startPage(pageInfo)
        val canvas = pdfPage.canvas

        // Білий фон
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        page.visibleLayersBottomUp().forEach { layer ->
            val layerAlpha = (layer.opacity * 255).toInt()

            layer.strokes.forEach { stroke ->
                val path = DrawingEngine.createSmoothPath(stroke.points).asAndroidPath()
                paint.strokeWidth = stroke.baseWidth
                paint.color = stroke.colorHsla.toAndroidColor()
                paint.alpha = (stroke.colorHsla.alpha * layerAlpha / 255f * 255).toInt()
                canvas.drawPath(path, paint)
            }

            layer.shapes.forEach { shape ->
                paint.strokeWidth = shape.strokeWidth
                paint.color = shape.strokeColor
                paint.alpha = layerAlpha
                canvas.drawRect(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height, paint)
            }

            layer.textBlocks.forEach { text ->
                val textPaint = Paint().apply {
                    color = text.color
                    textSize = text.fontSize * 1.5f
                    isAntiAlias = true
                    alpha = layerAlpha
                }
                canvas.drawText(text.text, text.x, text.y + text.fontSize, textPaint)
            }
        }

        pdfDocument.finishPage(pdfPage)
        FileOutputStream(outputFile).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
    }

    private fun buildSvgPathData(points: List<StrokePoint>): String {
        if (points.isEmpty()) return ""
        val sb = StringBuilder("M ${points[0].x} ${points[0].y}")
        if (points.size == 1) {
            sb.append(" L ${points[0].x + 0.1} ${points[0].y + 0.1}")
            return sb.toString()
        }
        for (i in 1 until points.size) {
            if (i < points.size - 1) {
                val cx = (points[i].x + points[i + 1].x) / 2
                val cy = (points[i].y + points[i + 1].y) / 2
                sb.append(" Q ${points[i].x} ${points[i].y} $cx $cy")
            } else {
                sb.append(" L ${points[i].x} ${points[i].y}")
            }
        }
        return sb.toString()
    }

    private fun colorToHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;")
    }
}

fun HslaColor.toHexColor(): String {
    val color = this.toColor()
    return String.format("#%02X%02X%02X",
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt())
}

fun HslaColor.toAndroidColor(): Int {
    val color = this.toColor()
    return android.graphics.Color.argb(
        (color.alpha * 255).toInt(),
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt()
    )
}
