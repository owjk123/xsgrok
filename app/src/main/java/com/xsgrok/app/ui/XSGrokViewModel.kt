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
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun updateCharacter(
        id: String,
        name: String,
        description: String,
        role: String,
        appearance: String,
        personality: String,
        background: String,
        abilities: String
    ) {
        val novel = _currentNovel.value ?: return
        val index = novel.characters.indexOfFirst { it.id == id }
        if (index >= 0) {
            novel.characters[index] = Character(
                id = id,
                name = name,
                description = description,
                role = role,
                appearance = appearance,
                personality = personality,
                background = background,
                abilities = abilities
            )
            viewModelScope.launch {
                localStorage.saveNovel(novel)
                _currentNovel.value = novel
            }
        }
    }
    
    fun deleteCharacter(characterId: String) {
        val novel = _currentNovel.value ?: return
        novel.characters.removeAll { it.id == characterId }
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    // ========== 世界观管理 ==========
    
    fun updateWorldBuilding(
        worldBackground: String? = null,
        powerSystem: String? = null,
        rules: String? = null
    ) {
        val novel = _currentNovel.value ?: return
        val wb = novel.worldBuilding
        val newWb = wb.copy(
            worldBackground = worldBackground ?: wb.worldBackground,
            powerSystem = powerSystem ?: wb.powerSystem,
            rules = rules ?: wb.rules
        )
        val newNovel = novel.copy(worldBuilding = newWb)
        viewModelScope.launch {
            localStorage.saveNovel(newNovel)
            _currentNovel.value = newNovel
        }
    }
    
    // 地点
    fun addLocation(name: String, description: String, type: String, significance: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.geography.add(Location(
            name = name,
            description = description,
            type = type,
            significance = significance
        ))
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun deleteLocation(id: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.geography.removeAll { it.id == id }
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    // 势力
    fun addFaction(name: String, description: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.factions.add(Faction(name = name, description = description))
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun deleteFaction(id: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.factions.removeAll { it.id == id }
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    // 物品
    fun addItem(name: String, description: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.items.add(GameItem(name = name, description = description))
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun deleteItem(id: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.items.removeAll { it.id == id }
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    // 技能
    fun addSkill(name: String, description: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.skills.add(Skill(name = name, description = description))
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun deleteSkill(id: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.skills.removeAll { it.id == id }
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    // 时间线
    fun addTimelineEvent(title: String, time: String, description: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.timeline.add(TimelineEvent(
            title = title,
            time = time,
            description = description
        ))
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun deleteTimelineEvent(id: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.timeline.removeAll { it.id == id }
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    // ========== AI 生成功能 ==========
    
    fun generateWorldBuilding() {
        val novel = _currentNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            
            val prompt = """
                为小说《${novel.title}》生成世界观设定。
                
                类型：${novel.type}
                风格：${novel.style}
                主角：${novel.mainCharacter}
                大纲：${novel.outline}
                
                请生成：
                1. 世界背景（200字以内）
                2. 力量体系（200字以内）
                3. 世界规则（100字以内）
                
                请严格按照以下JSON格式输出：
                {
                    "worldBackground": "世界背景描述",
                    "powerSystem": "力量体系描述",
                    "rules": "世界规则"
                }
            """.trimIndent()
            
            var result = ""
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = "你是一个专业的小说世界观设计师",
                userPrompt = prompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    result += content
                }
            }
            
            val wb = novel.worldBuilding.copy(
                worldBackground = extractField(result, "worldBackground") ?: novel.worldBuilding.worldBackground,
                powerSystem = extractField(result, "powerSystem") ?: novel.worldBuilding.powerSystem,
                rules = extractField(result, "rules") ?: novel.worldBuilding.rules
            )
            
            val newNovel = novel.copy(worldBuilding = wb)
            localStorage.saveNovel(newNovel)
            _currentNovel.value = newNovel
            _isGenerating.value = false
        }
    }
    
    fun generateCharacters() {
        val novel = _currentNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            
            val prompt = """
                为小说《${novel.title}》生成角色设定。
                
                类型：${novel.type}
                风格：${novel.style}
                主角：${novel.mainCharacter}
                世界背景：${novel.worldBuilding.worldBackground}
                
                请生成3-5个重要角色（包括主角的详细设定），每个角色包含：
                姓名、角色定位、外貌描述、性格特点、背景故事、能力特长
                
                按以下格式输出，每个角色一行：
                姓名|角色|外貌|性格|背景|能力
            """.trimIndent()
            
            var result = ""
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = "你是一个专业的小说角色设计师",
                userPrompt = prompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    result += content
                }
            }
            
            // 解析结果并添加角色
            result.lines().filter { it.contains("|") }.forEach { line ->
                val parts = line.split("|")
                if (parts.size >= 2) {
                    novel.characters.add(Character(
                        name = parts.getOrNull(0)?.trim() ?: "",
                        role = parts.getOrNull(1)?.trim() ?: "配角",
                        description = parts.getOrNull(2)?.trim() ?: "",
                        appearance = parts.getOrNull(2)?.trim() ?: "",
                        personality = parts.getOrNull(3)?.trim() ?: "",
                        background = parts.getOrNull(4)?.trim() ?: "",
                        abilities = parts.getOrNull(5)?.trim() ?: ""
                    ))
                }
            }
            
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
            _isGenerating.value = false
        }
    }
    
    fun generateLocations() {
        val novel = _currentNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            
            val prompt = """
                为小说《${novel.title}》生成重要地点。
                
                世界背景：${novel.worldBuilding.worldBackground}
                
                请生成3-5个重要地点，每个地点一行：
                地点名称|类型|描述|重要性
            """.trimIndent()
            
            var result = ""
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = "你是一个专业的小说场景设计师",
                userPrompt = prompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    result += content
                }
            }
            
            result.lines().filter { it.contains("|") }.forEach { line ->
                val parts = line.split("|")
                if (parts.size >= 2) {
                    novel.worldBuilding.geography.add(Location(
                        name = parts.getOrNull(0)?.trim() ?: "",
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
        generateSimpleItems("势力/组织", { name, desc ->
            novel.worldBuilding.factions.add(Faction(name = name, description = desc))
        })
    }
    
    fun generateItems() {
        generateSimpleItems("物品/装备", { name, desc ->
            novel.worldBuilding.items.add(GameItem(name = name, description = desc))
        })
    }
    
    fun generateSkills() {
        generateSimpleItems("技能/功法", { name, desc ->
            novel.worldBuilding.skills.add(Skill(name = name, description = desc))
        })
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
