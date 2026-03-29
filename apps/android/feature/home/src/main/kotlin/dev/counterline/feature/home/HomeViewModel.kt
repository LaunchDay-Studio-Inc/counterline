package dev.counterline.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetClaimsUseCase
import dev.counterline.core.domain.GetProgressUseCase
import dev.counterline.core.domain.GetQuickStartsUseCase
import dev.counterline.core.model.ClaimsManifest
import dev.counterline.core.model.ProofSummary
import dev.counterline.core.model.QuickStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val headline: String = "",
    val subtitle: String = "",
    val badges: List<String> = emptyList(),
    val disclaimers: List<String> = emptyList(),
    val proofSummary: ProofSummary? = null,
    val quickStarts: List<QuickStart> = emptyList(),
    val drillsCompletedToday: Int = 0,
    val dailyGoal: Int = 10,
    val dueForReview: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getClaims: GetClaimsUseCase,
    private val getQuickStarts: GetQuickStartsUseCase,
    private val getProgress: GetProgressUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        // Load claims (synchronous from cache/assets)
        val manifest = getClaims.manifest()
        val summary = getClaims.proofSummary()
        _uiState.update {
            it.copy(
                headline = manifest.approved_headline,
                subtitle = manifest.approved_subtitle,
                badges = manifest.approved_badges,
                disclaimers = manifest.required_disclaimers,
                proofSummary = summary,
            )
        }

        // Load quick starts
        viewModelScope.launch {
            getQuickStarts().collect { qs ->
                _uiState.update { it.copy(quickStarts = qs) }
            }
        }

        // Load today's progress
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        viewModelScope.launch {
            getProgress.completedToday(todayStart).collect { count ->
                _uiState.update { it.copy(drillsCompletedToday = count) }
            }
        }

        viewModelScope.launch {
            getProgress.dueForReview().collect { due ->
                _uiState.update { it.copy(dueForReview = due.size) }
            }
        }
    }
}
