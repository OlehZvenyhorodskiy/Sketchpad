package com.example.core.drawing

import android.view.MotionEvent

/**
 * Обробник тиску та нахилу стилуса.
 * Підтримує: S-Pen, Apple Pencil (через Android), Wacom, USI stylus.
 */
object PressureProcessor {

    data class StylusData(
        val x: Float,
        val y: Float,
        val pressure: Float,      // 0.0 - 1.0
        val tiltX: Float,         // -1.0 - 1.0 (радиани / PI)
        val tiltY: Float,
        val orientation: Float,   // 0 - 2PI
        val toolType: Int
    )

    fun extractStylusData(event: MotionEvent, pointerIndex: Int = 0): StylusData {
        val pressure = event.getPressure(pointerIndex).coerceIn(0f, 1f)

        val tilt = event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex)
        val orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION, pointerIndex)

        val tiltX = if (tilt > 0) {
            (Math.sin(tilt.toDouble()) * Math.cos(orientation.toDouble())).toFloat()
        } else 0f
        val tiltY = if (tilt > 0) {
            (Math.sin(tilt.toDouble()) * Math.sin(orientation.toDouble())).toFloat()
        } else 0f

        return StylusData(
            x = event.getX(pointerIndex),
            y = event.getY(pointerIndex),
            pressure = pressure,
            tiltX = tiltX,
            tiltY = tiltY,
            orientation = orientation,
            toolType = event.getToolType(pointerIndex)
        )
    }

    /**
     * Обчислює ширину лінії на основі pressure + tilt.
     */
    fun calculateWidth(
        baseWidth: Float,
        pressure: Float,
        tiltMagnitude: Float,
        pressureSensitivity: Float = 1f,
        tiltSensitivity: Float = 0f
    ): Float {
        val pressureFactor = 1f - pressureSensitivity + pressureSensitivity * pressure
        val tiltFactor = 1f + tiltSensitivity * tiltMagnitude * 2f
        return baseWidth * pressureFactor * tiltFactor
    }
}
