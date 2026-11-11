package com.example.data.util

import android.content.Context
import com.example.ui.OptionDraft
import com.example.ui.QuizQuestionDraft
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

data class CsvParseResult(
    val questions: List<QuizQuestionDraft>,
    val totalParsed: Int,
    val skippedRows: Int,
    val error: String? = null
)

object QuizCsvParser {

    /**
     * Parses CSV from an InputStream (file picker / storage URI).
     */
    fun parseCsvStream(inputStream: InputStream): CsvParseResult {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val lines = reader.readLines()
        return parseLines(lines)
    }

    /**
     * Parses raw CSV string pasted by the user.
     */
    fun parseCsvString(csvContent: String): CsvParseResult {
        val lines = csvContent.lines()
        return parseLines(lines)
    }

    private fun parseLines(rawLines: List<String>): CsvParseResult {
        val nonEmptyLines = rawLines.map { it.trim() }.filter { it.isNotBlank() }
        if (nonEmptyLines.isEmpty()) {
            return CsvParseResult(emptyList(), 0, 0, "The provided CSV is empty.")
        }

        // Determine delimiter from first line (, vs ;)
        val firstLine = nonEmptyLines.first()
        val commaCount = firstLine.count { it == ',' }
        val semiCount = firstLine.count { it == ';' }
        val delimiter = if (semiCount > commaCount) ';' else ','

        val questions = mutableListOf<QuizQuestionDraft>()
        var skipped = 0

        var startIndex = 0
        // Check if first line is a header
        if (isHeaderLine(firstLine, delimiter)) {
            startIndex = 1
        }

        for (i in startIndex until nonEmptyLines.size) {
            val line = nonEmptyLines[i]
            val tokens = splitCsvLine(line, delimiter)

            if (tokens.size < 2 || tokens[0].isBlank()) {
                skipped++
                continue
            }

            val questionText = tokens[0].trim().removeSurrounding("\"").trim()
            val correctAnswer = tokens[1].trim().removeSurrounding("\"").trim()

            if (questionText.isBlank() || correctAnswer.isBlank()) {
                skipped++
                continue
            }

            val distractors = mutableListOf<String>()
            for (col in 2..4) {
                if (col < tokens.size) {
                    val d = tokens[col].trim().removeSurrounding("\"").trim()
                    if (d.isNotBlank()) distractors.add(d)
                }
            }

            val feedback = if (tokens.size > 5) tokens[5].trim().removeSurrounding("\"").trim() else ""

            val options = mutableListOf<OptionDraft>()
            // Option 1: Correct Answer
            options.add(
                OptionDraft(
                    id = UUID.randomUUID().toString(),
                    text = correctAnswer,
                    isCorrect = true,
                    feedback = feedback
                )
            )

            // Distractors
            distractors.forEach { distractorText ->
                options.add(
                    OptionDraft(
                        id = UUID.randomUUID().toString(),
                        text = distractorText,
                        isCorrect = false
                    )
                )
            }

            // Ensure at least 2 choices
            if (options.size < 2) {
                options.add(OptionDraft(text = "Alternative Choice", isCorrect = false))
            }

            questions.add(
                QuizQuestionDraft(
                    id = UUID.randomUUID().toString(),
                    question = questionText,
                    options = options
                )
            )
        }

        if (questions.isEmpty()) {
            return CsvParseResult(emptyList(), 0, skipped, "No valid questions could be parsed from the CSV.")
        }

        return CsvParseResult(
            questions = questions,
            totalParsed = questions.size,
            skippedRows = skipped,
            error = null
        )
    }

    /**
     * Splits a CSV line observing quoted values and escaped quotes.
     */
    fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++ // skip second quote of pair
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString())
        return tokens
    }

    private fun isHeaderLine(line: String, delimiter: Char): Boolean {
        val lower = line.lowercase()
        return lower.contains("question") || 
               lower.contains("correct") || 
               lower.contains("distractor") || 
               lower.contains("answer") ||
               lower.contains("wrong")
    }

    /**
     * Standard template CSV content with pre-filled headers and sample rows.
     */
    fun getTemplateCsvContent(): String {
        return buildString {
            appendLine("Question,Correct Answer,Distractor 1,Distractor 2,Distractor 3,Feedback")
            appendLine("\"What is the capital of France?\",\"Paris\",\"London\",\"Berlin\",\"Madrid\",\"Paris has been the capital since 508 AD.\"")
            appendLine("\"Which planet is known as the Red Planet?\",\"Mars\",\"Venus\",\"Jupiter\",\"Mercury\",\"Mars gets its red color from iron oxide minerals on its surface.\"")
            appendLine("\"How many continents are there on Earth?\",\"7\",\"5\",\"6\",\"8\",\"Asia, Africa, North America, South America, Antarctica, Europe, Australia.\"")
            appendLine("\"What is the chemical symbol for Gold?\",\"Au\",\"Ag\",\"Fe\",\"Gd\",\"Au is derived from the Latin word aurum (shining dawn).\"")
            appendLine("\"Who developed the theory of General Relativity?\",\"Albert Einstein\",\"Isaac Newton\",\"Galileo Galilei\",\"Nikola Tesla\",\"Einstein published General Relativity in 1915.\"")
            appendLine("\"Which ocean is the largest on Earth?\",\"Pacific Ocean\",\"Atlantic Ocean\",\"Indian Ocean\",\"Arctic Ocean\",\"The Pacific Ocean covers over 30% of Earth's surface.\"")
        }
    }

    /**
     * Creates and caches a ready-to-share quiz template file.
     */
    fun createTemplateFile(context: Context): File {
        val exportDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
        val templateFile = File(exportDir, "quiz_template.csv")
        templateFile.writeText(getTemplateCsvContent(), Charsets.UTF_8)
        return templateFile
    }
}
