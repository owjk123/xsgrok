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
import com.xsgrok.app.data.model.ApiConfig
import com.xsgrok.app.ui.screens.*
import com.xsgrok.app.ui.theme.XSGrokTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XSGrokMainScreen(
    viewModel: XSGrokViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDarkMode = uiState.apiConfig.isDarkMode
    val apiConfig = uiState.apiConfig

    // 判断是否需要首次配置引导
    var showSetupDialog by remember { mutableStateOf(apiConfig.apiKey.isBlank()) }
    var setupApiKey by remember { mutableStateOf("") }
    var setupEndpoint by remember { mutableStateOf("https://api.edgefn.net/v1") }
    var setupModel by remember { mutableStateOf("GLM-5.1") }

    XSGrokTheme(darkTheme = isDarkMode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(getScreenTitle(uiState.currentScreen)) },
                    navigationIcon = {
                        if (uiState.currentScreen != Screen.Create &&
                            uiState.currentScreen != Screen.Bookshelf &&
                            uiState.currentScreen != Screen.Settings) {
                            IconButton(onClick = { viewModel.navigateTo(Screen.Bookshelf) }) {
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
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Create, contentDescription = "创作") },
                        label = { Text("创作") },
                        selected = uiState.currentScreen == Screen.Create,
                        onClick = { viewModel.navigateTo(Screen.Create) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = stringResource(R.string.bookshelf)) },
                        label = { Text(stringResource(R.string.bookshelf)) },
                        selected = uiState.currentScreen == Screen.Bookshelf,
                        onClick = { viewModel.navigateTo(Screen.Bookshelf) }
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

        // 首次配置引导
        if (showSetupDialog && apiConfig.apiKey.isBlank()) {
            AlertDialog(
                onDismissRequest = { /* 不允许关闭 */ },
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("欢迎使用 XSGrok")
                    }
                },
                text = {
                    Column {
                        Text("开始创作前，请先配置您的API信息。", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = setupApiKey,
                            onValueChange = { setupApiKey = it },
                            label = { Text("API Key") },
                            placeholder = { Text("sk-...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = setupEndpoint,
                            onValueChange = { setupEndpoint = it },
                            label = { Text("API Endpoint") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = setupModel,
                            onValueChange = { setupModel = it },
                            label = { Text("Model") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateApiKey(setupApiKey)
                            viewModel.updateEndpoint(setupEndpoint)
                            viewModel.updateModel(setupModel)
                            showSetupDialog = false
                        },
                        enabled = setupApiKey.isNotBlank()
                    ) {
                        Text("开始创作")
                    }
                }
            )
        }
    }
}

@Composable
private fun MainContent(
    screen: Screen,
    viewModel: XSGrokViewModel
) {
    when (screen) {
        Screen.Create -> CreateScreen(viewModel)
        Screen.Settings -> SettingsScreen(viewModel)
        Screen.Bookshelf -> BookshelfScreen(viewModel)
        Screen.NovelDetail -> NovelDetailScreen(viewModel)
        Screen.Characters -> CharactersScreen(viewModel)
        Screen.ChapterGeneration -> ChapterGenerationScreen(viewModel)
        Screen.Reading -> ReadingScreen(viewModel)
        Screen.WorldBuilding -> WorldBuildingScreen(viewModel)
    }
}

@Composable
private fun getScreenTitle(screen: Screen): String {
    return when (screen) {
        Screen.Create -> "创作"
        Screen.Settings -> stringResource(R.string.settings)
        Screen.Bookshelf -> stringResource(R.string.bookshelf)
        Screen.NovelDetail -> stringResource(R.string.novel_detail)
        Screen.Characters -> stringResource(R.string.characters)
        Screen.ChapterGeneration -> stringResource(R.string.generate_chapter)
        Screen.Reading -> stringResource(R.string.reading)
        Screen.WorldBuilding -> stringResource(R.string.world_building)
    }
}
