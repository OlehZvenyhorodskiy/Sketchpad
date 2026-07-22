package com.example.core.gesture

import android.view.MotionEvent

/**
 * Фільтр для відхилення дотиків долонею.
 * Логіка: якщо активний stylus — ігнорувати всі finger touches.
 * Якщо stylus не активний — приймати finger touches.
 * Додатково: фільтр за площею дотику (palm > 800px²).
 */
object PalmRejectionFilter {

    private const val PALM_AREA_THRESHOLD = 800f  // px²
    private const val STYLUS_TIMEOUT_MS = 300L    // після stylus — ігнорувати finger 300ms

    private var lastStylusEventTime = 0L
    private var isStylusActive = false

    fun shouldRejectEvent(event: MotionEvent): Boolean {
        val toolType = event.getToolType(0)

        return when (toolType) {
            MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.TOOL_TYPE_ERASER -> {
                isStylusActive = true
                lastStylusEventTime = System.currentTimeMillis()
                false  // НЕ відхиляти — це стилус
            }
            MotionEvent.TOOL_TYPE_FINGER -> {
                // Якщо стилус активний або був активний нещодавно — відхилити
                if (isStylusActive) return true
                if (System.currentTimeMillis() - lastStylusEventTime < STYLUS_TIMEOUT_MS) return true

                // Перевірка площі дотику (долоня має велику площу)
                val touchMajor = event.getTouchMajor(0)
                val touchMinor = event.getTouchMinor(0)
                val area = Math.PI * (touchMajor / 2f) * (touchMinor / 2f)
                if (area > PALM_AREA_THRESHOLD) return true

                false  // Прийняти — це нормальний finger touch
            }
            else -> false
        }
    }

    fun onStylusLifted() {
        isStylusActive = false
        lastStylusEventTime = System.currentTimeMillis()
    }

    fun reset() {
        isStylusActive = false
        lastStylusEventTime = 0L
    }
}
