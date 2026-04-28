package com.xsgrok.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xsgrok.app.data.local.LocalStorage
import com.xsgrok.app.data.model.*
import com.xsgrok.app.data.remote.ApiService
import com.xsgrok.app.ui.screens.AutoModeState
import com.xsgrok.app.generation.MultiStageGenerator
import com.xsgrok.app.generation.RhythmController
import com.xsgrok.app.generation.SceneDetector
import com.xsgrok.app.memory.ConsistencyManager
import com.xsgrok.app.prompt.PromptTemplates
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class XSGrokViewModel(application: Application) : AndroidViewModel(application) {
    
    private val localStorage = LocalStorage(application)
    private val apiService = ApiService()
    
    // P2/P3: 初始化新模块
    private val multiStageGenerator = MultiStageGenerator()
    private val rhythmController = RhythmController()
    private val sceneDetector = SceneDetector()
    private val consistencyManager = ConsistencyManager()
    
    private val _uiState = MutableStateFlow(XSGrokUiState())
    val uiState: StateFlow<XSGrokUiState> = _uiState.asStateFlow()
    
    private val _novels = MutableStateFlow<List<Novel>>(emptyList())
    val novels: StateFlow<List<Novel>> = _novels.asStateFlow()
    
    private val _currentNovel = MutableStateFlow<Novel?>(null)
    val currentNovel: StateFlow<Novel?> = _currentNovel.asStateFlow()
    
    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _autoModeState = MutableStateFlow(AutoModeState.IDLE)
    val autoModeState: StateFlow<AutoModeState> = _autoModeState.asStateFlow()
    
    private val _autoModeNovel = MutableStateFlow<Novel?>(null)
    val autoModeNovel: StateFlow<Novel?> = _autoModeNovel.asStateFlow()
    
    private var generationJob: Job? = null
    
    // ========== P0功能：温度计算和去AI味提示 ==========
    
    private fun calculateTemperature(chapterNum: Int, totalNodes: Int): Float {
        if (totalNodes <= 0) return 0.75f
        
        val progress = chapterNum.toFloat() / totalNodes
        return when {
            progress < 0.2f -> 0.9f
            progress < 0.8f -> 0.75f
            else -> 0.65f
        }
    }
    
    private fun getAntiAIWritingHints(): String {
        return PromptTemplates.getAntiAIWritingHints()
    }
    
    private fun parseKeyNodesFromOutline(outline: String): List<KeyNode> {
        val nodes = mutableListOf<KeyNode>()
        val lines = outline.lines().filter { it.isNotBlank() }
        
        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            val title = when {
                trimmed.matches(Regex("""^\d+[.、].+""")) -> trimmed.replace(Regex("""^\d+[.、]\s*"""), "")
                trimmed.startsWith("【") && trimmed.endsWith("】") -> trimmed.drop(1).dropLast(1)
                trimmed.startsWith("[") && trimmed.endsWith("]") -> trimmed.drop(1).dropLast(1)
                trimmed.length > 5 && index < 15 -> trimmed.take(50)
                else -> null
            }
            
            if (title != null && title.length >= 2) {
                nodes.add(KeyNode(
                    title = title,
                    description = trimmed,
                    targetChapter = (index + 1) * 2
                ))
            }
            
            if (nodes.size >= 10) break
        }
        
        if (nodes.isEmpty()) {
            listOf("开篇", "矛盾初现", "危机升级", "转折点", "高潮", "结局").forEachIndexed { i, title ->
                nodes.add(KeyNode(title = title, description = title, targetChapter = (i + 1) * 2))
            }
        }
        
        return nodes
    }
    
    private fun extractForeshadowings(content: String, chapterNum: Int): List<Foreshadowing> {
        val foreshadowings = mutableListOf<Foreshadowing>()
        val patterns = listOf(
            Regex("""【(.+?)】"""),
            Regex("""（(.+?)）"""),
            Regex("""\[(.+?)\]""")
        )
        
        for (pattern in patterns) {
            pattern.findAll(content).forEach { match ->
                val hint = match.groupValues[1]
                if (hint.length in 4..30) {
                    foreshadowings.add(Foreshadowing(
                        content = hint,
                        plantedChapter = chapterNum,
                        hint = "待回收"
                    ))
                }
            }
        }
        
        return foreshadowings
    }
    
    // ========== 伏笔管理 ==========
    
    fun resolveForeshadowing(foreshadowingId: String, chapterNum: Int) {
        val novel = _currentNovel.value ?: return
        
        val index = novel.foreshadowings.indexOfFirst { it.id == foreshadowingId }
        if (index >= 0) {
            novel.foreshadowings[index] = novel.foreshadowings[index].copy(
                isResolved = true,
                resolvedChapter = chapterNum
            )
            viewModelScope.launch {
                localStorage.saveNovel(novel)
            }
            _currentNovel.value = novel
        }
    }
    
    fun getForeshadowingStats(): ForeshadowingStats {
        return _currentNovel.value?.getForeshadowingStats() ?: ForeshadowingStats(0, 0, 0, 0f)
    }
    
    // ========== P1/P2: 增强生成逻辑 ==========
    
    /**
     * P2: 检测场景并自动增强
     */
    private fun detectAndEnhance(content: String, novel: Novel): String {
        val sceneType = sceneDetector.detectSceneType(content)
        
        // 如果检测到亲密场景，自动注入增强指令
        if (sceneDetector.shouldEnhanceMode(content)) {
            return buildString {
                append(content)
                appendLine()
                appendLine()
                append(sceneDetector.generateEnhancementHint(content, novel.sensoryProfile))
            }
        }
        
        return content
    }
    
    /**
     * P3: 注入记忆一致性信息
     */
    private fun injectConsistencyInfo(novel: Novel, chapterNum: Int): String {
        return consistencyManager.injectMemories(novel, chapterNum)
    }
    
    /**
     * P2: 生成增强版章节
     */
    fun generateEnhancedChapter(
        novel: Novel,
        chapterNum: Int,
        userGuide: String?,
        onStageComplete: (ChapterPhase, String) -> Unit = { _, _ -> }
    ): String {
        val previousContent = novel.chapters.lastOrNull()?.content
        
        return buildString {
            // P2: 添加节奏提示
            appendLine("=== 节奏控制 ===")
            append(rhythmController.generateRhythmHint(novel, chapterNum))
            appendLine()
            
            // P3: 添加一致性信息
            appendLine("=== 一致性检查 ===")
            append(consistencyManager.generateConsistencyHint(novel))
            appendLine()
            
            // P1: 添加场景检测结果
            if (previousContent != null) {
                appendLine("=== 场景检测 ===")
                val sceneType = sceneDetector.detectSceneType(previousContent)
                val intensity = sceneDetector.getSceneIntensity(previousContent, novel.sensoryProfile)
                appendLine("当前场景类型：${sceneType.name}")
                appendLine("场景强度：$intensity/10")
                appendLine()
            }
        }
    }
    
    // ========== P4: 角色档案管理 ==========
    
    fun createCharacterBodyProfile(
        characterId: String,
        characterName: String,
        height: String = "",
        build: String = "",
        skinTone: String = ""
    ): CharacterBodyProfile {
        return CharacterBodyProfile(
            characterId = characterId,
            characterName = characterName,
            bodyFeatures = BodyFeatures(
                height = height,
                build = build,
                skinTone = skinTone
            )
        )
    }
    
    fun addCharacterBodyProfile(novel: Novel, profile: CharacterBodyProfile): Novel {
        val existingIndex = novel.characterBodyProfiles.indexOfFirst { 
            it.characterId == profile.characterId 
        }
        
        if (existingIndex >= 0) {
            novel.characterBodyProfiles[existingIndex] = profile
        } else {
            novel.characterBodyProfiles.add(profile)
        }
        
        viewModelScope.launch {
            localStorage.saveNovel(novel)
        }
        
        _currentNovel.value = novel
        return novel
    }
    
    fun updateSensoryProfile(novel: Novel, profile: SensoryProfile): Novel {
        _currentNovel.value = novel.copy(sensoryProfile = profile)
        viewModelScope.launch {
            localStorage.saveNovel(novel.copy(sensoryProfile = profile))
        }
        return novel.copy(sensoryProfile = profile)
    }
    
    fun updateGenerationConfig(novel: Novel, config: GenerationConfig): Novel {
        _currentNovel.value = novel.copy(generationConfig = config)
        viewModelScope.launch {
            localStorage.saveNovel(novel.copy(generationConfig = config))
        }
        return novel.copy(generationConfig = config)
    }
    
    // ========== P3: 关系状态管理 ==========
    
    fun updateRelationship(
        novel: Novel,
        characterId1: String,
        characterId2: String,
        eventType: RelationshipEventType,
        description: String,
        chapterNum: Int
    ): Novel {
        val updatedNovel = consistencyManager.updateRelationshipState(
            novel, characterId1, characterId2, eventType, description, chapterNum
        )
        
        viewModelScope.launch {
            localStorage.saveNovel(updatedNovel)
        }
        
        _currentNovel.value = updatedNovel
        return updatedNovel
    }
    
    // ========== 原有功能 ==========
    
    init {
        loadApiConfig()
        loadNovels()
    }
    
    private fun loadApiConfig() {
        viewModelScope.launch {
            localStorage.apiConfig.collect { config ->
                _uiState.value = _uiState.value.copy(apiConfig = config)
            }
        }
    }
    
    private fun loadNovels() {
        viewModelScope.launch {
            localStorage.novels.collect { novelList ->
                _novels.value = novelList
            }
        }
    }
    
    fun updateApiKey(apiKey: String) {
        viewModelScope.launch {
            val config = _uiState.value.apiConfig.copy(apiKey = apiKey)
            localStorage.saveApiConfig(config)
        }
    }
    
    fun updateEndpoint(endpoint: String) {
        viewModelScope.launch {
            val config = _uiState.value.apiConfig.copy(endpoint = endpoint)
            localStorage.saveApiConfig(config)
        }
    }
    
    fun updateModel(model: String) {
        viewModelScope.launch {
            val config = _uiState.value.apiConfig.copy(model = model)
            localStorage.saveApiConfig(config)
        }
    }
    
    fun toggleDarkMode() {
        viewModelScope.launch {
            val config = _uiState.value.apiConfig.copy(isDarkMode = !_uiState.value.apiConfig.isDarkMode)
            localStorage.saveApiConfig(config)
        }
    }
    
    fun createNovel(title: String, type: String, style: String, mainCharacter: String) {
        viewModelScope.launch {
            val novel = Novel(
                title = title,
                type = type,
                style = style,
                mainCharacter = mainCharacter
            )
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
            _uiState.value = _uiState.value.copy(currentScreen = Screen.NovelDetail)
        }
    }
    
    fun selectNovel(novelId: String) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId)
            _currentNovel.value = novel
        }
    }
    
    fun deleteNovel(novelId: String) {
        viewModelScope.launch {
            localStorage.deleteNovel(novelId)
        }
    }
    
    fun updateNovelOutline(novelId: String, outline: String) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId)
            novel?.let {
                val updated = it.copy(outline = outline, updatedAt = System.currentTimeMillis())
                localStorage.saveNovel(updated)
                _currentNovel.value = updated
            }
        }
    }
    
    fun navigateTo(screen: Screen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }
    
    fun addCharacter(novelId: String, character: Character) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId)
            novel?.let {
                it.characters.add(character)
                // P4: 同时创建身体档案
                val bodyProfile = createCharacterBodyProfile(
                    characterId = character.id,
                    characterName = character.name,
                    height = "", // 可以从character.appearance中解析
                    build = "",
                    skinTone = ""
                )
                it.characterBodyProfiles.add(bodyProfile)
                localStorage.saveNovel(it)
                _currentNovel.value = it
            }
        }
    }
    
    fun addChapter(novelId: String, chapterTitle: String) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId)
            novel?.let {
                val chapter = Chapter(
                    title = chapterTitle,
                    content = "",
                    order = it.chapters.size
                )
                it.chapters.add(chapter)
                localStorage.saveNovel(it)
                _currentNovel.value = it
            }
        }
    }
    
    fun deleteChapter(novelId: String, chapterId: String) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId)
            novel?.let {
                it.chapters.removeAll { c -> c.id == chapterId }
                localStorage.saveNovel(it)
                _currentNovel.value = it
            }
        }
    }
    
    fun generateChapter(novelId: String, chapterTitle: String) {
        val novel = _currentNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        if (config.apiKey.isBlank()) {
            _errorMessage.value = "请先配置API密钥"
            return
        }
        
        generationJob = viewModelScope.launch {
            try {
                _isGenerating.value = true
                _streamingContent.value = ""
                
                val chapterNum = novel.chapters.size + 1
                val previousContent = novel.chapters.lastOrNull()?.content
                
                // P2: 生成增强信息
                val enhancement = generateEnhancedChapter(novel, chapterNum, null)
                
                var chapterError: String? = null
                
                apiService.generateContent(
                    apiKey = config.apiKey,
                    endpoint = config.endpoint,
                    model = config.model,
                    systemPrompt = buildChapterSystemPrompt(novel, chapterNum, enhancement),
                    userPrompt = buildChapterUserPrompt(novel, chapterTitle),
                    temperature = calculateTemperature(chapterNum, novel.keyNodes.size)
                ).collect { content ->
                    if (content.startsWith("[ERROR]")) {
                        chapterError = content
                    } else {
                        // P2: 实时检测场景
                        val enhancedContent = detectAndEnhance(content, novel)
                        _streamingContent.value = enhancedContent
                    }
                }
                
                _isGenerating.value = false
                
                if (!chapterError.isNullOrEmpty()) {
                    _errorMessage.value = "章节生成失败: $chapterError"
                    return@launch
                }
                
                val chapterContent = _streamingContent.value
                if (chapterContent.isNotBlank()) {
                    val chapter = Chapter(
                        title = chapterTitle,
                        content = chapterContent,
                        order = novel.chapters.size,
                        wordCount = chapterContent.length
                    )
                    novel.chapters.add(chapter)
                    localStorage.saveNovel(novel)
                    _currentNovel.value = novel
                } else {
                    _errorMessage.value = "章节内容为空，请重试"
                }
            } catch (e: Exception) {
                _isGenerating.value = false
                _errorMessage.value = "生成失败: ${e.message}"
            }
        }
    }
    
    fun stopGeneration() {
        generationJob?.cancel()
        _isGenerating.value = false
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    // ========== 全自动模式 ==========
    
    fun startAutoMode(userPrompt: String) {
        val config = _uiState.value.apiConfig
        
        if (config.apiKey.isBlank()) {
            _errorMessage.value = "请先配置API密钥"
            return
        }
        
        generationJob = viewModelScope.launch {
            try {
                _autoModeState.value = AutoModeState.GENERATING_OUTLINE
                _isGenerating.value = true
                
                val outlinePrompt = """
                    请根据以下设定，生成小说的完整大纲：

                    设定：$userPrompt

                    请以JSON格式输出，字段包括：
                    - title: 小说标题
                    - type: 小说类型（玄幻、都市、科幻等）
                    - style: 写作风格（热血、轻松、虐心等）
                    - mainCharacter: 主角简介
                    - outline: 详细大纲（8-12个关键节点）
                    - worldBackground: 世界背景
                    - powerSystem: 力量体系

                    确保大纲有起伏，有足够的情感线和剧情张力。
                """.trimIndent()
                
                var outlineResult = ""
                
                apiService.generateContent(
                    apiKey = config.apiKey,
                    endpoint = config.endpoint,
                    model = config.model,
                    systemPrompt = "你是一位资深网文作家，擅长创作高质量网络小说。请严格按照JSON格式输出。",
                    userPrompt = outlinePrompt,
                    temperature = 0.85f
                ).collect { content ->
                    if (!content.startsWith("[ERROR]")) {
                        outlineResult += content
                    }
                }
                
                _isGenerating.value = false
                
                if (outlineResult.isBlank()) {
                    _errorMessage.value = "大纲生成失败，请检查网络连接"
                    _autoModeState.value = AutoModeState.IDLE
                    return@launch
                }
                
                val outlineText = extractField(outlineResult, "outline") ?: outlineResult
                val keyNodes = parseKeyNodesFromOutline(outlineText)
                
                // P1: 使用默认的中度模板
                val defaultTemplate = PromptTemplates.getTemplate(TabooLevel.MODERATE)
                
                val novel = Novel(
                    title = extractField(outlineResult, "title") ?: "未命名小说",
                    type = extractField(outlineResult, "type") ?: "玄幻",
                    style = extractField(outlineResult, "style") ?: "热血",
                    mainCharacter = extractField(outlineResult, "mainCharacter") ?: "主角",
                    outline = outlineText,
                    worldBuilding = WorldBuilding(
                        worldBackground = extractField(outlineResult, "worldBackground") ?: "",
                        powerSystem = extractField(outlineResult, "powerSystem") ?: ""
                    ),
                    keyNodes = keyNodes.toMutableList(),
                    // P1: 初始化Prompt模板
                    promptTemplate = defaultTemplate
                )
                
                _autoModeNovel.value = novel
                _autoModeState.value = AutoModeState.REVIEW
                
            } catch (e: Exception) {
                _isGenerating.value = false
                _errorMessage.value = "生成失败: ${e.message}"
                _autoModeState.value = AutoModeState.IDLE
            }
        }
    }
    
    fun updateAutoModeNovel(
        title: String? = null,
        type: String? = null,
        style: String? = null,
        mainCharacter: String? = null,
        outline: String? = null,
        worldBackground: String? = null,
        powerSystem: String? = null
    ) {
        val novel = _autoModeNovel.value ?: return
        _autoModeNovel.value = novel.copy(
            title = title ?: novel.title,
            type = type ?: novel.type,
            style = style ?: novel.style,
            mainCharacter = mainCharacter ?: novel.mainCharacter,
            outline = outline ?: novel.outline,
            worldBuilding = novel.worldBuilding.copy(
                worldBackground = worldBackground ?: novel.worldBuilding.worldBackground,
                powerSystem = powerSystem ?: novel.worldBuilding.powerSystem
            )
        )
    }
    
    // P1: 更新小说的强度等级
    fun updateAutoModeTabooLevel(level: TabooLevel) {
        val novel = _autoModeNovel.value ?: return
        val template = PromptTemplates.getTemplate(level)
        _autoModeNovel.value = novel.copy(
            sensoryProfile = novel.sensoryProfile.copy(tabooLevel = level),
            promptTemplate = template
        )
    }
    
    // P2: 更新描写密度
    fun updateAutoModeDescriptionDensity(density: Int) {
        val novel = _autoModeNovel.value ?: return
        _autoModeNovel.value = novel.copy(
            sensoryProfile = novel.sensoryProfile.copy(descriptionDensity = density.coerceIn(1, 10))
        )
    }
    
    // P2: 更新节奏偏好
    fun updateAutoModeRhythmPreference(preference: RhythmPreference) {
        val novel = _autoModeNovel.value ?: return
        _autoModeNovel.value = novel.copy(
            generationConfig = novel.generationConfig.copy(rhythmPreference = preference)
        )
    }
    
    fun confirmAndStartWriting() {
        val novel = _autoModeNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        if (config.apiKey.isBlank()) {
            _errorMessage.value = "请先配置API密钥"
            return
        }
        
        generationJob = viewModelScope.launch {
            try {
                localStorage.saveNovel(novel)
                _currentNovel.value = novel
                
                _autoModeState.value = AutoModeState.GENERATING_CHAPTER
                _isGenerating.value = true
                _streamingContent.value = ""
                
                var chapterError: String? = null
                
                val chapterPrompt = buildAutoChapterPrompt(novel, 1, "")
                
                // P2: 计算节奏感知的temperature
                val temperature = calculateTemperature(1, novel.keyNodes.size)
                
                apiService.generateContent(
                    apiKey = config.apiKey,
                    endpoint = config.endpoint,
                    model = config.model,
                    systemPrompt = buildAutoChapterSystemPrompt(novel, 1),
                    userPrompt = chapterPrompt,
                    temperature = temperature
                ).collect { content ->
                    if (content.startsWith("[ERROR]")) {
                        chapterError = content
                    } else {
                        _streamingContent.value += content
                    }
                }
                
                _isGenerating.value = false
                
                if (!chapterError.isNullOrEmpty()) {
                    _errorMessage.value = "章节生成失败: $chapterError"
                    return@launch
                }
                
                val chapterContent = _streamingContent.value
                if (chapterContent.isNotBlank()) {
                    val chapter = Chapter(
                        title = "第一章",
                        content = chapterContent,
                        order = 0,
                        wordCount = chapterContent.length
                    )
                    novel.chapters.add(chapter)
                    localStorage.saveNovel(novel)
                    _currentNovel.value = novel
                } else {
                    _errorMessage.value = "章节内容为空，请重试"
                }
            } catch (e: Exception) {
                _isGenerating.value = false
                _errorMessage.value = "生成失败: ${e.message}"
            }
        }
    }
    
    fun continueAutoMode(guide: String) {
        val novel = _currentNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        generationJob = viewModelScope.launch {
            try {
                _autoModeState.value = AutoModeState.GENERATING_CHAPTER
                _isGenerating.value = true
                _streamingContent.value = ""
                
                val chapterNum = novel.chapters.size + 1
                val lastChapter = novel.chapters.lastOrNull()
                
                // P2: 生成章节增强信息
                val enhancement = generateEnhancedChapter(novel, chapterNum, guide)
                
                val prompt = buildString {
                    appendLine("请创作第${chapterNum}章。")
                    appendLine()
                    appendLine("用户引导：${guide.ifBlank { "继续推进故事" }}")
                    appendLine()
                    lastChapter?.let {
                        appendLine("上一章结尾：")
                        appendLine(it.content.takeLast(500))
                    }
                    appendLine()
                    appendLine("=== 章节增强信息 ===")
                    append(enhancement)
                }
                
                var chapterError: String? = null
                
                // P2: 计算节奏感知的temperature
                val temperature = calculateTemperature(chapterNum, novel.keyNodes.size)
                
                apiService.generateContent(
                    apiKey = config.apiKey,
                    endpoint = config.endpoint,
                    model = config.model,
                    systemPrompt = buildAutoChapterSystemPrompt(novel, chapterNum),
                    userPrompt = prompt,
                    temperature = temperature
                ).collect { content ->
                    if (content.startsWith("[ERROR]")) {
                        chapterError = content
                    } else {
                        // P2: 实时场景检测增强
                        val enhancedContent = detectAndEnhance(content, novel)
                        _streamingContent.value = enhancedContent
                    }
                }
                
                _isGenerating.value = false
                
                if (!chapterError.isNullOrEmpty()) {
                    _errorMessage.value = "章节生成失败: $chapterError"
                    return@launch
                }
                
                val chapterContent = _streamingContent.value
                if (chapterContent.isNotBlank()) {
                    val chapter = Chapter(
                        title = "第${chapterNum}章",
                        content = chapterContent,
                        order = novel.chapters.size,
                        wordCount = chapterContent.length
                    )
                    novel.chapters.add(chapter)
                    localStorage.saveNovel(novel)
                    _currentNovel.value = novel
                } else {
                    _errorMessage.value = "章节内容为空，请重试"
                }
            } catch (e: Exception) {
                _isGenerating.value = false
                _errorMessage.value = "生成失败: ${e.message}"
            }
        }
    }
    
    fun finishAutoMode() {
        _autoModeState.value = AutoModeState.COMPLETED
    }
    
    fun resetAutoMode() {
        _autoModeState.value = AutoModeState.IDLE
        _autoModeNovel.value = null
        _streamingContent.value = ""
    }
    
    fun retryAutoMode() {
        _autoModeState.value = AutoModeState.REVIEW
        _isGenerating.value = false
        _streamingContent.value = ""
    }
    
    private fun extractField(text: String, field: String): String? {
        val pattern = """"$field"\s*:\s*"([^"]+)"""".toRegex()
        return pattern.find(text)?.groupValues?.getOrNull(1)
    }
    
    private fun buildChapterSystemPrompt(novel: Novel, chapterNum: Int, enhancement: String): String {
        val progressInfo = novel.getProgressInfo(chapterNum)
        val unresolvedForeshadowings = novel.getUnresolvedForeshadowings()
        val isConvergence = novel.isConvergenceMode()
        val temperature = calculateTemperature(chapterNum, novel.keyNodes.size)
        
        // P1: 获取Prompt模板
        val template = PromptTemplates.getTemplate(novel.sensoryProfile.tabooLevel)
        
        // P2: 获取节奏提示
        val rhythmHint = rhythmController.generateRhythmHint(novel, chapterNum)
        
        // P3: 获取一致性信息
        val consistencyHint = consistencyManager.injectMemories(novel, chapterNum)
        
        return buildString {
            appendLine("你是一位资深的中文网络小说作家，正在创作《${novel.title}》。")
            appendLine()
            appendLine("小说类型：${novel.type}")
            appendLine("写作风格：${novel.style}")
            appendLine()
            
            // P1: 使用模板的基础Prompt
            appendLine("=== 写作风格指导 ===")
            appendLine(template.basePrompt)
            appendLine()
            
            appendLine("世界背景：")
            appendLine(novel.worldBuilding.worldBackground)
            appendLine()
            
            appendLine("力量体系：")
            appendLine(novel.worldBuilding.powerSystem)
            appendLine()
            
            appendLine("大纲：")
            appendLine(novel.outline)
            appendLine()
            
            // P2: 添加节奏控制
            appendLine("=== 节奏控制 ===")
            appendLine(rhythmHint)
            appendLine()
            
            // P3: 添加一致性检查
            appendLine("=== 一致性要求 ===")
            appendLine(consistencyHint)
            appendLine()
            
            // P0: 进度信息
            if (novel.keyNodes.isNotEmpty()) {
                appendLine("【当前进度】")
                appendLine(progressInfo.toModelHint())
                appendLine(progressInfo.toSensoryHint())
                appendLine()
            }
            
            // P0: 伏笔提示
            if (unresolvedForeshadowings.isNotEmpty()) {
                appendLine("【未回收伏笔（需择机回收）】")
                unresolvedForeshadowings.take(5).forEachIndexed { i, f ->
                    appendLine("${i + 1}. ${f.content}（第${f.plantedChapter}章埋下）")
                }
                if (isConvergence) {
                    appendLine()
                    appendLine("⚠️ 收束模式：必须在本章回收至少一条伏笔！")
                }
                appendLine()
            }
            
            appendLine(getAntiAIWritingHints())
            appendLine()
            
            appendLine("【章节要求】")
            appendLine("1. 纯中文写作")
            appendLine("2. 文笔流畅，引人入胜")
            appendLine("3. 每章3000-5000字")
            appendLine("4. 结尾留悬念")
            if (isConvergence) {
                appendLine("5. 【重要】本章必须推进主线结局，回收至少一条伏笔")
            }
        }.trimIndent()
    }
    
    private fun buildChapterUserPrompt(novel: Novel, chapterTitle: String): String {
        val previousContent = novel.chapters.lastOrNull()?.content ?: ""
        val characters = novel.characters.take(5).joinToString("\n") { 
            "- ${it.name}（${it.role}）：${it.description}" 
        }
        
        return """
            请创作章节：$chapterTitle
            
            ${if (characters.isNotBlank()) "主要角色：\n$characters" else ""}
            
            ${if (previousContent.isNotBlank()) "上一章结尾：\n${previousContent.takeLast(500)}" else ""}
            
            请继续创作，保持风格一致。
        """.trimIndent()
    }
    
    private fun buildAutoChapterSystemPrompt(novel: Novel, chapterNum: Int = 1): String {
        val progressInfo = novel.getProgressInfo(chapterNum)
        val unresolvedForeshadowings = novel.getUnresolvedForeshadowings()
        val isConvergence = novel.isConvergenceMode()
        val temperature = calculateTemperature(chapterNum, novel.keyNodes.size)
        
        // P1: 获取Prompt模板
        val template = PromptTemplates.getTemplate(novel.sensoryProfile.tabooLevel)
        
        // P2: 获取节奏提示
        val rhythmHint = rhythmController.generateRhythmHint(novel, chapterNum)
        
        // P3: 获取一致性信息
        val consistencyHint = consistencyManager.injectMemories(novel, chapterNum)
        
        // P2: 计算目标字数
        val targetWordCount = rhythmController.calculateTargetWordCount(novel, chapterNum)
        
        return buildString {
            appendLine("你是一位资深的中文网络小说作家，正在创作《${novel.title}》。")
            appendLine()
            
            // P1: 写作风格指导
            appendLine("=== 写作风格指导 ===")
            appendLine(template.basePrompt)
            appendLine()
            
            appendLine("小说类型：${novel.type}")
            appendLine("写作风格：${novel.style}")
            appendLine()
            
            appendLine("世界背景：")
            appendLine(novel.worldBuilding.worldBackground)
            appendLine()
            
            appendLine("力量体系：")
            appendLine(novel.worldBuilding.powerSystem)
            appendLine()
            
            appendLine("大纲：")
            appendLine(novel.outline)
            appendLine()
            
            // P2: 节奏控制
            appendLine("=== 节奏控制 ===")
            appendLine(rhythmHint)
            appendLine()
            
            // P3: 一致性要求
            appendLine("=== 一致性要求 ===")
            appendLine(consistencyHint)
            appendLine()
            
            // P0: 进度信息
            if (novel.keyNodes.isNotEmpty()) {
                appendLine("【当前进度】")
                appendLine(progressInfo.toModelHint())
                appendLine(progressInfo.toSensoryHint())
                appendLine("目标字数：约${targetWordCount}字")
                appendLine()
            }
            
            // P0: 伏笔提示
            if (unresolvedForeshadowings.isNotEmpty()) {
                appendLine("【未回收伏笔（需择机回收）】")
                unresolvedForeshadowings.take(5).forEachIndexed { i, f ->
                    appendLine("${i + 1}. ${f.content}（第${f.plantedChapter}章埋下）")
                }
                if (isConvergence) {
                    appendLine()
                    appendLine("⚠️ 收束模式：必须在本章回收至少一条伏笔！")
                }
                appendLine()
            }
            
            appendLine(getAntiAIWritingHints())
            appendLine()
            
            appendLine("【章节要求】")
            appendLine("1. 纯中文写作")
            appendLine("2. 文笔流畅，引人入胜")
            appendLine("3. 每章约${targetWordCount}字")
            appendLine("4. 结尾留悬念")
            if (isConvergence) {
                appendLine("5. 【重要】本章必须推进主线结局，回收至少一条伏笔")
            }
        }.trimIndent()
    }
    
    private fun buildAutoChapterPrompt(novel: Novel, chapterNum: Int, guide: String): String {
        return """
            请创作第${chapterNum}章。
            
            ${if (guide.isNotBlank()) "引导：$guide" else "这是开篇，请精彩地引入故事。"}
            
            主角设定：${novel.mainCharacter}
            
            请创作这一章，字数约3000-5000字。
        """.trimIndent()
    }
}

data class XSGrokUiState(
    val apiConfig: ApiConfig = ApiConfig(),
    val currentScreen: Screen = Screen.Home
)

enum class Screen {
    Home, Settings, NewNovel, NovelDetail, Characters, Drafts,
    ChapterGeneration, AutoMode, Bookshelf, Reading, WorldBuilding
}
