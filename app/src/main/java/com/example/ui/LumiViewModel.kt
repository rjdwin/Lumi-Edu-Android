package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.LumiDatabase
import com.example.data.model.ActivityEntity
import com.example.data.model.ActivityType
import com.example.data.model.H5PLibraryEntity
import com.example.data.repository.ActivityRepository
import com.example.data.repository.DependencyStatus
import com.example.data.repository.H5PLibraryManager
import com.example.data.repository.HubContentType
import com.example.data.util.QuizCsvParser
import com.example.engine.H5PPackager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

sealed class Screen {
    data object Library : Screen()
    data class Player(val activity: ActivityEntity) : Screen()
    data object Creator : Screen()
    data object LibraryManager : Screen()
}

data class OptionDraft(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val isCorrect: Boolean = false,
    val feedback: String = ""
)

data class CardDraft(
    val id: String = UUID.randomUUID().toString(),
    val front: String = "",
    val back: String = "",
    val hint: String = ""
)

data class SingleChoiceDraft(
    val id: String = UUID.randomUUID().toString(),
    val question: String = "",
    val correctAnswer: String = "",
    val wrongAlternative1: String = "",
    val wrongAlternative2: String = ""
)

data class QuizQuestionDraft(
    val id: String = UUID.randomUUID().toString(),
    val question: String = "",
    val options: List<OptionDraft> = listOf(
        OptionDraft(text = "Option 1", isCorrect = true),
        OptionDraft(text = "Option 2", isCorrect = false),
        OptionDraft(text = "Option 3", isCorrect = false)
    )
)

class LumiViewModel(application: Application) : AndroidViewModel(application) {
    private val database = LumiDatabase.getDatabase(application)
    private val activityRepo = ActivityRepository(database.activityDao())
    val libraryManager = H5PLibraryManager(application, database.libraryDao())
    private val packager = H5PPackager(application, database.libraryDao())

    // Navigation
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Library)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Search and filter
    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow<ActivityType?>(null)

    val activities: StateFlow<List<ActivityEntity>> = combine(
        activityRepo.allActivities,
        searchQuery,
        selectedFilter
    ) { list, query, filter ->
        list.filter { item ->
            val matchesQuery = query.isBlank() || 
                item.title.contains(query, ignoreCase = true) || 
                item.description.contains(query, ignoreCase = true)
            val matchesFilter = filter == null || item.type == filter
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installedLibraries: StateFlow<List<H5PLibraryEntity>> = libraryManager.allLibraries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hubCatalog = MutableStateFlow<List<HubContentType>>(emptyList())

    fun loadHubCatalog() {
        viewModelScope.launch {
            hubCatalog.value = libraryManager.getHubCatalog()
        }
    }

    // Player state
    private val _activePlayerActivity = MutableStateFlow<ActivityEntity?>(null)
    val activePlayerActivity = _activePlayerActivity.asStateFlow()

    val currentScore = MutableStateFlow(0)
    val maxScore = MutableStateFlow(0)
    val showScoreDialog = MutableStateFlow(false)

    // Creator State
    val creatorType = MutableStateFlow(ActivityType.QUESTION_SET)
    val creatorTitle = MutableStateFlow("")
    val creatorDescription = MutableStateFlow("")
    
    // Quiz (Question Set) drafts - multi-question quiz
    val qsPassPercentage = MutableStateFlow(50)
    val qsQuestions = MutableStateFlow(
        listOf(
            QuizQuestionDraft(
                question = "What is the capital of France?",
                options = listOf(
                    OptionDraft(text = "Paris", isCorrect = true, feedback = "Correct! Paris is the capital."),
                    OptionDraft(text = "London", isCorrect = false),
                    OptionDraft(text = "Berlin", isCorrect = false)
                )
            ),
            QuizQuestionDraft(
                question = "Which planet is known as the Red Planet?",
                options = listOf(
                    OptionDraft(text = "Mars", isCorrect = true, feedback = "Correct! Mars has iron oxide on its surface."),
                    OptionDraft(text = "Venus", isCorrect = false),
                    OptionDraft(text = "Jupiter", isCorrect = false)
                )
            )
        )
    )

    // MultiChoice drafts (Single-question component)
    val mcPrompt = MutableStateFlow("Which of the following is true?")
    val mcOptions = MutableStateFlow(
        listOf(
            OptionDraft(text = "Option A", isCorrect = true, feedback = "Well done!"),
            OptionDraft(text = "Option B", isCorrect = false, feedback = "Try again."),
            OptionDraft(text = "Option C", isCorrect = false, feedback = "Not quite.")
        )
    )

    // Flashcards drafts
    val flashcards = MutableStateFlow(
        listOf(
            CardDraft(front = "Term 1", back = "Definition 1", hint = "Helpful hint"),
            CardDraft(front = "Term 2", back = "Definition 2", hint = "Remember concept")
        )
    )

    // Blanks drafts
    val blanksPrompt = MutableStateFlow("Fill in the missing words:")
    val blanksStatements = MutableStateFlow(
        listOf(
            "The capital of France is *Paris*.",
            "Water boils at *100* degrees Celsius."
        )
    )

    // True/False drafts
    val tfPrompt = MutableStateFlow("The Earth revolves around the Sun.")
    val tfCorrect = MutableStateFlow(true)
    val tfFeedbackTrue = MutableStateFlow("Correct! The Earth orbits the Sun once every 365.25 days.")
    val tfFeedbackFalse = MutableStateFlow("Incorrect. The Earth orbits the Sun.")

    // Single Choice Set drafts
    val scsQuestions = MutableStateFlow(
        listOf(
            SingleChoiceDraft(question = "What is 5 + 7?", correctAnswer = "12", wrongAlternative1 = "10", wrongAlternative2 = "14"),
            SingleChoiceDraft(question = "Which planet is known as the Red Planet?", correctAnswer = "Mars", wrongAlternative1 = "Jupiter", wrongAlternative2 = "Venus")
        )
    )

    // Drag the Words drafts
    val dragTextPrompt = MutableStateFlow("Drag the words into the correct blanks:")
    val dragTextPassage = MutableStateFlow("An apple is *red*, a banana is *yellow*, and grass is *green*.")

    // Mark the Words drafts
    val mtwPrompt = MutableStateFlow("Click on all the verbs in the sentence:")
    val mtwPassage = MutableStateFlow("The students *read* the book, *wrote* a summary, and *discussed* the ideas.")

    // Custom Harvested Library drafts
    val customLibraryName = MutableStateFlow("H5P.CustomActivity")
    val customLibraryVersion = MutableStateFlow("1.0")
    val customJsonContent = MutableStateFlow("{\n  \"description\": \"Custom parameters for this activity\"\n}")

    val creatorDependencyStatus = MutableStateFlow<DependencyStatus?>(null)
    val isPreviewMode = MutableStateFlow(false)

    // Feedback message
    val userToastMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            libraryManager.initializePrepackagedLibraries()
            activityRepo.seedSampleActivitiesIfEmpty()
            checkCreatorDependencies()
            loadHubCatalog()
        }
    }

    fun navigateTo(screen: Screen) {
        if (screen is Screen.Player) {
            _activePlayerActivity.value = screen.activity
            currentScore.value = 0
            maxScore.value = screen.activity.maxScore
            showScoreDialog.value = false
        }
        _currentScreen.value = screen
    }

    fun onScoreReceived(score: Int, max: Int) {
        currentScore.value = score
        maxScore.value = max
        val activity = _activePlayerActivity.value ?: return
        viewModelScope.launch {
            activityRepo.recordAttempt(activity.id, score, max)
        }
    }

    fun completeActivity() {
        showScoreDialog.value = true
    }

    fun checkCreatorDependencies() {
        viewModelScope.launch {
            val type = creatorType.value
            val libName = type.mainLibrary
            val verParts = type.defaultVersion.split(".")
            val maj = verParts.getOrNull(0)?.toIntOrNull() ?: 1
            val min = verParts.getOrNull(1)?.toIntOrNull() ?: 0
            val status = libraryManager.checkDependenciesForLibrary(libName, maj, min)
            creatorDependencyStatus.value = status
        }
    }

    fun addOption() {
        val current = mcOptions.value.toMutableList()
        current.add(OptionDraft(text = "New Option", isCorrect = false))
        mcOptions.value = current
    }

    fun removeOption(id: String) {
        if (mcOptions.value.size > 2) {
            mcOptions.value = mcOptions.value.filter { it.id != id }
        }
    }

    fun updateOption(id: String, text: String, isCorrect: Boolean, feedback: String) {
        mcOptions.value = mcOptions.value.map {
            if (it.id == id) it.copy(text = text, isCorrect = isCorrect, feedback = feedback) else it
        }
    }

    fun addCard() {
        val current = flashcards.value.toMutableList()
        current.add(CardDraft(front = "New Question", back = "Answer"))
        flashcards.value = current
    }

    fun removeCard(id: String) {
        if (flashcards.value.size > 1) {
            flashcards.value = flashcards.value.filter { it.id != id }
        }
    }

    fun updateCard(id: String, front: String, back: String, hint: String) {
        flashcards.value = flashcards.value.map {
            if (it.id == id) it.copy(front = front, back = back, hint = hint) else it
        }
    }

    fun addBlankStatement() {
        val current = blanksStatements.value.toMutableList()
        current.add("Complete statement with *answer*.")
        blanksStatements.value = current
    }

    fun removeBlankStatement(index: Int) {
        if (blanksStatements.value.size > 1) {
            val current = blanksStatements.value.toMutableList()
            current.removeAt(index)
            blanksStatements.value = current
        }
    }

    fun updateBlankStatement(index: Int, text: String) {
        val current = blanksStatements.value.toMutableList()
        if (index in current.indices) {
            current[index] = text
            blanksStatements.value = current
        }
    }
    fun addScsQuestion() {
        val current = scsQuestions.value.toMutableList()
        current.add(SingleChoiceDraft(question = "New Question", correctAnswer = "Correct Answer", wrongAlternative1 = "Alternative 1", wrongAlternative2 = "Alternative 2"))
        scsQuestions.value = current
    }

    fun removeScsQuestion(id: String) {
        if (scsQuestions.value.size > 1) {
            scsQuestions.value = scsQuestions.value.filter { it.id != id }
        }
    }

    fun updateScsQuestion(id: String, q: String, correct: String, w1: String, w2: String) {
        scsQuestions.value = scsQuestions.value.map {
            if (it.id == id) it.copy(question = q, correctAnswer = correct, wrongAlternative1 = w1, wrongAlternative2 = w2) else it
        }
    }

    fun addQuizQuestion() {
        val current = qsQuestions.value.toMutableList()
        current.add(
            QuizQuestionDraft(
                question = "New Question ${current.size + 1}",
                options = listOf(
                    OptionDraft(text = "Option 1", isCorrect = true),
                    OptionDraft(text = "Option 2", isCorrect = false),
                    OptionDraft(text = "Option 3", isCorrect = false)
                )
            )
        )
        qsQuestions.value = current
    }

    fun removeQuizQuestion(id: String) {
        if (qsQuestions.value.size > 1) {
            qsQuestions.value = qsQuestions.value.filter { it.id != id }
        }
    }

    fun updateQuizQuestionText(id: String, text: String) {
        qsQuestions.value = qsQuestions.value.map {
            if (it.id == id) it.copy(question = text) else it
        }
    }

    fun addOptionToQuizQuestion(questionId: String) {
        qsQuestions.value = qsQuestions.value.map { q ->
            if (q.id == questionId) {
                val opts = q.options.toMutableList()
                opts.add(OptionDraft(text = "Option ${opts.size + 1}", isCorrect = false))
                q.copy(options = opts)
            } else q
        }
    }

    fun removeOptionFromQuizQuestion(questionId: String, optionId: String) {
        qsQuestions.value = qsQuestions.value.map { q ->
            if (q.id == questionId && q.options.size > 2) {
                q.copy(options = q.options.filter { it.id != optionId })
            } else q
        }
    }

    fun updateQuizOption(questionId: String, optionId: String, text: String, isCorrect: Boolean, feedback: String) {
        qsQuestions.value = qsQuestions.value.map { q ->
            if (q.id == questionId) {
                val opts = q.options.map { opt ->
                    if (opt.id == optionId) {
                        opt.copy(text = text, isCorrect = isCorrect, feedback = feedback)
                    } else {
                        if (isCorrect) opt.copy(isCorrect = false) else opt
                    }
                }
                q.copy(options = opts)
            } else q
        }
    }

    fun selectActivityType(type: ActivityType) {
        creatorType.value = type
        checkCreatorDependencies()
    }

    fun buildDraftActivity(): ActivityEntity {
        val type = creatorType.value
        val title = creatorTitle.value.ifBlank { "Untitled ${type.label}" }
        val desc = creatorDescription.value.ifBlank { "Created with Lumi Mobile" }
        val id = UUID.randomUUID().toString()

        val contentObj = JSONObject()
        val manifestObj = JSONObject().apply {
            put("title", title)
            put("mainLibrary", type.mainLibrary.ifBlank { customLibraryName.value })
            val versionStr = if (type == ActivityType.IMPORTED) customLibraryVersion.value else type.defaultVersion
            val parts = versionStr.split(".")
            put("majorVersion", parts.getOrNull(0)?.toIntOrNull() ?: 1)
            put("minorVersion", parts.getOrNull(1)?.toIntOrNull() ?: 0)
        }

        var maxSc = 1

        when (type) {
            ActivityType.QUESTION_SET -> {
                contentObj.put("introPage", JSONObject().apply {
                    put("showIntroPage", false)
                    put("title", title)
                    put("introduction", desc)
                })
                contentObj.put("progressType", "dots")
                contentObj.put("passPercentage", qsPassPercentage.value)

                val questionsArray = JSONArray()
                qsQuestions.value.forEachIndexed { idx, qDraft ->
                    val qItem = JSONObject()
                    qItem.put("library", "H5P.MultiChoice 1.14")
                    qItem.put("subContentId", "q_${idx + 1}_${qDraft.id.take(8)}")
                    val qParams = JSONObject().apply {
                        put("question", "<p>${qDraft.question}</p>")
                        val answersArray = JSONArray()
                        qDraft.options.forEach { opt ->
                            answersArray.put(JSONObject().apply {
                                put("correct", opt.isCorrect)
                                put("text", "<div>${opt.text}</div>")
                                if (opt.feedback.isNotBlank()) {
                                    put("tipsAndFeedback", JSONObject().put("chosenFeedback", opt.feedback))
                                }
                            })
                        }
                        put("answers", answersArray)
                        put("behaviour", JSONObject().apply {
                            put("enableRetry", true)
                            put("enableSolutionsButton", true)
                            put("singlePoint", true)
                            put("randomAnswers", true)
                        })
                    }
                    qItem.put("params", qParams)
                    questionsArray.put(qItem)
                }
                contentObj.put("questions", questionsArray)

                manifestObj.put("preloadedDependencies", JSONArray().apply {
                    put(JSONObject().put("machineName", "H5P.QuestionSet").put("majorVersion", 2).put("minorVersion", 0))
                    put(JSONObject().put("machineName", "H5P.Question").put("majorVersion", 1).put("minorVersion", 4))
                    put(JSONObject().put("machineName", "H5P.MultiChoice").put("majorVersion", 1).put("minorVersion", 14))
                    put(JSONObject().put("machineName", "H5P.JoubelUI").put("majorVersion", 1).put("minorVersion", 3))
                    put(JSONObject().put("machineName", "FontAwesome").put("majorVersion", 4).put("minorVersion", 5))
                })
                maxSc = qsQuestions.value.size.coerceAtLeast(1)
            }
            ActivityType.MULTI_CHOICE -> {
                contentObj.put("question", "<p>${mcPrompt.value}</p>")
                val answers = JSONArray()
                mcOptions.value.forEach { opt ->
                    answers.put(JSONObject().apply {
                        put("correct", opt.isCorrect)
                        put("text", "<div>${opt.text}</div>")
                        if (opt.feedback.isNotBlank()) {
                            put("tipsAndFeedback", JSONObject().put("chosenFeedback", opt.feedback))
                        }
                    })
                }
                contentObj.put("answers", answers)
                contentObj.put("behaviour", JSONObject().apply {
                    put("enableRetry", true)
                    put("enableSolutionsButton", true)
                    put("singlePoint", true)
                    put("randomAnswers", true)
                })
                manifestObj.put("preloadedDependencies", JSONArray().apply {
                    put(JSONObject().put("machineName", "H5P.Question").put("majorVersion", 1).put("minorVersion", 4))
                    put(JSONObject().put("machineName", "H5P.JoubelUI").put("majorVersion", 1).put("minorVersion", 3))
                    put(JSONObject().put("machineName", "FontAwesome").put("majorVersion", 4).put("minorVersion", 5))
                })
                maxSc = 1
            }
            ActivityType.FLASHCARDS -> {
                contentObj.put("title", title)
                contentObj.put("description", desc)
                val dialogs = JSONArray()
                flashcards.value.forEach { card ->
                    dialogs.put(JSONObject().apply {
                        put("text", card.front)
                        put("answer", card.back)
                        if (card.hint.isNotBlank()) put("tips", card.hint)
                    })
                }
                contentObj.put("dialogs", dialogs)
                manifestObj.put("preloadedDependencies", JSONArray().apply {
                    put(JSONObject().put("machineName", "H5P.JoubelUI").put("majorVersion", 1).put("minorVersion", 3))
                    put(JSONObject().put("machineName", "FontAwesome").put("majorVersion", 4).put("minorVersion", 5))
                })
                maxSc = flashcards.value.size
            }
            ActivityType.BLANKS -> {
                contentObj.put("text", "<p>${blanksPrompt.value}</p>")
                val questions = JSONArray()
                blanksStatements.value.forEach { s ->
                    questions.put("<p>$s</p>")
                }
                contentObj.put("questions", questions)
                contentObj.put("behaviour", JSONObject().apply {
                    put("enableRetry", true)
                    put("enableSolutionsButton", true)
                    put("caseSensitive", false)
                })
                manifestObj.put("preloadedDependencies", JSONArray().apply {
                    put(JSONObject().put("machineName", "H5P.Question").put("majorVersion", 1).put("minorVersion", 4))
                    put(JSONObject().put("machineName", "H5P.JoubelUI").put("majorVersion", 1).put("minorVersion", 3))
                })
                maxSc = blanksStatements.value.size
            }
            ActivityType.SINGLE_CHOICE_SET -> {
                val choices = JSONArray()
                scsQuestions.value.forEach { item ->
                    val qObj = JSONObject().apply {
                        put("question", item.question)
                        val answers = JSONArray().apply {
                            put(item.correctAnswer)
                            if (item.wrongAlternative1.isNotBlank()) put(item.wrongAlternative1)
                            if (item.wrongAlternative2.isNotBlank()) put(item.wrongAlternative2)
                        }
                        put("answers", answers)
                    }
                    choices.put(qObj)
                }
                contentObj.put("choices", choices)
                manifestObj.put("preloadedDependencies", JSONArray().apply {
                    put(JSONObject().put("machineName", "H5P.Question").put("majorVersion", 1).put("minorVersion", 4))
                    put(JSONObject().put("machineName", "H5P.JoubelUI").put("majorVersion", 1).put("minorVersion", 3))
                    put(JSONObject().put("machineName", "FontAwesome").put("majorVersion", 4).put("minorVersion", 5))
                })
                maxSc = scsQuestions.value.size
            }
            ActivityType.TRUE_FALSE -> {
                contentObj.put("question", "<p>${tfPrompt.value}</p>")
                contentObj.put("correct", if (tfCorrect.value) "true" else "false")
                contentObj.put("feedbackOnCorrect", tfFeedbackTrue.value)
                contentObj.put("feedbackOnWrong", tfFeedbackFalse.value)
                manifestObj.put("preloadedDependencies", JSONArray().apply {
                    put(JSONObject().put("machineName", "H5P.Question").put("majorVersion", 1).put("minorVersion", 4))
                    put(JSONObject().put("machineName", "H5P.JoubelUI").put("majorVersion", 1).put("minorVersion", 3))
                    put(JSONObject().put("machineName", "FontAwesome").put("majorVersion", 4).put("minorVersion", 5))
                })
                maxSc = 1
            }
            ActivityType.DRAG_TEXT -> {
                contentObj.put("taskDescription", "<p>${dragTextPrompt.value}</p>")
                contentObj.put("textField", dragTextPassage.value)
                manifestObj.put("preloadedDependencies", JSONArray().apply {
                    put(JSONObject().put("machineName", "H5P.Question").put("majorVersion", 1).put("minorVersion", 4))
                    put(JSONObject().put("machineName", "H5P.JoubelUI").put("majorVersion", 1).put("minorVersion", 3))
                })
                val blanksCount = "\\*([^*]+)\\*".toRegex().findAll(dragTextPassage.value).count()
                maxSc = if (blanksCount > 0) blanksCount else 1
            }
            ActivityType.MARK_WORDS -> {
                contentObj.put("taskDescription", "<p>${mtwPrompt.value}</p>")
                contentObj.put("textField", mtwPassage.value)
                manifestObj.put("preloadedDependencies", JSONArray().apply {
                    put(JSONObject().put("machineName", "H5P.Question").put("majorVersion", 1).put("minorVersion", 4))
                    put(JSONObject().put("machineName", "H5P.JoubelUI").put("majorVersion", 1).put("minorVersion", 3))
                    put(JSONObject().put("machineName", "FontAwesome").put("majorVersion", 4).put("minorVersion", 5))
                })
                val targetCount = "\\*([^*]+)\\*".toRegex().findAll(mtwPassage.value).count()
                maxSc = if (targetCount > 0) targetCount else 1
            }
            ActivityType.IMPORTED -> {
                manifestObj.put("mainLibrary", customLibraryName.value)
                val parts = customLibraryVersion.value.split(".")
                manifestObj.put("majorVersion", parts.getOrNull(0)?.toIntOrNull() ?: 1)
                manifestObj.put("minorVersion", parts.getOrNull(1)?.toIntOrNull() ?: 0)
                try {
                    val parsed = JSONObject(customJsonContent.value)
                    val keys = parsed.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        contentObj.put(k, parsed.get(k))
                    }
                } catch (e: Exception) {
                    contentObj.put("description", customJsonContent.value)
                }
                maxSc = 1
            }
        }

        return ActivityEntity(
            id = id,
            title = title,
            description = desc,
            type = type,
            mainLibrary = if (type == ActivityType.IMPORTED) customLibraryName.value else type.mainLibrary,
            libraryVersion = if (type == ActivityType.IMPORTED) customLibraryVersion.value else type.defaultVersion,
            contentJson = contentObj.toString(),
            h5pJson = manifestObj.toString(),
            isPreloaded = false,
            bestScore = 0,
            maxScore = maxSc
        )
    }

    fun saveCreatedActivity() {
        viewModelScope.launch {
            val activity = buildDraftActivity()
            activityRepo.insertActivity(activity)
            userToastMessage.value = "Activity '${activity.title}' saved to library!"
            navigateTo(Screen.Library)
        }
    }

    fun exportActivity(activity: ActivityEntity): File {
        return packager.exportActivityToH5P(activity)
    }

    fun exportActivityToUri(activity: ActivityEntity, uri: Uri): Boolean {
        return try {
            val stream = getApplication<Application>().contentResolver.openOutputStream(uri) ?: return false
            stream.use { packager.exportActivityToStream(activity, it) }
            userToastMessage.value = "Exported '${activity.title}' to .h5p package!"
            true
        } catch (e: Exception) {
            userToastMessage.value = "Export failed: ${e.message}"
            false
        }
    }

    fun importQuestionsFromCsv(uri: Uri, replaceExisting: Boolean = false): Pair<Int, String?> {
        return try {
            val context = getApplication<Application>()
            val stream = context.contentResolver.openInputStream(uri)
                ?: return Pair(0, "Could not open selected CSV file.")
            val result = stream.use { QuizCsvParser.parseCsvStream(it) }
            if (result.error != null) {
                userToastMessage.value = result.error
                Pair(0, result.error)
            } else {
                if (replaceExisting) {
                    qsQuestions.value = result.questions
                } else {
                    qsQuestions.value = qsQuestions.value + result.questions
                }
                userToastMessage.value = "Imported ${result.totalParsed} questions from CSV!"
                Pair(result.totalParsed, null)
            }
        } catch (e: Exception) {
            val err = "Error reading CSV: ${e.message}"
            userToastMessage.value = err
            Pair(0, err)
        }
    }

    fun importQuestionsFromCsvText(csvText: String, replaceExisting: Boolean = false): Pair<Int, String?> {
        val result = QuizCsvParser.parseCsvString(csvText)
        return if (result.error != null) {
            userToastMessage.value = result.error
            Pair(0, result.error)
        } else {
            if (replaceExisting) {
                qsQuestions.value = result.questions
            } else {
                qsQuestions.value = qsQuestions.value + result.questions
            }
            userToastMessage.value = "Imported ${result.totalParsed} questions from pasted text!"
            Pair(result.totalParsed, null)
        }
    }

    fun getTemplateCsvFile(): File {
        return QuizCsvParser.createTemplateFile(getApplication())
    }

    fun importPackageFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val filenameHint = uri.lastPathSegment ?: "imported.h5p"
                val (activity, harvestedCount) = packager.importPackage(inputStream, filenameHint)
                activityRepo.insertActivity(activity)
                userToastMessage.value = "Imported '${activity.title}' ($harvestedCount libraries harvested)"
                navigateTo(Screen.Library)
            } catch (e: Exception) {
                userToastMessage.value = "Failed to import package: ${e.message}"
            }
        }
    }

    fun harvestLibrariesFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val count = packager.harvestLibrariesFromStream(inputStream)
                userToastMessage.value = "Successfully harvested $count libraries from package!"
                checkCreatorDependencies()
            } catch (e: Exception) {
                userToastMessage.value = "Failed to harvest libraries: ${e.message}"
            }
        }
    }

    fun deleteActivity(id: String) {
        viewModelScope.launch {
            activityRepo.deleteActivity(id)
            userToastMessage.value = "Activity removed"
            if (_currentScreen.value is Screen.Player) {
                navigateTo(Screen.Library)
            }
        }
    }

    fun deleteLibrary(id: String) {
        viewModelScope.launch {
            libraryManager.deleteLibrary(id)
            userToastMessage.value = "Library $id removed"
            checkCreatorDependencies()
        }
    }

    fun clearToast() {
        userToastMessage.value = null
    }
}
