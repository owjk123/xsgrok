package com.xsgrok.app.data.local

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xsgrok.app.data.model.ApiConfig
import com.xsgrok.app.data.model.Novel
import com.xsgrok.app.data.model.NovelFoundation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "xsgrok_prefs")

/**
 * 本地存储 - 安全增强版
 * - API Key使用Base64编码存储
 * - 增加数据迁移支持
 */
class LocalStorage(private val context: Context) {
    
    private val gson = Gson()
    
    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val API_KEY_ENCODED = stringPreferencesKey("api_key_encoded")
        private val API_ENDPOINT = stringPreferencesKey("api_endpoint")
        private val API_MODEL = stringPreferencesKey("api_model")
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val NOVELS = stringPreferencesKey("novels")
        private val CURRENT_NOVEL_ID = stringPreferencesKey("current_novel_id")
        
        // Base64编码辅助
        private fun encode(str: String): String = Base64.encodeToString(str.toByteArray(), Base64.NO_WRAP)
        private fun decode(str: String): String = String(Base64.decode(str, Base64.NO_WRAP))
    }
    
    /**
     * 安全地解析小说列表，处理旧数据迁移
     */
    private fun safeParseNovels(json: String): List<Novel> {
        return try {
            val type = object : TypeToken<List<Novel>>() {}.type
            val novels: List<Novel>? = gson.fromJson<List<Novel>>(json, type)
            novels?.map { novel ->
                val safeFoundation = try {
                    novel.foundation ?: NovelFoundation()
                } catch (e: NullPointerException) {
                    NovelFoundation()
                }
                novel.copy(
                    foundation = safeFoundation,
                    chapters = novel.chapters ?: mutableListOf(),
                    characters = novel.characters ?: mutableListOf()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    val apiConfig: Flow<ApiConfig> = context.dataStore.data.map { prefs ->
        // 优先读取编码后的API Key
        val encodedKey = prefs[API_KEY_ENCODED]
        val apiKey = if (encodedKey != null) {
            try { decode(encodedKey) } catch (e: Exception) { prefs[API_KEY] ?: "" }
        } else {
            prefs[API_KEY] ?: ""
        }
        
        ApiConfig(
            apiKey = apiKey,
            endpoint = prefs[API_ENDPOINT] ?: "https://api.edgefn.net/v1",
            model = prefs[API_MODEL] ?: "GLM-5.1",
            isDarkMode = prefs[DARK_MODE] ?: false
        )
    }
    
    /**
     * 保存API配置 - API Key使用Base64编码
     */
    suspend fun saveApiConfig(config: ApiConfig) {
        context.dataStore.edit { prefs ->
            // 保存API Key（编码后）
            if (config.apiKey.isNotBlank()) {
                prefs[API_KEY_ENCODED] = encode(config.apiKey)
                prefs[API_KEY] = ""  // 清除明文
            } else {
                prefs[API_KEY] = ""
                prefs[API_KEY_ENCODED] = ""
            }
            prefs[API_ENDPOINT] = config.endpoint
            prefs[API_MODEL] = config.model
            prefs[DARK_MODE] = config.isDarkMode
        }
    }
    
    val novels: Flow<List<Novel>> = context.dataStore.data.map { prefs ->
        val json = prefs[NOVELS] ?: "[]"
        safeParseNovels(json)
    }
    
    suspend fun saveNovel(novel: Novel) {
        context.dataStore.edit { prefs ->
            val json = prefs[NOVELS] ?: "[]"
            val novelList = safeParseNovels(json).toMutableList()
            
            val existingIndex = novelList.indexOfFirst { it.id == novel.id }
            if (existingIndex >= 0) {
                novelList[existingIndex] = novel.copy(updatedAt = System.currentTimeMillis())
            } else {
                novelList.add(novel)
            }
            
            prefs[NOVELS] = gson.toJson(novelList)
        }
    }
    
    suspend fun deleteNovel(novelId: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[NOVELS] ?: "[]"
            val novelList = safeParseNovels(json).toMutableList()
            novelList.removeAll { it.id == novelId }
            prefs[NOVELS] = gson.toJson(novelList)
        }
    }
    
    suspend fun getNovel(novelId: String): Novel? {
        return try {
            context.dataStore.data.map { prefs ->
                val json = prefs[NOVELS] ?: "[]"
                safeParseNovels(json).find { it.id == novelId }
            }.first()
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
