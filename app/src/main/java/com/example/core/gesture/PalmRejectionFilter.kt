package com.example.core.gesture

import android.os.Build
import android.view.MotionEvent

/**
 * Розумний фільтр відхилення долоні (Smart Palm Rejection Filter).
 * Підтримує активні та пасивні стилуси з адаптивним порогом в зависимости от плотности экрана.
 */
object PalmRejectionFilter {

    private const val PALM_TOUCH_MAJOR_DP = 42f
    private const val PALM_TOOL_MAJOR_DP = 38f
    private const val PALM_AREA_DP2 = 800f
    private const val TOOL_TYPE_PALM_VALUE = 4

    fun shouldRejectEvent(
        event: MotionEvent,
        pointerIndex: Int = 0,
        displayDensity: Float = 1.0f
    ): Boolean {
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                val toolType = event.getToolType(pointerIndex)
                if (toolType == TOOL_TYPE_PALM_VALUE) {
                    return true
                }
                if (toolType == MotionEvent.TOOL_TYPE_STYLUS ||
                    toolType == MotionEvent.TOOL_TYPE_ERASER) {
                    return false
                }
            } catch (e: Exception) {
                // Safe fallback
            }
        }

        val density = displayDensity.coerceAtLeast(0.5f)
        val touchMajorDp = event.getTouchMajor(pointerIndex) / density
        val touchMinorDp = event.getTouchMinor(pointerIndex) / density
        val areaDp2 = (Math.PI * (touchMajorDp / 2f) * (touchMinorDp / 2f)).toFloat()

        return touchMajorDp > PALM_TOUCH_MAJOR_DP || areaDp2 > PALM_AREA_DP2
    }

    fun shouldRejectMultiTouch(
        event: MotionEvent,
        displayDensity: Float = 1.0f
    ): Boolean {
        for (i in 0 until event.pointerCount) {
            if (shouldRejectEvent(event, i, displayDensity)) {
                return true
            }
        }
        return false
    }

    fun onStylusLifted() {}
    fun reset() {}
}
