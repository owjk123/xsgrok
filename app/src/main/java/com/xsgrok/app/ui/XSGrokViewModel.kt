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
    
    fun deleteCharacter(novelId: String, characterId: String) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId) ?: return@launch
            novel.characters.removeAll { it.id == characterId }
            localStorage.saveNovel(novel)
            _currentNovel.value = novel
        }
    }
    
    fun updateCharacter(novelId: String, character: Character) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId) ?: return@launch
            val index = novel.characters.indexOfFirst { it.id == character.id }
            if (index >= 0) {
                novel.characters[index] = character
                localStorage.saveNovel(novel)
                _currentNovel.value = novel
            }
        }
    }
    
    // ========== 继续写作（从书架） ==========
    fun continueNovel(novelId: String) {
        viewModelScope.launch {
            val novel = localStorage.getNovel(novelId) ?: return@launch
            _currentNovel.value = novel
            _uiState.value = _uiState.value.copy(currentScreen = Screen.Reading)
        }
    }
    
    // ========== 章节生成 ==========
    fun generateChapter(novelId: String, chapterTitle: String?) {
        if (_isGenerating.value) return
        
        viewModelScope.launch {
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
                    novel.chapters.add(chapter)
                    localStorage.saveNovel(novel)
                    _currentNovel.value = novel
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
        
        viewModelScope.launch {
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
                    novel.chapters.add(chapter)
                    localStorage.saveNovel(novel)
                    _currentNovel.value = novel
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "生成失败: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    // ========== AI生成辅助 ==========
    fun generateWorldBuilding(novelId: String, prompt: String) {
        // 简化实现：暂不支持
    }
    
    fun generateCharacters(novelId: String, prompt: String) {
        // 简化实现：暂不支持
    }
    
    fun addCharacterFull(novelId: String, name: String, role: String, description: String) {
        addCharacter(novelId, name, description, role)
    }
    
    // ========== 自动模式核心流程 ==========
    fun startAutoMode(userPrompt: String) {
        if (_isGenerating.value) return
        
        viewModelScope.launch {
            _autoModeState.value = AutoModeState.GENERATING
            _isGenerating.value = true
            _streamingContent.value = ""
            _errorMessage.value = null
            
            try {
                val novel = _currentNovel.value ?: createTempNovel(userPrompt)
                val chapterNum = novel.chapters.size + 1
                
                val (systemPrompt, userPromptText) = SimplePromptBuilder.buildChapterPrompt(
                    novel = novel,
                    chapterNum = chapterNum,
                    userGuide = userPrompt,
                    preset = _currentPreset.value
                )
                
                val apiConfig = _uiState.value.apiConfig
                if (apiConfig.apiKey.isBlank()) {
                    _errorMessage.value = "请先设置API Key"
                    _autoModeState.value = AutoModeState.IDLE
                    _isGenerating.value = false
                    return@launch
                }
                
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
    
    private suspend fun createTempNovel(idea: String): Novel {
        val novel = Novel(
            title = "新小说",
            mainCharacter = idea
        )
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
        return novel
    }
    
    private suspend fun saveChapter(novel: Novel, chapterNum: Int, content: String) {
        val title = extractChapterTitle(content, chapterNum)
        val cleanContent = cleanChapterContent(content, title)
        
        val chapter = Chapter(
            title = title,
            content = cleanContent,
            order = chapterNum
        )
        
        novel.chapters.add(chapter)
        novel.updatedAt = System.currentTimeMillis()
        localStorage.saveNovel(novel)
        _currentNovel.value = novel
    }
    
    private fun extractChapterTitle(content: String, defaultNum: Int): String {
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
    
    private fun cleanChapterContent(content: String, title: String): String {
        var cleaned = content
        val titlePattern = Regex("""第[一二三四五六七八九十百千万\\d]+章[：:].+""")
        cleaned = cleaned.replaceFirst(titlePattern, "")
        return cleaned.trim()
    }
    
    fun continueAutoMode(nextGuide: String?) {
        val novel = _currentNovel.value ?: return
        startAutoMode(nextGuide ?: "")
    }
    
    fun stopGeneration() {
        generationJob?.cancel()
        _isGenerating.value = false
        _autoModeState.value = AutoModeState.IDLE
    }
    
    fun resetAutoMode() {
        _autoModeState.value = AutoModeState.IDLE
        _streamingContent.value = ""
        _errorMessage.value = null
    }
    
    fun finishAutoMode() {
        _autoModeState.value = AutoModeState.COMPLETED
        navigateTo(Screen.Reading)
    }
    
    // ========== 模式选择 ==========
    fun setGenerationPreset(presetId: String) {
        _currentPreset.value = GenerationPresets.getById(presetId)
    }
    
    // ========== 兼容性方法 ==========
    val generationMode: StateFlow<String> = MutableStateFlow("single")
    
    fun setGenerationMode(mode: String) {
        // 兼容性方法
    }
    
    val currentPresetId: StateFlow<String> = MutableStateFlow("balanced")
    
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
