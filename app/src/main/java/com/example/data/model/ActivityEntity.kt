package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ActivityType(val label: String, val mainLibrary: String, val defaultVersion: String) {
    MULTI_CHOICE("Multiple Choice", "H5P.MultiChoice", "1.14"),
    FLASHCARDS("Flashcards", "H5P.Dialogcards", "1.8"),
    BLANKS("Fill in Blanks", "H5P.Blanks", "1.12"),
    IMPORTED("Imported Package", "", "")
}

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val type: ActivityType,
    val mainLibrary: String,
    val libraryVersion: String,
    val contentJson: String,
    val h5pJson: String,
    val isPreloaded: Boolean = false,
    val bestScore: Int = 0,
    val maxScore: Int = 0,
    val attemptsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
