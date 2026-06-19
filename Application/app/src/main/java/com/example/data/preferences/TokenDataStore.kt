package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.api.ApiUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "auth_prefs")

class TokenDataStore(private val context: Context) {

    companion object {
        val TOKEN_KEY      = stringPreferencesKey("jwt_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        val USER_ID_KEY    = intPreferencesKey("user_id")
        val USER_NAME_KEY  = stringPreferencesKey("user_name")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        val USER_ROLE_KEY  = stringPreferencesKey("user_role")
        val THEME_KEY      = stringPreferencesKey("theme_dark")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }
    val userId: Flow<Int?>   = context.dataStore.data.map { it[USER_ID_KEY] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME_KEY] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL_KEY] }
    val userRole: Flow<String?> = context.dataStore.data.map { it[USER_ROLE_KEY] }
    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val saved = prefs[THEME_KEY]
        if (saved != null) {
            saved == "true"
        } else {
            val uiMode = context.resources.configuration.uiMode
            (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }

    suspend fun saveAuth(token: String, refreshToken: String, user: ApiUser) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY]         = token
            prefs[REFRESH_TOKEN_KEY] = refreshToken
            prefs[USER_ID_KEY]       = user.id
            prefs[USER_NAME_KEY]     = user.name
            prefs[USER_EMAIL_KEY]    = user.email
            prefs[USER_ROLE_KEY]     = user.role
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
            prefs.remove(USER_NAME_KEY)
            prefs.remove(USER_EMAIL_KEY)
            prefs.remove(USER_ROLE_KEY)
        }
    }

    suspend fun saveTheme(isDark: Boolean) {
        context.dataStore.edit { it[THEME_KEY] = if (isDark) "true" else "false" }
    }
}
