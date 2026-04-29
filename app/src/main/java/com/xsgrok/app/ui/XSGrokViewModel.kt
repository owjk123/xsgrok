package com.xsgrok.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xsgrok.app.agent.AgentOrchestrator
import com.xsgrok.app.agent.PlotController
import com.xsgrok.app.agent.CharacterMindSystem
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
    
    // Agent系统
    private val agentOrchestrator = AgentOrchestrator()
    private val characterMindSystem = CharacterMindSystem()
    
    private val _uiState = MutableStateFlow(XSGrokUiState())
    val uiState: StateFlow<XSGrokUiState> = _uiState.asStateFlow()
    
    private val _novels = MutableStateFlow<List<Novel>>(emptyList())
    val novels: StateFlow<List<Novel>> = _novels.asStateFlow()
    
    private val _currentNovel = MutableStateFlow<Novel?>(null)
    val currentNovel: StateFlow<Novel?> = _currentNovel.asStateFlow()
    
    private val _streamingContent = MutableStateFlow("")
    
    // 生成模式: "single" = 单次生成, "agent" = 分层Agent生成
    private val _generationMode = MutableStateFlow("agent")
    val generationMode: StateFlow<String> = _generationMode.asStateFlow()
    
    // Agent生成阶段
    private val _agentStage = MutableStateFlow<AgentOrchestrator.GenerationStage?>(null)
    val agentStage: StateFlow<AgentOrchestrator.GenerationStage?> = _agentStage.asStateFlow()
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _autoModeState = MutableStateFlow(AutoModeState.IDLE)
    val autoModeState: StateFlow<AutoModeState> = _autoModeState.asStateFlow()
    
    // 全自动模式待审阅的小说
    private val _autoModeNovel = MutableStateFlow<Novel?>(null)
    val autoModeNovel: StateFlow<Novel?> = _autoModeNovel.asStateFlow()
    
    private var generationJob: Job? = null
    

    // ========== P0功能：温度计算和去AI味提示 ==========
    
    /**
     * 根据章节进度计算动态temperature
     * 开场章 0.85~0.95：高创意
     * 推进章 0.7~0.8：平衡
     * 收束章 0.6~0.7：确定性
     */
    private fun calculateTemperature(chapterNum: Int, totalNodes: Int): Float {
        if (totalNodes <= 0) return 0.75f
        
        val progress = chapterNum.toFloat() / totalNodes
        return when {
            progress < 0.2f -> 0.9f   // 开场
            progress < 0.8f -> 0.75f  // 推进
            else -> 0.65f             // 收束
        }
    }
    
    /**
     * 获取去AI味的写作提示
     */
    private fun getAntiAIWritingHints(): String {
        return """
【写作要求（严格遵守）】
1. 段落长度必须有变化：每章至少一个长段落（8句以上），多个极短段落（1-2句）
2. 禁止句式重复：同一段落内相同句式结构不超过2次
3. 禁止心理总结：用具体动作、细节代替
   - 错误：「他很愤怒」
   - 正确：「把烟头摁进掌心，烟灰簌簌落下」
4. 对话要自然：夹杂语气词、打断、省略，不要工整的一问一答
5. 场景描写要粗糙：不要面面俱到，留白给读者想象
""".trimIndent()
    }
    
    /**
     * 生成关键节点（8~12个）
     */
    private fun parseKeyNodesFromOutline(outline: String): List<KeyNode> {
        val nodes = mutableListOf<KeyNode>()
        val lines = outline.lines().filter { it.isNotBlank() }
        
        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            // 尝试解析节点（格式：1. 标题 或 【标题】 等）
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
                    targetChapter = (index + 1) * 2  // 预估每2章一个节点
                ))
            }
            
            if (nodes.size >= 10) break  // 最多10个节点
        }
        
        // 如果解析失败，生成默认节点
        if (nodes.isEmpty()) {
            listOf("开篇", "矛盾初现", "危机升级", "转折点", "高潮", "结局").forEachIndexed { i, title ->
                nodes.add(KeyNode(title = title, description = title, targetChapter = (i + 1) * 2))
            }
        }
        
        return nodes
    }
    
    /**
     * 从生成内容中提取伏笔
     */
    private fun extractForeshadowings(content: String, chapterNum: Int): List<Foreshadowing> {
        val foreshadowings = mutableListOf<Foreshadowing>()
        // 简单的伏笔提取逻辑：查找括号内容或特定标记
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
        characterId: String,
        name: String,
        description: String,
        role: String,
        appearance: String,
        personality: String,
        background: String,
        abilities: String
    ) {
        val novel = _currentNovel.value ?: return
        val index = novel.characters.indexOfFirst { it.id == characterId }
        if (index >= 0) {
            novel.characters[index] = Character(
                id = characterId,
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
    
    fun updateWorldBuilding(worldBackground: String, powerSystem: String, rules: String = "") {
        val novel = _currentNovel.value ?: return
        val updatedWorldBuilding = novel.worldBuilding.copy(
            worldBackground = worldBackground,
            powerSystem = powerSystem,
            rules = rules
        )
        val updatedNovel = novel.copy(worldBuilding = updatedWorldBuilding)
        viewModelScope.launch {
            localStorage.saveNovel(updatedNovel)
            _currentNovel.value = updatedNovel
        }
    }
    
    fun updateWorldBackground(background: String) {
        val novel = _currentNovel.value ?: return
        val updatedWorldBuilding = novel.worldBuilding.copy(worldBackground = background)
        val updatedNovel = novel.copy(worldBuilding = updatedWorldBuilding)
        viewModelScope.launch {
            localStorage.saveNovel(updatedNovel)
            _currentNovel.value = updatedNovel
        }
    }
    
    fun updatePowerSystem(system: String) {
        val novel = _currentNovel.value ?: return
        val updatedWorldBuilding = novel.worldBuilding.copy(powerSystem = system)
        val updatedNovel = novel.copy(worldBuilding = updatedWorldBuilding)
        viewModelScope.launch {
            localStorage.saveNovel(updatedNovel)
            _currentNovel.value = updatedNovel
        }
    }
    
    fun addLocation(name: String, description: String, type: String = "", significance: String = "") {
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
    
    fun deleteLocation(locationId: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.geography.removeAll { it.id == locationId }
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun addFaction(name: String, description: String, leader: String = "", goals: String = "") {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.factions.add(Faction(
            name = name,
            description = description,
            leader = leader,
            goals = goals
        ))
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun deleteFaction(factionId: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.factions.removeAll { it.id == factionId }
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun addItem(name: String, description: String, type: String = "", abilities: String = "") {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.items.add(GameItem(
            name = name,
            description = description,
            type = type,
            abilities = abilities
        ))
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun deleteItem(itemId: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.items.removeAll { it.id == itemId }
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun addSkill(name: String, description: String, type: String = "", requirements: String = "") {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.skills.add(Skill(
            name = name,
            description = description,
            type = type,
            requirements = requirements
        ))
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun deleteSkill(skillId: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.skills.removeAll { it.id == skillId }
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun addTimelineEvent(title: String, description: String, time: String = "") {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.timeline.add(TimelineEvent(
            title = title,
            description = description,
            time = time
        ))
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun deleteTimelineEvent(eventId: String) {
        val novel = _currentNovel.value ?: return
        novel.worldBuilding.timeline.removeAll { it.id == eventId }
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    // ========== AI生成功能 ==========
    
    fun generateWorldBuilding() {
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
                5. 力量体系
                
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
                val updatedWorldBuilding = novel.worldBuilding.copy(worldBackground = result)
                val updatedNovel = novel.copy(worldBuilding = updatedWorldBuilding)
                localStorage.saveNovel(updatedNovel)
                _currentNovel.value = updatedNovel
            }
            _isGenerating.value = false
        }
    }
    
    fun generateWorldBackground() {
        generateWorldBuilding()
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
                val updatedWorldBuilding = novel.worldBuilding.copy(powerSystem = result)
                val updatedNovel = novel.copy(worldBuilding = updatedWorldBuilding)
                localStorage.saveNovel(updatedNovel)
                _currentNovel.value = updatedNovel
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
                _streamingContent.value = ""
                
                // 生成小说基础资料
                var outlineResult = ""
                var outlineError: String? = null
                
                apiService.generateContent(
                    apiKey = config.apiKey,
                    endpoint = config.endpoint,
                    model = config.model,
                    systemPrompt = "你是一个专业的小说创作顾问，请严格按照JSON格式输出。",
                    userPrompt = """
                        用户想写：$userPrompt
                        
                        请生成完整的小说基础设定，包括：
                        1. 小说标题（有创意且吸引人）
                        2. 类型（玄幻/都市/科幻/悬疑/仙侠/游戏/历史等）
                        3. 风格（热血/轻松/黑暗/搞笑/温馨等）
                        4. 主角设定（姓名、性格、特殊能力等）
                        5. 详细大纲（500字左右，包含开头、发展、高潮、结局）
                        6. 世界背景（世界观、历史、地理等，200字以上）
                        7. 力量体系（修炼等级、能力划分等）
                        8. 世界规则（社会规则、禁忌、特殊法则等，100字以上）
                        9. 关键节点（8-10个故事关键转折点，每个一行，格式：节点标题|节点描述）
                        10. 主要角色（至少3个，包含配角和反派）
                        11. 重要地点（至少3个）
                        12. 势力组织（至少2个）
                        
                        请严格按JSON格式输出，不要添加任何其他内容：
                        {"title":"标题","type":"类型","style":"风格","mainCharacter":"主角设定","outline":"详细大纲\n关键节点：\n1. 开篇\n2. 矛盾初现\n3. 危机升级...","worldBackground":"世界背景","powerSystem":"力量体系","worldRules":"世界规则","characters":[{"name":"角色名","description":"描述","role":"主角/配角/反派","appearance":"外貌","personality":"性格","background":"背景","abilities":"能力","relationships":"关系"}],"locations":[{"name":"地名","description":"描述","type":"城市/荒野/秘境","significance":"重要性"}],"factions":[{"name":"势力名","description":"描述","leader":"首领","goals":"目标","relationships":"与其他势力关系"}]}
                    """.trimIndent()
                ).collect { content ->
                    if (content.startsWith("[ERROR]")) {
                        outlineError = content
                    } else {
                        outlineResult += content
                        _streamingContent.value += content
                    }
                }
                
                // 检查大纲生成是否成功
                if (!outlineError.isNullOrEmpty()) {
                    _errorMessage.value = "资料生成失败: $outlineError"
                    _autoModeState.value = AutoModeState.IDLE
                    return@launch
                }
                
                if (outlineResult.isBlank()) {
                    _errorMessage.value = "资料生成失败，请检查网络连接"
                    _autoModeState.value = AutoModeState.IDLE
                    return@launch
                }
                
                // 解析并创建待审阅的小说
                val outlineText = extractField(outlineResult, "outline") ?: outlineResult
                val keyNodes = parseKeyNodesFromOutline(outlineText)
                
                // 解析角色数组
                val characters = parseCharacterArray(outlineResult)
                
                // 解析地点数组
                val locations = parseLocationArray(outlineResult)
                
                // 解析势力数组
                val factions = parseFactionArray(outlineResult)
                
                val novel = Novel(
                    title = extractField(outlineResult, "title") ?: "未命名小说",
                    type = extractField(outlineResult, "type") ?: "玄幻",
                    style = extractField(outlineResult, "style") ?: "热血",
                    mainCharacter = extractField(outlineResult, "mainCharacter") ?: "主角",
                    outline = outlineText,
                    characters = characters.toMutableList(),
                    worldBuilding = WorldBuilding(
                        worldBackground = extractField(outlineResult, "worldBackground") ?: "",
                        powerSystem = extractField(outlineResult, "powerSystem") ?: "",
                        geography = locations.toMutableList(),
                        factions = factions.toMutableList(),
                        rules = extractField(outlineResult, "worldRules") ?: ""
                    ),
                    keyNodes = keyNodes.toMutableList()  // P0：添加关键节点
                )
                
                // 初始化Agent系统
                agentOrchestrator.initializeFromOutline(novel)
                novel.characters.forEach { char ->
                    characterMindSystem.initializeCharacter(char, novel.type)
                }
                
                // 保存到待审阅状态，进入审阅阶段
                _autoModeNovel.value = novel
                _autoModeState.value = AutoModeState.REVIEW
                
            } catch (e: Exception) {
                _isGenerating.value = false
                _errorMessage.value = "生成失败: ${e.message}"
                _autoModeState.value = AutoModeState.IDLE
            }
        }
    }
    
    // 更新待审阅的小说资料
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
    
    // 确认资料并开始写作
    fun confirmAndStartWriting() {
        val novel = _autoModeNovel.value ?: return
        val config = _uiState.value.apiConfig
        
        if (config.apiKey.isBlank()) {
            _errorMessage.value = "请先配置API密钥"
            return
        }
        
        generationJob = viewModelScope.launch {
            try {
                // 保存小说
                localStorage.saveNovel(novel)
                _currentNovel.value = novel
                
                // 开始生成第一章
                _autoModeState.value = AutoModeState.GENERATING_CHAPTER
                _isGenerating.value = true
                _streamingContent.value = ""
                
                var chapterError: String? = null
                
                val chapterPrompt = buildAutoChapterPrompt(novel, 1, "")
                
                // P0：计算动态temperature
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
                
                val prompt = """
                    请创作第${chapterNum}章。
                    
                    ${if (guide.isNotBlank()) "用户引导：$guide" else ""}
                    
                    ${if (lastChapter != null) "上一章结尾：\n${lastChapter.content.takeLast(500)}" else ""}
                    
                    请继续推进故事，保持风格一致。
                """.trimIndent()
                
                var chapterError: String? = null
                
                // P0：计算动态temperature
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
    
    // Bug1修复：从书架继续写作
    fun continueNovel(novelId: String) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId) ?: return@launch
            _currentNovel.value = novel
            _autoModeNovel.value = novel
            _autoModeState.value = AutoModeState.REVIEW
            _uiState.value = _uiState.value.copy(currentScreen = Screen.AutoMode)
        }
    }
    
    fun retryAutoMode() {
        // 保持当前小说数据，重置到审阅状态
        _autoModeState.value = AutoModeState.REVIEW
        _isGenerating.value = false
        _streamingContent.value = ""
    }
    
    // ========== 数组解析方法 ==========
    
    private fun parseCharacterArray(text: String): List<Character> {
        val characters = mutableListOf<Character>()
        try {
            val arrayPattern = """"characters"\s*:\s*\[""".toRegex()
            val arrayMatch = arrayPattern.find(text) ?: return characters
            val arrayStart = arrayMatch.range.first
            var depth = 0
            var arrayEnd = arrayStart
            for (i in arrayStart until text.length) {
                if (text[i] == '[') depth++
                else if (text[i] == ']') {
                    depth--
                    if (depth == 0) { arrayEnd = i + 1; break }
                }
            }
            val arrayText = text.substring(arrayStart, arrayEnd)
            val objPattern = """\{[^{}]*"name"\s*:\s*"([^"]+)"[^{}]*\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
            objPattern.findAll(arrayText).forEach { match ->
                val objText = match.value
                characters.add(Character(
                    name = extractField(objText, "name") ?: "未命名",
                    description = extractField(objText, "description") ?: "",
                    role = extractField(objText, "role") ?: "配角",
                    appearance = extractField(objText, "appearance") ?: "",
                    personality = extractField(objText, "personality") ?: "",
                    background = extractField(objText, "background") ?: "",
                    abilities = extractField(objText, "abilities") ?: "",
                    relationships = extractField(objText, "relationships") ?: ""
                ))
            }
        } catch (e: Exception) { }
        return characters
    }
    
    private fun parseLocationArray(text: String): List<Location> {
        val locations = mutableListOf<Location>()
        try {
            val arrayPattern = """"locations"\s*:\s*\[""".toRegex()
            val arrayMatch = arrayPattern.find(text) ?: return locations
            val arrayStart = arrayMatch.range.first
            var depth = 0
            var arrayEnd = arrayStart
            for (i in arrayStart until text.length) {
                if (text[i] == '[') depth++
                else if (text[i] == ']') {
                    depth--
                    if (depth == 0) { arrayEnd = i + 1; break }
                }
            }
            val arrayText = text.substring(arrayStart, arrayEnd)
            val objPattern = """\{[^{}]*"name"\s*:\s*"([^"]+)"[^{}]*\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
            objPattern.findAll(arrayText).forEach { match ->
                val objText = match.value
                locations.add(Location(
                    name = extractField(objText, "name") ?: "未命名",
                    description = extractField(objText, "description") ?: "",
                    type = extractField(objText, "type") ?: "",
                    significance = extractField(objText, "significance") ?: ""
                ))
            }
        } catch (e: Exception) { }
        return locations
    }
    
    private fun parseFactionArray(text: String): List<Faction> {
        val factions = mutableListOf<Faction>()
        try {
            val arrayPattern = """"factions"\s*:\s*\[""".toRegex()
            val arrayMatch = arrayPattern.find(text) ?: return factions
            val arrayStart = arrayMatch.range.first
            var depth = 0
            var arrayEnd = arrayStart
            for (i in arrayStart until text.length) {
                if (text[i] == '[') depth++
                else if (text[i] == ']') {
                    depth--
                    if (depth == 0) { arrayEnd = i + 1; break }
                }
            }
            val arrayText = text.substring(arrayStart, arrayEnd)
            val objPattern = """\{[^{}]*"name"\s*:\s*"([^"]+)"[^{}]*\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
            objPattern.findAll(arrayText).forEach { match ->
                val objText = match.value
                factions.add(Faction(
                    name = extractField(objText, "name") ?: "未命名",
                    description = extractField(objText, "description") ?: "",
                    leader = extractField(objText, "leader") ?: "",
                    goals = extractField(objText, "goals") ?: "",
                    relationships = extractField(objText, "relationships") ?: ""
                ))
            }
        } catch (e: Exception) { }
        return factions
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
    
    private fun buildAutoChapterSystemPrompt(novel: Novel, chapterNum: Int = 1): String {
        // P0：计算进度信息
        val progressInfo = novel.getProgressInfo(chapterNum)
        val unresolvedForeshadowings = novel.getUnresolvedForeshadowings()
        val isConvergence = novel.isConvergenceMode()
        val temperature = calculateTemperature(chapterNum, novel.keyNodes.size)
        
        // P0：进度提示
        val progressHint = if (novel.keyNodes.isNotEmpty()) {
            """
【当前进度】
${progressInfo.toModelHint()}
阶段：${if (chapterNum.toFloat() / novel.keyNodes.size < 0.2f) "开场阶段（高创意）"
                  else if (chapterNum.toFloat() / novel.keyNodes.size < 0.8f) "推进阶段（稳健发展）"
                  else "收束阶段（即将结局）"}
Temperature: $temperature
"""
        } else ""
        
        // P0：伏笔提示
        val foreshadowingHint = if (unresolvedForeshadowings.isNotEmpty()) {
            """
【未回收伏笔（需择机回收）】
${unresolvedForeshadowings.take(5).mapIndexed { i, f -> "${i + 1}. ${f.content}（第${f.plantedChapter}章埋下）" }.joinToString("\n")}
${if (isConvergence) "\n⚠️ 收束模式：必须在本章回收至少一条伏笔！" else ""}
"""
        } else ""
        
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

$progressHint
$foreshadowingHint

${getAntiAIWritingHints()}

【章节要求】
1. 纯中文写作
2. 文笔流畅，引人入胜
3. 每章3000-5000字
4. 结尾留悬念
${if (isConvergence) "5. 【重要】本章必须推进主线结局，回收至少一条伏笔" else ""}
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

    // ========== 生成模式控制 ==========
    
    fun setGenerationMode(mode: String) {
        _generationMode.value = mode
    }
    
    // ========== P1-P4 新增方法 ==========
    
    // P1: 更新感官配置
    fun updateSensoryProfile(novel: Novel, profile: SensoryProfile): Novel {
        val updated = novel.copy(sensoryProfile = profile)
        viewModelScope.launch {
            localStorage.saveNovel(updated)
            _currentNovel.value = updated
        }
        return updated
    }
    
    // P1: 更新生成配置
    fun updateGenerationConfig(novel: Novel, config: GenerationConfig): Novel {
        val updated = novel.copy(generationConfig = config)
        viewModelScope.launch {
            localStorage.saveNovel(updated)
            _currentNovel.value = updated
        }
        return updated
    }
    
    // P4: 全自动模式设置更新
    fun updateAutoModeTabooLevel(level: TabooLevel) {
        val novel = _autoModeNovel.value ?: return
        val newProfile = novel.sensoryProfile.copy(tabooLevel = level)
        _autoModeNovel.value = novel.copy(sensoryProfile = newProfile)
    }
    
    fun updateAutoModeDescriptionDensity(density: Int) {
        val novel = _autoModeNovel.value ?: return
        val newProfile = novel.sensoryProfile.copy(descriptionDensity = density)
        _autoModeNovel.value = novel.copy(sensoryProfile = newProfile)
    }
    
    fun updateAutoModeRhythmPreference(preference: RhythmPreference) {
        val novel = _autoModeNovel.value ?: return
        val newConfig = novel.generationConfig.copy(rhythmPreference = preference)
        _autoModeNovel.value = novel.copy(generationConfig = newConfig)
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
