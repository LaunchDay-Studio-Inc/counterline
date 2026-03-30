package dev.counterline.core.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.counterline.core.domain.ReviewScheduler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideReviewScheduler(): ReviewScheduler = ReviewScheduler()
}
