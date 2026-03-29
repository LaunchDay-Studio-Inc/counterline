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
    val memoryHookBreakdownJson: String, // serialized List<String>
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
)

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey val id: String,
    val side: String,
    val title: String,
    val description: String,
    val priority: Int,
)

@Entity(tableName = "themes")
data class ThemeEntity(
    @PrimaryKey val id: String,
    val side: String,
    val title: String,
    val description: String,
    val occurrenceRate: String?,
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
    val annotationsJson: String, // serialized List<GameAnnotation>
    val evaluationProgression: String,
)

@Entity(tableName = "drills")
data class DrillEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val question: String,
    val optionsJson: String?, // serialized List<String>?
    val correctAnswer: String,
    val explanation: String,
    val side: String?,
    val fen: String?,
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
