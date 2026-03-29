package dev.counterline.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.counterline.core.database.entity.BadgeEntity
import dev.counterline.core.database.entity.DeviationEntity
import dev.counterline.core.database.entity.DrillEntity
import dev.counterline.core.database.entity.ExamResultEntity
import dev.counterline.core.database.entity.MistakeItemEntity
import dev.counterline.core.database.entity.ModelGameEntity
import dev.counterline.core.database.entity.NodeReviewStateEntity
import dev.counterline.core.database.entity.PiecePlacementEntity
import dev.counterline.core.database.entity.PlanEntity
import dev.counterline.core.database.entity.QuickStartEntity
import dev.counterline.core.database.entity.RepertoireLineEntity
import dev.counterline.core.database.entity.RepertoireMoveEntity
import dev.counterline.core.database.entity.StudySessionEntity
import dev.counterline.core.database.entity.ThemeEntity
import dev.counterline.core.database.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepertoireDao {
    @Query("SELECT * FROM repertoire_lines ORDER BY screeningRank ASC")
    fun getAllLines(): Flow<List<RepertoireLineEntity>>

    @Query("SELECT * FROM repertoire_lines WHERE side = :side ORDER BY screeningRank ASC")
    fun getLinesBySide(side: String): Flow<List<RepertoireLineEntity>>

    @Query("SELECT * FROM repertoire_lines WHERE id = :id")
    suspend fun getLineById(id: String): RepertoireLineEntity?

    @Query("SELECT * FROM repertoire_moves WHERE lineId = :lineId ORDER BY moveNumber ASC")
    suspend fun getMovesForLine(lineId: String): List<RepertoireMoveEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<RepertoireLineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoves(moves: List<RepertoireMoveEntity>)

    @Transaction
    suspend fun insertLineWithMoves(
        line: RepertoireLineEntity,
        moves: List<RepertoireMoveEntity>,
    ) {
        insertLines(listOf(line))
        insertMoves(moves)
    }
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans ORDER BY priority ASC")
    fun getAllPlans(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans WHERE side = :side ORDER BY priority ASC")
    fun getPlansBySide(side: String): Flow<List<PlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<PlanEntity>)
}

@Dao
interface ThemeDao {
    @Query("SELECT * FROM themes")
    fun getAllThemes(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE side = :side")
    fun getThemesBySide(side: String): Flow<List<ThemeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThemes(themes: List<ThemeEntity>)
}

@Dao
interface PiecePlacementDao {
    @Query("SELECT * FROM piece_placements WHERE side = :side")
    fun getPlacementsBySide(side: String): Flow<List<PiecePlacementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlacements(placements: List<PiecePlacementEntity>)
}

@Dao
interface DeviationDao {
    @Query("SELECT * FROM deviations")
    fun getAllDeviations(): Flow<List<DeviationEntity>>

    @Query("SELECT * FROM deviations WHERE side = :side")
    fun getDeviationsBySide(side: String): Flow<List<DeviationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviations(deviations: List<DeviationEntity>)
}

@Dao
interface ModelGameDao {
    @Query("SELECT * FROM model_games")
    fun getAllGames(): Flow<List<ModelGameEntity>>

    @Query("SELECT * FROM model_games WHERE side = :side")
    fun getGamesBySide(side: String): Flow<List<ModelGameEntity>>

    @Query("SELECT * FROM model_games WHERE id = :id")
    suspend fun getGameById(id: String): ModelGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<ModelGameEntity>)
}

@Dao
interface DrillDao {
    @Query("SELECT * FROM drills")
    fun getAllDrills(): Flow<List<DrillEntity>>

    @Query("SELECT * FROM drills WHERE side = :side")
    fun getDrillsBySide(side: String): Flow<List<DrillEntity>>

    @Query("SELECT * FROM drills WHERE type = :type")
    fun getDrillsByType(type: String): Flow<List<DrillEntity>>

    @Query("SELECT * FROM drills WHERE id = :id")
    suspend fun getDrillById(id: String): DrillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrills(drills: List<DrillEntity>)
}

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress")
    fun getAllProgress(): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress WHERE lineId = :lineId")
    fun getProgressForLine(lineId: String): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress WHERE drillId = :drillId")
    suspend fun getProgressForDrill(drillId: String): UserProgressEntity?

    @Query("SELECT * FROM user_progress WHERE nextReviewEpochMs <= :now")
    fun getDueForReview(now: Long): Flow<List<UserProgressEntity>>

    @Query("SELECT COUNT(*) FROM user_progress WHERE lastReviewedEpochMs >= :dayStart")
    fun getDrillsCompletedToday(dayStart: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: UserProgressEntity)

    @Query("SELECT MAX(streakDays) FROM user_progress")
    suspend fun getLongestStreak(): Int?
}

// --- New DAOs for training system ---

@Dao
interface NodeReviewStateDao {
    @Query("SELECT * FROM node_review_states WHERE nodeId = :nodeId")
    suspend fun getByNodeId(nodeId: String): NodeReviewStateEntity?

    @Query("SELECT * FROM node_review_states WHERE lineId = :lineId")
    fun getByLineId(lineId: String): Flow<List<NodeReviewStateEntity>>

    @Query("SELECT * FROM node_review_states WHERE side = :side")
    fun getBySide(side: String): Flow<List<NodeReviewStateEntity>>

    @Query("SELECT * FROM node_review_states WHERE nextReviewEpochMs <= :now ORDER BY nextReviewEpochMs ASC")
    fun getDueForReview(now: Long): Flow<List<NodeReviewStateEntity>>

    @Query("SELECT COUNT(*) FROM node_review_states WHERE nextReviewEpochMs <= :now")
    fun getDueCount(now: Long): Flow<Int>

    @Query("SELECT * FROM node_review_states ORDER BY easeFactor ASC, lapseCount DESC LIMIT :limit")
    fun getWeakestNodes(limit: Int): Flow<List<NodeReviewStateEntity>>

    @Query("SELECT * FROM node_review_states")
    fun getAll(): Flow<List<NodeReviewStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: NodeReviewStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(states: List<NodeReviewStateEntity>)
}

@Dao
interface MistakeItemDao {
    @Query("SELECT * FROM mistake_items WHERE resolved = 0 ORDER BY nextReviewEpochMs ASC")
    fun getUnresolved(): Flow<List<MistakeItemEntity>>

    @Query("SELECT * FROM mistake_items WHERE nextReviewEpochMs <= :now AND resolved = 0")
    fun getDueForReview(now: Long): Flow<List<MistakeItemEntity>>

    @Query("SELECT * FROM mistake_items WHERE lineId = :lineId AND resolved = 0")
    fun getByLineId(lineId: String): Flow<List<MistakeItemEntity>>

    @Query("SELECT * FROM mistake_items ORDER BY createdEpochMs DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<MistakeItemEntity>>

    @Query("SELECT COUNT(*) FROM mistake_items WHERE resolved = 0")
    fun getUnresolvedCount(): Flow<Int>

    @Insert
    suspend fun insert(item: MistakeItemEntity): Long

    @Update
    suspend fun update(item: MistakeItemEntity)

    @Query("UPDATE mistake_items SET resolved = 1 WHERE id = :id")
    suspend fun markResolved(id: Long)
}

@Dao
interface ExamResultDao {
    @Query("SELECT * FROM exam_results ORDER BY finishedEpochMs DESC")
    fun getAll(): Flow<List<ExamResultEntity>>

    @Query("SELECT * FROM exam_results WHERE side = :side ORDER BY finishedEpochMs DESC")
    fun getBySide(side: String): Flow<List<ExamResultEntity>>

    @Query("SELECT * FROM exam_results WHERE side = :side ORDER BY accuracy DESC LIMIT 1")
    suspend fun getBestBySide(side: String): ExamResultEntity?

    @Insert
    suspend fun insert(result: ExamResultEntity): Long
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY startedEpochMs DESC")
    fun getAll(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE startedEpochMs >= :since")
    fun getSince(since: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT SUM(endedEpochMs - startedEpochMs) FROM study_sessions WHERE endedEpochMs > 0")
    fun getTotalStudyTimeMs(): Flow<Long?>

    @Query("SELECT SUM(endedEpochMs - startedEpochMs) FROM study_sessions WHERE mode = :mode AND endedEpochMs > 0")
    fun getStudyTimeMsByMode(mode: String): Flow<Long?>

    @Query("SELECT DISTINCT(startedEpochMs / 86400000) as studyDay FROM study_sessions ORDER BY studyDay DESC")
    suspend fun getDistinctStudyDays(): List<Long>

    @Insert
    suspend fun insert(session: StudySessionEntity): Long

    @Update
    suspend fun update(session: StudySessionEntity)
}

@Dao
interface BadgeDao {
    @Query("SELECT * FROM badges")
    fun getAll(): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges WHERE earnedEpochMs IS NOT NULL")
    fun getEarned(): Flow<List<BadgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(badge: BadgeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(badges: List<BadgeEntity>)
}

@Dao
interface QuickStartDao {
    @Query("SELECT * FROM quick_starts")
    fun getAll(): Flow<List<QuickStartEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QuickStartEntity>)
}
