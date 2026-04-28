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
            _errorMessage.value = "Please configure API Key first"
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
            _errorMessage.value = "Please configure API Key first"
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
    
    private fun buildChapterSystemPrompt(novel: Novel): String {
        return """
            You are a professional Chinese novel writer with years of experience in creative writing.
            You specialize in writing engaging ${novel.type} novels with ${novel.style} style.
            
            Guidelines:
            1. Write in Chinese
            2. Use vivid descriptions and engaging storytelling
            3. Create compelling characters that fit the ${novel.mainCharacter} as protagonist
            4. Use proper novel formatting with dialogue, descriptions, and scene transitions
            5. Write chapters of 2000-5000 Chinese characters
            6. Keep readers engaged with cliffhangers and plot twists
        """.trimIndent()
    }
    
    private fun buildChapterUserPrompt(novel: Novel, chapterTitle: String): String {
        val previousContent = novel.chapters.lastOrNull()?.content ?: ""
        val characters = novel.characters.joinToString("\n") { 
            "- ${it.name} (${it.role}): ${it.description}" 
        }
        
        return """
            Write a chapter titled "$chapterTitle" for the novel "${novel.title}".
            
            Novel Type: ${novel.type}
            Writing Style: ${novel.style}
            Main Character: ${novel.mainCharacter}
            
            ${if (characters.isNotBlank()) "Characters:\n$characters" else ""}
            
            ${if (previousContent.isNotBlank()) "Previous Chapter Content (for continuity):\n$previousContent" else ""}
            
            Please write this chapter following the established style and continuing the story naturally.
        """.trimIndent()
    }
    
    private fun buildContinueSystemPrompt(novel: Novel): String {
        return """
            You are continuing a Chinese novel "${novel.title}".
            Continue writing in the same style and maintain character consistency.
            Write in Chinese with proper novel formatting.
        """.trimIndent()
    }
    
    private fun buildContinueUserPrompt(lastChapter: Chapter): String {
        return """
            Continue the following chapter naturally:
            
            ${lastChapter.content}
            
            Please continue the story from where it left off.
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
    ChapterGeneration
}
