package com.example.core.drawing

import com.example.data.models.StrokePoint
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PathSmoothingTest {

    @Test
    fun `createCatmullRomPath generates path for multiple points`() {
        val points = listOf(
            StrokePoint(x = 0f, y = 0f, pressure = 0.5f),
            StrokePoint(x = 10f, y = 20f, pressure = 0.5f),
            StrokePoint(x = 30f, y = 10f, pressure = 0.5f),
            StrokePoint(x = 50f, y = 40f, pressure = 0.5f)
        )

        val path = PathSmoothing.createCatmullRomPath(points = points, tension = 0.5f, segments = 8)
        assertNotNull(path)
        assertTrue(!path.isEmpty)
    }

    @Test
    fun `createCatmullRomPath handles single point`() {
        val points = listOf(StrokePoint(x = 10f, y = 10f, pressure = 0.5f))
        val path = PathSmoothing.createCatmullRomPath(points = points)
        assertNotNull(path)
        assertTrue(!path.isEmpty)
    }
}
