package dev.counterline.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetSettingsUseCase
import dev.counterline.core.domain.UpdateSettingsUseCase
import dev.counterline.core.model.DarkMode
import dev.counterline.core.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettings: GetSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            getSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun setDarkMode(mode: DarkMode) {
        viewModelScope.launch { updateSettings.darkMode(mode) }
    }

    fun setBoardFlipped(flipped: Boolean) {
        viewModelScope.launch { updateSettings.boardFlipped(flipped) }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { updateSettings.dailyDrillGoal(goal) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { updateSettings.notifications(enabled) }
    }
}
