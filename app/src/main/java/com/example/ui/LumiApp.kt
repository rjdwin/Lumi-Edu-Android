package com.example.ui

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.CreatorScreen
import com.example.ui.screens.LibraryManagerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen

@Composable
fun LumiApp(
    viewModel: LumiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val toastMessage by viewModel.userToastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val showBottomBar = currentScreen !is Screen.Player

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("lumi_bottom_navigation")
                ) {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Library,
                        onClick = { viewModel.navigateTo(Screen.Library) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is Screen.Library) Icons.Filled.LocalLibrary else Icons.Outlined.LocalLibrary,
                                contentDescription = "Library"
                            )
                        },
                        label = { Text("Library") },
                        modifier = Modifier.testTag("nav_library_tab")
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Creator,
                        onClick = { viewModel.navigateTo(Screen.Creator) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is Screen.Creator) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline,
                                contentDescription = "Create"
                            )
                        },
                        label = { Text("Create") },
                        modifier = Modifier.testTag("nav_create_tab")
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.LibraryManager,
                        onClick = { viewModel.navigateTo(Screen.LibraryManager) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is Screen.LibraryManager) Icons.Filled.Extension else Icons.Outlined.Extension,
                                contentDescription = "Libraries"
                            )
                        },
                        label = { Text("Libraries") },
                        modifier = Modifier.testTag("nav_libraries_tab")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                when (screen) {
                    is Screen.Library -> LibraryScreen(viewModel = viewModel)
                    is Screen.Player -> PlayerScreen(activity = screen.activity, viewModel = viewModel)
                    is Screen.Creator -> CreatorScreen(viewModel = viewModel)
                    is Screen.LibraryManager -> LibraryManagerScreen(viewModel = viewModel)
                }
            }
        }
    }
}
