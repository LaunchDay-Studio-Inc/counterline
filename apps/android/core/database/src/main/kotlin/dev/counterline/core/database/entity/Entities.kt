package dev.counterline.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "repertoire_lines")
data class RepertoireLineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val family: String,
    val eco: String,
    val side: String,
    val seedLine: String,
    val exitFen: String,
    val exitEpd: String,
    val exitMoveNumber: Int,
    val specialistType: String,
    val specialistSize: String,
    val screeningRank: Int,
    val screeningScorePct: Double,
    val evaluationAtExit: String,
    val memoryHook: String,
    val memoryHookBreakdownJson: String,
    val skillLevel: String = "INTERMEDIATE",
)

@Entity(
    tableName = "repertoire_moves",
    foreignKeys = [ForeignKey(
        entity = RepertoireLineEntity::class,
        parentColumns = ["id"],
        childColumns = ["lineId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("lineId")],
)
data class RepertoireMoveEntity(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    val lineId: String,
    val moveNumber: Int,
    val san: String,
    val purpose: String,
    val isWhiteMove: Boolean,
    val whyThisMove: String = "",
    val keyPlanCallout: String = "",
)

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey val id: String,
    val side: String,
    val title: String,
    val description: String,
    val priority: Int,
    val skillLevel: String = "INTERMEDIATE",
)

@Entity(tableName = "themes")
data class ThemeEntity(
    @PrimaryKey val id: String,
    val side: String,
    val title: String,
    val description: String,
    val occurrenceRate: String?,
    val skillLevel: String = "INTERMEDIATE",
)

@Entity(tableName = "piece_placements")
data class PiecePlacementEntity(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    val piece: String,
    val idealSquare: String,
    val purpose: String,
    val side: String,
)

@Entity(tableName = "deviations")
data class DeviationEntity(
    @PrimaryKey val id: String,
    val side: String,
    val deviationName: String,
    val move: String,
    val description: String,
    val response: String,
    val strategicIdea: String = "",
    val skillLevel: String = "INTERMEDIATE",
)

@Entity(tableName = "model_games")
data class ModelGameEntity(
    @PrimaryKey val id: String,
    val title: String,
    val side: String,
    val opening: String,
    val result: String,
    val moveCount: Int,
    val keyTheme: String,
    val annotationsJson: String,
    val evaluationProgression: String,
)

@Entity(tableName = "drills")
data class DrillEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val question: String,
    val optionsJson: String?,
    val correctAnswer: String,
    val explanation: String,
    val side: String?,
    val fen: String?,
    val skillLevel: String = "INTERMEDIATE",
    val lineId: String? = null,
)

@Entity(tableName = "user_progress", indices = [Index("lineId"), Index("drillId")])
data class UserProgressEntity(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    val lineId: String,
    val drillId: String?,
    val correctCount: Int,
    val totalAttempts: Int,
    val lastReviewedEpochMs: Long,
    val nextReviewEpochMs: Long,
    val streakDays: Int,
)

@Entity(tableName = "node_review_states", indices = [Index("lineId"), Index("side")])
data class NodeReviewStateEntity(
    @PrimaryKey val nodeId: String,
    val side: String,
    val lineId: String,
    val easeFactor: Float = 2.5f,
    val intervalDays: Float = 0f,
    val repetitions: Int = 0,
    val lastReviewEpochMs: Long = 0,
    val nextReviewEpochMs: Long = 0,
    val lapseCount: Int = 0,
    val lastGrade: String = "GOOD",
)

@Entity(tableName = "mistake_items", indices = [Index("lineId"), Index("nodeId")])
data class MistakeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nodeId: String,
    val lineId: String,
    val side: String,
    val fen: String,
    val expectedMove: String,
    val userMove: String,
    val explanation: String,
    val createdEpochMs: Long,
    val nextReviewEpochMs: Long,
    val reviewCount: Int = 0,
    val resolved: Boolean = false,
    val mistakeTheme: String = "MOVE_ORDER",
    val severity: String = "MINOR",
    val consecutiveCorrect: Int = 0,
)

@Entity(tableName = "exam_results", indices = [Index("side")])
data class ExamResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val side: String,
    val startedEpochMs: Long,
    val finishedEpochMs: Long,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val accuracy: Float,
    val avgResponseTimeMs: Long,
    val branchCoverage: Float,
    val passed: Boolean,
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val side: String?,
    val startedEpochMs: Long,
    val endedEpochMs: Long = 0,
    val itemsCompleted: Int = 0,
    val correctCount: Int = 0,
)

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val side: String?,
    val earnedEpochMs: Long?,
    val iconName: String = "star",
)

@Entity(tableName = "quick_starts")
data class QuickStartEntity(
    @PrimaryKey(autoGenerate = true) val uid: Long = 0,
    val side: String,
    val lineName: String,
    val seedLine: String,
    val memoryHook: String,
    val memoryHookBreakdownJson: String,
    val threeKeyActionsJson: String,
    val exitFen: String,
    val exitEvaluation: String,
    val typicalResult: String,
)

// --- Phase 2: Tactical Motifs ---

@Entity(tableName = "tactical_motifs", indices = [Index("lineId"), Index("side")])
data class TacticalMotifEntity(
    @PrimaryKey val id: String,
    val lineId: String,
    val side: String,
    val fen: String,
    val solutionSanJson: String,
    val motifType: String,
    val difficulty: String = "INTERMEDIATE",
    val explanation: String,
    val repetitionCycle: Int = 0,
    val lastAttemptCorrect: Boolean? = null,
)

// --- Phase 2: Transition Plans ---

@Entity(tableName = "transition_plans", indices = [Index("lineId")])
data class TransitionPlanEntity(
    @PrimaryKey val id: String,
    val lineId: String,
    val side: String,
    val tabiyaFen: String,
    val pawnBreaksJson: String,
    val strategicGoalsJson: String,
    val endgameTendency: String = "",
    val skillLevel: String = "INTERMEDIATE",
)

// --- Phase 4: User Notes ---

@Entity(tableName = "user_notes", indices = [Index("lineId"), Index("nodeId")])
data class UserNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nodeId: String,
    val lineId: String,
    val side: String,
    val content: String,
    val createdEpochMs: Long,
    val updatedEpochMs: Long,
)

// --- Phase 4: Bookmarks ---

@Entity(tableName = "bookmarks", indices = [Index("lineId")])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lineId: String,
    val nodeId: String?,
    val side: String,
    val label: String,
    val fen: String,
    val createdEpochMs: Long,
    val isFavorite: Boolean = false,
    val isTabiya: Boolean = false,
)

// --- Phase 4: Imported Games ---

@Entity(tableName = "imported_games")
data class ImportedGameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pgn: String,
    val white: String,
    val black: String,
    val result: String,
    val date: String,
    val opening: String = "",
    val importedEpochMs: Long,
    val deviationMoveNumber: Int? = null,
    val deviationSide: String? = null,
    val matchedLineId: String? = null,
)

// --- Phase 4: Repertoire Snapshots ---

@Entity(tableName = "repertoire_snapshots")
data class RepertoireSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val version: String,
    val createdEpochMs: Long,
    val changesSummary: String,
    val linesAddedJson: String = "[]",
    val linesRemovedJson: String = "[]",
    val linesModifiedJson: String = "[]",
)

// --- Phase 5: Achievements ---

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val earnedEpochMs: Long? = null,
    val progress: Float = 0f,
    val target: Float = 1f,
)
