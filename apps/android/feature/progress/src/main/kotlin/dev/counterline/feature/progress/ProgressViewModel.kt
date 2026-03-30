package dev.counterline.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.counterline.core.data.repository.AchievementRepository
import dev.counterline.core.domain.CalculateReadinessUseCase
import dev.counterline.core.domain.DetectWeaknessesUseCase
import dev.counterline.core.domain.GetBadgesUseCase
import dev.counterline.core.domain.GetExamResultsUseCase
import dev.counterline.core.domain.GetMasteryUseCase
import dev.counterline.core.domain.GetMistakesUseCase
import dev.counterline.core.domain.GetProgressUseCase
import dev.counterline.core.domain.GetStudySessionsUseCase
import dev.counterline.core.domain.GetWeeklyArcsUseCase
import dev.counterline.core.model.Achievement
import dev.counterline.core.model.Badge
import dev.counterline.core.model.ExamResult
import dev.counterline.core.model.NodeReviewState
import dev.counterline.core.model.ProgressStats
import dev.counterline.core.model.ReadinessScore
import dev.counterline.core.model.Side
import dev.counterline.core.model.StudyMode
import dev.counterline.core.model.WeeklyArc
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val whiteMastery: Float = 0f,
    val blackMastery: Float = 0f,
    val badges: List<Badge> = emptyList(),
    val weakestNodes: List<NodeReviewState> = emptyList(),
    val unresolvedMistakes: Int = 0,
    val whiteExamBest: ExamResult? = null,
    val blackExamBest: ExamResult? = null,
    val currentStreak: Int = 0,
    val totalStudyTimeMs: Long = 0,
    // Phase 5: readiness, arcs, achievements, weaknesses
    val whiteReadiness: ReadinessScore? = null,
    val blackReadiness: ReadinessScore? = null,
    val weeklyArcs: List<WeeklyArc> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val chronicMissNodes: List<String> = emptyList(),
    val fragileLines: List<String> = emptyList(),
    val sideImbalance: Float = 0f,
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val getProgress: GetProgressUseCase,
    private val getMastery: GetMasteryUseCase,
    private val getBadges: GetBadgesUseCase,
    private val getMistakes: GetMistakesUseCase,
    private val getExamResults: GetExamResultsUseCase,
    private val getStudySessions: GetStudySessionsUseCase,
    private val calculateReadiness: CalculateReadinessUseCase,
    private val detectWeaknesses: DetectWeaknessesUseCase,
    private val getWeeklyArcs: GetWeeklyArcsUseCase,
    private val achievementRepo: AchievementRepository,
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
                    totalStudyTimeMinutes = allProgress.size * 2,
                    drillsCompletedToday = completedToday,
                    dueForReview = dueItems.size,
                )
            }.collect { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }

        viewModelScope.launch {
            getMastery.forSide(Side.WHITE).collect { mastery ->
                _uiState.update { it.copy(whiteMastery = mastery) }
            }
        }

        viewModelScope.launch {
            getMastery.forSide(Side.BLACK).collect { mastery ->
                _uiState.update { it.copy(blackMastery = mastery) }
            }
        }

        viewModelScope.launch {
            getBadges.earned().collect { badges ->
                _uiState.update { it.copy(badges = badges) }
            }
        }

        viewModelScope.launch {
            getMastery.weakestNodes(5).collect { nodes ->
                _uiState.update { it.copy(weakestNodes = nodes) }
            }
        }

        viewModelScope.launch {
            getMistakes.unresolvedCount().collect { count ->
                _uiState.update { it.copy(unresolvedMistakes = count) }
            }
        }

        viewModelScope.launch {
            val whiteBest = getExamResults.bestBySide(Side.WHITE)
            val blackBest = getExamResults.bestBySide(Side.BLACK)
            _uiState.update { it.copy(whiteExamBest = whiteBest, blackExamBest = blackBest) }
        }

        viewModelScope.launch {
            val streak = getStudySessions.currentStreak()
            _uiState.update { it.copy(currentStreak = streak) }
        }

        viewModelScope.launch {
            getStudySessions.totalStudyTimeMs().collect { time ->
                _uiState.update { it.copy(totalStudyTimeMs = time ?: 0L) }
            }
        }

        // Phase 5: readiness scores
        viewModelScope.launch {
            val whiteReadiness = calculateReadiness(Side.WHITE)
            val blackReadiness = calculateReadiness(Side.BLACK)
            _uiState.update {
                it.copy(
                    whiteReadiness = whiteReadiness,
                    blackReadiness = blackReadiness,
                )
            }
        }

        // Phase 5: weaknesses
        viewModelScope.launch {
            val weaknesses = detectWeaknesses()
            _uiState.update {
                it.copy(
                    chronicMissNodes = weaknesses.chronicMissNodes,
                    fragileLines = weaknesses.fragileLines,
                    sideImbalance = weaknesses.sideImbalance,
                )
            }
        }

        // Phase 5: weekly arcs
        viewModelScope.launch {
            getWeeklyArcs(4).collect { arcs ->
                _uiState.update { it.copy(weeklyArcs = arcs) }
            }
        }

        // Phase 5: achievements
        viewModelScope.launch {
            achievementRepo.getAll().collect { achievements ->
                _uiState.update { it.copy(achievements = achievements) }
            }
        }
    }
}
