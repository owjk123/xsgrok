package com.xsgrok.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xsgrok.app.R
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
                                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleDarkMode() }) {
                            Icon(
                                if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = stringResource(R.string.toggle_theme)
                            )
                        }
                        IconButton(onClick = { viewModel.navigateTo(Screen.Settings) }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home)) },
                        label = { Text(stringResource(R.string.home)) },
                        selected = uiState.currentScreen == Screen.Home,
                        onClick = { viewModel.navigateTo(Screen.Home) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.auto_mode)) },
                        label = { Text(stringResource(R.string.auto_mode)) },
                        selected = uiState.currentScreen == Screen.AutoMode,
                        onClick = { viewModel.navigateTo(Screen.AutoMode) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = stringResource(R.string.bookshelf)) },
                        label = { Text(stringResource(R.string.bookshelf)) },
                        selected = uiState.currentScreen == Screen.Bookshelf,
                        onClick = { viewModel.navigateTo(Screen.Bookshelf) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_label)) },
                        label = { Text(stringResource(R.string.new_label)) },
                        selected = uiState.currentScreen == Screen.NewNovel,
                        onClick = { viewModel.navigateTo(Screen.NewNovel) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) },
                        label = { Text(stringResource(R.string.settings)) },
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
        Screen.AutoMode -> AutoModeScreen(viewModel)
        Screen.Bookshelf -> BookshelfScreen(viewModel)
        Screen.Reading -> ReadingScreen(viewModel)
        Screen.WorldBuilding -> WorldBuildingScreen(viewModel)
    }
}

@Composable
private fun getScreenTitle(screen: Screen): String {
    return when (screen) {
        Screen.Home -> stringResource(R.string.app_name)
        Screen.Settings -> stringResource(R.string.settings)
        Screen.NewNovel -> stringResource(R.string.new_novel)
        Screen.NovelDetail -> stringResource(R.string.novel_detail)
        Screen.Characters -> stringResource(R.string.characters)
        Screen.Drafts -> stringResource(R.string.drafts)
        Screen.ChapterGeneration -> stringResource(R.string.generate_chapter)
        Screen.AutoMode -> stringResource(R.string.auto_mode)
        Screen.Bookshelf -> stringResource(R.string.bookshelf)
        Screen.Reading -> stringResource(R.string.reading)
        Screen.WorldBuilding -> stringResource(R.string.world_building)
    }
}
