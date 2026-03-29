package dev.counterline.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.counterline.core.model.DarkMode
import dev.counterline.core.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val BOARD_FLIPPED = booleanPreferencesKey("board_flipped")
        val DAILY_DRILL_GOAL = intPreferencesKey("daily_drill_goal")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            darkMode = prefs[Keys.DARK_MODE]?.let { DarkMode.valueOf(it) } ?: DarkMode.SYSTEM,
            boardFlipped = prefs[Keys.BOARD_FLIPPED] ?: false,
            dailyDrillGoal = prefs[Keys.DAILY_DRILL_GOAL] ?: 10,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            notificationHour = prefs[Keys.NOTIFICATION_HOUR] ?: 9,
        )
    }

    suspend fun updateDarkMode(mode: DarkMode) {
        context.dataStore.edit { it[Keys.DARK_MODE] = mode.name }
    }

    suspend fun updateBoardFlipped(flipped: Boolean) {
        context.dataStore.edit { it[Keys.BOARD_FLIPPED] = flipped }
    }

    suspend fun updateDailyDrillGoal(goal: Int) {
        context.dataStore.edit { it[Keys.DAILY_DRILL_GOAL] = goal }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun updateNotificationHour(hour: Int) {
        context.dataStore.edit { it[Keys.NOTIFICATION_HOUR] = hour }
    }
}
