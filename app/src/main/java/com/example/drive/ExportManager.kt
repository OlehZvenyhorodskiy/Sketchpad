package com.example.drive

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.core.drawing.DrawingEngine
import com.example.core.drawing.RasterStrokeCompositor
import com.example.data.models.*
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ObsidianFormat { PNG, SVG }

object ExportManager {

    /**
     * Експорт сторінки у SVG (векторний формат).
     */
    fun exportToSvg(
        page: PageEntity,
        outputFile: File,
        pageWidth: Float = 1920f,
        pageHeight: Float = 1080f,
        backgroundColor: Int = android.graphics.Color.WHITE
    ) {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<svg xmlns="http://www.w3.org/2000/svg" width="$pageWidth" height="$pageHeight" viewBox="0 0 $pageWidth $pageHeight">""")
        sb.appendLine("""  <rect width="$pageWidth" height="$pageHeight" fill="${colorToHex(backgroundColor)}"/>""")

        page.visibleLayersBottomUp().forEach { layer ->
            sb.appendLine("""  <g opacity="${layer.opacity}" ${if (!layer.isVisible) """visibility="hidden"""" else ""}>""")

            // Shapes → <rect>, <ellipse>, <line>
            layer.shapes.forEach { shape ->
                when (shape.shapeType) {
                    ShapeType.SQUARE -> {
                        val fill = if (shape.fillColor != 0) """fill="${colorToHex(shape.fillColor)}"""" else """fill="none""""
                        sb.appendLine("""    <rect x="${shape.x}" y="${shape.y}" width="${shape.width}" height="${shape.height}" $fill stroke="${colorToHex(shape.strokeColor)}" stroke-width="${shape.strokeWidth}"/>""")
                    }
                    ShapeType.CIRCLE -> {
                        val cx = shape.x + shape.width / 2f
                        val cy = shape.y + shape.height / 2f
                        sb.appendLine("""    <ellipse cx="$cx" cy="$cy" rx="${shape.width / 2f}" ry="${shape.height / 2f}" fill="none" stroke="${colorToHex(shape.strokeColor)}" stroke-width="${shape.strokeWidth}"/>""")
                    }
                    else -> {
                        val cx = shape.x + shape.width / 2f
                        val cy = shape.y + shape.height / 2f
                        sb.appendLine("""    <rect x="${shape.x}" y="${shape.y}" width="${shape.width}" height="${shape.height}" fill="none" stroke="${colorToHex(shape.strokeColor)}" stroke-width="${shape.strokeWidth}"/>""")
                    }
                }
            }

            // Text → <text>
            layer.charts.forEach { chart -> appendChartSvg(sb, chart, backgroundColor) }

            layer.textBlocks.forEach { text ->
                sb.appendLine("""    <text x="${text.x}" y="${text.y + text.fontSize}" font-size="${text.fontSize}" fill="${colorToHex(text.color)}">${escapeXml(text.text)}</text>""")
            }

            // A mask is local to one stroke, so erasing reveals the chart or page underneath
            // instead of repainting the document background over later content.
            layer.strokes.forEach { stroke ->
                val pathData = buildSvgPathData(stroke.points)
                val color = stroke.colorHsla.toHexColor()
                val width = DrawingEngine.strokeRenderWidth(stroke.tool, stroke.baseWidth)
                val masks = layer.eraserMarks.filter { mark ->
                    stroke.id in mark.affectedStrokeIds ||
                        (mark.affectedStrokeIds.isEmpty() && DrawingEngine.doesEraserPathAffectStroke(
                            mark.points,
                            mark.width,
                            stroke
                        ))
                }
                if (masks.isEmpty()) {
                    sb.appendLine("""    <path d="$pathData" fill="none" stroke="$color" stroke-width="$width" stroke-opacity="${stroke.colorHsla.alpha}" stroke-linecap="round" stroke-linejoin="round"/>""")
                } else {
                    val maskId = "stroke-mask-${stroke.id}"
                    sb.appendLine("""    <mask id="$maskId"><rect x="0" y="0" width="$pageWidth" height="$pageHeight" fill="white"/>""")
                    masks.forEach { mark ->
                        sb.appendLine("""      <path d="${buildSvgPathData(mark.points)}" fill="none" stroke="black" stroke-width="${mark.width}" stroke-linecap="round" stroke-linejoin="round"/>""")
                    }
                    sb.appendLine("""    </mask>""")
                    sb.appendLine("""    <path d="$pathData" fill="none" stroke="$color" stroke-width="$width" stroke-opacity="${stroke.colorHsla.alpha}" stroke-linecap="round" stroke-linejoin="round" mask="url(#$maskId)"/>""")
                }
            }

            layer.codeBlocks.forEach { codeBlock ->
                appendCodeBlockSvg(sb, codeBlock)
            }

            sb.appendLine("""  </g>""")
        }

        sb.appendLine("</svg>")
        outputFile.writeText(sb.toString())
    }

    /**
     * Експорт однієї сторінки у PDF (векторний).
     */
    fun exportToPdf(
        page: PageEntity,
        outputFile: File,
        context: Context,
        pageWidth: Int = 1920,
        pageHeight: Int = 1080,
        backgroundColor: Int = android.graphics.Color.WHITE
    ) {
        exportPagesToPdf(listOf(page), outputFile, context, pageWidth, pageHeight, backgroundColor)
    }

    /**
     * Посторінковий експорт списку сторінок у PDF із захистом від OOM (BUG-012).
     */
    fun exportPagesToPdf(
        pages: List<PageEntity>,
        outputFile: File,
        context: Context,
        pageWidth: Int = 1920,
        pageHeight: Int = 1080,
        backgroundColor: Int = android.graphics.Color.WHITE
    ) {
        val pdfDocument = PdfDocument()

        pages.forEachIndexed { index, page ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val pdfPage = pdfDocument.startPage(pageInfo)
            val canvas = pdfPage.canvas

            canvas.drawColor(backgroundColor)

            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            page.visibleLayersBottomUp().forEach { layer ->
                val layerAlpha = (layer.opacity * 255).toInt()

                // Render layer images
                renderLayerImages(context, canvas, layer.images, layerAlpha)

                layer.shapes.forEach { shape ->
                    if (shape.fillColor != 0) {
                        paint.style = Paint.Style.FILL
                        paint.color = shape.fillColor
                        paint.alpha = layerAlpha
                        canvas.drawRect(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height, paint)
                    }
                    paint.strokeWidth = shape.strokeWidth
                    paint.color = shape.strokeColor
                    paint.alpha = layerAlpha
                    paint.style = Paint.Style.STROKE
                    canvas.drawRect(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height, paint)
                }

                renderCharts(canvas, layer.charts, paint, layerAlpha, backgroundColor)

                layer.textBlocks.forEach { text ->
                    val textPaint = android.text.TextPaint().apply {
                        color = text.color
                        textSize = text.fontSize * 1.5f
                        isAntiAlias = true
                        alpha = layerAlpha
                    }
                    val maxWidth = text.width.toInt().coerceAtLeast(100)
                    val staticLayout = android.text.StaticLayout.Builder
                        .obtain(text.text, 0, text.text.length, textPaint, maxWidth)
                        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(2f, 1f)
                        .build()

                    canvas.save()
                    canvas.translate(text.x, text.y)
                    staticLayout.draw(canvas)
                    canvas.restore()
                }

                if (layer.strokes.isNotEmpty() || layer.eraserMarks.isNotEmpty()) {
                    renderStrokeLayer(canvas, layer)
                }

                renderCodeBlocks(canvas, layer.codeBlocks, layerAlpha)
            }

            pdfDocument.finishPage(pdfPage)

        }

        FileOutputStream(outputFile).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
    }

    /**
     * Експорт сторінки у PNG (растровий формат) з OOM-захистом, масштабуванням та обрізкою.
     */
    fun exportToPng(
        page: PageEntity,
        outputFile: File,
        requestedScale: Float = 3.0f,
        cropRect: android.graphics.RectF? = null,
        pageWidth: Int = 1920,
        pageHeight: Int = 1080,
        backgroundColor: Int = android.graphics.Color.WHITE,
        context: Context? = null
    ) {
        val maxDimensionPx = 4096f
        val rawW = pageWidth * requestedScale
        val rawH = pageHeight * requestedScale
        val safeScale = if (rawW > maxDimensionPx || rawH > maxDimensionPx) {
            val maxTarget = maxOf(rawW, rawH)
            requestedScale * (maxDimensionPx / maxTarget)
        } else {
            requestedScale.coerceAtLeast(0.1f)
        }

        val bmpW = (pageWidth * safeScale).toInt().coerceAtLeast(1)
        val bmpH = (pageHeight * safeScale).toInt().coerceAtLeast(1)

        val bitmap = android.graphics.Bitmap.createBitmap(bmpW, bmpH, android.graphics.Bitmap.Config.ARGB_8888)
        var outputBitmap: android.graphics.Bitmap? = null

        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(backgroundColor)
            canvas.scale(safeScale, safeScale)

            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            page.visibleLayersBottomUp().forEach { layer ->
                val layerAlpha = (layer.opacity * 255).toInt()

                context?.let { ctx ->
                    renderLayerImages(ctx, canvas, layer.images, layerAlpha)
                }

                layer.shapes.forEach { shape ->
                    if (shape.fillColor != 0) {
                        paint.style = Paint.Style.FILL
                        paint.color = shape.fillColor
                        paint.alpha = layerAlpha
                        canvas.drawRect(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height, paint)
                    }
                    paint.strokeWidth = shape.strokeWidth
                    paint.color = shape.strokeColor
                    paint.alpha = layerAlpha
                    paint.style = Paint.Style.STROKE
                    canvas.drawRect(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height, paint)
                }

                renderCharts(canvas, layer.charts, paint, layerAlpha, backgroundColor)

                layer.textBlocks.forEach { text ->
                    val textPaint = android.text.TextPaint().apply {
                        color = text.color
                        textSize = text.fontSize * 1.5f
                        isAntiAlias = true
                        alpha = layerAlpha
                    }
                    val maxWidth = text.width.toInt().coerceAtLeast(100)
                    val staticLayout = android.text.StaticLayout.Builder
                        .obtain(text.text, 0, text.text.length, textPaint, maxWidth)
                        .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(2f, 1f)
                        .build()

                    canvas.save()
                    canvas.translate(text.x, text.y)
                    staticLayout.draw(canvas)
                    canvas.restore()
                }

                if (layer.strokes.isNotEmpty() || layer.eraserMarks.isNotEmpty()) {
                    renderStrokeLayer(canvas, layer)
                }

                renderCodeBlocks(canvas, layer.codeBlocks, layerAlpha)
            }

            outputBitmap = if (cropRect != null) {
                val cx = (cropRect.left * safeScale).toInt().coerceIn(0, bmpW)
                val cy = (cropRect.top * safeScale).toInt().coerceIn(0, bmpH)
                val cw = (cropRect.width() * safeScale).toInt().coerceAtMost(bmpW - cx)
                val ch = (cropRect.height() * safeScale).toInt().coerceAtMost(bmpH - cy)
                if (cw > 0 && ch > 0) {
                    android.graphics.Bitmap.createBitmap(bitmap, cx, cy, cw, ch)
                } else bitmap
            } else bitmap

            FileOutputStream(outputFile).use { fos ->
                outputBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos)
            }
        } finally {
            if (outputBitmap != null && outputBitmap !== bitmap) {
                outputBitmap.recycle()
            }
            bitmap.recycle()
        }
    }

    private fun renderLayerImages(context: Context, canvas: Canvas, images: List<ImageElementEntity>, layerAlpha: Int) {
        images.forEach { img ->
            val bitmap = try {
                val uri = android.net.Uri.parse(img.sourceUri)
                if (uri.scheme == "file") {
                    android.graphics.BitmapFactory.decodeFile(uri.path)
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        android.graphics.BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (e: Exception) {
                null
            }

            if (bitmap != null) {
                canvas.save()
                val imgAlpha = (img.opacity * layerAlpha / 255f * 255).toInt().coerceIn(0, 255)
                val imgPaint = Paint().apply {
                    isAntiAlias = true
                    alpha = imgAlpha
                }
                if (img.rotation != 0f) {
                    canvas.rotate(img.rotation, img.x + img.width / 2f, img.y + img.height / 2f)
                }
                val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                val dstRect = android.graphics.RectF(img.x, img.y, img.x + img.width, img.y + img.height)
                canvas.drawBitmap(bitmap, srcRect, dstRect, imgPaint)
                canvas.restore()
                bitmap.recycle()
            }
        }
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

    private fun appendChartSvg(
        sb: StringBuilder,
        chart: ChartElementEntity,
        pageBackgroundColor: Int
    ) {
        val darkPage = isDarkColor(pageBackgroundColor)
        val background = when {
            chart.backgroundColor != 0 -> colorToHex(chart.backgroundColor)
            darkPage -> "#1E293B"
            else -> "#F8FAFC"
        }
        val border = if (darkPage) "#475569" else "#CBD5E1"
        val axis = if (darkPage) "#CBD5E1" else "#475569"
        val grid = if (darkPage) "#FFFFFF" else "#000000"
        val clipId = "chart-clip-${chart.id}"
        val centerX = chart.x + chart.width / 2f
        val centerY = chart.y + chart.height / 2f
        val transform = if (chart.rotation != 0f) {
            " transform=\"rotate(${chart.rotation} $centerX $centerY)\""
        } else {
            ""
        }
        val unit = chart.squarePixelsPerUnit()
        val originX = chart.x + (chart.originOffsetX.takeIf { it >= 0f } ?: chart.width / 2f)
        val originY = chart.y + (chart.originOffsetY.takeIf { it >= 0f } ?: chart.height / 2f)
        val right = chart.x + chart.width
        val bottom = chart.y + chart.height
        val firstX = kotlin.math.ceil((chart.x - originX) / unit).toInt()
        val lastX = kotlin.math.floor((right - originX) / unit).toInt()
        val firstY = kotlin.math.ceil((chart.y - originY) / unit).toInt()
        val lastY = kotlin.math.floor((bottom - originY) / unit).toInt()
        val xAxisVisible = originY in chart.y..bottom
        val yAxisVisible = originX in chart.x..right

        sb.appendLine("""    <g$transform>""")
        sb.appendLine("""      <clipPath id="$clipId"><rect x="${chart.x}" y="${chart.y}" width="${chart.width}" height="${chart.height}"/></clipPath>""")
        sb.appendLine("""      <rect x="${chart.x}" y="${chart.y}" width="${chart.width}" height="${chart.height}" fill="$background" stroke="$border" stroke-width="1.5"/>""")
        sb.appendLine("""      <g clip-path="url(#$clipId)">""")
        for (index in firstX..lastX) {
            val x = originX + index * unit
            sb.appendLine("""        <line x1="$x" y1="${chart.y}" x2="$x" y2="$bottom" stroke="$grid" stroke-opacity="0.19" stroke-width="1"/>""")
        }
        for (index in firstY..lastY) {
            val y = originY + index * unit
            sb.appendLine("""        <line x1="${chart.x}" y1="$y" x2="$right" y2="$y" stroke="$grid" stroke-opacity="0.19" stroke-width="1"/>""")
        }
        if (xAxisVisible) sb.appendLine("""        <line x1="${chart.x}" y1="$originY" x2="$right" y2="$originY" stroke="$axis" stroke-width="2.5"/>""")
        if (yAxisVisible) sb.appendLine("""        <line x1="$originX" y1="${chart.y}" x2="$originX" y2="$bottom" stroke="$axis" stroke-width="2.5"/>""")

        val xTicks = chartAxisTickValues(-(originX - chart.x) / unit, (right - originX) / unit, chart.xStep)
        val yTicks = chartAxisTickValues(-(bottom - originY) / unit, (originY - chart.y) / unit, chart.yStep)
        if (xAxisVisible) for (index in firstX..lastX) {
            val x = originX + index * unit
            if (x in chart.x..right) sb.appendLine("""        <line x1="$x" y1="${originY - 4f}" x2="$x" y2="${originY + 4f}" stroke="$axis" stroke-width="2"/>""")
        }
        if (yAxisVisible) for (index in firstY..lastY) {
            val y = originY + index * unit
            if (y in chart.y..bottom) sb.appendLine("""        <line x1="${originX - 4f}" y1="$y" x2="${originX + 4f}" y2="$y" stroke="$axis" stroke-width="2"/>""")
        }
        if (chart.showAxisLabels && chart.axisLabelsVisible) {
            val labelColor = if (darkPage) "#D1D5DB" else "#4B5563"
            if (xAxisVisible) xTicks.forEach { value ->
                val x = originX + value * unit
                if (x in chart.x..right) sb.appendLine("""        <text x="${x - 6f}" y="${(originY + 14f).coerceAtMost(bottom - 4f)}" font-size="11" fill="$labelColor">${formatAxisValue(value)}</text>""")
            }
            if (yAxisVisible) yTicks.forEach { value ->
                if (kotlin.math.abs(value) <= 0.0001f) return@forEach
                val y = originY - value * unit
                if (y in chart.y..bottom) sb.appendLine("""        <text x="${(originX + 4f).coerceAtMost(right - 12f)}" y="${y + 4f}" font-size="11" fill="$labelColor">${formatAxisValue(value)}</text>""")
            }
            if (xAxisVisible) sb.appendLine("""        <text x="${right - 18f}" y="${originY - 8f}" font-size="11" fill="$labelColor">X</text>""")
            if (yAxisVisible) sb.appendLine("""        <text x="${originX + 8f}" y="${chart.y + 18f}" font-size="11" fill="$labelColor">Y</text>""")
        }
        sb.appendLine("""      </g>""")
        sb.appendLine("""    </g>""")
    }

    private fun colorToHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;")
    }

    /**
     * Рендеринг сторінки у Bitmap високої роздільної здатності для AI Vision (offscreen).
     */
    fun captureCanvasHighRes(
        page: PageEntity,
        context: Context,
        scale: Float = 3.0f,
        pageWidth: Int = 1920,
        pageHeight: Int = 1080,
        backgroundColor: Int = android.graphics.Color.WHITE
    ): android.graphics.Bitmap {
        val maxDimensionPx = 4096f
        val rawW = pageWidth * scale
        val rawH = pageHeight * scale
        val safeScale = if (rawW > maxDimensionPx || rawH > maxDimensionPx) {
            val maxTarget = maxOf(rawW, rawH)
            scale * (maxDimensionPx / maxTarget)
        } else {
            scale.coerceAtLeast(0.1f)
        }

        val bmpW = (pageWidth * safeScale).toInt().coerceAtLeast(1)
        val bmpH = (pageHeight * safeScale).toInt().coerceAtLeast(1)

        val bitmap = android.graphics.Bitmap.createBitmap(bmpW, bmpH, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)
        canvas.scale(safeScale, safeScale)

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        page.visibleLayersBottomUp().forEach { layer ->
            val layerAlpha = (layer.opacity * 255).toInt()

            renderLayerImages(context, canvas, layer.images, layerAlpha)

            layer.shapes.forEach { shape ->
                paint.strokeWidth = shape.strokeWidth
                paint.color = shape.strokeColor
                paint.alpha = layerAlpha
                paint.style = Paint.Style.STROKE
                canvas.drawRect(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height, paint)
            }

            renderCharts(canvas, layer.charts, paint, layerAlpha, backgroundColor)

            layer.textBlocks.forEach { text ->
                val textPaint = android.text.TextPaint().apply {
                    color = text.color
                    textSize = text.fontSize * 1.5f
                    isAntiAlias = true
                    alpha = layerAlpha
                }
                val maxWidth = text.width.toInt().coerceAtLeast(100)
                val staticLayout = android.text.StaticLayout.Builder
                    .obtain(text.text, 0, text.text.length, textPaint, maxWidth)
                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(2f, 1f)
                    .build()

                canvas.save()
                canvas.translate(text.x, text.y)
                staticLayout.draw(canvas)
                canvas.restore()
            }

            if (layer.strokes.isNotEmpty() || layer.eraserMarks.isNotEmpty()) {
                renderStrokeLayer(canvas, layer)
            }

            renderCodeBlocks(canvas, layer.codeBlocks, layerAlpha)
        }
        return bitmap
    }

    private fun renderCharts(
        canvas: Canvas,
        charts: List<ChartElementEntity>,
        paint: Paint,
        alpha: Int,
        pageBackgroundColor: Int
    ) {
        val darkPage = isDarkColor(pageBackgroundColor)
        charts.forEach { chart ->
            val background = when {
                chart.backgroundColor != 0 -> chart.backgroundColor
                darkPage -> android.graphics.Color.parseColor("#1E293B")
                else -> android.graphics.Color.parseColor("#F8FAFC")
            }
            val border = if (darkPage) {
                android.graphics.Color.parseColor("#475569")
            } else {
                android.graphics.Color.parseColor("#CBD5E1")
            }
            val axis = if (darkPage) {
                android.graphics.Color.parseColor("#CBD5E1")
            } else {
                android.graphics.Color.parseColor("#475569")
            }
            canvas.save()
            if (chart.rotation != 0f) {
                canvas.rotate(chart.rotation, chart.x + chart.width / 2f, chart.y + chart.height / 2f)
            }
            paint.style = Paint.Style.FILL
            paint.color = background
            paint.alpha = alpha.coerceIn(0, 255)
            canvas.drawRect(chart.x, chart.y, chart.x + chart.width, chart.y + chart.height, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = border
            paint.alpha = alpha.coerceIn(0, 255)
            canvas.drawRect(chart.x, chart.y, chart.x + chart.width, chart.y + chart.height, paint)
            drawStableChartAxes(canvas, chart, paint, alpha, axis, darkPage)
            canvas.restore()
        }
    }

    /** Matches the editor's fixed coordinate origin so exports do not recenter X/Y axes. */
    private fun drawStableChartAxes(
        canvas: Canvas,
        chart: ChartElementEntity,
        paint: Paint,
        alpha: Int,
        axisColor: Int,
        darkPage: Boolean
    ) {
        val unitsToPixelsX = chart.squarePixelsPerUnit()
        val unitsToPixelsY = unitsToPixelsX
        val originX = chart.x + (chart.originOffsetX.takeIf { it >= 0f } ?: chart.width / 2f)
        val originY = chart.y + (chart.originOffsetY.takeIf { it >= 0f } ?: chart.height / 2f)
        val right = chart.x + chart.width
        val bottom = chart.y + chart.height
        val xTicks = chartAxisTickValues(
            minimum = -(originX - chart.x) / unitsToPixelsX,
            maximum = (right - originX) / unitsToPixelsX,
            step = chart.xStep
        )
        val yTicks = chartAxisTickValues(
            minimum = -(bottom - originY) / unitsToPixelsY,
            maximum = (originY - chart.y) / unitsToPixelsY,
            step = chart.yStep
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.alpha = alpha.coerceIn(0, 255) / 2
        paint.color = axisColor
        val firstX = kotlin.math.ceil((chart.x - originX) / unitsToPixelsX).toInt()
        val lastX = kotlin.math.floor((right - originX) / unitsToPixelsX).toInt()
        for (index in firstX..lastX) {
            val x = originX + index * unitsToPixelsX
            canvas.drawLine(x, chart.y, x, bottom, paint)
        }
        val firstY = kotlin.math.ceil((chart.y - originY) / unitsToPixelsY).toInt()
        val lastY = kotlin.math.floor((bottom - originY) / unitsToPixelsY).toInt()
        for (index in firstY..lastY) {
            val y = originY + index * unitsToPixelsY
            canvas.drawLine(chart.x, y, right, y, paint)
        }

        paint.strokeWidth = 2.5f
        paint.alpha = alpha.coerceIn(0, 255)
        val xAxisVisible = originY in chart.y..bottom
        val yAxisVisible = originX in chart.x..right
        if (xAxisVisible) canvas.drawLine(chart.x, originY, right, originY, paint)
        if (yAxisVisible) canvas.drawLine(originX, chart.y, originX, bottom, paint)

        val tickHalfLength = 4f
        if (xAxisVisible) for (index in firstX..lastX) {
            val x = originX + index * unitsToPixelsX
            if (x in chart.x..right) {
                canvas.drawLine(x, originY - tickHalfLength, x, originY + tickHalfLength, paint)
            }
        }
        if (yAxisVisible) for (index in firstY..lastY) {
            val y = originY + index * unitsToPixelsY
            if (y in chart.y..bottom) {
                canvas.drawLine(originX - tickHalfLength, y, originX + tickHalfLength, y, paint)
            }
        }

        if (chart.showAxisLabels && chart.axisLabelsVisible) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (darkPage) android.graphics.Color.LTGRAY else android.graphics.Color.DKGRAY
                this.alpha = alpha.coerceIn(0, 255)
                textSize = 11f
            }
            if (xAxisVisible) xTicks.forEach { value ->
                val x = originX + value * unitsToPixelsX
                if (x in chart.x..right) {
                    canvas.drawText(formatAxisValue(value), x - 6f, (originY + 14f).coerceAtMost(bottom - 4f), textPaint)
                }
            }
            if (yAxisVisible) yTicks.forEach { value ->
                if (kotlin.math.abs(value) <= 0.0001f) return@forEach
                val y = originY - value * unitsToPixelsY
                if (y in chart.y..bottom) {
                    canvas.drawText(formatAxisValue(value), (originX + 4f).coerceAtMost(right - 12f), y + 4f, textPaint)
                }
            }
            if (xAxisVisible) canvas.drawText("X", right - 18f, originY - 8f, textPaint)
            if (yAxisVisible) canvas.drawText("Y", originX + 8f, chart.y + 18f, textPaint)
        }
    }

    private fun chartAxisTickValues(minimum: Float, maximum: Float, step: Float): List<Float> {
        val safeStep = step.takeIf { it.isFinite() && it > 0f } ?: 1f
        val first = kotlin.math.ceil(minimum / safeStep).toInt()
        val last = kotlin.math.floor(maximum / safeStep).toInt()
        if (last < first) return emptyList()
        val stride = kotlin.math.ceil((last - first + 1) / 501f).toInt().coerceAtLeast(1)
        return buildList {
            for (index in first..last step stride) add(index * safeStep)
        }
    }

    private fun formatAxisValue(value: Float): String =
        if (value == value.toInt().toFloat()) value.toInt().toString()
        else String.format(java.util.Locale.US, "%.1f", value)

    private fun isDarkColor(color: Int): Boolean {
        val red = android.graphics.Color.red(color) / 255f
        val green = android.graphics.Color.green(color) / 255f
        val blue = android.graphics.Color.blue(color) / 255f
        return red * 0.299f + green * 0.587f + blue * 0.114f < 0.5f
    }

    private fun renderStrokeLayer(canvas: Canvas, layer: LayerEntity) {
        if (layer.strokes.isEmpty()) return
        val outerLayer = canvas.saveLayer(null, null)

        layer.strokes.forEach { stroke ->
            val masks = layer.eraserMarks.filter { mark ->
                stroke.id in mark.affectedStrokeIds ||
                    (mark.affectedStrokeIds.isEmpty() && DrawingEngine.doesEraserPathAffectStroke(
                        mark.points,
                        mark.width,
                        stroke
                    ))
            }
            RasterStrokeCompositor.drawRasterStroke(canvas, stroke, masks, layerAlpha = layer.opacity)
        }
        canvas.restoreToCount(outerLayer)
    }

    private fun renderCodeBlocks(
        canvas: Canvas,
        codeBlocks: List<CodeBlockEntity>,
        layerAlpha: Int
    ) {
        codeBlocks.forEach { block ->
            val rect = android.graphics.RectF(block.x, block.y, block.x + block.width, block.y + block.height)
            val radius = 14f
            val headerHeight = 38f.coerceAtMost(block.height)
            val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = android.graphics.Color.rgb(17, 24, 39)
                alpha = layerAlpha.coerceIn(0, 255)
            }
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = when (block.language) {
                    CodeLanguage.PYTHON -> android.graphics.Color.rgb(55, 118, 171)
                    CodeLanguage.C -> android.graphics.Color.rgb(92, 107, 192)
                    CodeLanguage.CPP -> android.graphics.Color.rgb(0, 89, 156)
                }
                alpha = layerAlpha.coerceIn(0, 255)
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(229, 231, 235)
                textSize = 15f
                typeface = android.graphics.Typeface.MONOSPACE
                alpha = layerAlpha.coerceIn(0, 255)
            }
            val labelPaint = Paint(textPaint).apply {
                color = android.graphics.Color.WHITE
                textSize = 14f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            }

            canvas.save()
            canvas.clipRect(rect)
            canvas.drawRoundRect(rect, radius, radius, backgroundPaint)
            canvas.drawRect(block.x, block.y, block.x + block.width, block.y + headerHeight, headerPaint)
            canvas.drawText(block.language.displayName, block.x + 12f, block.y + 25f, labelPaint)

            val sourceTop = block.y + headerHeight + 20f
            val outputReserved = if (block.consoleOutput.isNotBlank() || block.diagnostics.isNotEmpty()) 72f else 12f
            val maximumSourceY = block.y + block.height - outputReserved
            var lineY = sourceTop
            block.source.lines().forEach { line ->
                if (lineY > maximumSourceY) return@forEach
                canvas.drawText(line.take(100), block.x + 12f, lineY, textPaint)
                lineY += 19f
            }

            val consoleText = buildString {
                append(block.consoleOutput)
                if (block.diagnostics.isNotEmpty()) {
                    if (isNotEmpty()) append('\n')
                    append(block.diagnostics.joinToString(" | "))
                }
            }
            if (consoleText.isNotBlank()) {
                val consoleTop = (block.y + block.height - 62f).coerceAtLeast(sourceTop)
                val consolePaint = Paint(backgroundPaint).apply { color = android.graphics.Color.rgb(11, 18, 32) }
                canvas.drawRect(block.x, consoleTop, block.x + block.width, block.y + block.height, consolePaint)
                val outputPaint = Paint(textPaint).apply {
                    color = if (block.diagnostics.isEmpty()) {
                        android.graphics.Color.rgb(134, 239, 172)
                    } else {
                        android.graphics.Color.rgb(253, 230, 138)
                    }
                    textSize = 13f
                }
                consoleText.lines().take(2).forEachIndexed { index, line ->
                    canvas.drawText(line.take(110), block.x + 12f, consoleTop + 22f + index * 18f, outputPaint)
                }
            }
            canvas.restore()
        }
    }

    private fun appendCodeBlockSvg(sb: StringBuilder, block: CodeBlockEntity) {
        val headerColor = when (block.language) {
            CodeLanguage.PYTHON -> "#3776AB"
            CodeLanguage.C -> "#5C6BC0"
            CodeLanguage.CPP -> "#00599C"
        }
        sb.appendLine("""    <g class="sketchpad-code-block">""")
        sb.appendLine("""      <rect x="${block.x}" y="${block.y}" width="${block.width}" height="${block.height}" rx="14" fill="#111827"/>""")
        sb.appendLine("""      <path d="M ${block.x + 14} ${block.y} H ${block.x + block.width - 14} Q ${block.x + block.width} ${block.y} ${block.x + block.width} ${block.y + 14} V ${block.y + 38} H ${block.x} V ${block.y + 14} Q ${block.x} ${block.y} ${block.x + 14} ${block.y} Z" fill="$headerColor"/>""")
        sb.appendLine("""      <text x="${block.x + 12}" y="${block.y + 25}" font-family="monospace" font-size="14" font-weight="bold" fill="#FFFFFF">${escapeXml(block.language.displayName)}</text>""")
        var y = block.y + 58f
        val maxY = block.y + block.height - if (block.consoleOutput.isNotBlank()) 65f else 10f
        block.source.lines().forEach { line ->
            if (y <= maxY) {
                sb.appendLine("""      <text x="${block.x + 12}" y="$y" font-family="monospace" font-size="14" fill="#E5E7EB">${escapeXml(line.take(120))}</text>""")
                y += 19f
            }
        }
        if (block.consoleOutput.isNotBlank()) {
            val consoleY = block.y + block.height - 58f
            sb.appendLine("""      <rect x="${block.x}" y="$consoleY" width="${block.width}" height="58" fill="#0B1220"/>""")
            block.consoleOutput.lines().take(2).forEachIndexed { index, line ->
                sb.appendLine("""      <text x="${block.x + 12}" y="${consoleY + 21f + index * 18f}" font-family="monospace" font-size="12" fill="#86EFAC">${escapeXml(line.take(120))}</text>""")
            }
        }
        sb.appendLine("""    </g>""")
    }

    suspend fun exportToObsidian(
        page: PageEntity,
        shapes: List<ShapeEntity>,
        vaultUri: Uri,
        context: Context,
        format: ObsidianFormat = ObsidianFormat.PNG,
        pageName: String = "Sketchpad page ${page.pageIndex + 1}",
        aiSummary: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = System.currentTimeMillis()
            val safeName = pageName
                .replace(Regex("[\\/:*?\"<>|#^\\[\\]]"), "_")
                .trim()
                .ifBlank { "Sketchpad_page_${page.pageIndex + 1}" }
            val extension = if (format == ObsidianFormat.PNG) "png" else "svg"
            val attachmentName = "sketchpad_${safeName}_$timestamp.$extension"
            val tempFile = File(context.cacheDir, attachmentName)

            try {
                when (format) {
                    ObsidianFormat.PNG -> exportToPng(page.copy(shapes = shapes), tempFile, context = context)
                    ObsidianFormat.SVG -> exportToSvg(page.copy(shapes = shapes), tempFile)
                }

                val rootUri = DocumentsContract.buildDocumentUriUsingTree(
                    vaultUri,
                    DocumentsContract.getTreeDocumentId(vaultUri)
                )
                val attachmentsUri = findOrCreateDirectory(context, rootUri, "attachments")
                val sketchpadUri = findOrCreateDirectory(context, rootUri, "Sketchpad")
                val attachmentMimeType = if (format == ObsidianFormat.PNG) "image/png" else "image/svg+xml"
                val attachmentUri = createDocument(context, attachmentsUri, attachmentName, attachmentMimeType)
                context.contentResolver.openOutputStream(attachmentUri)?.use { output ->
                    tempFile.inputStream().use { input -> input.copyTo(output) }
                } ?: error("Unable to write Obsidian attachment")

                val markdown = """
                    ---
                    tags:
                      - sketchpad
                      - lecture
                    date: ${java.time.LocalDate.now()}
                    source: Sketchpad
                    ---

                    # $pageName

                    ![[$attachmentName]]

                    ## Нотатки
                    ${aiSummary.orEmpty()}
                """.trimIndent() + "\n"
                val noteUri = createDocument(context, sketchpadUri, "$safeName.md", "text/markdown")
                context.contentResolver.openOutputStream(noteUri)?.bufferedWriter()?.use { it.write(markdown) }
                    ?: error("Unable to write Obsidian note")
            } finally {
                tempFile.delete()
            }
        }
    }

    private fun findOrCreateDirectory(context: Context, parentUri: Uri, name: String): Uri {
        val resolver = context.contentResolver
        val parentDocumentId = DocumentsContract.getDocumentId(parentUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentUri, parentDocumentId)
        resolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == name) {
                    return DocumentsContract.buildDocumentUriUsingTree(parentUri, cursor.getString(idIndex))
                }
            }
        }
        return DocumentsContract.createDocument(
            resolver,
            parentUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            name
        ) ?: error("Unable to create Obsidian directory: $name")
    }

    private fun createDocument(context: Context, parentUri: Uri, name: String, mimeType: String): Uri =
        DocumentsContract.createDocument(context.contentResolver, parentUri, mimeType, name)
            ?: error("Unable to create Obsidian file: $name")

    /**
     * Видалення тимчасових файлів експорту з папки кешу.
     */
    fun cleanupTempExports(context: Context) {
        try {
            context.cacheDir.listFiles { file ->
                file.name.startsWith("export_") && (file.name.endsWith(".png") || file.name.endsWith(".pdf") || file.name.endsWith(".svg"))
            }?.forEach { it.delete() }
        } catch (e: Exception) {
            android.util.Log.e("ExportManager", "Failed to cleanup temp export files", e)
        }
    }
}

fun HslaColor.toHexColor(): String {
    val color = this.toColor()
    return String.format("#%02X%02X%02X",
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt())
}
