package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val activityId: String,
    val score: Int,
    val maxScore: Int,
    val percentage: Int = if (maxScore > 0) (score * 100) / maxScore else 0,
    val timestamp: Long = System.currentTimeMillis()
)
