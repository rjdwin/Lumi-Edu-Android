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

    // Player state
    private val _activePlayerActivity = MutableStateFlow<ActivityEntity?>(null)
    val activePlayerActivity = _activePlayerActivity.asStateFlow()

    val currentScore = MutableStateFlow(0)
    val maxScore = MutableStateFlow(0)
    val showScoreDialog = MutableStateFlow(false)

    // Creator State
    val creatorType = MutableStateFlow(ActivityType.MULTI_CHOICE)
    val creatorTitle = MutableStateFlow("")
    val creatorDescription = MutableStateFlow("")
    
    // MultiChoice drafts
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

    val creatorDependencyStatus = MutableStateFlow<DependencyStatus?>(null)
    val isPreviewMode = MutableStateFlow(false)

    // Feedback message
    val userToastMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            libraryManager.initializePrepackagedLibraries()
            activityRepo.seedSampleActivitiesIfEmpty()
            checkCreatorDependencies()
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

    fun buildDraftActivity(): ActivityEntity {
        val type = creatorType.value
        val title = creatorTitle.value.ifBlank { "Untitled ${type.label}" }
        val desc = creatorDescription.value.ifBlank { "Created with Lumi Mobile" }
        val id = UUID.randomUUID().toString()

        val contentObj = JSONObject()
        val manifestObj = JSONObject().apply {
            put("title", title)
            put("mainLibrary", type.mainLibrary)
            val parts = type.defaultVersion.split(".")
            put("majorVersion", parts.getOrNull(0)?.toIntOrNull() ?: 1)
            put("minorVersion", parts.getOrNull(1)?.toIntOrNull() ?: 0)
        }

        var maxSc = 1

        when (type) {
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
            ActivityType.IMPORTED -> {}
        }

        return ActivityEntity(
            id = id,
            title = title,
            description = desc,
            type = type,
            mainLibrary = type.mainLibrary,
            libraryVersion = type.defaultVersion,
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
