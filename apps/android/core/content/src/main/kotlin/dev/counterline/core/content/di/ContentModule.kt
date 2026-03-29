package dev.counterline.core.content.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.counterline.core.content.ContentAssetLoader
import dev.counterline.core.content.ContentSeeder
import dev.counterline.core.database.CounterLineDatabase
import dev.counterline.core.domain.ReviewScheduler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ContentModule {

    @Provides
    @Singleton
    fun provideContentSeeder(
        loader: ContentAssetLoader,
        database: CounterLineDatabase,
    ): ContentSeeder = ContentSeeder(loader, database)

    @Provides
    @Singleton
    fun provideReviewScheduler(): ReviewScheduler = ReviewScheduler()
}
