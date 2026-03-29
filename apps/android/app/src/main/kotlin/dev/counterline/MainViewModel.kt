package dev.counterline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.content.ContentSeeder
import dev.counterline.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val contentSeeder: ContentSeeder,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _onboardingComplete = MutableStateFlow(true)
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete

    init {
        viewModelScope.launch {
            contentSeeder.seedIfEmpty()
            _onboardingComplete.value = settingsRepository.isOnboardingComplete.first()
            _isReady.value = true
        }
    }

    fun onOnboardingComplete() {
        _onboardingComplete.value = true
    }
}
