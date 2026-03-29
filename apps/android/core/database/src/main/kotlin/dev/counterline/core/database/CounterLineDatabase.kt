package dev.counterline.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.counterline.core.database.dao.DeviationDao
import dev.counterline.core.database.dao.DrillDao
import dev.counterline.core.database.dao.ModelGameDao
import dev.counterline.core.database.dao.PiecePlacementDao
import dev.counterline.core.database.dao.PlanDao
import dev.counterline.core.database.dao.QuickStartDao
import dev.counterline.core.database.dao.RepertoireDao
import dev.counterline.core.database.dao.ThemeDao
import dev.counterline.core.database.dao.UserProgressDao
import dev.counterline.core.database.entity.DeviationEntity
import dev.counterline.core.database.entity.DrillEntity
import dev.counterline.core.database.entity.ModelGameEntity
import dev.counterline.core.database.entity.PiecePlacementEntity
import dev.counterline.core.database.entity.PlanEntity
import dev.counterline.core.database.entity.QuickStartEntity
import dev.counterline.core.database.entity.RepertoireLineEntity
import dev.counterline.core.database.entity.RepertoireMoveEntity
import dev.counterline.core.database.entity.ThemeEntity
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
    ],
    version = 1,
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
}
