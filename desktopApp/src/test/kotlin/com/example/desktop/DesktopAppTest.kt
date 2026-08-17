package com.example.desktop

import com.example.desktop.export.DesktopExportManager
import com.example.desktop.input.DesktopShortcutManager
import com.example.desktop.input.WindowsInkHandler
import com.example.shared.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class DesktopAppTest {

    @Test
    fun testDesktopViewModelLayersAndUndo() {
        val vm = DesktopViewModel()
        assertEquals(1, vm.currentPage.getEffectiveLayers().size)

        // Add layer
        vm.addLayer("Шар 2")
        assertEquals(2, vm.currentPage.getEffectiveLayers().size)

        // Add Stroke
        val stroke = StrokeEntity(
            tool = ToolType.PEN,
            colorHsla = HslaColor.BLUE,
            baseWidth = 4f,
            points = listOf(StrokePoint(10f, 10f), StrokePoint(20f, 20f))
        )
        vm.commitStroke(stroke)
        assertEquals(1, vm.currentPage.getActiveLayer().strokes.size)

        // Test Undo
        vm.undo()
        assertEquals(0, vm.currentPage.getActiveLayer().strokes.size)

        // Test Redo
        vm.redo()
        assertEquals(1, vm.currentPage.getActiveLayer().strokes.size)

        vm.onDispose()
    }

    @Test
    fun testDesktopViewModelSnapshots() {
        val vm = DesktopViewModel()
        vm.createSnapshot("Initial State")
        assertEquals(1, vm.snapshots.value.size)

        vm.addNewPage()
        assertEquals(2, vm.pages.value.size)

        vm.onDispose()
    }

    @Test
    fun testExportToSvg() {
        val page = PageEntity(
            canvasId = "test",
            pageIndex = 0,
            strokes = listOf(
                StrokeEntity(
                    tool = ToolType.PEN,
                    colorHsla = HslaColor.BLACK,
                    baseWidth = 3f,
                    points = listOf(StrokePoint(0f, 0f), StrokePoint(50f, 50f))
                )
            ),
            shapes = listOf(
                ShapeEntity(shapeType = ShapeType.SQUARE, x = 10f, y = 10f, width = 100f, height = 100f)
            )
        )
        val tempSvg = File.createTempFile("test_svg", ".svg")
        try {
            DesktopExportManager.exportToSvg(page, tempSvg)
            assertTrue(tempSvg.exists())
            val content = tempSvg.readText()
            assertTrue(content.contains("<svg"))
            assertTrue(content.contains("<path"))
            assertTrue(content.contains("<rect"))
        } finally {
            tempSvg.delete()
        }
    }

    @Test
    fun testExportToRasterAndPdf() {
        val page = PageEntity(
            canvasId = "test",
            pageIndex = 0,
            strokes = listOf(
                StrokeEntity(
                    tool = ToolType.PEN,
                    colorHsla = HslaColor.RED,
                    baseWidth = 2f,
                    points = listOf(StrokePoint(20f, 20f), StrokePoint(80f, 80f))
                )
            )
        )

        val tempPng = File.createTempFile("test_png", ".png")
        val tempPdf = File.createTempFile("test_pdf", ".pdf")
        try {
            DesktopExportManager.exportToRaster(page, tempPng, "PNG")
            assertTrue(tempPng.exists() && tempPng.length() > 0)

            DesktopExportManager.exportToPdf(listOf(page), tempPdf)
            assertTrue(tempPdf.exists() && tempPdf.length() > 0)
        } finally {
            tempPng.delete()
            tempPdf.delete()
        }
    }

    @Test
    fun testExportToSketchpadProject() {
        val canvas = CanvasEntity(title = "Тестовий проект")
        val page = PageEntity(canvasId = canvas.id, pageIndex = 0)
        val tempZip = File.createTempFile("test_proj", ".sketchpad")
        try {
            DesktopExportManager.exportToSketchpadProject(canvas, listOf(page), tempZip)
            assertTrue(tempZip.exists() && tempZip.length() > 0)
        } finally {
            tempZip.delete()
        }
    }
}
