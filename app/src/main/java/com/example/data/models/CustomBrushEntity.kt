package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity storing custom user-defined brush profiles.
 *
 * [profileJson] contains the Moshi-serialized [com.example.brush.BrushProfile].
 */
@Entity(tableName = "custom_brushes")
data class CustomBrushEntity(
    @PrimaryKey val id: String,
    val name: String,
    val profileJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
