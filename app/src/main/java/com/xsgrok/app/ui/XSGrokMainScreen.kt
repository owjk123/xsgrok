package com.xsgrok.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.window.layout.WindowInfoTracker
import com.xsgrok.app.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun XSGrokMainScreen(
    windowInfoTracker: WindowInfoTracker,
    viewModel: XSGrokViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val novels by viewModel.novels.collectAsState()
    val currentNovel by viewModel.currentNovel.collectAsState()
    val isDarkMode = uiState.apiConfig.isDarkMode
    
    var windowWidthSizeClass by remember { mutableStateOf(WindowWidthSizeClass.COMPACT) }
    
    LaunchedEffect(windowInfoTracker) {
        windowInfoTracker.windowLayoutInfo(android.app.Activity()).collect { layoutInfo ->
            windowWidthSizeClass = layoutInfo.displayFeatures
                .filterIsInstance<androidx.window.layout.WindowLayoutInfo>()
                .firstOrNull()
                ?.displayFeatures
                ?.firstOrNull()
                ?.let { 
                    if (it is androidx.window.layout.FoldingFeature && it.isTableTop) {
                        WindowWidthSizeClass.EXPANDED
                    } else {
                        WindowWidthSizeClass.COMPACT
                    }
                } ?: WindowWidthSizeClass.COMPACT
        }
    }
    
    val isExpandedScreen = windowWidthSizeClass != WindowWidthSizeClass.COMPACT
    
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
            }
        ) { paddingValues ->
            if (isExpandedScreen) {
                Row(modifier = Modifier.padding(paddingValues)) {
                    NavigationRail(modifier = Modifier.width(80.dp)) {
                        NavigationRailItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            selected = uiState.currentScreen == Screen.Home,
                            onClick = { viewModel.navigateTo(Screen.Home) }
                        )
                        NavigationRailItem(
                            icon = { Icon(Icons.Default.Add, contentDescription = "New") },
                            label = { Text("New") },
                            selected = uiState.currentScreen == Screen.NewNovel,
                            onClick = { viewModel.navigateTo(Screen.NewNovel) }
                        )
                    }
                    MainContent(
                        screen = uiState.currentScreen,
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                MainContent(
                    screen = uiState.currentScreen,
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    screen: Screen,
    viewModel: XSGrokViewModel,
    modifier: Modifier = Modifier
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
