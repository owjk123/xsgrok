package com.xsgrok.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xsgrok.app.data.local.LocalStorage
import com.xsgrok.app.data.model.*
import com.xsgrok.app.data.remote.ApiService
import com.xsgrok.app.prompt.SimplePromptBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 精简版ViewModel - 第一性原理优化
 * 职责：UI状态管理 + 用户意图分发
 * 
 * 新流程：用户输入一句话 → AI生成6大基础设定 → 用户审阅编辑 → 确认后生成章节
 */
class XSGrokViewModel(application: Application) : AndroidViewModel(application) {
    
    private val localStorage = LocalStorage(application)
    private val apiService = ApiService()
    
    // ========== UI状态 ==========
    private val _uiState = MutableStateFlow(XSGrokUiState())
    val uiState: StateFlow<XSGrokUiState> = _uiState.asStateFlow()
    
    // ========== 数据状态 ==========
    private val _novels = MutableStateFlow<List<Novel>>(emptyList())
    val novels: StateFlow<List<Novel>> = _novels.asStateFlow()
    
    private val _currentNovel = MutableStateFlow<Novel?>(null)
    val currentNovel: StateFlow<Novel?> = _currentNovel.asStateFlow()
    
    // ========== 生成状态 ==========
    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _autoModeState = MutableStateFlow(AutoModeState.IDLE)
    val autoModeState: StateFlow<AutoModeState> = _autoModeState.asStateFlow()
    
    // ========== 当前基础设定（用于编辑） ==========
    private val _currentFoundation = MutableStateFlow(NovelFoundation())
    val currentFoundation: StateFlow<NovelFoundation> = _currentFoundation.asStateFlow()
    
    // ========== 当前配置 ==========
    private val _currentPreset = MutableStateFlow(GenerationPresets.BALANCED)
    val currentPreset: StateFlow<GenerationPreset> = _currentPreset.asStateFlow()
    
    // ========== 用户创意（用于生成标题） ==========
    private var _userIdea = ""
    
    private var generationJob: Job? = null
    
    // ========== 初始化 ==========
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
    
    // ========== API配置 ==========
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
    
    fun toggleDarkMode() {
        viewModelScope.launch {
            val config = _uiState.value.apiConfig.copy(
                isDarkMode = !_uiState.value.apiConfig.isDarkMode
            )
            localStorage.saveApiConfig(config)
        }
    }
    
    // ========== 导航 ==========
    fun navigateTo(screen: Screen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }
    
    // ========== 小说管理 ==========
    fun createNovel(title: String, genre: String, style: String) {
        viewModelScope.launch {
            val novel = Novel(
                title = title,
                genre = genre,
                style = style
            )
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
            _uiState.value = _uiState.value.copy(currentScreen = Screen.NovelDetail)
        }
    }
    
    fun selectNovel(novelId: String) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId)
            if (novel != null) {
                _currentNovel.value = novel
                _uiState.value = _uiState.value.copy(currentScreen = Screen.NovelDetail)
            }
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
    
    // ========== 小说详情更新 ==========
    fun updateNovelOutline(novelId: String, outline: String) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId) ?: return@launch
            val updated = novel.copy(outline = outline)
            localStorage.saveNovel(updated)
            _currentNovel.value = updated
        }
    }
    
    fun updateNovelFoundation(
        novelId: String,
        characterSettings: String = "",
        characterRelationships: String = "",
        timeline: String = "",
        chapterPlotDirection: String = "",
        writingStyle: String = "",
        chapterSummaries: String = ""
    ) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId) ?: return@launch
            val updated = novel.copy(
                foundation = (novel.foundation ?: NovelFoundation()).copy(
                    characterSettings = characterSettings,
                    characterRelationships = characterRelationships,
                    timeline = timeline,
                    chapterPlotDirection = chapterPlotDirection,
                    writingStyle = writingStyle,
                    chapterSummaries = chapterSummaries
                ),
                updatedAt = System.currentTimeMillis()
            )
            localStorage.saveNovel(updated)
            _currentNovel.value = updated
        }
    }
    
    fun addCharacter(novelId: String, name: String, description: String, role: String) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId) ?: return@launch
            val newCharacter = Character(name = name, description = description, role = role)
            val updatedNovel = novel.copy(
                characters = (novel.characters + newCharacter).toMutableList(),
                updatedAt = System.currentTimeMillis()
            )
            localStorage.saveNovel(updatedNovel)
            _currentNovel.value = updatedNovel
        }
    }
    
    fun deleteCharacter(novelId: String, characterId: String) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId) ?: return@launch
            val updatedNovel = novel.copy(
                characters = novel.characters.filter { it.id != characterId }.toMutableList(),
                updatedAt = System.currentTimeMillis()
            )
            localStorage.saveNovel(updatedNovel)
            _currentNovel.value = updatedNovel
        }
    }
    
    fun updateCharacter(novelId: String, character: Character) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId) ?: return@launch
            val updatedNovel = novel.copy(
                characters = novel.characters.map { if (it.id == character.id) character else it }.toMutableList(),
                updatedAt = System.currentTimeMillis()
            )
            localStorage.saveNovel(updatedNovel)
            _currentNovel.value = updatedNovel
        }
    }
    
    // ========== 继续写作（从书架） ==========
    fun continueNovel(novelId: String) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId) ?: return@launch
            _currentNovel.value = novel
            _uiState.value = _uiState.value.copy(currentScreen = Screen.Bookshelf)
        }
    }
    
    // ========== 章节生成 ==========
    fun generateChapter(novelId: String, chapterTitle: String?) {
        if (_isGenerating.value) return
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val novel = localStorage.getNovel(novelId) ?: return@launch
            
            _isGenerating.value = true
            _streamingContent.value = ""
            _errorMessage.value = null
            
            try {
                val chapterNum = novel.chapters.size + 1
                val title = chapterTitle ?: "第${chapterNum}章"
                
                val (systemPrompt, userPrompt) = SimplePromptBuilder.buildChapterPrompt(
                    novel = novel,
                    chapterNum = chapterNum,
                    userGuide = null,
                    preset = _currentPreset.value
                )
                
                val apiConfig = _uiState.value.apiConfig
                if (apiConfig.apiKey.isBlank()) {
                    _errorMessage.value = "请先设置API Key"
                    return@launch
                }
                
                val fullContent = StringBuilder()
                apiService.generateContent(
                    apiKey = apiConfig.apiKey,
                    endpoint = apiConfig.endpoint,
                    model = apiConfig.model,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    temperature = _currentPreset.value.temperature
                ).collect { chunk ->
                    if (chunk.startsWith("[ERROR]")) {
                        _errorMessage.value = chunk
                    } else {
                        fullContent.append(chunk)
                        _streamingContent.value = fullContent.toString()
                    }
                }
                
                // 保存章节
                if (fullContent.isNotEmpty()) {
                    val chapter = Chapter(
                        title = title,
                        content = fullContent.toString(),
                        order = chapterNum
                    )
                    val updatedNovel = novel.copy(
                        chapters = (novel.chapters + chapter).toMutableList(),
                        updatedAt = System.currentTimeMillis()
                    )
                    localStorage.saveNovel(updatedNovel)
                    _currentNovel.value = updatedNovel
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "生成失败: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    fun continueChapter(novelId: String, guide: String?) {
        if (_isGenerating.value) return
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val novel = localStorage.getNovel(novelId) ?: return@launch
            
            _isGenerating.value = true
            _streamingContent.value = ""
            _errorMessage.value = null
            
            try {
                val chapterNum = novel.chapters.size + 1
                
                val (systemPrompt, userPrompt) = SimplePromptBuilder.buildChapterPrompt(
                    novel = novel,
                    chapterNum = chapterNum,
                    userGuide = guide,
                    preset = _currentPreset.value
                )
                
                val apiConfig = _uiState.value.apiConfig
                if (apiConfig.apiKey.isBlank()) {
                    _errorMessage.value = "请先设置API Key"
                    return@launch
                }
                
                val fullContent = StringBuilder()
                apiService.generateContent(
                    apiKey = apiConfig.apiKey,
                    endpoint = apiConfig.endpoint,
                    model = apiConfig.model,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    temperature = _currentPreset.value.temperature
                ).collect { chunk ->
                    if (chunk.startsWith("[ERROR]")) {
                        _errorMessage.value = chunk
                    } else {
                        fullContent.append(chunk)
                        _streamingContent.value = fullContent.toString()
                    }
                }
                
                // 保存章节
                if (fullContent.isNotEmpty()) {
                    val chapter = Chapter(
                        title = "第${chapterNum}章",
                        content = fullContent.toString(),
                        order = chapterNum
                    )
                    val updatedNovel = novel.copy(
                        chapters = (novel.chapters + chapter).toMutableList(),
                        updatedAt = System.currentTimeMillis()
                    )
                    localStorage.saveNovel(updatedNovel)
                    _currentNovel.value = updatedNovel
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "生成失败: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    // ========== AI生成辅助 ==========
    
    fun generateCharacters(novelId: String, prompt: String) {
        // 简化实现：暂不支持
    }
    
    fun addCharacterFull(novelId: String, name: String, role: String, description: String) {
        addCharacter(novelId, name, description, role)
    }
    
    // ========== 自动模式核心流程（重构） ==========
    
    /**
     * 启动自动模式 - 第一阶段：生成基础设定
     * 用户输入一句话，AI 自动分解补全为 6 大基础设定
     */
    fun startAutoMode(userIdea: String) {
        if (_isGenerating.value) return
        if (userIdea.isBlank()) return
        
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            // 保存用户创意
            _userIdea = userIdea
            
            // 阶段1：生成基础设定
            _autoModeState.value = AutoModeState.GENERATING_FOUNDATION
            _isGenerating.value = true
            _streamingContent.value = ""
            _errorMessage.value = null
            
            try {
                val apiConfig = _uiState.value.apiConfig
                if (apiConfig.apiKey.isBlank()) {
                    _errorMessage.value = "请先设置API Key"
                    _autoModeState.value = AutoModeState.IDLE
                    _isGenerating.value = false
                    return@launch
                }
                
                val (systemPrompt, userPrompt) = SimplePromptBuilder.buildFoundationPrompt(userIdea)
                
                val fullResponse = StringBuilder()
                apiService.generateContent(
                    apiKey = apiConfig.apiKey,
                    endpoint = apiConfig.endpoint,
                    model = apiConfig.model,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    temperature = 0.7f  // 基础设定生成使用较低温度以保证稳定性
                ).collect { chunk ->
                    if (chunk.startsWith("[ERROR]")) {
                        _errorMessage.value = chunk
                    } else {
                        fullResponse.append(chunk)
                        _streamingContent.value = fullResponse.toString()
                    }
                }
                
                // 解析基础设定
                if (fullResponse.isNotEmpty()) {
                    val foundation = SimplePromptBuilder.parseFoundationResponse(fullResponse.toString())
                    _currentFoundation.value = foundation
                    
                    // 同时生成标题
                    val title = generateNovelTitle(userIdea, apiConfig)
                    
                    // 创建临时 Novel 对象（用于审阅阶段）
                    val tempNovel = Novel(
                        title = title,
                        foundation = foundation
                    )
                    _currentNovel.value = tempNovel
                    
                    // 进入审阅阶段
                    _autoModeState.value = AutoModeState.REVIEW_FOUNDATION
                } else {
                    _errorMessage.value = "生成基础设定失败，请重试"
                    _autoModeState.value = AutoModeState.IDLE
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "生成失败: ${e.message}"
                _autoModeState.value = AutoModeState.IDLE
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    /**
     * 生成小说标题
     */
    private suspend fun generateNovelTitle(userIdea: String, apiConfig: ApiConfig): String {
        try {
            val (systemPrompt, userPrompt) = SimplePromptBuilder.buildTitlePrompt(userIdea)
            
            val response = StringBuilder()
            apiService.generateContent(
                apiKey = apiConfig.apiKey,
                endpoint = apiConfig.endpoint,
                model = apiConfig.model,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                temperature = 0.5f
            ).collect { chunk ->
                if (!chunk.startsWith("[ERROR]")) {
                    response.append(chunk)
                }
            }
            
            return SimplePromptBuilder.parseTitle(response.toString())
        } catch (e: Exception) {
            return "新小说"
        }
    }
    
    /**
     * 更新基础设定字段（用户编辑）
     */
    fun updateFoundationField(field: String, value: String) {
        val current = _currentFoundation.value
        val updated = when (field) {
            "characterSettings" -> current.copy(characterSettings = value)
            "characterRelationships" -> current.copy(characterRelationships = value)
            "timeline" -> current.copy(timeline = value)
            "chapterPlotDirection" -> current.copy(chapterPlotDirection = value)
            "writingStyle" -> current.copy(writingStyle = value)
            "chapterSummaries" -> current.copy(chapterSummaries = value)
            else -> current
        }
        _currentFoundation.value = updated
        
        // 同时更新 currentNovel 的 foundation
        _currentNovel.value?.let { novel ->
            _currentNovel.value = novel.copy(foundation = updated)
        }
    }
    
    /**
     * 重新生成基础设定
     */
    fun regenerateFoundation() {
        if (_userIdea.isNotBlank()) {
            _streamingContent.value = ""
            startAutoMode(_userIdea)
        }
    }
    
    /**
     * 确认基础设定并开始写作 - 第二阶段
     * 用户审阅编辑完 6 大设定后，确认开始生成第一章
     */
    fun confirmFoundationAndStartWriting() {
        if (_isGenerating.value) return
        
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val novel = _currentNovel.value ?: return@launch
            val foundation = _currentFoundation.value
            
            // 更新 novel 的 foundation（以防用户编辑过）
            val updatedNovel = novel.copy(
                foundation = foundation,
                updatedAt = System.currentTimeMillis()
            )
            
            // 保存到存储
            localStorage.saveNovel(updatedNovel)
            _currentNovel.value = updatedNovel
            
            // 阶段2：生成第一章
            _autoModeState.value = AutoModeState.GENERATING_CHAPTER
            _isGenerating.value = true
            _streamingContent.value = ""
            _errorMessage.value = null
            
            try {
                val apiConfig = _uiState.value.apiConfig
                if (apiConfig.apiKey.isBlank()) {
                    _errorMessage.value = "请先设置API Key"
                    _autoModeState.value = AutoModeState.REVIEW_FOUNDATION
                    _isGenerating.value = false
                    return@launch
                }
                
                val chapterNum = 1
                val (systemPrompt, userPrompt) = SimplePromptBuilder.buildChapterPrompt(
                    novel = updatedNovel,
                    chapterNum = chapterNum,
                    userGuide = null,
                    preset = _currentPreset.value
                )
                
                val fullContent = StringBuilder()
                apiService.generateContent(
                    apiKey = apiConfig.apiKey,
                    endpoint = apiConfig.endpoint,
                    model = apiConfig.model,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    temperature = _currentPreset.value.temperature
                ).collect { chunk ->
                    if (chunk.startsWith("[ERROR]")) {
                        _errorMessage.value = chunk
                    } else {
                        fullContent.append(chunk)
                        _streamingContent.value = fullContent.toString()
                    }
                }
                
                // 保存章节
                if (fullContent.isNotEmpty()) {
                    val chapter = saveChapter(updatedNovel, chapterNum, fullContent.toString())
                    
                    // 更新章节摘要
                    updateChapterSummaries(chapter, updatedNovel)
                    
                    _autoModeState.value = AutoModeState.COMPLETED
                } else {
                    _errorMessage.value = "生成章节失败，请重试"
                    _autoModeState.value = AutoModeState.REVIEW_FOUNDATION
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "生成失败: ${e.message}"
                _autoModeState.value = AutoModeState.REVIEW_FOUNDATION
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    /**
     * 续写下一章
     */
    fun continueAutoMode(nextGuide: String?) {
        if (_isGenerating.value) return
        
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val novel = _currentNovel.value ?: return@launch
            
            _autoModeState.value = AutoModeState.GENERATING_CHAPTER
            _isGenerating.value = true
            _streamingContent.value = ""
            _errorMessage.value = null
            
            try {
                val apiConfig = _uiState.value.apiConfig
                if (apiConfig.apiKey.isBlank()) {
                    _errorMessage.value = "请先设置API Key"
                    _autoModeState.value = AutoModeState.COMPLETED
                    _isGenerating.value = false
                    return@launch
                }
                
                val chapterNum = novel.chapters.size + 1
                val (systemPrompt, userPrompt) = SimplePromptBuilder.buildChapterPrompt(
                    novel = novel,
                    chapterNum = chapterNum,
                    userGuide = nextGuide,
                    preset = _currentPreset.value
                )
                
                val fullContent = StringBuilder()
                apiService.generateContent(
                    apiKey = apiConfig.apiKey,
                    endpoint = apiConfig.endpoint,
                    model = apiConfig.model,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    temperature = _currentPreset.value.temperature
                ).collect { chunk ->
                    if (chunk.startsWith("[ERROR]")) {
                        _errorMessage.value = chunk
                    } else {
                        fullContent.append(chunk)
                        _streamingContent.value = fullContent.toString()
                    }
                }
                
                // 保存章节
                if (fullContent.isNotEmpty()) {
                    val chapter = saveChapter(novel, chapterNum, fullContent.toString())
                    
                    // 更新章节摘要
                    updateChapterSummaries(chapter, novel)
                    
                    _autoModeState.value = AutoModeState.COMPLETED
                } else {
                    _errorMessage.value = "生成章节失败，请重试"
                    _autoModeState.value = AutoModeState.COMPLETED
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "生成失败: ${e.message}"
                _autoModeState.value = AutoModeState.COMPLETED
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    /**
     * 更新章节摘要
     */
    private suspend fun updateChapterSummaries(newChapter: Chapter, novel: Novel) {
        try {
            val apiConfig = _uiState.value.apiConfig
            if (apiConfig.apiKey.isBlank()) return
            
            // 先为新章节生成摘要
            val chapterSummary = generateChapterSummary(newChapter, apiConfig)
            
            // 再更新全局摘要
            val updatedChapter = newChapter.copy(summary = chapterSummary)
            
            val (systemPrompt, userPrompt) = SimplePromptBuilder.buildChapterSummaryUpdatePrompt(
                novel.copy(chapters = novel.chapters.map { 
                    if (it.id == newChapter.id) updatedChapter else it 
                }.toMutableList()),
                newChapter = updatedChapter
            )
            
            val response = StringBuilder()
            apiService.generateContent(
                apiKey = apiConfig.apiKey,
                endpoint = apiConfig.endpoint,
                model = apiConfig.model,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                temperature = 0.5f
            ).collect { chunk ->
                if (!chunk.startsWith("[ERROR]")) {
                    response.append(chunk)
                }
            }
            
            // 更新 novel 的 foundation
            val updatedFoundation = _currentFoundation.value.copy(
                chapterSummaries = response.toString().trim()
            )
            _currentFoundation.value = updatedFoundation
            
            // 保存更新后的 novel
            val updatedNovel = novel.copy(
                foundation = updatedFoundation,
                chapters = novel.chapters.map {
                    if (it.id == newChapter.id) updatedChapter else it
                }.toMutableList(),
                updatedAt = System.currentTimeMillis()
            )
            localStorage.saveNovel(updatedNovel)
            _currentNovel.value = updatedNovel
            
        } catch (e: Exception) {
            // 摘要更新失败不影响主流程
        }
    }
    
    /**
     * 为章节生成摘要
     */
    private suspend fun generateChapterSummary(chapter: Chapter, apiConfig: ApiConfig): String {
        try {
            val (systemPrompt, userPrompt) = SimplePromptBuilder.buildSummaryPrompt(chapter)
            
            val response = StringBuilder()
            apiService.generateContent(
                apiKey = apiConfig.apiKey,
                endpoint = apiConfig.endpoint,
                model = apiConfig.model,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                temperature = 0.5f
            ).collect { chunk ->
                if (!chunk.startsWith("[ERROR]")) {
                    response.append(chunk)
                }
            }
            
            return response.toString().trim()
        } catch (e: Exception) {
            return ""
        }
    }
    
    private suspend fun saveChapter(novel: Novel, chapterNum: Int, content: String): Chapter {
        val title = extractChapterTitle(content, chapterNum)
        val cleanContent = cleanChapterContent(content, title)
        
        val chapter = Chapter(
            title = title,
            content = cleanContent,
            order = chapterNum
        )
        
        val updatedNovel = novel.copy(
            chapters = (novel.chapters + chapter).toMutableList(),
            updatedAt = System.currentTimeMillis()
        )
        localStorage.saveNovel(updatedNovel)
        _currentNovel.value = updatedNovel
        
        return chapter
    }
    
    private fun extractChapterTitle(content: String, defaultNum: Int): String {
        val patterns = listOf(
            Regex("""第[一二三四五六七八九十百千万\d]+章[：:](.+)"""),
            Regex("""^第[一二三四五六七八九十百千万\d]+\s*(.+)""", RegexOption.MULTILINE),
            Regex("""^第\d+\s*(.+)""", RegexOption.MULTILINE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(content)
            if (match != null) {
                val title = match.groupValues[1].trim()
                if (title.isNotBlank() && title.length < 30) {
                    return "第${defaultNum}章 $title"
                }
            }
        }
        
        return "第${defaultNum}章"
    }
    
    private fun cleanChapterContent(content: String, title: String): String {
        var cleaned = content
        val titlePattern = Regex("""第[一二三四五六七八九十百千万\d]+章[：:].+""")
        cleaned = cleaned.replaceFirst(titlePattern, "")
        return cleaned.trim()
    }
    
    fun stopGeneration() {
        generationJob?.cancel()
        _isGenerating.value = false
        
        // 根据当前状态决定返回到哪个状态
        when (_autoModeState.value) {
            AutoModeState.GENERATING_FOUNDATION -> {
                _autoModeState.value = AutoModeState.IDLE
            }
            AutoModeState.GENERATING_CHAPTER -> {
                _autoModeState.value = AutoModeState.COMPLETED
            }
            else -> {}
        }
    }
    
    fun resetAutoMode() {
        _autoModeState.value = AutoModeState.IDLE
        _streamingContent.value = ""
        _errorMessage.value = null
        _currentFoundation.value = NovelFoundation()
        _currentNovel.value = null
        _userIdea = ""
    }
    
    fun finishAutoMode() {
        _autoModeState.value = AutoModeState.COMPLETED
        navigateTo(Screen.Reading)
    }
    
    // ========== 模式选择 ==========
    fun setGenerationPreset(presetId: String) {
        _currentPreset.value = GenerationPresets.getById(presetId)
    }
    
    // ========== 错误处理 ==========
    fun clearError() {
        _errorMessage.value = null
    }
}

// ========== UI状态 ==========
data class XSGrokUiState(
    val apiConfig: ApiConfig = ApiConfig(),
    val currentScreen: Screen = Screen.Creation
)

// ========== 屏幕枚举 ==========
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
