package dev.counterline.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.domain.GetProgressUseCase
import dev.counterline.core.model.ProgressStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ProgressUiState(
    val stats: ProgressStats = ProgressStats(
        totalDrillsCompleted = 0,
        whiteLineAccuracy = 0f,
        blackLineAccuracy = 0f,
        currentStreak = 0,
        longestStreak = 0,
        totalStudyTimeMinutes = 0,
        drillsCompletedToday = 0,
        dueForReview = 0,
    ),
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val getProgress: GetProgressUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState

    init {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        viewModelScope.launch {
            combine(
                getProgress(),
                getProgress.dueForReview(),
                getProgress.completedToday(todayStart),
            ) { allProgress, dueItems, completedToday ->
                val whiteItems = allProgress.filter { it.lineId.startsWith("WHITE", ignoreCase = true) }
                val blackItems = allProgress.filter { it.lineId.startsWith("BLACK", ignoreCase = true) }

                val whiteAcc = if (whiteItems.isNotEmpty()) {
                    whiteItems.sumOf { it.correctCount }.toFloat() /
                        whiteItems.sumOf { it.totalAttempts }.coerceAtLeast(1)
                } else 0f

                val blackAcc = if (blackItems.isNotEmpty()) {
                    blackItems.sumOf { it.correctCount }.toFloat() /
                        blackItems.sumOf { it.totalAttempts }.coerceAtLeast(1)
                } else 0f

                val maxStreak = allProgress.maxOfOrNull { it.streakDays } ?: 0

                ProgressStats(
                    totalDrillsCompleted = allProgress.sumOf { it.totalAttempts },
                    whiteLineAccuracy = whiteAcc,
                    blackLineAccuracy = blackAcc,
                    currentStreak = allProgress.maxOfOrNull { it.streakDays } ?: 0,
                    longestStreak = maxStreak,
                    totalStudyTimeMinutes = allProgress.size * 2, // rough estimate
                    drillsCompletedToday = completedToday,
                    dueForReview = dueItems.size,
                )
            }.collect { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }
    }
}
