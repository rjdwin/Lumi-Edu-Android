package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Info
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityType
import com.example.data.model.H5PLibraryEntity
import com.example.ui.CardDraft
import com.example.ui.LumiViewModel
import com.example.ui.OptionDraft
import com.example.ui.Screen
import com.example.ui.SingleChoiceDraft
import com.example.ui.theme.LumiAmber
import com.example.ui.theme.LumiTeal
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorScreen(
    viewModel: LumiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val creatorType by viewModel.creatorType.collectAsState()
    val installedLibraries by viewModel.installedLibraries.collectAsState()
    val title by viewModel.creatorTitle.collectAsState()
    val description by viewModel.creatorDescription.collectAsState()
    val dependencyStatus by viewModel.creatorDependencyStatus.collectAsState()
    var isPreviewActive by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Quiz (Question Set) state
    val qsPassPercentage by viewModel.qsPassPercentage.collectAsState()
    val qsQuestions by viewModel.qsQuestions.collectAsState()

    // MultiChoice state
    val mcPrompt by viewModel.mcPrompt.collectAsState()
    val mcOptions by viewModel.mcOptions.collectAsState()

    // Flashcards state
    val flashcards by viewModel.flashcards.collectAsState()

    // Blanks state
    val blanksPrompt by viewModel.blanksPrompt.collectAsState()
    val blanksStatements by viewModel.blanksStatements.collectAsState()

    // True/False state
    val tfPrompt by viewModel.tfPrompt.collectAsState()
    val tfCorrect by viewModel.tfCorrect.collectAsState()
    val tfFeedbackTrue by viewModel.tfFeedbackTrue.collectAsState()
    val tfFeedbackFalse by viewModel.tfFeedbackFalse.collectAsState()

    // Single Choice Set state
    val scsQuestions by viewModel.scsQuestions.collectAsState()

    // Drag the Words state
    val dragTextPrompt by viewModel.dragTextPrompt.collectAsState()
    val dragTextPassage by viewModel.dragTextPassage.collectAsState()

    // Mark the Words state
    val mtwPrompt by viewModel.mtwPrompt.collectAsState()
    val mtwPassage by viewModel.mtwPassage.collectAsState()

    // Custom Harvested Library state
    val customLibraryName by viewModel.customLibraryName.collectAsState()
    val customLibraryVersion by viewModel.customLibraryVersion.collectAsState()
    val customJsonContent by viewModel.customJsonContent.collectAsState()

    // Launcher for harvesting missing libraries
    val harvestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.harvestLibrariesFromUri(it) }
    }

    var showPasteCsvDialog by remember { mutableStateOf(false) }
    var pastedCsvText by remember { mutableStateOf("") }
    var replaceOnPaste by remember { mutableStateOf(false) }

    // Launcher for importing CSV file
    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importQuestionsFromCsv(it)
        }
    }

    // Launcher for saving/exporting .h5p directly to file via Storage Access Framework
    val h5pSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            val draft = viewModel.buildDraftActivity()
            viewModel.exportActivityToUri(draft, it)
        }
    }

    BackHandler {
        if (isPreviewActive) {
            isPreviewActive = false
        } else {
            viewModel.navigateTo(Screen.Library)
        }
    }

    // Prepare list of selectable runnable libraries from installed libraries
    val runnableLibraries = remember(installedLibraries) {
        val libs = installedLibraries.filter { it.runnable }
        if (libs.isNotEmpty()) libs else listOf(
            H5PLibraryEntity(id = "H5P.MultiChoice-1.14", machineName = "H5P.MultiChoice", majorVersion = 1, minorVersion = 14, patchVersion = 2, title = "Multiple Choice"),
            H5PLibraryEntity(id = "H5P.Dialogcards-1.8", machineName = "H5P.Dialogcards", majorVersion = 1, minorVersion = 8, patchVersion = 1, title = "Dialog Cards / Flashcards"),
            H5PLibraryEntity(id = "H5P.Blanks-1.12", machineName = "H5P.Blanks", majorVersion = 1, minorVersion = 12, patchVersion = 0, title = "Fill in the Blanks")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isPreviewActive) "Previewing Activity" else "Activity Creator Studio",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isPreviewActive) isPreviewActive = false else viewModel.navigateTo(Screen.Library)
                        },
                        modifier = Modifier.testTag("creator_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isPreviewActive = !isPreviewActive },
                        modifier = Modifier.testTag("creator_preview_toggle")
                    ) {
                        Icon(
                            imageVector = if (isPreviewActive) Icons.Default.Edit else Icons.Default.PlayArrow,
                            contentDescription = if (isPreviewActive) "Edit" else "Preview",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            val draft = viewModel.buildDraftActivity()
                            val file = viewModel.exportActivity(draft)
                            shareH5PFile(context, file)
                        },
                        modifier = Modifier.testTag("creator_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export and share .h5p package",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.saveCreatedActivity() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("creator_save_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (isPreviewActive) {
            val draft = viewModel.buildDraftActivity()
            PlayerScreen(
                activity = draft,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Dropdown Selection for Activity Type (based on installed libraries)
                item {
                    Text(
                        text = "SELECT ACTIVITY TYPE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = creatorType.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Activity Type (Installed Libraries)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = getActivityIcon(creatorType),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .testTag("activity_type_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            runnableLibraries.forEach { lib ->
                                val matchedType = ActivityType.fromLibrary(lib.machineName)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = getActivityIcon(matchedType),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = lib.title,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = "${lib.machineName} • v${lib.majorVersion}.${lib.minorVersion}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (lib.isPrepackaged) LumiTeal.copy(alpha = 0.12f)
                                                        else SuccessGreen.copy(alpha = 0.12f)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (lib.isPrepackaged) "Core" else "Harvested",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (lib.isPrepackaged) LumiTeal else SuccessGreen
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectActivityType(matchedType)
                                        if (matchedType == ActivityType.IMPORTED) {
                                            viewModel.customLibraryName.value = lib.machineName
                                            viewModel.customLibraryVersion.value = "${lib.majorVersion}.${lib.minorVersion}"
                                        }
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Dependency Health Banner
                dependencyStatus?.let { status ->
                    if (!status.isSatisfied) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = LumiAmber.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Missing Library Dependencies",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFFB45309)
                                        )
                                        Text(
                                            text = "Missing: ${status.missingLibraries.joinToString()}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { harvestLauncher.launch(arrayOf("*/*")) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Harvest", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Basic Details (Title & Description)
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Activity Information",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = title,
                                onValueChange = { viewModel.creatorTitle.value = it },
                                label = { Text("Activity Title") },
                                placeholder = { Text("e.g. Science Challenge") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("creator_title_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = description,
                                onValueChange = { viewModel.creatorDescription.value = it },
                                label = { Text("Instructions / Description") },
                                placeholder = { Text("Short guidance for the learner") },
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("creator_desc_input")
                            )
                        }
                    }
                }

                // Dynamic Editor based on selected ActivityType
                when (creatorType) {
                    ActivityType.QUESTION_SET -> {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Quiz Configuration",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "A Quiz packages multiple MCQ questions into a full test with navigation dots, instant score feedback, and final review.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = qsPassPercentage.toString(),
                                        onValueChange = { 
                                            it.toIntOrNull()?.let { pct -> viewModel.qsPassPercentage.value = pct.coerceIn(0, 100) }
                                        },
                                        label = { Text("Passing Percentage (%)") },
                                        placeholder = { Text("e.g. 50") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Bulk Question Import (CSV / Excel)
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bulk_csv_import_card")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.TableChart,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Bulk Question Import (CSV / Excel)",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${qsQuestions.size} loaded",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Import many questions at once. Format: Question, Correct Answer, 3 Distractors (Wrong Answers), Feedback.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Randomization assurance badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(LumiTeal.copy(alpha = 0.1f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shuffle,
                                            contentDescription = null,
                                            tint = LumiTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Answer choices are automatically randomized when displaying the quiz to learners.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = LumiTeal
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val templateFile = viewModel.getTemplateCsvFile()
                                                shareTemplateCsv(context, templateFile)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("download_template_csv_button")
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Template", fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                csvPickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .testTag("import_csv_file_button")
                                        ) {
                                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Import CSV", fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { showPasteCsvDialog = true },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("paste_csv_data_button")
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Paste Data", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Question cards list
                        itemsIndexed(qsQuestions, key = { _, q -> q.id }) { qIndex, qDraft ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "MCQ Question #${qIndex + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (qsQuestions.size > 1) {
                                            IconButton(
                                                onClick = { viewModel.removeQuizQuestion(qDraft.id) }
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Question",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = qDraft.question,
                                        onValueChange = { viewModel.updateQuizQuestionText(qDraft.id, it) },
                                        label = { Text("Question Prompt") },
                                        placeholder = { Text("e.g. Which planet is closest to the Sun?") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Answer Choices (select the correct radio button):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    qDraft.options.forEachIndexed { optIndex, opt ->
                                        Card(
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (opt.isCorrect) SuccessGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = opt.isCorrect,
                                                        onClick = {
                                                            viewModel.updateQuizOption(qDraft.id, opt.id, opt.text, true, opt.feedback)
                                                        }
                                                    )
                                                    Text(
                                                        text = if (opt.isCorrect) "Correct Choice" else "Incorrect Choice",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (opt.isCorrect) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    if (qDraft.options.size > 2) {
                                                        IconButton(
                                                            onClick = { viewModel.removeOptionFromQuizQuestion(qDraft.id, opt.id) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Delete Option",
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                OutlinedTextField(
                                                    value = opt.text,
                                                    onValueChange = {
                                                        viewModel.updateQuizOption(qDraft.id, opt.id, it, opt.isCorrect, opt.feedback)
                                                    },
                                                    label = { Text("Choice #${optIndex + 1}") },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                OutlinedTextField(
                                                    value = opt.feedback,
                                                    onValueChange = {
                                                        viewModel.updateQuizOption(qDraft.id, opt.id, opt.text, opt.isCorrect, it)
                                                    },
                                                    label = { Text("Feedback (optional)") },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { viewModel.addOptionToQuizQuestion(qDraft.id) },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Choice")
                                    }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = { viewModel.addQuizQuestion() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("quiz_add_question_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add MCQ Question to Quiz", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    ActivityType.MULTI_CHOICE -> {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Question & Choices",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = mcPrompt,
                                        onValueChange = { viewModel.mcPrompt.value = it },
                                        label = { Text("Question Prompt") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        itemsIndexed(mcOptions, key = { _, opt -> opt.id }) { index, opt ->
                            OptionEditorItem(
                                index = index + 1,
                                option = opt,
                                onUpdate = { text, isCorrect, feedback ->
                                    viewModel.updateOption(opt.id, text, isCorrect, feedback)
                                },
                                onDelete = { viewModel.removeOption(opt.id) },
                                canDelete = mcOptions.size > 2
                            )
                        }

                        item {
                            Button(
                                onClick = { viewModel.addOption() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Option")
                            }
                        }
                    }

                    ActivityType.FLASHCARDS -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Flashcard Deck (${flashcards.size} Cards)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                TextButton(onClick = { viewModel.addCard() }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Card")
                                }
                            }
                        }

                        itemsIndexed(flashcards, key = { _, card -> card.id }) { index, card ->
                            CardEditorItem(
                                index = index + 1,
                                card = card,
                                onUpdate = { front, back, hint ->
                                    viewModel.updateCard(card.id, front, back, hint)
                                },
                                onDelete = { viewModel.removeCard(card.id) },
                                canDelete = flashcards.size > 1
                            )
                        }
                    }

                    ActivityType.BLANKS -> {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Fill in the Blanks Rules",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Wrap missing words in asterisks: e.g. 'A byte has *8* bits.' For multiple accepted answers, use slash: '*CPU/processor*'.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = blanksPrompt,
                                        onValueChange = { viewModel.blanksPrompt.value = it },
                                        label = { Text("Task Instructions") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        itemsIndexed(blanksStatements) { index, statement ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = statement,
                                        onValueChange = { viewModel.updateBlankStatement(index, it) },
                                        label = { Text("Statement #${index + 1}") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (blanksStatements.size > 1) {
                                        IconButton(onClick = { viewModel.removeBlankStatement(index) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete statement", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = { viewModel.addBlankStatement() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Statement")
                            }
                        }
                    }

                    ActivityType.SINGLE_CHOICE_SET -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Single Choice Questions (${scsQuestions.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                TextButton(onClick = { viewModel.addScsQuestion() }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Question")
                                }
                            }
                        }

                        itemsIndexed(scsQuestions, key = { _, item -> item.id }) { index, item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Question #${index + 1}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (scsQuestions.size > 1) {
                                            IconButton(onClick = { viewModel.removeScsQuestion(item.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Question", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = item.question,
                                        onValueChange = { viewModel.updateScsQuestion(item.id, it, item.correctAnswer, item.wrongAlternative1, item.wrongAlternative2) },
                                        label = { Text("Question Prompt") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = item.correctAnswer,
                                        onValueChange = { viewModel.updateScsQuestion(item.id, item.question, it, item.wrongAlternative1, item.wrongAlternative2) },
                                        label = { Text("Correct Answer (First Option)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = item.wrongAlternative1,
                                            onValueChange = { viewModel.updateScsQuestion(item.id, item.question, item.correctAnswer, it, item.wrongAlternative2) },
                                            label = { Text("Alternative 1") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = item.wrongAlternative2,
                                            onValueChange = { viewModel.updateScsQuestion(item.id, item.question, item.correctAnswer, item.wrongAlternative1, it) },
                                            label = { Text("Alternative 2") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ActivityType.TRUE_FALSE -> {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "True / False Statement",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = tfPrompt,
                                        onValueChange = { viewModel.tfPrompt.value = it },
                                        label = { Text("Statement to Evaluate") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Correct Answer:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { viewModel.tfCorrect.value = true }
                                        ) {
                                            RadioButton(selected = tfCorrect, onClick = { viewModel.tfCorrect.value = true })
                                            Text("True", fontWeight = FontWeight.Bold)
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { viewModel.tfCorrect.value = false }
                                        ) {
                                            RadioButton(selected = !tfCorrect, onClick = { viewModel.tfCorrect.value = false })
                                            Text("False", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    OutlinedTextField(
                                        value = tfFeedbackTrue,
                                        onValueChange = { viewModel.tfFeedbackTrue.value = it },
                                        label = { Text("Feedback on True") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = tfFeedbackFalse,
                                        onValueChange = { viewModel.tfFeedbackFalse.value = it },
                                        label = { Text("Feedback on False") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    ActivityType.DRAG_TEXT -> {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Drag the Words Passage",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Wrap words to be dragged in asterisks: e.g. 'An apple is *red*, and a banana is *yellow*.'",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = dragTextPrompt,
                                        onValueChange = { viewModel.dragTextPrompt.value = it },
                                        label = { Text("Instructions") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = dragTextPassage,
                                        onValueChange = { viewModel.dragTextPassage.value = it },
                                        label = { Text("Passage with *draggables*") },
                                        maxLines = 6,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    ActivityType.MARK_WORDS -> {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Mark the Words Task",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Wrap words to be highlighted in asterisks: e.g. 'The dog *barked* and *ran*.'",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = mtwPrompt,
                                        onValueChange = { viewModel.mtwPrompt.value = it },
                                        label = { Text("Instructions") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = mtwPassage,
                                        onValueChange = { viewModel.mtwPassage.value = it },
                                        label = { Text("Text with *target words*") },
                                        maxLines = 6,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    ActivityType.IMPORTED -> {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Custom / Harvested Library Parameters",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = customLibraryName,
                                        onValueChange = { viewModel.customLibraryName.value = it },
                                        label = { Text("Main Library Name") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = customLibraryVersion,
                                        onValueChange = { viewModel.customLibraryVersion.value = it },
                                        label = { Text("Library Version") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = customJsonContent,
                                        onValueChange = { viewModel.customJsonContent.value = it },
                                        label = { Text("Parameters JSON (content.json)") },
                                        maxLines = 8,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Save & Export Actions
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.saveCreatedActivity() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("creator_bottom_save_button")
                    ) {
                        Text("Save Activity to Library", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val draft = viewModel.buildDraftActivity()
                            val file = viewModel.exportActivity(draft)
                            shareH5PFile(context, file)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("creator_share_h5p_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share .h5p Package", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val draft = viewModel.buildDraftActivity()
                            val sanitized = draft.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                            h5pSaveLauncher.launch("$sanitized.h5p")
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("creator_save_storage_h5p_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download .h5p to Storage", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    if (showPasteCsvDialog) {
        AlertDialog(
            onDismissRequest = { showPasteCsvDialog = false },
            title = {
                Text("Paste CSV / Spreadsheet Data", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Paste comma or semicolon separated rows:\nQuestion, Correct Answer, Distractor 1, Distractor 2, Distractor 3, Feedback",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pastedCsvText,
                        onValueChange = { pastedCsvText = it },
                        placeholder = {
                            Text("e.g.\n\"What is 5+5?\",\"10\",\"8\",\"9\",\"12\"\n\"What is the boiling point of water?\",\"100 C\",\"90 C\",\"80 C\",\"110 C\"")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 10
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { replaceOnPaste = !replaceOnPaste }
                    ) {
                        Checkbox(
                            checked = replaceOnPaste,
                            onCheckedChange = { replaceOnPaste = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Replace existing quiz questions", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val (count, err) = viewModel.importQuestionsFromCsvText(pastedCsvText, replaceOnPaste)
                        if (err != null) {
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Imported $count questions!", Toast.LENGTH_SHORT).show()
                            pastedCsvText = ""
                            showPasteCsvDialog = false
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteCsvDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun shareTemplateCsv(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Quiz Questions CSV Template")
            putExtra(Intent.EXTRA_TEXT, "Use this pre-structured CSV template to author quiz questions and import them directly into Lumi Mobile!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Download or Share Template CSV"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share template: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun getActivityIcon(type: ActivityType): ImageVector {
    return when (type) {
        ActivityType.QUESTION_SET -> Icons.Default.Quiz
        ActivityType.MULTI_CHOICE -> Icons.Default.RadioButtonChecked
        ActivityType.FLASHCARDS -> Icons.Default.ViewCarousel
        ActivityType.BLANKS -> Icons.Default.EditNote
        ActivityType.SINGLE_CHOICE_SET -> Icons.Default.Bolt
        ActivityType.TRUE_FALSE -> Icons.Default.CheckCircle
        ActivityType.DRAG_TEXT -> Icons.Default.DragIndicator
        ActivityType.MARK_WORDS -> Icons.Default.Highlight
        ActivityType.IMPORTED -> Icons.Default.Extension
    }
}

@Composable
fun OptionEditorItem(
    index: Int,
    option: OptionDraft,
    onUpdate: (String, Boolean, String) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = option.isCorrect,
                    onCheckedChange = { onUpdate(option.text, it, option.feedback) }
                )
                Text(
                    text = if (option.isCorrect) "Correct Answer" else "Incorrect Choice",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (option.isCorrect) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Option", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            OutlinedTextField(
                value = option.text,
                onValueChange = { onUpdate(it, option.isCorrect, option.feedback) },
                label = { Text("Option $index text") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = option.feedback,
                onValueChange = { onUpdate(option.text, option.isCorrect, it) },
                label = { Text("Optional Feedback (shown on selection)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CardEditorItem(
    index: Int,
    card: CardDraft,
    onUpdate: (String, String, String) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Card #$index",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Card", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            OutlinedTextField(
                value = card.front,
                onValueChange = { onUpdate(it, card.back, card.hint) },
                label = { Text("Front (Question / Prompt)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = card.back,
                onValueChange = { onUpdate(card.front, it, card.hint) },
                label = { Text("Back (Answer / Meaning)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = card.hint,
                onValueChange = { onUpdate(card.front, card.back, it) },
                label = { Text("Optional Hint") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
