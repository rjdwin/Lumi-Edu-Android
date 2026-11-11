package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ActivityType(val label: String, val mainLibrary: String, val defaultVersion: String) {
    QUESTION_SET("Quiz (Question Set)", "H5P.QuestionSet", "2.0"),
    MULTI_CHOICE("Multiple Choice", "H5P.MultiChoice", "1.14"),
    FLASHCARDS("Flashcards", "H5P.Dialogcards", "1.8"),
    BLANKS("Fill in Blanks", "H5P.Blanks", "1.12"),
    SINGLE_CHOICE_SET("Single Choice Set", "H5P.SingleChoiceSet", "1.11"),
    TRUE_FALSE("True / False Question", "H5P.TrueFalse", "1.8"),
    DRAG_TEXT("Drag the Words", "H5P.DragText", "1.8"),
    MARK_WORDS("Mark the Words", "H5P.MarkTheWords", "1.11"),
    IMPORTED("Custom / Harvested Package", "", "");

    companion object {
        fun fromLibrary(mainLibrary: String): ActivityType {
            return when {
                mainLibrary.contains("QuestionSet", ignoreCase = true) -> QUESTION_SET
                mainLibrary.contains("MultiChoice", ignoreCase = true) -> MULTI_CHOICE
                mainLibrary.contains("Dialogcards", ignoreCase = true) -> FLASHCARDS
                mainLibrary.contains("Blanks", ignoreCase = true) -> BLANKS
                mainLibrary.contains("SingleChoiceSet", ignoreCase = true) -> SINGLE_CHOICE_SET
                mainLibrary.contains("TrueFalse", ignoreCase = true) -> TRUE_FALSE
                mainLibrary.contains("DragText", ignoreCase = true) -> DRAG_TEXT
                mainLibrary.contains("MarkTheWords", ignoreCase = true) -> MARK_WORDS
                else -> IMPORTED
            }
        }
    }
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
