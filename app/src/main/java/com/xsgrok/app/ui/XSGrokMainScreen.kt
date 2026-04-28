package com.xsgrok.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xsgrok.app.ui.screens.*
import com.xsgrok.app.ui.theme.XSGrokTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XSGrokMainScreen(
    viewModel: XSGrokViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDarkMode = uiState.apiConfig.isDarkMode
    
    XSGrokTheme(darkTheme = isDarkMode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(getScreenTitle(uiState.currentScreen)) },
                    navigationIcon = {
                        if (uiState.currentScreen != Screen.Home) {
                            IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleDarkMode() }) {
                            Icon(
                                if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                        IconButton(onClick = { viewModel.navigateTo(Screen.Settings) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = uiState.currentScreen == Screen.Home,
                        onClick = { viewModel.navigateTo(Screen.Home) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Add, contentDescription = "New") },
                        label = { Text("New") },
                        selected = uiState.currentScreen == Screen.NewNovel,
                        onClick = { viewModel.navigateTo(Screen.NewNovel) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = uiState.currentScreen == Screen.Settings,
                        onClick = { viewModel.navigateTo(Screen.Settings) }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                MainContent(
                    screen = uiState.currentScreen,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    screen: Screen,
    viewModel: XSGrokViewModel
) {
    when (screen) {
        Screen.Home -> HomeScreen(viewModel)
        Screen.Settings -> SettingsScreen(viewModel)
        Screen.NewNovel -> NewNovelScreen(viewModel)
        Screen.NovelDetail -> NovelDetailScreen(viewModel)
        Screen.Characters -> CharactersScreen(viewModel)
        Screen.Drafts -> DraftsScreen(viewModel)
        Screen.ChapterGeneration -> ChapterGenerationScreen(viewModel)
    }
}

private fun getScreenTitle(screen: Screen): String {
    return when (screen) {
        Screen.Home -> "XSGrok"
        Screen.Settings -> "Settings"
        Screen.NewNovel -> "New Novel"
        Screen.NovelDetail -> "Novel Detail"
        Screen.Characters -> "Characters"
        Screen.Drafts -> "Drafts"
        Screen.ChapterGeneration -> "Generate Chapter"
    }
}
