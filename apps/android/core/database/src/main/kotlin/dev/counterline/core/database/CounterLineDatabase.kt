package dev.counterline.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.counterline.core.database.dao.AchievementDao
import dev.counterline.core.database.dao.BadgeDao
import dev.counterline.core.database.dao.BookmarkDao
import dev.counterline.core.database.dao.DeviationDao
import dev.counterline.core.database.dao.DrillDao
import dev.counterline.core.database.dao.ExamResultDao
import dev.counterline.core.database.dao.ImportedGameDao
import dev.counterline.core.database.dao.MistakeItemDao
import dev.counterline.core.database.dao.ModelGameDao
import dev.counterline.core.database.dao.NodeReviewStateDao
import dev.counterline.core.database.dao.PiecePlacementDao
import dev.counterline.core.database.dao.PlanDao
import dev.counterline.core.database.dao.QuickStartDao
import dev.counterline.core.database.dao.RepertoireDao
import dev.counterline.core.database.dao.RepertoireSnapshotDao
import dev.counterline.core.database.dao.StudySessionDao
import dev.counterline.core.database.dao.TacticalMotifDao
import dev.counterline.core.database.dao.ThemeDao
import dev.counterline.core.database.dao.TransitionPlanDao
import dev.counterline.core.database.dao.UserNoteDao
import dev.counterline.core.database.dao.UserProgressDao
import dev.counterline.core.database.entity.AchievementEntity
import dev.counterline.core.database.entity.BadgeEntity
import dev.counterline.core.database.entity.BookmarkEntity
import dev.counterline.core.database.entity.DeviationEntity
import dev.counterline.core.database.entity.DrillEntity
import dev.counterline.core.database.entity.ExamResultEntity
import dev.counterline.core.database.entity.ImportedGameEntity
import dev.counterline.core.database.entity.MistakeItemEntity
import dev.counterline.core.database.entity.ModelGameEntity
import dev.counterline.core.database.entity.NodeReviewStateEntity
import dev.counterline.core.database.entity.PiecePlacementEntity
import dev.counterline.core.database.entity.PlanEntity
import dev.counterline.core.database.entity.QuickStartEntity
import dev.counterline.core.database.entity.RepertoireLineEntity
import dev.counterline.core.database.entity.RepertoireMoveEntity
import dev.counterline.core.database.entity.RepertoireSnapshotEntity
import dev.counterline.core.database.entity.StudySessionEntity
import dev.counterline.core.database.entity.TacticalMotifEntity
import dev.counterline.core.database.entity.ThemeEntity
import dev.counterline.core.database.entity.TransitionPlanEntity
import dev.counterline.core.database.entity.UserNoteEntity
import dev.counterline.core.database.entity.UserProgressEntity

@Database(
    entities = [
        RepertoireLineEntity::class,
        RepertoireMoveEntity::class,
        PlanEntity::class,
        ThemeEntity::class,
        PiecePlacementEntity::class,
        DeviationEntity::class,
        ModelGameEntity::class,
        DrillEntity::class,
        UserProgressEntity::class,
        QuickStartEntity::class,
        NodeReviewStateEntity::class,
        MistakeItemEntity::class,
        ExamResultEntity::class,
        StudySessionEntity::class,
        BadgeEntity::class,
        TacticalMotifEntity::class,
        TransitionPlanEntity::class,
        UserNoteEntity::class,
        BookmarkEntity::class,
        ImportedGameEntity::class,
        RepertoireSnapshotEntity::class,
        AchievementEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class CounterLineDatabase : RoomDatabase() {
    abstract fun repertoireDao(): RepertoireDao
    abstract fun planDao(): PlanDao
    abstract fun themeDao(): ThemeDao
    abstract fun piecePlacementDao(): PiecePlacementDao
    abstract fun deviationDao(): DeviationDao
    abstract fun modelGameDao(): ModelGameDao
    abstract fun drillDao(): DrillDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun quickStartDao(): QuickStartDao
    abstract fun nodeReviewStateDao(): NodeReviewStateDao
    abstract fun mistakeItemDao(): MistakeItemDao
    abstract fun examResultDao(): ExamResultDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun badgeDao(): BadgeDao
    abstract fun tacticalMotifDao(): TacticalMotifDao
    abstract fun transitionPlanDao(): TransitionPlanDao
    abstract fun userNoteDao(): UserNoteDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun importedGameDao(): ImportedGameDao
    abstract fun repertoireSnapshotDao(): RepertoireSnapshotDao
    abstract fun achievementDao(): AchievementDao
}
