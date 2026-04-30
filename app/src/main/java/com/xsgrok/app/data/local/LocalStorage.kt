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
        val type = object : TypeToken<List<Novel>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    }
    
    suspend fun saveNovel(novel: Novel) {
        context.dataStore.edit { prefs ->
            val json = prefs[NOVELS] ?: "[]"
            val type = object : TypeToken<MutableList<Novel>>() {}.type
            val novelList: MutableList<Novel> = gson.fromJson(json, type) ?: mutableListOf()
            
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
            val type = object : TypeToken<MutableList<Novel>>() {}.type
            val novelList: MutableList<Novel> = gson.fromJson(json, type) ?: mutableListOf()
            novelList.removeAll { it.id == novelId }
            prefs[NOVELS] = gson.toJson(novelList)
        }
    }
    
    suspend fun getNovel(novelId: String): Novel? {
        return context.dataStore.data.map { prefs ->
            val json = prefs[NOVELS] ?: "[]"
            val type = object : TypeToken<List<Novel>>() {}.type
            val novelList: List<Novel> = gson.fromJson(json, type) ?: emptyList()
            novelList.find { it.id == novelId }
        }.first()
    }
}
