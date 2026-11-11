package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.H5PLibraryEntity
import com.example.ui.LumiViewModel
import com.example.ui.Screen
import com.example.ui.theme.LumiAmber
import com.example.ui.theme.LumiTeal
import com.example.ui.theme.SuccessGreen
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryManagerScreen(
    viewModel: LumiViewModel,
    modifier: Modifier = Modifier
) {
    val libraries by viewModel.installedLibraries.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedLibraryForDetails by remember { mutableStateOf<H5PLibraryEntity?>(null) }
    var libraryToDelete by remember { mutableStateOf<H5PLibraryEntity?>(null) }

    val harvestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.harvestLibrariesFromUri(it) }
    }

    BackHandler {
        viewModel.navigateTo(Screen.Library)
    }

    val filteredLibraries = when (selectedFilter) {
        "Bundled" -> libraries.filter { it.isPrepackaged }
        "Harvested" -> libraries.filter { !it.isPrepackaged }
        else -> libraries
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "H5P Library Registry",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${libraries.size} libraries installed offline",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Library) },
                        modifier = Modifier.testTag("lib_manager_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { harvestLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.testTag("harvest_libraries_appbar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Harvest Libraries",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Hero / Harvester Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Library Harvester & Updater",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "If an activity requires a new or updated library, simply select any .h5p file containing that library. Lumi will automatically extract, validate, and store it in your offline repository.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { harvestLauncher.launch(arrayOf("*/*")) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("harvest_from_file_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Harvest Libraries from .h5p File", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Bundled", "Harvested").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Libraries List
            items(filteredLibraries, key = { it.id }) { lib ->
                LibraryItemCard(
                    library = lib,
                    onClick = { selectedLibraryForDetails = lib },
                    onDelete = { libraryToDelete = lib }
                )
            }
        }
    }

    // Details Modal
    selectedLibraryForDetails?.let { lib ->
        val depsArray = try { JSONArray(lib.dependenciesJson) } catch (e: Exception) { JSONArray() }
        AlertDialog(
            onDismissRequest = { selectedLibraryForDetails = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = lib.title, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Identifier: ${lib.machineName} (${lib.majorVersion}.${lib.minorVersion}.${lib.patchVersion})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Origin: ${if (lib.isPrepackaged) "Pre-packaged Core Asset" else "Harvested from User File"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (lib.isPrepackaged) LumiTeal else LumiAmber
                    )
                    if (lib.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = lib.description, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Preloaded Dependencies (${depsArray.length()}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (depsArray.length() == 0) {
                        Text(text = "None (Standalone library)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        for (i in 0 until depsArray.length()) {
                            val dep = depsArray.getJSONObject(i)
                            Text(
                                text = "• ${dep.optString("machineName")} ${dep.optInt("majorVersion")}.${dep.optInt("minorVersion")}",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLibraryForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Modal
    libraryToDelete?.let { lib ->
        AlertDialog(
            onDismissRequest = { libraryToDelete = null },
            title = { Text("Delete Library?") },
            text = { Text("Remove '${lib.title}' (${lib.machineName}) from your offline library registry?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteLibrary(lib.id)
                        libraryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { libraryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LibraryItemCard(
    library: H5PLibraryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("library_card_${library.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (library.isPrepackaged) LumiTeal.copy(alpha = 0.12f) else LumiAmber.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (library.isPrepackaged) Icons.Default.Verified else Icons.Default.Extension,
                    contentDescription = null,
                    tint = if (library.isPrepackaged) LumiTeal else Color(0xFFD97706),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = library.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${library.machineName} • v${library.majorVersion}.${library.minorVersion}.${library.patchVersion}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (library.isPrepackaged) LumiTeal.copy(alpha = 0.12f) else SuccessGreen.copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (library.isPrepackaged) "Core" else "Harvested",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (library.isPrepackaged) LumiTeal else SuccessGreen
                )
            }
            if (!library.isPrepackaged) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Library",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
