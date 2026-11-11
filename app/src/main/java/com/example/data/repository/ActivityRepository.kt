package com.example.data.repository

import com.example.data.db.ActivityDao
import com.example.data.model.ActivityEntity
import com.example.data.model.ActivityType
import com.example.data.model.AttemptEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ActivityRepository(
    private val activityDao: ActivityDao
) {
    val allActivities: Flow<List<ActivityEntity>> = activityDao.getAllActivities()

    suspend fun getActivityById(id: String): ActivityEntity? = withContext(Dispatchers.IO) {
        activityDao.getActivityById(id)
    }

    suspend fun insertActivity(activity: ActivityEntity) = withContext(Dispatchers.IO) {
        activityDao.insertActivity(activity)
    }

    suspend fun deleteActivity(id: String) = withContext(Dispatchers.IO) {
        activityDao.deleteActivityById(id)
    }

    suspend fun recordAttempt(activityId: String, score: Int, maxScore: Int) = withContext(Dispatchers.IO) {
        activityDao.recordAttempt(activityId, score, maxScore)
        activityDao.insertAttempt(
            AttemptEntity(
                activityId = activityId,
                score = score,
                maxScore = maxScore
            )
        )
    }

    fun getAttemptsForActivity(activityId: String): Flow<List<AttemptEntity>> {
        return activityDao.getAttemptsForActivity(activityId)
    }

    suspend fun seedSampleActivitiesIfEmpty() = withContext(Dispatchers.IO) {
        if (activityDao.getActivitiesCount() == 0) {
            val samples = listOf(
                createScienceQuizSample(),
                createLanguageFlashcardsSample(),
                createComputingBlanksSample()
            )
            activityDao.insertActivities(samples)
        }
    }

    private fun createScienceQuizSample(): ActivityEntity {
        val contentObj = JSONObject().apply {
            put("question", "<p>What celestial body is at the center of our Solar System?</p>")
            val answersArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("correct", true)
                    put("text", "<div>The Sun</div>")
                    put("tipsAndFeedback", JSONObject().put("chosenFeedback", "Correct! The Sun accounts for 99.8% of the Solar System's mass."))
                })
                put(JSONObject().apply {
                    put("correct", false)
                    put("text", "<div>Jupiter</div>")
                    put("tipsAndFeedback", JSONObject().put("chosenFeedback", "Jupiter is the largest planet, but not at the center!"))
                })
                put(JSONObject().apply {
                    put("correct", false)
                    put("text", "<div>The Earth</div>")
                    put("tipsAndFeedback", JSONObject().put("chosenFeedback", "The geocentric model was replaced by the heliocentric model!"))
                })
                put(JSONObject().apply {
                    put("correct", false)
                    put("text", "<div>Proxima Centauri</div>")
                    put("tipsAndFeedback", JSONObject().put("chosenFeedback", "Proxima Centauri is the closest neighboring star, 4.2 light-years away."))
                })
            }
            put("answers", answersArray)
            put("behaviour", JSONObject().apply {
                put("enableRetry", true)
                put("enableSolutionsButton", true)
                put("singlePoint", true)
            })
        }

        val manifestObj = JSONObject().apply {
            put("title", "Solar System Explorer Quiz")
            put("mainLibrary", "H5P.MultiChoice")
            put("majorVersion", 1)
            put("minorVersion", 14)
            put("preloadedDependencies", JSONArray().apply {
                put(JSONObject().put("machineName", "H5P.Question").put("majorVersion", 1).put("minorVersion", 4))
                put(JSONObject().put("machineName", "H5P.JoubelUI").put("majorVersion", 1).put("minorVersion", 3))
                put(JSONObject().put("machineName", "FontAwesome").put("majorVersion", 4).put("minorVersion", 5))
            })
        }

        return ActivityEntity(
            id = "sample_quiz_solar_system",
            title = "Solar System Explorer Quiz",
            description = "Test your astronomy knowledge with an interactive multiple-choice question.",
            type = ActivityType.MULTI_CHOICE,
            mainLibrary = "H5P.MultiChoice",
            libraryVersion = "1.14",
            contentJson = contentObj.toString(),
            h5pJson = manifestObj.toString(),
            isPreloaded = true,
            bestScore = 0,
            maxScore = 1
        )
    }

    private fun createLanguageFlashcardsSample(): ActivityEntity {
        val contentObj = JSONObject().apply {
            put("title", "Everyday French Vocabulary")
            put("description", "Flip through key French expressions and test your recall.")
            val dialogs = JSONArray().apply {
                put(JSONObject().apply {
                    put("text", "Bonjour")
                    put("answer", "Hello / Good morning")
                    put("tips", "Standard daytime greeting")
                })
                put(JSONObject().apply {
                    put("text", "Merci beaucoup")
                    put("answer", "Thank you very much")
                    put("tips", "Polite expression of gratitude")
                })
                put(JSONObject().apply {
                    put("text", "S'il vous plaît")
                    put("answer", "Please")
                    put("tips", "Formal request phrase")
                })
                put(JSONObject().apply {
                    put("text", "Au revoir")
                    put("answer", "Goodbye")
                    put("tips", "Farewell expression")
                })
                put(JSONObject().apply {
                    put("text", "Enchanté(e)")
                    put("answer", "Nice to meet you")
                    put("tips", "Used when introduced to someone")
                })
            }
            put("dialogs", dialogs)
        }

        val manifestObj = JSONObject().apply {
            put("title", "Everyday French Vocabulary")
            put("mainLibrary", "H5P.Dialogcards")
            put("majorVersion", 1)
            put("minorVersion", 8)
            put("preloadedDependencies", JSONArray().apply {
                put(JSONObject().put("machineName", "H5P.JoubelUI").put("majorVersion", 1).put("minorVersion", 3))
                put(JSONObject().put("machineName", "FontAwesome").put("majorVersion", 4).put("minorVersion", 5))
            })
        }

        return ActivityEntity(
            id = "sample_cards_french",
            title = "Everyday French Vocabulary",
            description = "5 interactive dialog cards for practicing French conversational phrases.",
            type = ActivityType.FLASHCARDS,
            mainLibrary = "H5P.Dialogcards",
            libraryVersion = "1.8",
            contentJson = contentObj.toString(),
            h5pJson = manifestObj.toString(),
            isPreloaded = true,
            bestScore = 0,
            maxScore = 5
        )
    }

    private fun createComputingBlanksSample(): ActivityEntity {
        val contentObj = JSONObject().apply {
            put("text", "<p>Complete the computer science statements below:</p>")
            val questions = JSONArray().apply {
                put("<p>A byte consists of *8* bits.</p>")
                put("<p>The brain of a computer is the *CPU/processor*.</p>")
                put("<p>RAM stands for *Random Access Memory*.</p>")
                put("<p>A program that translates source code into machine code is a *compiler*.</p>")
            }
            put("questions", questions)
            put("behaviour", JSONObject().apply {
                put("enableRetry", true)
                put("enableSolutionsButton", true)
                put("caseSensitive", false)
            })
        }

        val manifestObj = JSONObject().apply {
            put("title", "Computer Architecture Blanks")
            put("mainLibrary", "H5P.Blanks")
            put("majorVersion", 1)
            put("minorVersion", 12)
            put("preloadedDependencies", JSONArray().apply {
                put(JSONObject().put("machineName", "H5P.Question").put("majorVersion", 1).put("minorVersion", 4))
                put(JSONObject().put("machineName", "H5P.JoubelUI").put("majorVersion", 1).put("minorVersion", 3))
            })
        }

        return ActivityEntity(
            id = "sample_blanks_computing",
            title = "Computer Architecture Blanks",
            description = "Fill in the blanks to test your understanding of foundational computer concepts.",
            type = ActivityType.BLANKS,
            mainLibrary = "H5P.Blanks",
            libraryVersion = "1.12",
            contentJson = contentObj.toString(),
            h5pJson = manifestObj.toString(),
            isPreloaded = true,
            bestScore = 0,
            maxScore = 4
        )
    }
}
