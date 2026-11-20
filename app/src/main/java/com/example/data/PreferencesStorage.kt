package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.overtimePrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "overtime_tracker_local_prefs"
)

class PreferencesStorage(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val ACTIVE_OVERTIME_START_MILLIS = longPreferencesKey("active_overtime_start_millis")
    }

    val preferencesFlow: Flow<LocalAppPreferences> = context.overtimePrefsDataStore.data.map { prefs ->
        val startMillis = prefs[Keys.ACTIVE_OVERTIME_START_MILLIS]?.takeIf { it > 0L }
        LocalAppPreferences(
            themeMode = AppThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            language = AppLanguage.fromKey(prefs[Keys.LANGUAGE]),
            activeOvertimeStartMillis = startMillis
        )
    }

    suspend fun saveThemeMode(themeMode: AppThemeMode) {
        context.overtimePrefsDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.key
        }
    }

    suspend fun saveLanguage(language: AppLanguage) {
        context.overtimePrefsDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.key
        }
    }

    suspend fun setActiveOvertimeStartMillis(startMillis: Long?) {
        context.overtimePrefsDataStore.edit { prefs ->
            if (startMillis == null || startMillis <= 0L) {
                prefs.remove(Keys.ACTIVE_OVERTIME_START_MILLIS)
            } else {
                prefs[Keys.ACTIVE_OVERTIME_START_MILLIS] = startMillis
            }
        }
    }
}
