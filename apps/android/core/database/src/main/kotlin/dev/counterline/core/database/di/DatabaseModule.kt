package dev.counterline.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.counterline.core.database.CounterLineDatabase
import dev.counterline.core.database.dao.DeviationDao
import dev.counterline.core.database.dao.DrillDao
import dev.counterline.core.database.dao.ModelGameDao
import dev.counterline.core.database.dao.PiecePlacementDao
import dev.counterline.core.database.dao.PlanDao
import dev.counterline.core.database.dao.QuickStartDao
import dev.counterline.core.database.dao.RepertoireDao
import dev.counterline.core.database.dao.ThemeDao
import dev.counterline.core.database.dao.UserProgressDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CounterLineDatabase =
        Room.databaseBuilder(
            context,
            CounterLineDatabase::class.java,
            "counterline.db",
        ).build()

    @Provides fun provideRepertoireDao(db: CounterLineDatabase): RepertoireDao = db.repertoireDao()
    @Provides fun providePlanDao(db: CounterLineDatabase): PlanDao = db.planDao()
    @Provides fun provideThemeDao(db: CounterLineDatabase): ThemeDao = db.themeDao()
    @Provides fun providePiecePlacementDao(db: CounterLineDatabase): PiecePlacementDao = db.piecePlacementDao()
    @Provides fun provideDeviationDao(db: CounterLineDatabase): DeviationDao = db.deviationDao()
    @Provides fun provideModelGameDao(db: CounterLineDatabase): ModelGameDao = db.modelGameDao()
    @Provides fun provideDrillDao(db: CounterLineDatabase): DrillDao = db.drillDao()
    @Provides fun provideUserProgressDao(db: CounterLineDatabase): UserProgressDao = db.userProgressDao()
    @Provides fun provideQuickStartDao(db: CounterLineDatabase): QuickStartDao = db.quickStartDao()
}
