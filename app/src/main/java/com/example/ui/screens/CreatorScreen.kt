package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityType
import com.example.ui.CardDraft
import com.example.ui.LumiViewModel
import com.example.ui.OptionDraft
import com.example.ui.Screen
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
    val title by viewModel.creatorTitle.collectAsState()
    val description by viewModel.creatorDescription.collectAsState()
    val dependencyStatus by viewModel.creatorDependencyStatus.collectAsState()
    var isPreviewActive by remember { mutableStateOf(false) }

    // MultiChoice state
    val mcPrompt by viewModel.mcPrompt.collectAsState()
    val mcOptions by viewModel.mcOptions.collectAsState()

    // Flashcards state
    val flashcards by viewModel.flashcards.collectAsState()

    // Blanks state
    val blanksPrompt by viewModel.blanksPrompt.collectAsState()
    val blanksStatements by viewModel.blanksStatements.collectAsState()

    // Launcher for harvesting missing libraries
    val harvestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.harvestLibrariesFromUri(it) }
    }

    BackHandler {
        if (isPreviewActive) {
            isPreviewActive = false
        } else {
            viewModel.navigateTo(Screen.Library)
        }
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
                // Activity Type Selector Tabs
                item {
                    Text(
                        text = "SELECT ACTIVITY TYPE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActivityType.entries.filter { it != ActivityType.IMPORTED }.forEach { type ->
                            FilterChip(
                                selected = creatorType == type,
                                onClick = {
                                    viewModel.creatorType.value = type
                                    viewModel.checkCreatorDependencies()
                                },
                                label = { Text(type.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
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
                                placeholder = { Text("e.g. European Capitals Quiz") },
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

                // Dynamic Editor based on CreatorType
                when (creatorType) {
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

                    ActivityType.IMPORTED -> {}
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
                    ) {
                        Text("Save & Export as .h5p Archive", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
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
