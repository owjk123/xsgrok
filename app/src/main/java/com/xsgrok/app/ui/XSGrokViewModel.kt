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
 * 移除：复杂的多阶段生成逻辑、Agent编排、状态追踪
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
    
    // ========== 当前配置 ==========
    private val _currentPreset = MutableStateFlow(GenerationPresets.BALANCED)
    val currentPreset: StateFlow<GenerationPreset> = _currentPreset.asStateFlow()
    
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
    fun createNovel(title: String, genre: String, style: String, mainCharacter: String) {
        viewModelScope.launch {
            val novel = Novel(
                title = title,
                genre = genre,
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
            _uiState.value = _uiState.value.copy(currentScreen = Screen.NovelDetail)
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
    
    fun updateWorldBuilding(novelId: String, worldBackground: String, powerSystem: String, rules: String) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId) ?: return@launch
            val updated = novel.copy(
                worldBuilding = WorldBuilding(
                    worldBackground = worldBackground,
                    powerSystem = powerSystem,
                    rules = rules
                )
            )
            localStorage.saveNovel(updated)
            _currentNovel.value = updated
        }
    }
    
    fun addCharacter(novelId: String, name: String, description: String, role: String) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId) ?: return@launch
            val newCharacter = Character(name = name, description = description, role = role)
            novel.characters.add(newCharacter)
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    // ========== 自动模式核心流程 ==========
    /**
     * 开始全自动生成流程
     * 简化版：输入想法 → 一键生成 → 阅读/续写
     */
    fun startAutoMode(userPrompt: String) {
        if (_isGenerating.value) return
        
        viewModelScope.launch {
            _autoModeState.value = AutoModeState.GENERATING
            _isGenerating.value = true
            _streamingContent.value = ""
            _errorMessage.value = null
            
            try {
                // 如果还没有小说，创建一个
                val novel = _currentNovel.value ?: createTempNovel(userPrompt)
                val chapterNum = novel.chapters.size + 1
                
                // 构建Prompt
                val (systemPrompt, userPromptText) = SimplePromptBuilder.buildChapterPrompt(
                    novel = novel,
                    chapterNum = chapterNum,
                    userGuide = userPrompt,
                    preset = _currentPreset.value
                )
                
                // 调用API
                val apiConfig = _uiState.value.apiConfig
                if (apiConfig.apiKey.isBlank()) {
                    _errorMessage.value = "请先设置API Key"
                    _autoModeState.value = AutoModeState.IDLE
                    _isGenerating.value = false
                    return@launch
                }
                
                // 流式收集生成内容
                val fullContent = StringBuilder()
                apiService.generateContent(
                    apiKey = apiConfig.apiKey,
                    endpoint = apiConfig.endpoint,
                    model = apiConfig.model,
                    systemPrompt = systemPrompt,
                    userPrompt = userPromptText,
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
                    saveChapter(novel, chapterNum, fullContent.toString())
                }
                
                _autoModeState.value = AutoModeState.COMPLETED
                
            } catch (e: Exception) {
                _errorMessage.value = "生成失败: ${e.message}"
                _autoModeState.value = AutoModeState.IDLE
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    /**
     * 从一句话创意创建临时小说
     */
    private suspend fun createTempNovel(idea: String): Novel {
        val novel = Novel(
            title = "新小说",
            mainCharacter = idea
        )
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
        return novel
    }
    
    /**
     * 保存生成的章节
     */
    private suspend fun saveChapter(novel: Novel, chapterNum: Int, content: String) {
        // 尝试提取章节标题
        val title = extractChapterTitle(content, chapterNum)
        val cleanContent = cleanChapterContent(content, title)
        
        val chapter = Chapter(
            title = title,
            content = cleanContent,
            order = chapterNum,
            summary = "" // 可以后续调用API生成摘要
        )
        
        novel.chapters.add(chapter)
        novel.updatedAt = System.currentTimeMillis()
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    /**
     * 从内容中提取章节标题
     */
    private fun extractChapterTitle(content: String, defaultNum: Int): String {
        // 尝试匹配常见标题格式
        val patterns = listOf(
            Regex("""第[一二三四五六七八九十百千万\\d]+章[：:](.+)"""),
            Regex("""^第[一二三四五六七八九十百千万\\d]+章\s*(.+)""", RegexOption.MULTILINE),
            Regex("""^第\\d+章\s*(.+)""", RegexOption.MULTILINE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(content)
            if (match != null) {
                val title = match.groupValues[1].trim()
                if (title.isNotBlank() && title.length < 30) {
                    return title
                }
            }
        }
        
        return "第${defaultNum}章"
    }
    
    /**
     * 清理章节内容（去除标题重复等）
     */
    private fun cleanChapterContent(content: String, title: String): String {
        var cleaned = content
        
        // 移除标题行的重复
        if (title != "第${_currentNovel.value?.chapters?.size?.plus(1) ?: 1}章") {
            val titlePattern = Regex("""第[一二三四五六七八九十百千万\\d]+章[：:].+""")
            cleaned = cleaned.replaceFirst(titlePattern, "")
        }
        
        return cleaned.trim()
    }
    
    /**
     * 继续生成下一章
     */
    fun continueAutoMode(nextGuide: String?) {
        val novel = _currentNovel.value ?: return
        startAutoMode(nextGuide ?: "")
    }
    
    /**
     * 停止生成
     */
    fun stopGeneration() {
        generationJob?.cancel()
        _isGenerating.value = false
        _autoModeState.value = AutoModeState.IDLE
    }
    
    /**
     * 重置自动模式
     */
    fun resetAutoMode() {
        _autoModeState.value = AutoModeState.IDLE
        _streamingContent.value = ""
        _errorMessage.value = null
    }
    
    /**
     * 完成自动模式
     */
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
    val currentScreen: Screen = Screen.Home
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
