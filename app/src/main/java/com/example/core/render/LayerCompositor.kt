package com.example.core.render

import android.graphics.Canvas
import android.graphics.RenderNode
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Hardware-accelerated рендеринг через RenderNode.
 * Для >1000 strokes — використовує GPU-прискорення.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class LayerCompositor {

    private val renderNodes = mutableMapOf<String, RenderNode>()
    private val dirtyLayers = mutableSetOf<String>()

    fun invalidateLayer(layerId: String) {
        dirtyLayers.add(layerId)
    }

    fun getRenderNode(
        layerId: String,
        width: Int,
        height: Int,
        drawContent: (Canvas) -> Unit
    ): RenderNode {
        val existing = renderNodes[layerId]
        val isDirty = layerId in dirtyLayers

        if (existing != null && !isDirty) return existing

        val node = existing ?: RenderNode("layer_$layerId").also {
            it.setPosition(0, 0, width, height)
        }

        if (isDirty || existing == null) {
            val recordingCanvas = node.beginRecording(width, height)
            drawContent(recordingCanvas)
            node.endRecording()
            dirtyLayers.remove(layerId)
        }

        renderNodes[layerId] = node
        return node
    }

    fun destroy() {
        renderNodes.values.forEach { it.discardDisplayList() }
        renderNodes.clear()
    }
}
