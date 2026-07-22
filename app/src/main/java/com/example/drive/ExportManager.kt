package com.example.drive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.graphics.asAndroidPath
import com.example.core.drawing.DrawingEngine
import com.example.data.models.CanvasEntity
import com.example.data.models.PageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ExportManager(private val context: Context) {

    suspend fun exportCanvasToPdf(
        canvas: CanvasEntity,
        pages: List<PageEntity>,
        targetWidthPx: Int = 1240,
        targetHeightPx: Int = 1754
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()

        pages.forEachIndexed { index, page ->
            val pageInfo = PdfDocument.PageInfo.Builder(targetWidthPx, targetHeightPx, index + 1).create()
            val pdfPage = pdfDocument.startPage(pageInfo)
            val pdfCanvas = pdfPage.canvas

            // Render page background color
            pdfCanvas.drawColor(canvas.backgroundColor)

            // Render images
            renderImages(pdfCanvas, page)

            // Render charts
            renderCharts(pdfCanvas, page)

            // Render shapes
            page.shapes.forEach { shape ->
                val paint = Paint().apply {
                    color = shape.fillColor
                    style = Paint.Style.FILL
                }
                val rect = RectF(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height)
                pdfCanvas.drawRect(rect, paint)

                val strokePaint = Paint().apply {
                    color = shape.strokeColor
                    strokeWidth = shape.strokeWidth
                    style = Paint.Style.STROKE
                }
                pdfCanvas.drawRect(rect, strokePaint)
            }

            // Render strokes
            page.strokes.forEach { stroke ->
                val strokePaint = Paint().apply {
                    color = stroke.colorHsla.toArgbInt()
                    strokeWidth = stroke.baseWidth
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val path = DrawingEngine.createSmoothPath(stroke.points)
                pdfCanvas.drawPath(path.asAndroidPath(), strokePaint)
            }

            // Render text blocks
            page.textBlocks.forEach { textBlock ->
                val textPaint = Paint().apply {
                    color = textBlock.color
                    textSize = textBlock.fontSize * 1.5f
                    isFakeBoldText = textBlock.isBold
                }
                pdfCanvas.drawText(textBlock.text, textBlock.x, textBlock.y + textBlock.fontSize, textPaint)
            }

            pdfDocument.finishPage(pdfPage)
        }

        val exportFile = File(context.cacheDir, "${canvas.title.replace(" ", "_")}_export.pdf")
        FileOutputStream(exportFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return@withContext exportFile
    }

    suspend fun exportPageToBitmap(
        canvas: CanvasEntity,
        page: PageEntity,
        scaleRatio: Float = 1.0f,
        targetWidthPx: Int = 1200,
        targetHeightPx: Int = 1600
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = (targetWidthPx * scaleRatio).toInt()
        val height = (targetHeightPx * scaleRatio).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bitmapCanvas = Canvas(bitmap)
        bitmapCanvas.scale(scaleRatio, scaleRatio)

        // Draw background
        bitmapCanvas.drawColor(canvas.backgroundColor)

        // Draw images
        renderImages(bitmapCanvas, page)

        // Draw charts
        renderCharts(bitmapCanvas, page)

        // Draw shapes
        page.shapes.forEach { shape ->
            val paint = Paint().apply {
                color = shape.fillColor
                style = Paint.Style.FILL
            }
            val rect = RectF(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height)
            bitmapCanvas.drawRect(rect, paint)

            val strokePaint = Paint().apply {
                color = shape.strokeColor
                strokeWidth = shape.strokeWidth
                style = Paint.Style.STROKE
            }
            bitmapCanvas.drawRect(rect, strokePaint)
        }

        // Draw strokes
        page.strokes.forEach { stroke ->
            val strokePaint = Paint().apply {
                color = stroke.colorHsla.toArgbInt()
                strokeWidth = stroke.baseWidth
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val path = DrawingEngine.createSmoothPath(stroke.points)
            bitmapCanvas.drawPath(path.asAndroidPath(), strokePaint)
        }

        // Draw text blocks
        page.textBlocks.forEach { textBlock ->
            val textPaint = Paint().apply {
                color = textBlock.color
                textSize = textBlock.fontSize * 1.5f
                isFakeBoldText = textBlock.isBold
            }
            bitmapCanvas.drawText(textBlock.text, textBlock.x, textBlock.y + textBlock.fontSize, textPaint)
        }

        return@withContext bitmap
    }

    private fun renderImages(canvas: Canvas, page: PageEntity) {
        page.images.forEach { image ->
            try {
                val file = File(image.sourceUri)
                val imgBitmap = if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else null
                if (imgBitmap != null) {
                    val rect = RectF(image.x, image.y, image.x + image.width, image.y + image.height)
                    canvas.drawBitmap(imgBitmap, null, rect, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun renderCharts(canvas: Canvas, page: PageEntity) {
        page.charts.forEach { chart ->
            val paint = Paint().apply {
                color = 0xFF94A3B8.toInt()
                strokeWidth = 1.5f
                style = Paint.Style.STROKE
            }
            val rect = RectF(chart.x, chart.y, chart.x + chart.width, chart.y + chart.height)
            canvas.drawRect(rect, paint)

            val cols = 10
            val rows = 10
            val dx = chart.width / cols
            val dy = chart.height / rows
            for (i in 1 until cols) {
                canvas.drawLine(chart.x + i * dx, chart.y, chart.x + i * dx, chart.y + chart.height, paint)
            }
            for (j in 1 until rows) {
                canvas.drawLine(chart.x, chart.y + j * dy, chart.x + chart.width, chart.y + j * dy, paint)
            }
        }
    }

    suspend fun saveBitmapToFile(bitmap: Bitmap, filename: String): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return@withContext file
    }
}
