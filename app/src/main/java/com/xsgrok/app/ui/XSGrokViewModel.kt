package com.xsgrok.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xsgrok.app.data.local.LocalStorage
import com.xsgrok.app.data.model.ApiConfig
import com.xsgrok.app.data.model.Chapter
import com.xsgrok.app.data.model.Character
import com.xsgrok.app.data.model.Novel
import com.xsgrok.app.data.remote.ApiEndpoints
import com.xsgrok.app.data.remote.ApiService
import com.xsgrok.app.ui.screens.AutoModeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class XSGrokViewModel(application: Application) : AndroidViewModel(application) {
    
    private val localStorage = LocalStorage(application)
    private val apiService = ApiService()
    
    // UI State
    private val _uiState = MutableStateFlow(XSGrokUiState())
    val uiState: StateFlow<XSGrokUiState> = _uiState.asStateFlow()
    
    // Novels list
    private val _novels = MutableStateFlow<List<Novel>>(emptyList())
    val novels: StateFlow<List<Novel>> = _novels.asStateFlow()
    
    // Current novel
    private val _currentNovel = MutableStateFlow<Novel?>(null)
    val currentNovel: StateFlow<Novel?> = _currentNovel.asStateFlow()
    
    // Streaming content
    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()
    
    // Is generating
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Auto mode state
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
            if (novel != null) {
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
            
            // Save chapter
            val newContent = _streamingContent.value
            if (newContent.isNotBlank()) {
                val chapter = Chapter(
                    title = chapterTitle,
                    content = newContent,
                    order = novel.chapters.size
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
        
        if (config.apiKey.isBlank()) {
            _errorMessage.value = "请先配置API密钥"
            return
        }
        
        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            
            val systemPrompt = buildContinueSystemPrompt(novel)
            val userPrompt = buildContinueUserPrompt(lastChapter)
            
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    _streamingContent.value += content
                } else {
                    _errorMessage.value = content
                }
            }
            
            _isGenerating.value = false
            
            // Update chapter
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
    
    fun addCharacter(name: String, description: String, role: String) {
        val novel = _currentNovel.value ?: return
        val character = Character(name = name, description = description, role = role)
        novel.characters.add(character)
        viewModelScope.launch {
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
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
    
    fun navigateTo(screen: Screen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    // ========== 全自动模式功能 ==========
    
    fun startAutoMode(userPrompt: String) {
        val config = _uiState.value.apiConfig
        
        if (config.apiKey.isBlank()) {
            _errorMessage.value = "请先配置API密钥"
            return
        }
        
        generationJob = viewModelScope.launch {
            _autoModeState.value = AutoModeState.GENERATING_OUTLINE
            
            // 步骤1: 根据用户一句话生成小说信息
            val outlineSystemPrompt = """
                你是一个专业的小说创作顾问。用户会给你一句话描述，你需要：
                1. 为这个故事生成一个合适的中文标题
                2. 确定小说类型（如：玄幻、都市、科幻、悬疑等）
                3. 确定写作风格（如：轻松、严肃、热血、温馨等）
                4. 创建主角基本信息
                5. 生成一个详细的小说大纲（包含主要情节走向）
                
                请严格按照以下JSON格式输出：
                {
                    "title": "小说标题",
                    "type": "小说类型",
                    "style": "写作风格",
                    "mainCharacter": "主角名字和简介",
                    "outline": "详细大纲内容"
                }
            """.trimIndent()
            
            val outlineUserPrompt = "用户想写的小说：$userPrompt"
            
            var outlineResult = ""
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = outlineSystemPrompt,
                userPrompt = outlineUserPrompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    outlineResult += content
                }
            }
            
            // 解析大纲结果（简化处理）
            val novel = parseOutlineResult(outlineResult, userPrompt)
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
            
            // 步骤2: 自动开始生成第一章
            _autoModeState.value = AutoModeState.GENERATING_CHAPTER
            _isGenerating.value = true
            _streamingContent.value = ""
            
            val chapterSystemPrompt = buildAutoChapterSystemPrompt(novel)
            val chapterUserPrompt = """
                请根据以下大纲开始创作第一章：
                
                小说标题：${novel.title}
                类型：${novel.type}
                风格：${novel.style}
                主角：${novel.mainCharacter}
                
                大纲：
                ${novel.outline}
                
                请写出精彩的开篇第一章，字数在3000-5000字左右。
            """.trimIndent()
            
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = chapterSystemPrompt,
                userPrompt = chapterUserPrompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    _streamingContent.value += content
                }
            }
            
            _isGenerating.value = false
            
            // 保存第一章
            val chapterContent = _streamingContent.value
            if (chapterContent.isNotBlank()) {
                val chapter = Chapter(
                    title = "第一章",
                    content = chapterContent,
                    order = 0
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
            
            val lastChapter = novel.chapters.lastOrNull()
            val chapterNum = novel.chapters.size + 1
            
            val systemPrompt = buildAutoChapterSystemPrompt(novel)
            val userPrompt = """
                请继续创作第${chapterNum}章。
                
                ${if (guide.isNotBlank()) "用户引导：$guide" else ""}
                
                ${if (lastChapter != null) "上一章内容：\n${lastChapter.content.takeLast(1000)}" else ""}
                
                请继续推进故事发展，保持风格一致，字数3000-5000字。
            """.trimIndent()
            
            apiService.generateContent(
                apiKey = config.apiKey,
                endpoint = config.endpoint,
                model = config.model,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt
            ).collect { content ->
                if (!content.startsWith("[ERROR]")) {
                    _streamingContent.value += content
                }
            }
            
            _isGenerating.value = false
            
            // 保存新章节
            val chapterContent = _streamingContent.value
            if (chapterContent.isNotBlank()) {
                val chapter = Chapter(
                    title = "第${chapterNum}章",
                    content = chapterContent,
                    order = novel.chapters.size
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
    
    private fun parseOutlineResult(result: String, originalPrompt: String): Novel {
        // 简化的解析逻辑，实际应该解析JSON
        return try {
            Novel(
                title = extractField(result, "title") ?: "未命名小说",
                type = extractField(result, "type") ?: "玄幻",
                style = extractField(result, "style") ?: "热血",
                mainCharacter = extractField(result, "mainCharacter") ?: "主角",
                outline = extractField(result, "outline") ?: result
            )
        } catch (e: Exception) {
            Novel(
                title = "未命名小说",
                type = "玄幻",
                style = "热血",
                mainCharacter = "主角",
                outline = originalPrompt
            )
        }
    }
    
    private fun extractField(text: String, field: String): String? {
        val pattern = """"$field"\s*:\s*"([^"]+)"""".toRegex()
        return pattern.find(text)?.groupValues?.getOrNull(1)
    }
    
    private fun buildAutoChapterSystemPrompt(novel: Novel): String {
        return """
            你是一位资深的中文网络小说作家，擅长创作${novel.type}类型的小说。
            
            写作要求：
            1. 使用中文写作
            2. 文笔流畅，情节紧凑
            3. 人物形象鲜明，对话生动
            4. 适当使用环境描写和心理活动
            5. 每章结尾留有悬念
            6. 字数控制在3000-5000字
            7. 风格：${novel.style}
        """.trimIndent()
    }
    
    private fun buildChapterSystemPrompt(novel: Novel): String {
        return """
            你是一位专业的中文小说作家。
            你擅长创作${novel.type}类型的小说，风格为${novel.style}。
            
            写作要求：
            1. 使用中文
            2. 描写生动，情节引人入胜
            3. 主角设定：${novel.mainCharacter}
            4. 使用标准的小说格式
            5. 每章2000-5000字
        """.trimIndent()
    }
    
    private fun buildChapterUserPrompt(novel: Novel, chapterTitle: String): String {
        val previousContent = novel.chapters.lastOrNull()?.content ?: ""
        val characters = novel.characters.joinToString("\n") { 
            "- ${it.name} (${it.role}): ${it.description}" 
        }
        
        return """
            请为小说《${novel.title}》创作章节：$chapterTitle
            
            类型：${novel.type}
            风格：${novel.style}
            主角：${novel.mainCharacter}
            
            ${if (characters.isNotBlank()) "角色设定：\n$characters" else ""}
            
            ${if (previousContent.isNotBlank()) "上一章内容（保持连贯性）：\n${previousContent.takeLast(500)}" else ""}
            
            请按小说风格继续创作。
        """.trimIndent()
    }
    
    private fun buildContinueSystemPrompt(novel: Novel): String {
        return """
            你正在续写小说《${novel.title}》。
            保持风格一致，继续创作。
            使用中文。
        """.trimIndent()
    }
    
    private fun buildContinueUserPrompt(lastChapter: Chapter): String {
        return """
            请继续以下内容：
            
            ${lastChapter.content}
            
            请自然地续写。
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
    AutoMode
}
