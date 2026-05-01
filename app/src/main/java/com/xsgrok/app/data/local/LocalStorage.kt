package com.xsgrok.app.data.local

import android.content.Context
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

class LocalStorage(private val context: Context) {
    
    private val gson = Gson()
    
    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val API_ENDPOINT = stringPreferencesKey("api_endpoint")
        private val API_MODEL = stringPreferencesKey("api_model")
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val NOVELS = stringPreferencesKey("novels")
        private val CURRENT_NOVEL_ID = stringPreferencesKey("current_novel_id")
    }
    
    /**
     * 安全地解析小说列表，处理旧数据迁移
     * 旧版Novel没有foundation字段，Gson会设为null，需要补上默认值
     */
    private fun safeParseNovels(json: String): List<Novel> {
        return try {
            val type = object : TypeToken<List<Novel>>() {}.type
            val novels: List<Novel>? = gson.fromJson<List<Novel>>(json, type)
            novels?.map { novel ->
                // 修复Gson反序列化null问题：旧数据没有foundation字段
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
            // 反序列化完全失败时返回空列表，防止闪退
            emptyList()
        }
    }
    
    val apiConfig: Flow<ApiConfig> = context.dataStore.data.map { prefs ->
        ApiConfig(
            apiKey = prefs[API_KEY] ?: "",
            endpoint = prefs[API_ENDPOINT] ?: "https://api.edgefn.net/v1",
            model = prefs[API_MODEL] ?: "GLM-5.1",
            isDarkMode = prefs[DARK_MODE] ?: false
        )
    }
    
    suspend fun saveApiConfig(config: ApiConfig) {
        context.dataStore.edit { prefs ->
            prefs[API_KEY] = config.apiKey
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
}
