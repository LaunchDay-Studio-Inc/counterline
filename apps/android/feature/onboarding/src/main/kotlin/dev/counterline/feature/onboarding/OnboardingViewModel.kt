package dev.counterline.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.data.repository.SettingsRepository
import dev.counterline.core.domain.GetClaimsUseCase
import dev.counterline.core.model.SkillLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 5,
    val headline: String = "",
    val subtitle: String = "",
    val selectedSkillLevel: SkillLevel = SkillLevel.INTERMEDIATE,
    val selectedFocus: StudyFocus = StudyFocus.BOTH,
    val dailyGoal: Int = 10,
    val isComplete: Boolean = false,
)

enum class StudyFocus { WHITE, BLACK, BOTH }

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val claimsUseCase: GetClaimsUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        val claims = claimsUseCase.manifest()
        _uiState.update {
            it.copy(
                headline = claims.approved_headline,
                subtitle = claims.approved_subtitle,
            )
        }
    }

    fun nextPage() {
        _uiState.update { it.copy(currentPage = (it.currentPage + 1).coerceAtMost(it.totalPages - 1)) }
    }

    fun previousPage() {
        _uiState.update { it.copy(currentPage = (it.currentPage - 1).coerceAtLeast(0)) }
    }

    fun setSkillLevel(level: SkillLevel) {
        _uiState.update { it.copy(selectedSkillLevel = level) }
    }

    fun setStudyFocus(focus: StudyFocus) {
        _uiState.update { it.copy(selectedFocus = focus) }
    }

    fun setDailyGoal(goal: Int) {
        _uiState.update { it.copy(dailyGoal = goal) }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val state = _uiState.value
            settingsRepository.updateSkillLevel(state.selectedSkillLevel)
            settingsRepository.updateDailyDrillGoal(state.dailyGoal)
            settingsRepository.setOnboardingComplete(true)
            _uiState.update { it.copy(isComplete = true) }
        }
    }
}
