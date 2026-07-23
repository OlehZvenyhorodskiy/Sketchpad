package com.example.academic

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.data.models.ShapeEntity

data class PhysicsBody(
    val id: String,
    var position: Offset,
    var velocity: Offset = Offset(0f, 0f),
    val width: Float,
    val height: Float,
    val mass: Float = 1.0f,
    val restitution: Float = 0.75f // Bounciness coefficient
)

object PhysicsEngine2D {

    private const val GRAVITY = 980f // px / s^2

    fun stepSimulation(
        bodies: List<PhysicsBody>,
        bounds: Rect,
        dtSeconds: Float = 0.016f
    ): List<PhysicsBody> {
        return bodies.map { body ->
            // Apply gravity
            val newVy = body.velocity.y + GRAVITY * dtSeconds
            var newVx = body.velocity.x * 0.99f // air resistance friction

            var newX = body.position.x + newVx * dtSeconds
            var newY = body.position.y + newVy * dtSeconds

            // Boundary collision check (Floor)
            val maxY = bounds.bottom - body.height
            var finalVy = newVy
            if (newY >= maxY) {
                newY = maxY
                finalVy = -newVy * body.restitution
                if (kotlin.math.abs(finalVy) < 20f) finalVy = 0f
            }

            // Left & Right walls
            if (newX <= bounds.left) {
                newX = bounds.left
                newVx = -newVx * body.restitution
            } else if (newX >= bounds.right - body.width) {
                newX = bounds.right - body.width
                newVx = -newVx * body.restitution
            }

            body.copy(
                position = Offset(newX, newY),
                velocity = Offset(newVx, finalVy)
            )
        }
    }
}
