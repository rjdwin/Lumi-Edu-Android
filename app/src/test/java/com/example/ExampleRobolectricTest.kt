package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LumiDatabase
import com.example.data.model.ActivityEntity
import com.example.data.model.ActivityType
import com.example.data.util.QuizCsvParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Lumi Mobile", appName)
    }

    @Test
    fun `database insert and query activity`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = LumiDatabase.getDatabase(context)
        val activity = ActivityEntity(
            id = "test_1",
            title = "Test Quiz",
            description = "A test activity",
            type = ActivityType.MULTI_CHOICE,
            mainLibrary = "H5P.MultiChoice",
            libraryVersion = "1.14",
            contentJson = "{}",
            h5pJson = "{}",
            isPreloaded = false,
            bestScore = 0,
            maxScore = 1
        )
        db.activityDao().insertActivity(activity)
        val loaded = db.activityDao().getActivityById("test_1")
        assertNotNull(loaded)
        assertEquals("Test Quiz", loaded?.title)
    }

    @Test
    fun `question set activity type resolution and persistence`() = runBlocking {
        val type = ActivityType.fromLibrary("H5P.QuestionSet 2.0")
        assertEquals(ActivityType.QUESTION_SET, type)
        assertEquals("Quiz (Question Set)", type.label)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = LumiDatabase.getDatabase(context)
        val qsActivity = ActivityEntity(
            id = "test_qs",
            title = "Sample Multi-Question Quiz",
            description = "Quiz with multiple MCQs",
            type = ActivityType.QUESTION_SET,
            mainLibrary = "H5P.QuestionSet",
            libraryVersion = "2.0",
            contentJson = "{\"questions\":[]}",
            h5pJson = "{\"mainLibrary\":\"H5P.QuestionSet\"}",
            bestScore = 3,
            maxScore = 4
        )
        db.activityDao().insertActivity(qsActivity)
        val loaded = db.activityDao().getActivityById("test_qs")
        assertNotNull(loaded)
        assertEquals(ActivityType.QUESTION_SET, loaded?.type)
        assertEquals(4, loaded?.maxScore)
    }

    @Test
    fun `quiz csv parser parses rows with question, correct answer and 3 distractors`() {
        val sampleCsv = """
            Question,Correct Answer,Distractor 1,Distractor 2,Distractor 3,Feedback
            "What is 2 + 2?","4","3","5","6","Basic arithmetic"
            "What is the capital of Spain?","Madrid","Barcelona","Seville","Valencia","Madrid is the central capital"
        """.trimIndent()

        val result = QuizCsvParser.parseCsvString(sampleCsv)
        assertEquals(2, result.totalParsed)
        assertEquals(0, result.skippedRows)
        assertEquals(null, result.error)

        val q1 = result.questions[0]
        assertEquals("What is 2 + 2?", q1.question)
        assertEquals(4, q1.options.size)
        assertTrue(q1.options[0].isCorrect)
        assertEquals("4", q1.options[0].text)
        assertEquals("Basic arithmetic", q1.options[0].feedback)
        assertEquals("3", q1.options[1].text)
        assertEquals("5", q1.options[2].text)
        assertEquals("6", q1.options[3].text)
    }

    @Test
    fun `quiz csv parser handles commas inside quotes and template generation`() {
        val template = QuizCsvParser.getTemplateCsvContent()
        assertTrue(template.contains("Question,Correct Answer,Distractor 1,Distractor 2,Distractor 3"))
        val parseResult = QuizCsvParser.parseCsvString(template)
        assertTrue(parseResult.totalParsed >= 5)
    }
}
