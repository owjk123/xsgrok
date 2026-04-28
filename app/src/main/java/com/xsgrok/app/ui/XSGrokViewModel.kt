package com.xsgrok.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xsgrok.app.data.local.LocalStorage
import com.xsgrok.app.data.model.*
import com.xsgrok.app.data.remote.ApiService
import com.xsgrok.app.ui.screens.AutoModeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class XSGrokViewModel(application: Application) : AndroidViewModel(application) {
    
    private val localStorage = LocalStorage(application)
    private val apiService = ApiService()
    
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
    
    private var generationJob: Job? = null
    
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
    
    // ========== API 配置 ==========
    
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
    
    // ========== 小说管理 ==========
    
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
            if (_currentNovel.value?.id == novelId) {
                _currentNovel.value = null
            }
        }
    }
    
    fun navigateTo(screen: Screen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    // ========== 章节生成 ==========
    
    fun generateChapter(chapterTitle: String) {
        val novel = _currentNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        if (config.apiKey.isBlank()) {
            _errorMessage.value = "请先配置API密钥"
            return
        }
        
        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            _streamingContent.value = ""
            
            val systemPrompt = buildChapterSystemPrompt(novel)
            val userPrompt = buildChapterUserPrompt(novel, chapterTitle)
            
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt
            ).collect { content ->
                if (content.startsWith("[ERROR]")) {
                    _errorMessage.value = content
                } else {
                    _streamingContent.value += content
                }
            }
            
            _isGenerating.value = false
            
            val newContent = _streamingContent.value
            if (newContent.isNotBlank()) {
                val chapter = Chapter(
                    title = chapterTitle,
                    content = newContent,
                    order = novel.chapters.size,
                    wordCount = newContent.length
                )
                novel.chapters.add(chapter)
                localStorage.saveNovel(novel)
                _currentNovel.value = novel
            }
        }
    }
    
    fun continueChapter() {
        val novel = _currentNovel.value ?: return
        val lastChapter = novel.chapters.lastOrNull() ?: return
        val config = _uiState.value.apiConfig
        
        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = "继续写小说《${novel.title}》，保持风格一致",
                userPrompt = "请继续以下内容：\n\n${lastChapter.content.takeLast(1000)}"
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    _streamingContent.value += content
                }
            }
            
            _isGenerating.value = false
            
            val newContent = _streamingContent.value
            if (newContent.isNotBlank()) {
                val chapterIndex = novel.chapters.indexOfLast { it.id == lastChapter.id }
                if (chapterIndex >= 0) {
                    novel.chapters[chapterIndex] = lastChapter.copy(content = newContent)
                    localStorage.saveNovel(novel)
                    _currentNovel.value = novel
                }
            }
        }
    }
    
    fun stopGeneration() {
        generationJob?.cancel()
        _isGenerating.value = false
    }
    
    // ========== 角色管理 ==========
    
    fun addCharacter(name: String, description: String, role: String) {
        addCharacterFull(name, description, role, "", "", "", "")
    }
    
    fun addCharacterFull(
        name: String, 
        description: String, 
        role: String,
        appearance: String,
        personality: String,
        background: String,
        abilities: String
    ) {
        val novel = _currentNovel.value ?: return
        val character = Character(
            name = name,
            description = description,
            role = role,
            appearance = appearance,
            personality = personality,
            background = background,
            abilities = abilities
        )
        novel.characters.add(character)
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    fun deleteCharacter(characterId: String) {
        val novel = _currentNovel.value ?: return
        novel.characters.removeAll { it.id == characterId }
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    fun generateCharacters() {
        val novel = _currentNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        if (config.apiKey.isBlank()) {
            _errorMessage.value = "请先配置API密钥"
            return
        }
        
        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            
            val prompt = """
                为小说《${novel.title}》设计角色。
                
                小说类型：${novel.type}
                风格：${novel.style}
                主角：${novel.mainCharacter}
                
                请生成3-5个角色，每个角色一行：
                角色名|角色定位|角色描述
            """.trimIndent()
            
            var result = ""
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = "你是专业的小说角色设计师",
                userPrompt = prompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    result += content
                }
            }
            
            result.lines().filter { it.contains("|") }.forEach { line ->
                val parts = line.split("|")
                if (parts.size >= 3) {
                    novel.characters.add(Character(
                        name = parts[0].trim(),
                        role = parts[1].trim(),
                        description = parts[2].trim()
                    ))
                }
            }
            
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
            _isGenerating.value = false
        }
    }
    
    // ========== 世界观管理 ==========
    
    fun updateWorldBackground(background: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding = novel.worldBuilding.copy(worldBackground = background)
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    fun updatePowerSystem(system: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding = novel.worldBuilding.copy(powerSystem = system)
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    fun addLocation(name: String, description: String, type: String = "", significance: String = "") {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.geography.add(Location(
            name = name,
            description = description,
            type = type,
            significance = significance
        ))
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    fun deleteLocation(locationId: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.geography.removeAll { it.id == locationId }
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    fun addFaction(name: String, description: String, leader: String = "", goals: String = "") {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.factions.add(Faction(
            name = name,
            description = description,
            leader = leader,
            goals = goals
        ))
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    fun deleteFaction(factionId: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.factions.removeAll { it.id == factionId }
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    fun addItem(name: String, description: String, type: String = "", abilities: String = "") {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.items.add(GameItem(
            name = name,
            description = description,
            type = type,
            abilities = abilities
        ))
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    fun deleteItem(itemId: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.items.removeAll { it.id == itemId }
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    fun addSkill(name: String, description: String, type: String = "", requirements: String = "") {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.skills.add(Skill(
            name = name,
            description = description,
            type = type,
            requirements = requirements
        ))
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    fun deleteSkill(skillId: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.skills.removeAll { it.id == skillId }
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    fun addTimelineEvent(title: String, description: String, time: String = "") {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.timeline.add(TimelineEvent(
            title = title,
            description = description,
            time = time
        ))
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    fun deleteTimelineEvent(eventId: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.timeline.removeAll { it.id == eventId }
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    // ========== AI生成功能 ==========
    
    fun generateWorldBackground() {
        val novel = _currentNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        if (config.apiKey.isBlank()) {
            _errorMessage.value = "请先配置API密钥"
            return
        }
        
        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            
            val prompt = """
                为小说《${novel.title}》设计一个完整的世界观背景。
                
                小说类型：${novel.type}
                风格：${novel.style}
                主角：${novel.mainCharacter}
                
                请详细描述：
                1. 世界的基本设定和历史
                2. 世界的地理环境
                3. 社会结构和文化
                4. 重要势力和种族
                
                请用中文回答，详细且有创意。
            """.trimIndent()
            
            var result = ""
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = "你是专业的小说世界观设计师",
                userPrompt = prompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    result += content
                }
            }
            
            if (result.isNotBlank()) {
                novel.worldBuilding = novel.worldBuilding.copy(worldBackground = result)
                localStorage.saveNovel(novel)
                _currentNovel.value = novel
            }
            _isGenerating.value = false
        }
    }
    
    fun generatePowerSystem() {
        val novel = _currentNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        if (config.apiKey.isBlank()) {
            _errorMessage.value = "请先配置API密钥"
            return
        }
        
        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            
            val prompt = """
                为小说《${novel.title}》设计力量体系。
                
                世界背景：${novel.worldBuilding.worldBackground}
                
                请设计：
                1. 力量来源和修炼方式
                2. 等级划分和名称
                3. 各类能力的特点和限制
                4. 突破条件的设定
                
                请用中文回答，有创意且逻辑自洽。
            """.trimIndent()
            
            var result = ""
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = "你是专业的小说设定设计师",
                userPrompt = prompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    result += content
                }
            }
            
            if (result.isNotBlank()) {
                novel.worldBuilding = novel.worldBuilding.copy(powerSystem = result)
                localStorage.saveNovel(novel)
                _currentNovel.value = novel
            }
            _isGenerating.value = false
        }
    }
    
    fun generateLocations() {
        val novel = _currentNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        if (config.apiKey.isBlank()) {
            _errorMessage.value = "请先配置API密钥"
            return
        }
        
        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            
            val prompt = """
                为小说《${novel.title}》生成地点/场景。
                
                世界背景：${novel.worldBuilding.worldBackground}
                力量体系：${novel.worldBuilding.powerSystem}
                
                请生成3-5个地点，每个一行：
                地点名|地点类型|地点描述|重要程度
            """.trimIndent()
            
            var result = ""
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = "你是专业的小说场景设计师",
                userPrompt = prompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    result += content
                }
            }
            
            result.lines().filter { it.contains("|") }.forEach { line ->
                val parts = line.split("|")
                if (parts.size >= 3) {
                    novel.worldBuilding.geography.add(Location(
                        name = parts[0].trim(),
                        type = parts.getOrNull(1)?.trim() ?: "",
                        description = parts.getOrNull(2)?.trim() ?: "",
                        significance = parts.getOrNull(3)?.trim() ?: ""
                    ))
                }
            }
            
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
            _isGenerating.value = false
        }
    }
    
    fun generateFactions() {
        val novel = _currentNovel.value ?: return
        generateSimpleItems("势力/门派") { name, desc ->
            novel.worldBuilding.factions.add(Faction(name = name, description = desc))
        }
    }
    
    fun generateItems() {
        val novel = _currentNovel.value ?: return
        generateSimpleItems("物品/装备") { name, desc ->
            novel.worldBuilding.items.add(GameItem(name = name, description = desc))
        }
    }
    
    fun generateSkills() {
        val novel = _currentNovel.value ?: return
        generateSimpleItems("技能/功法") { name, desc ->
            novel.worldBuilding.skills.add(Skill(name = name, description = desc))
        }
    }
    
    fun generateTimeline() {
        val novel = _currentNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            
            val prompt = """
                根据小说《${novel.title}》的大纲，生成故事时间线。
                
                大纲：${novel.outline}
                
                请生成5-10个关键事件，每个事件一行：
                时间|事件标题|事件描述
            """.trimIndent()
            
            var result = ""
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = "你是一个专业的小说剧情设计师",
                userPrompt = prompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    result += content
                }
            }
            
            result.lines().filter { it.contains("|") }.forEach { line ->
                val parts = line.split("|")
                if (parts.size >= 2) {
                    novel.worldBuilding.timeline.add(TimelineEvent(
                        time = parts.getOrNull(0)?.trim() ?: "",
                        title = parts.getOrNull(1)?.trim() ?: "",
                        description = parts.getOrNull(2)?.trim() ?: ""
                    ))
                }
            }
            
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
            _isGenerating.value = false
        }
    }
    
    private fun generateSimpleItems(typeName: String, adder: Novel.(String, String) -> Unit) {
        val novel = _currentNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            
            val prompt = """
                为小说《${novel.title}》生成${typeName}。
                
                世界背景：${novel.worldBuilding.worldBackground}
                力量体系：${novel.worldBuilding.powerSystem}
                
                请生成3-5个${typeName}，每个一行：
                名称|描述
            """.trimIndent()
            
            var result = ""
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = "你是专业的小说设定设计师",
                userPrompt = prompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    result += content
                }
            }
            
            result.lines().filter { it.contains("|") }.forEach { line ->
                val parts = line.split("|")
                if (parts.size >= 2) {
                    novel.adder(parts[0].trim(), parts.getOrNull(1)?.trim() ?: "")
                }
            }
            
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
            _isGenerating.value = false
        }
    }
    
    // ========== 全自动模式 ==========
    
    fun startAutoMode(userPrompt: String) {
        val config = _uiState.value.apiConfig
        
        if (config.apiKey.isBlank()) {
            _errorMessage.value = "请先配置API密钥"
            return
        }
        
        generationJob = viewModelScope.launch {
            _autoModeState.value = AutoModeState.GENERATING_OUTLINE
            
            // 步骤1: 生成小说信息和大纲
            var outlineResult = ""
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = "你是一个专业的小说创作顾问",
                userPrompt = """
                    用户想写：$userPrompt
                    
                    请生成完整的小说设定：
                    1. 小说标题
                    2. 类型（玄幻/都市/科幻/悬疑等）
                    3. 风格（热血/轻松/黑暗等）
                    4. 主角设定
                    5. 详细大纲（500字左右）
                    6. 世界背景
                    7. 力量体系
                    
                    请严格按JSON格式输出：
                    {
                        "title": "标题",
                        "type": "类型",
                        "style": "风格",
                        "mainCharacter": "主角设定",
                        "outline": "详细大纲",
                        "worldBackground": "世界背景",
                        "powerSystem": "力量体系"
                    }
                """.trimIndent()
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    outlineResult += content
                }
            }
            
            // 解析并创建小说
            val novel = Novel(
                title = extractField(outlineResult, "title") ?: "未命名小说",
                type = extractField(outlineResult, "type") ?: "玄幻",
                style = extractField(outlineResult, "style") ?: "热血",
                mainCharacter = extractField(outlineResult, "mainCharacter") ?: "主角",
                outline = extractField(outlineResult, "outline") ?: outlineResult,
                worldBuilding = WorldBuilding(
                    worldBackground = extractField(outlineResult, "worldBackground") ?: "",
                    powerSystem = extractField(outlineResult, "powerSystem") ?: ""
                )
            )
            
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
            
            // 步骤2: 生成第一章
            _autoModeState.value = AutoModeState.GENERATING_CHAPTER
            _isGenerating.value = true
            _streamingContent.value = ""
            
            val chapterPrompt = buildAutoChapterPrompt(novel, 1, "")
            
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = buildAutoChapterSystemPrompt(novel),
                userPrompt = chapterPrompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    _streamingContent.value += content
                }
            }
            
            _isGenerating.value = false
            
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
            }
        }
    }
    
    fun continueAutoMode(guide: String) {
        val novel = _currentNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        generationJob = viewModelScope.launch {
            _autoModeState.value = AutoModeState.GENERATING_CHAPTER
            _isGenerating.value = true
            _streamingContent.value = ""
            
            val chapterNum = novel.chapters.size + 1
            val lastChapter = novel.chapters.lastOrNull()
            
            val prompt = """
                请创作第${chapterNum}章。
                
                ${if (guide.isNotBlank()) "用户引导：$guide" else ""}
                
                ${if (lastChapter != null) "上一章结尾：\n${lastChapter.content.takeLast(500)}" else ""}
                
                请继续推进故事，保持风格一致。
            """.trimIndent()
            
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = buildAutoChapterSystemPrompt(novel),
                userPrompt = prompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    _streamingContent.value += content
                }
            }
            
            _isGenerating.value = false
            
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
            }
        }
    }
    
    fun finishAutoMode() {
        _autoModeState.value = AutoModeState.COMPLETED
    }
    
    fun resetAutoMode() {
        _autoModeState.value = AutoModeState.IDLE
    }
    
    // ========== 辅助方法 ==========
    
    private fun extractField(text: String, field: String): String? {
        val pattern = """"$field"\s*:\s*"([^"]+)"""".toRegex()
        return pattern.find(text)?.groupValues?.getOrNull(1)
    }
    
    private fun buildChapterSystemPrompt(novel: Novel): String {
        return """
            你是一位专业的中文小说作家。
            
            小说信息：
            - 标题：《${novel.title}》
            - 类型：${novel.type}
            - 风格：${novel.style}
            - 主角：${novel.mainCharacter}
            
            世界观：
            ${novel.worldBuilding.worldBackground}
            
            力量体系：
            ${novel.worldBuilding.powerSystem}
            
            写作要求：
            1. 使用中文
            2. 描写生动，情节紧凑
            3. 每章3000-5000字
        """.trimIndent()
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
    
    private fun buildAutoChapterSystemPrompt(novel: Novel): String {
        return """
            你是一位资深的中文网络小说作家，正在创作《${novel.title}》。
            
            小说类型：${novel.type}
            写作风格：${novel.style}
            
            世界背景：
            ${novel.worldBuilding.worldBackground}
            
            力量体系：
            ${novel.worldBuilding.powerSystem}
            
            大纲：
            ${novel.outline}
            
            写作要求：
            1. 纯中文写作
            2. 文笔流畅，引人入胜
            3. 每章3000-5000字
            4. 结尾留悬念
        """.trimIndent()
    }
    
    private fun buildAutoChapterPrompt(novel: Novel, chapterNum: Int, guide: String): String {
        return """
            请创作第${chapterNum}章。
            
            ${if (guide.isNotBlank()) "引导：$guide" else "这是开篇，请精彩地引入故事。"}
            
            主角设定：${novel.mainCharacter}
            
            请创作这一章，字数3000-5000字。
        """.trimIndent()
    }
}

data class XSGrokUiState(
    val apiConfig: ApiConfig = ApiConfig(),
    val currentScreen: Screen = Screen.Home
)

enum class Screen {
    Home,
    Settings,
    NewNovel,
    NovelDetail,
    Characters,
    Drafts,
    ChapterGeneration,
    AutoMode,
    Bookshelf,
    Reading,
    WorldBuilding
}
