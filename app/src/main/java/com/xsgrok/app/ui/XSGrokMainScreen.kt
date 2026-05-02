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

/**
 * 精简版主屏幕 - 3个Tab（创作|书架|设置）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XSGrokMainScreen(
    viewModel: XSGrokViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDarkMode = uiState.apiConfig.isDarkMode
    
    // 检测是否需要首次配置引导
    var showApiSetupDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // 首次启动检测
        if (uiState.apiConfig.apiKey.isBlank()) {
            showApiSetupDialog = true
        }
    }
    
    XSGrokTheme(darkTheme = isDarkMode) {
        Scaffold(
            topBar = {
                // 不再每个页面都显示TopAppBar，只在详情页显示
                if (uiState.currentScreen.shouldShowTopBar()) {
                    TopAppBar(
                        title = { Text(getScreenTitle(uiState.currentScreen)) },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.navigateTo(getDefaultScreen(uiState.currentScreen)) }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                }
            },
            bottomBar = {
                // 只在主要Tab页面显示底部导航
                if (uiState.currentScreen.isMainTab()) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            label = { Text("创作") },
                            selected = uiState.currentScreen == Screen.Creation,
                            onClick = { viewModel.navigateTo(Screen.Creation) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                            label = { Text("书架") },
                            selected = uiState.currentScreen == Screen.Bookshelf,
                            onClick = { viewModel.navigateTo(Screen.Bookshelf) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text("设置") },
                            selected = uiState.currentScreen == Screen.Settings,
                            onClick = { viewModel.navigateTo(Screen.Settings) }
                        )
                    }
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
        
        // 首次API配置引导Dialog
        if (showApiSetupDialog) {
            ApiSetupDialog(
                onDismiss = { showApiSetupDialog = false },
                onSave = { apiKey, endpoint, model ->
                    viewModel.updateApiKey(apiKey)
                    viewModel.updateEndpoint(endpoint)
                    viewModel.updateModel(model)
                    showApiSetupDialog = false
                },
                onTestConnection = { apiKey, endpoint, model, onResult ->
                    viewModel.testApiConnection(apiKey, endpoint, model, onResult)
                }
            )
        }
    }
}

/**
 * API首次配置引导Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSetupDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onTestConnection: (String, String, String, (Boolean, String) -> Unit) -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("https://api.edgefn.net/v1") }
    var model by remember { mutableStateOf("GLM-5.1") }
    var showApiKey by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    
    val models = listOf("GLM-5.1", "gpt-4o", "gpt-4o-mini", "claude-3-sonnet")
    
    AlertDialog(
        onDismissRequest = { },
        title = { 
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("首次配置引导")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "请配置您的AI API以开始创作",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-...") },
                    visualTransformation = if (showApiKey) androidx.compose.ui.text.input.VisualTransformation.None 
                        else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text("Endpoint") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 模型选择
                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = { }
                ) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { },
                        label = { Text("模型") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 测试连接按钮
                OutlinedButton(
                    onClick = { 
                        isTesting = true
                        testResult = null
                        onTestConnection(apiKey, endpoint, model) { success, message ->
                            isTesting = false
                            testResult = Pair(success, message)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = apiKey.isNotBlank() && !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("测试中...")
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("测试连接")
                    }
                }
                
                // 测试结果
                testResult?.let { (success, message) ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (success) MaterialTheme.colorScheme.secondaryContainer 
                                else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(
                                if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (success) MaterialTheme.colorScheme.secondary 
                                    else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(apiKey, endpoint, model) },
                enabled = apiKey.isNotBlank()
            ) {
                Text("开始创作")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后设置")
            }
        }
    )
}

@Composable
private fun MainContent(
    screen: Screen,
    viewModel: XSGrokViewModel
) {
    when (screen) {
        // 主Tab页面
        Screen.Creation -> AutoModeScreen(viewModel)  // 创作页 = 自动模式
        Screen.Bookshelf -> BookshelfScreen(viewModel)  // 书架
        Screen.Settings -> SettingsScreen(viewModel)     // 设置
        
        // 子页面
        Screen.Home -> HomeScreen(viewModel)  // 保留旧首页（用于跳转）
        Screen.NewNovel -> NewNovelScreen(viewModel)
        Screen.NovelDetail -> NovelDetailScreen(viewModel)
        Screen.Characters -> CharactersScreen(viewModel)
        Screen.Drafts -> BookshelfScreen(viewModel)  // Drafts合并到书架
        Screen.ChapterGeneration -> ChapterGenerationScreen(viewModel)
        Screen.AutoMode -> AutoModeScreen(viewModel)
        Screen.Reading -> ReadingScreen(viewModel)
        Screen.WorldBuilding -> WorldBuildingScreen(viewModel)
    }
}

@Composable
private fun getScreenTitle(screen: Screen): String {
    return when (screen) {
        Screen.Creation -> "创作"
        Screen.Bookshelf -> stringResource(R.string.bookshelf)
        Screen.Settings -> stringResource(R.string.settings)
        Screen.Home -> stringResource(R.string.app_name)
        Screen.NewNovel -> stringResource(R.string.new_novel)
        Screen.NovelDetail -> stringResource(R.string.novel_detail)
        Screen.Characters -> stringResource(R.string.characters)
        Screen.Drafts -> stringResource(R.string.drafts)
        Screen.ChapterGeneration -> stringResource(R.string.generate_chapter)
        Screen.AutoMode -> stringResource(R.string.auto_mode)
        Screen.Reading -> stringResource(R.string.reading)
        Screen.WorldBuilding -> stringResource(R.string.world_building)
    }
}

/**
 * 判断是否为需要显示TopBar的页面
 */
private fun Screen.shouldShowTopBar(): Boolean {
    return this !in listOf(Screen.Creation, Screen.Bookshelf, Screen.Settings)
}

/**
 * 判断是否为底部导航Tab页面
 */
private fun Screen.isMainTab(): Boolean {
    return this in listOf(Screen.Creation, Screen.Bookshelf, Screen.Settings)
}

/**
 * 获取返回时默认跳转的屏幕
 */
private fun getDefaultScreen(currentScreen: Screen): Screen {
    return when (currentScreen) {
        Screen.NewNovel, Screen.NovelDetail, Screen.Characters, Screen.Drafts,
        Screen.ChapterGeneration, Screen.WorldBuilding -> Screen.Creation
        else -> Screen.Creation
    }
}
