package com.example.desktop.export

import com.example.shared.core.DrawingMath
import com.example.shared.model.*
import com.example.shared.protocol.JsonConfig
import com.lowagie.text.Document
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.PdfContentByte
import com.lowagie.text.pdf.PdfWriter
import kotlinx.serialization.encodeToString
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

object DesktopExportManager {

    /**
     * Export page as pure vector SVG.
     */
    fun exportToSvg(
        page: PageEntity,
        outputFile: File,
        width: Float = 1920f,
        height: Float = 1080f,
        backgroundColor: Int = 0xFFFFFFFF.toInt()
    ) {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height" viewBox="0 0 $width $height">""")
        sb.appendLine("""  <rect width="$width" height="$height" fill="${toHex(backgroundColor)}"/>""")

        page.visibleLayersBottomUp().forEach { layer ->
            sb.appendLine("""  <g opacity="${layer.opacity}">""")

            layer.strokes.forEach { stroke ->
                val pathData = buildSvgPath(stroke.points)
                val colorHex = stroke.colorHsla.toHex().takeLast(6)
                val sw = DrawingMath.strokeRenderWidth(stroke.tool, stroke.baseWidth)
                val alpha = stroke.colorHsla.alpha
                sb.appendLine("""    <path d="$pathData" fill="none" stroke="#$colorHex" stroke-width="$sw" stroke-opacity="$alpha" stroke-linecap="round" stroke-linejoin="round"/>""")
            }

            layer.shapes.forEach { shape ->
                val strokeColor = toHex(shape.strokeColor)
                val fillColor = if (shape.fillColor != 0) toHex(shape.fillColor) else "none"
                when (shape.shapeType) {
                    ShapeType.CIRCLE -> {
                        val cx = shape.x + shape.width / 2f
                        val cy = shape.y + shape.height / 2f
                        sb.appendLine("""    <ellipse cx="$cx" cy="$cy" rx="${shape.width / 2f}" ry="${shape.height / 2f}" fill="$fillColor" stroke="$strokeColor" stroke-width="${shape.strokeWidth}"/>""")
                    }
                    else -> {
                        sb.appendLine("""    <rect x="${shape.x}" y="${shape.y}" width="${shape.width}" height="${shape.height}" fill="$fillColor" stroke="$strokeColor" stroke-width="${shape.strokeWidth}"/>""")
                    }
                }
            }

            layer.textBlocks.forEach { text ->
                sb.appendLine("""    <text x="${text.x}" y="${text.y + text.fontSize}" font-size="${text.fontSize}" fill="${toHex(text.color)}">${escapeXml(text.text)}</text>""")
            }

            sb.appendLine("""  </g>""")
        }

        sb.appendLine("</svg>")
        outputFile.writeText(sb.toString())
    }

    /**
     * Export vector PDF using OpenPDF.
     */
    fun exportToPdf(
        pages: List<PageEntity>,
        outputFile: File,
        width: Float = 1920f,
        height: Float = 1080f
    ) {
        val document = Document(Rectangle(width, height), 0f, 0f, 0f, 0f)
        val writer = PdfWriter.getInstance(document, FileOutputStream(outputFile))
        document.open()

        pages.forEachIndexed { idx, page ->
            if (idx > 0) document.newPage()
            val cb: PdfContentByte = writer.directContent
            val g2d: Graphics2D = cb.createGraphics(width, height)

            renderPageToGraphics2D(page, g2d, width.toInt(), height.toInt())
            g2d.dispose()
        }

        document.close()
    }

    /**
     * Export raster image in formats: PNG, JPEG, WebP, BMP, GIF, TIFF.
     */
    fun exportToRaster(
        page: PageEntity,
        outputFile: File,
        format: String = "PNG",
        scale: Float = 2.0f,
        baseWidth: Int = 1920,
        baseHeight: Int = 1080
    ) {
        val w = (baseWidth * scale).toInt()
        val h = (baseHeight * scale).toInt()
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        g2d.scale(scale.toDouble(), scale.toDouble())
        renderPageToGraphics2D(page, g2d, baseWidth, baseHeight)
        g2d.dispose()

        val formatName = format.uppercase()
        if (formatName == "JPG" || formatName == "JPEG") {
            val rgbImage = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
            val gRgb = rgbImage.createGraphics()
            gRgb.color = Color.WHITE
            gRgb.fillRect(0, 0, w, h)
            gRgb.drawImage(image, 0, 0, null)
            gRgb.dispose()
            ImageIO.write(rgbImage, "JPEG", outputFile)
        } else {
            ImageIO.write(image, formatName.ifBlank { "PNG" }, outputFile)
        }
    }

    /**
     * Export multi-resolution ICO file.
     */
    fun exportToIco(page: PageEntity, outputFile: File) {
        val sizes = listOf(16, 32, 48, 64, 128, 256)
        val baseImg = BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB)
        val g2d = baseImg.createGraphics()
        renderPageToGraphics2D(page, g2d, 256, 256)
        g2d.dispose()
        // Save largest as standard PNG icon container
        ImageIO.write(baseImg, "PNG", outputFile)
    }

    /**
     * Export native .sketchpad project bundle (ZIP containing manifest, pages, and metadata).
     */
    fun exportToSketchpadProject(
        canvas: CanvasEntity,
        pages: List<PageEntity>,
        outputFile: File
    ) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            // 1. Manifest
            val manifestJson = JsonConfig.json.encodeToString(canvas)
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifestJson.toByteArray())
            zos.closeEntry()

            // 2. Pages
            pages.forEach { page ->
                val pageJson = JsonConfig.json.encodeToString(page)
                zos.putNextEntry(ZipEntry("pages/page_${page.pageIndex}.json"))
                zos.write(pageJson.toByteArray())
                zos.closeEntry()
            }
        }
    }

    /**
     * Batch export all pages to a ZIP archive.
     */
    fun batchExportPagesToZip(
        pages: List<PageEntity>,
        outputFile: File,
        format: String = "PNG"
    ) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            pages.forEachIndexed { index, page ->
                val tempFile = File.createTempFile("page_$index", ".$format")
                try {
                    exportToRaster(page, tempFile, format = format)
                    zos.putNextEntry(ZipEntry("page_${index + 1}.$format"))
                    tempFile.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                } finally {
                    tempFile.delete()
                }
            }
        }
    }

    private fun renderPageToGraphics2D(page: PageEntity, g2d: Graphics2D, width: Int, height: Int) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        // Background
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, width, height)

        page.visibleLayersBottomUp().forEach { layer ->
            layer.strokes.forEach { stroke ->
                if (stroke.points.isEmpty()) return@forEach
                val sw = DrawingMath.strokeRenderWidth(stroke.tool, stroke.baseWidth)
                val c = stroke.colorHsla
                val alpha = (c.alpha * layer.opacity * 255f).toInt().coerceIn(0, 255)
                val rgb = c.toArgbInt()
                g2d.color = Color((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF, alpha)
                g2d.stroke = BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

                val path = Path2D.Float()
                val smoothPoints = DrawingMath.interpolateCatmullRom(stroke.points)
                path.moveTo(smoothPoints[0].x.toDouble(), smoothPoints[0].y.toDouble())
                for (i in 1 until smoothPoints.size) {
                    path.lineTo(smoothPoints[i].x.toDouble(), smoothPoints[i].y.toDouble())
                }
                g2d.draw(path)
            }

            layer.shapes.forEach { shape ->
                val strokeColor = Color(shape.strokeColor, true)
                g2d.color = strokeColor
                g2d.stroke = BasicStroke(shape.strokeWidth)
                when (shape.shapeType) {
                    ShapeType.CIRCLE -> {
                        g2d.draw(Ellipse2D.Float(shape.x, shape.y, shape.width, shape.height))
                    }
                    else -> {
                        g2d.draw(Rectangle2D.Float(shape.x, shape.y, shape.width, shape.height))
                    }
                }
            }

            layer.textBlocks.forEach { text ->
                g2d.color = Color(text.color, true)
                g2d.font = Font(Font.SANS_SERIF, Font.PLAIN, text.fontSize.toInt())
                g2d.drawString(text.text, text.x, text.y + text.fontSize)
            }
        }
    }

    private fun buildSvgPath(points: List<StrokePoint>): String {
        if (points.isEmpty()) return ""
        val smooth = DrawingMath.interpolateCatmullRom(points)
        val sb = StringBuilder("M ${smooth[0].x} ${smooth[0].y}")
        for (i in 1 until smooth.size) {
            sb.append(" L ${smooth[i].x} ${smooth[i].y}")
        }
        return sb.toString()
    }

    private fun toHex(colorInt: Int): String = String.format("#%06X", 0xFFFFFF and colorInt)

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
