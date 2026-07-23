package com.example.academic

import androidx.compose.ui.geometry.Offset
import com.example.data.models.PageEntity
import com.example.data.models.ShapeEntity
import com.example.data.models.ShapeType
import com.example.data.models.StrokeEntity
import kotlin.math.hypot

data class GraphNode(
    val id: String,
    val label: String,
    val center: Offset
)

data class GraphEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val weight: Float = 1.0f
)

data class GraphAnalysisResult(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val adjacencyMatrix: List<List<Int>>,
    val jsonRepresentation: String,
    val connectedComponentsCount: Int,
    val hasCycles: Boolean
)

object GraphSchemaAnalyzer {

    fun analyzePageGraph(pageEntity: PageEntity): GraphAnalysisResult {
        val nodes = mutableListOf<GraphNode>()
        val edges = mutableListOf<GraphEdge>()

        // 1. Identify nodes from CIRCLE shapes or text blocks
        val shapes = pageEntity.getEffectiveLayers().flatMap { it.shapes }
        var nodeIndex = 0
        shapes.forEach { shape ->
            val center = Offset(shape.x + shape.width / 2f, shape.y + shape.height / 2f)
            val label = "V${nodeIndex + 1}"
            nodes.add(GraphNode(shape.id, label, center))
            nodeIndex++
        }

        // Fallback: If no explicit circle shapes, create synthetic nodes from strokes
        if (nodes.isEmpty()) {
            val strokes = pageEntity.getEffectiveLayers().flatMap { it.strokes }
            val strokeClusters = strokes.take(6)
            strokeClusters.forEachIndexed { idx, st ->
                if (st.points.isNotEmpty()) {
                    val avgX = st.points.map { it.x }.average().toFloat()
                    val avgY = st.points.map { it.y }.average().toFloat()
                    nodes.add(GraphNode("synthetic_$idx", "V${idx + 1}", Offset(avgX, avgY)))
                }
            }
        }

        // 2. Connect nodes via proximity / edge lines
        val n = nodes.size
        val adj = Array(n) { IntArray(n) }

        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val dist = hypot(nodes[i].center.x - nodes[j].center.x, nodes[i].center.y - nodes[j].center.y)
                if (dist < 400f) {
                    adj[i][j] = 1
                    adj[j][i] = 1
                    edges.add(GraphEdge(nodes[i].id, nodes[j].id))
                }
            }
        }

        // 3. Perform BFS / DFS analysis
        val visited = BooleanArray(n)
        var components = 0
        var hasCycle = false

        for (i in 0 until n) {
            if (!visited[i]) {
                components++
                if (dfsCheckCycle(i, -1, visited, adj, n)) {
                    hasCycle = true
                }
            }
        }

        val matrixList = adj.map { row -> row.toList() }
        val jsonStr = buildString {
            append("{\n")
            append("  \"node_count\": ${nodes.size},\n")
            append("  \"edge_count\": ${edges.size},\n")
            append("  \"nodes\": [${nodes.joinToString { "\"${it.label}\"" }}],\n")
            append("  \"components\": $components,\n")
            append("  \"has_cycles\": $hasCycle\n")
            append("}")
        }

        return GraphAnalysisResult(
            nodes = nodes,
            edges = edges,
            adjacencyMatrix = matrixList,
            jsonRepresentation = jsonStr,
            connectedComponentsCount = components,
            hasCycles = hasCycle
        )
    }

    private fun dfsCheckCycle(
        curr: Int,
        parent: Int,
        visited: BooleanArray,
        adj: Array<IntArray>,
        n: Int
    ): Boolean {
        visited[curr] = true
        for (next in 0 until n) {
            if (adj[curr][next] == 1) {
                if (!visited[next]) {
                    if (dfsCheckCycle(next, curr, visited, adj, n)) return true
                } else if (next != parent) {
                    return true
                }
            }
        }
        return false
    }
}
