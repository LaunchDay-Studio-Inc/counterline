package dev.counterline.core.data.repository

import dev.counterline.core.database.dao.AchievementDao
import dev.counterline.core.database.dao.BookmarkDao
import dev.counterline.core.database.dao.ImportedGameDao
import dev.counterline.core.database.dao.RepertoireSnapshotDao
import dev.counterline.core.database.dao.TacticalMotifDao
import dev.counterline.core.database.dao.TransitionPlanDao
import dev.counterline.core.database.dao.UserNoteDao
import dev.counterline.core.database.entity.AchievementEntity
import dev.counterline.core.database.entity.BookmarkEntity
import dev.counterline.core.database.entity.ImportedGameEntity
import dev.counterline.core.database.entity.RepertoireSnapshotEntity
import dev.counterline.core.database.entity.TacticalMotifEntity
import dev.counterline.core.database.entity.TransitionPlanEntity
import dev.counterline.core.database.entity.UserNoteEntity
import dev.counterline.core.model.Achievement
import dev.counterline.core.model.AchievementCategory
import dev.counterline.core.model.Bookmark
import dev.counterline.core.model.ImportedGame
import dev.counterline.core.model.PiecePlacement
import dev.counterline.core.model.RepertoireSnapshot
import dev.counterline.core.model.Side
import dev.counterline.core.model.SkillLevel
import dev.counterline.core.model.TacticalMotif
import dev.counterline.core.model.TransitionPlan
import dev.counterline.core.model.UserNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// ──────────────────────────────────────────────────────────────────────
// Phase 2: Tactical Motif Repository
// ──────────────────────────────────────────────────────────────────────

@Singleton
class TacticalMotifRepository @Inject constructor(
    private val dao: TacticalMotifDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getBySide(side: Side): Flow<List<TacticalMotif>> =
        dao.getBySide(side.name).map { entities -> entities.map { it.toModel() } }

    fun getByLineId(lineId: String): Flow<List<TacticalMotif>> =
        dao.getByLineId(lineId).map { entities -> entities.map { it.toModel() } }

    suspend fun getRandomMotifs(count: Int): List<TacticalMotif> =
        dao.getRandomMotifs(count).map { it.toModel() }

    suspend fun getWoodpeckerCycle(count: Int): List<TacticalMotif> =
        dao.getWoodpeckerCycle(count).map { it.toModel() }

    suspend fun update(motif: TacticalMotif) = dao.update(motif.toEntity())

    private fun TacticalMotifEntity.toModel() = TacticalMotif(
        id = id, lineId = lineId, side = Side.valueOf(side),
        fen = fen, solutionSan = json.decodeFromString(solutionSanJson),
        motifType = motifType, difficulty = SkillLevel.valueOf(difficulty),
        explanation = explanation, repetitionCycle = repetitionCycle,
        lastAttemptCorrect = lastAttemptCorrect,
    )

    private fun TacticalMotif.toEntity() = TacticalMotifEntity(
        id = id, lineId = lineId, side = side.name,
        fen = fen, solutionSanJson = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer<String>()), solutionSan),
        motifType = motifType, difficulty = difficulty.name,
        explanation = explanation, repetitionCycle = repetitionCycle,
        lastAttemptCorrect = lastAttemptCorrect,
    )
}

// ──────────────────────────────────────────────────────────────────────
// Phase 2: Transition Plan Repository
// ──────────────────────────────────────────────────────────────────────

@Singleton
class TransitionPlanRepository @Inject constructor(
    private val dao: TransitionPlanDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getBySide(side: Side): Flow<List<TransitionPlan>> =
        dao.getBySide(side.name).map { entities -> entities.map { it.toModel() } }

    fun getByLineId(lineId: String): Flow<List<TransitionPlan>> =
        dao.getByLineId(lineId).map { entities -> entities.map { it.toModel() } }

    private fun TransitionPlanEntity.toModel() = TransitionPlan(
        id = id, lineId = lineId, side = Side.valueOf(side),
        tabiyaFen = tabiyaFen,
        typicalPiecePlacements = emptyList(), // loaded separately from piece_placements table
        pawnBreaks = json.decodeFromString(pawnBreaksJson),
        strategicGoals = json.decodeFromString(strategicGoalsJson),
        endgameTendency = endgameTendency,
        skillLevel = SkillLevel.valueOf(skillLevel),
    )
}

// ──────────────────────────────────────────────────────────────────────
// Phase 4: User Note Repository
// ──────────────────────────────────────────────────────────────────────

@Singleton
class UserNoteRepository @Inject constructor(
    private val dao: UserNoteDao,
) {
    fun getAll(): Flow<List<UserNote>> =
        dao.getAll().map { entities -> entities.map { it.toModel() } }

    fun getByLineId(lineId: String): Flow<List<UserNote>> =
        dao.getByLineId(lineId).map { entities -> entities.map { it.toModel() } }

    suspend fun getByNodeId(nodeId: String): UserNote? =
        dao.getByNodeId(nodeId)?.toModel()

    suspend fun upsert(note: UserNote): Long = dao.upsert(note.toEntity())

    suspend fun delete(id: Long) = dao.delete(id)

    private fun UserNoteEntity.toModel() = UserNote(
        id = id, nodeId = nodeId, lineId = lineId,
        side = Side.valueOf(side), content = content,
        createdEpochMs = createdEpochMs, updatedEpochMs = updatedEpochMs,
    )

    private fun UserNote.toEntity() = UserNoteEntity(
        id = id, nodeId = nodeId, lineId = lineId,
        side = side.name, content = content,
        createdEpochMs = createdEpochMs, updatedEpochMs = updatedEpochMs,
    )
}

// ──────────────────────────────────────────────────────────────────────
// Phase 4: Bookmark Repository
// ──────────────────────────────────────────────────────────────────────

@Singleton
class BookmarkRepository @Inject constructor(
    private val dao: BookmarkDao,
) {
    fun getAll(): Flow<List<Bookmark>> =
        dao.getAll().map { entities -> entities.map { it.toModel() } }

    fun getFavorites(): Flow<List<Bookmark>> =
        dao.getFavorites().map { entities -> entities.map { it.toModel() } }

    fun getTabiyas(): Flow<List<Bookmark>> =
        dao.getTabiyas().map { entities -> entities.map { it.toModel() } }

    fun getByLineId(lineId: String): Flow<List<Bookmark>> =
        dao.getByLineId(lineId).map { entities -> entities.map { it.toModel() } }

    suspend fun upsert(bookmark: Bookmark): Long = dao.upsert(bookmark.toEntity())

    suspend fun delete(id: Long) = dao.delete(id)

    private fun BookmarkEntity.toModel() = Bookmark(
        id = id, lineId = lineId, nodeId = nodeId,
        side = Side.valueOf(side), label = label, fen = fen,
        createdEpochMs = createdEpochMs, isFavorite = isFavorite, isTabiya = isTabiya,
    )

    private fun Bookmark.toEntity() = BookmarkEntity(
        id = id, lineId = lineId, nodeId = nodeId,
        side = side.name, label = label, fen = fen,
        createdEpochMs = createdEpochMs, isFavorite = isFavorite, isTabiya = isTabiya,
    )
}

// ──────────────────────────────────────────────────────────────────────
// Phase 4: Imported Game Repository
// ──────────────────────────────────────────────────────────────────────

@Singleton
class ImportedGameRepository @Inject constructor(
    private val dao: ImportedGameDao,
) {
    fun getAll(): Flow<List<ImportedGame>> =
        dao.getAll().map { entities -> entities.map { it.toModel() } }

    fun getWithDeviations(): Flow<List<ImportedGame>> =
        dao.getWithDeviations().map { entities -> entities.map { it.toModel() } }

    suspend fun insert(game: ImportedGame): Long = dao.insert(game.toEntity())

    suspend fun delete(id: Long) = dao.delete(id)

    private fun ImportedGameEntity.toModel() = ImportedGame(
        id = id, pgn = pgn, white = white, black = black,
        result = result, date = date, opening = opening,
        importedEpochMs = importedEpochMs,
        deviationMoveNumber = deviationMoveNumber,
        deviationSide = deviationSide?.let { Side.valueOf(it) },
        matchedLineId = matchedLineId,
    )

    private fun ImportedGame.toEntity() = ImportedGameEntity(
        id = id, pgn = pgn, white = white, black = black,
        result = result, date = date, opening = opening,
        importedEpochMs = importedEpochMs,
        deviationMoveNumber = deviationMoveNumber,
        deviationSide = deviationSide?.name,
        matchedLineId = matchedLineId,
    )
}

// ──────────────────────────────────────────────────────────────────────
// Phase 4: Repertoire Snapshot Repository
// ──────────────────────────────────────────────────────────────────────

@Singleton
class RepertoireSnapshotRepository @Inject constructor(
    private val dao: RepertoireSnapshotDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getAll(): Flow<List<RepertoireSnapshot>> =
        dao.getAll().map { entities -> entities.map { it.toModel() } }

    suspend fun getLatest(): RepertoireSnapshot? = dao.getLatest()?.toModel()

    suspend fun insert(snapshot: RepertoireSnapshot): Long = dao.insert(snapshot.toEntity())

    private fun RepertoireSnapshotEntity.toModel() = RepertoireSnapshot(
        id = id, version = version, createdEpochMs = createdEpochMs,
        changesSummary = changesSummary,
        linesAdded = json.decodeFromString(linesAddedJson),
        linesRemoved = json.decodeFromString(linesRemovedJson),
        linesModified = json.decodeFromString(linesModifiedJson),
    )

    private fun RepertoireSnapshot.toEntity() = RepertoireSnapshotEntity(
        id = id, version = version, createdEpochMs = createdEpochMs,
        changesSummary = changesSummary,
        linesAddedJson = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer<String>()), linesAdded),
        linesRemovedJson = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer<String>()), linesRemoved),
        linesModifiedJson = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer<String>()), linesModified),
    )
}

// ──────────────────────────────────────────────────────────────────────
// Phase 5: Achievement Repository
// ──────────────────────────────────────────────────────────────────────

@Singleton
class AchievementRepository @Inject constructor(
    private val dao: AchievementDao,
) {
    fun getAll(): Flow<List<Achievement>> =
        dao.getAll().map { entities -> entities.map { it.toModel() } }

    fun getEarned(): Flow<List<Achievement>> =
        dao.getEarned().map { entities -> entities.map { it.toModel() } }

    fun getByCategory(category: AchievementCategory): Flow<List<Achievement>> =
        dao.getByCategory(category.name).map { entities -> entities.map { it.toModel() } }

    suspend fun upsert(achievement: Achievement) = dao.upsert(achievement.toEntity())

    suspend fun seedDefaults() {
        val defaults = listOf(
            AchievementEntity("first_session", "First Steps", "Complete your first study session", "CONSISTENCY"),
            AchievementEntity("week_streak_7", "Week Warrior", "Maintain a 7-day study streak", "CONSISTENCY", target = 7f),
            AchievementEntity("week_streak_30", "Monthly Dedication", "Maintain a 30-day study streak", "CONSISTENCY", target = 30f),
            AchievementEntity("white_mastery_50", "White Apprentice", "Reach 50% mastery on White weapon", "MASTERY", target = 0.5f),
            AchievementEntity("black_mastery_50", "Black Apprentice", "Reach 50% mastery on Black weapon", "MASTERY", target = 0.5f),
            AchievementEntity("white_mastery_90", "White Expert", "Reach 90% mastery on White weapon", "MASTERY", target = 0.9f),
            AchievementEntity("black_mastery_90", "Black Expert", "Reach 90% mastery on Black weapon", "MASTERY", target = 0.9f),
            AchievementEntity("mistakes_resolved_10", "Error Corrector", "Resolve 10 mistakes through re-drilling", "RESILIENCE", target = 10f),
            AchievementEntity("mistakes_resolved_50", "Persistent Learner", "Resolve 50 mistakes through re-drilling", "RESILIENCE", target = 50f),
            AchievementEntity("exam_pass_white", "White Certification", "Pass the White repertoire exam", "DEPTH"),
            AchievementEntity("exam_pass_black", "Black Certification", "Pass the Black repertoire exam", "DEPTH"),
            AchievementEntity("all_deviations", "Deviation Master", "Successfully drill all known deviations", "BREADTH"),
            AchievementEntity("model_games_5", "Pattern Collector", "Study 5 model games with guess-the-move", "DEPTH", target = 5f),
            AchievementEntity("daily_100", "Century Club", "Complete 100 daily sessions", "CONSISTENCY", target = 100f),
        )
        dao.upsertAll(defaults)
    }

    private fun AchievementEntity.toModel() = Achievement(
        id = id, title = title, description = description,
        category = AchievementCategory.valueOf(category),
        earnedEpochMs = earnedEpochMs, progress = progress, target = target,
    )

    private fun Achievement.toEntity() = AchievementEntity(
        id = id, title = title, description = description,
        category = category.name,
        earnedEpochMs = earnedEpochMs, progress = progress, target = target,
    )
}
