package com.example.core.gesture

import android.view.MotionEvent

/**
 * Розумний фільтр відхилення долоні (Smart Palm Rejection Filter).
 * Підтримує як офіційні активні стилуси, так і неофіційні/пасивні стилуси (Xiaomi, Baseus тощо).
 * Неофіційні стилуси передаються системою як TOOL_TYPE_FINGER, але мають малу площу дотику.
 * Долоня має великий радіус/площу дотику (touchMajor > 55px або area > 900px²).
 */
object PalmRejectionFilter {

    private const val PALM_AREA_THRESHOLD = 900f   // px²
    private const val PALM_MAJOR_THRESHOLD = 55f    // px
    private const val TOOL_TYPE_PALM = 4            // MotionEvent.TOOL_TYPE_PALM (API 34+)

    fun shouldRejectEvent(event: MotionEvent): Boolean {
        val toolType = event.getToolType(0)

        // 1. Активний стилус або стирачка — завжди дозволяти
        if (toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER) {
            return false
        }

        // 2. Якщо система вказує на долоню (TOOL_TYPE_PALM = 4)
        if (toolType == TOOL_TYPE_PALM) {
            return true
        }

        // 3. Для неофіційних стилусів (які відображаються як TOOL_TYPE_FINGER):
        // Оцінюємо геометричні параметри плями дотику
        val touchMajor = event.getTouchMajor(0)
        val touchMinor = event.getTouchMinor(0)

        if (touchMajor > PALM_MAJOR_THRESHOLD) {
            return true // Велика пляма = долоня
        }

        val area = Math.PI * (touchMajor / 2f) * (touchMinor / 2f)
        if (area > PALM_AREA_THRESHOLD) {
            return true // Велика площа = долоня
        }

        // Мала площа (неофіційний стилус або пальчик) = ДОЗВОЛИТИ
        return false
    }

    fun onStylusLifted() {}
    fun reset() {}
}
